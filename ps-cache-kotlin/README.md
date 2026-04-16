# PS-Cache Kotlin — JDBC Prepared Statement Cache Mock Mismatch Reproduction

This sample demonstrates a bug in Keploy's Postgres mock matcher where **JDBC prepared statement caching combined with connection pool eviction causes the replay to return the wrong person's data**.

## The Bug

The JDBC driver (PostgreSQL JDBC + HikariCP) caches prepared statements per connection. When the connection pool evicts and creates a new connection, the PS cache is cold — but the recorded mocks from the evicted connection had warm-cache structure (Bind-only, no Parse). During replay, the matcher can't distinguish between mocks from different connection windows because:

1. All mocks have the same parameterized SQL: `SELECT ... WHERE member_id = ?`
2. `bindParamMatchLen` mode only checks parameter byte-length (all int4 are 4 bytes)
3. Sort-order prediction starts from 0 on a fresh connection, pointing to the wrong window's mocks

### Real-world impact
This was reported by a customer running a Kotlin/Spring Boot app with Agoda's travel account service. The post-eviction test returned Alice's data (member_id=19) instead of Charlie's — **silently returning the wrong customer's financial data**.

## Architecture

```
                    ┌──────────────────────┐
  HTTP requests ──> │ Kotlin + Spring Boot │
                    │   HikariCP pool=1    │
                    │ prepareThreshold=1   │
                    └──────────┬───────────┘
                               │
            ┌──────────────────┼──────────────────┐
            │                  │                  │
       /account            /evict            /account
      member=19         (pool evict)        member=31
            │                  │                  │
       Connection A       destroyed          Connection B
       PS cache: cold→warm                  PS cache: cold
            │                                    │
       1st: Parse+Bind+Desc+Exec          Parse+Bind+Desc+Exec
       2nd: Bind+Exec (cached PS)
            │                                    │
       mocks connID=0                      mocks connID=2
       (Alice, 1000)                      (Charlie, 500)
```

## How to Reproduce the Bug

### Option A: Using Docker Compose (recommended)

```bash
docker compose up --build
```

This starts PostgreSQL (with schema + seed data via `init.sql`) and the app on port 8080.

### Option B: Standalone

**Prerequisites:** Java 21, Maven, PostgreSQL running on localhost:5432.

```bash
# Start Postgres (if not already running)
docker run -d --name pg-demo \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=testdb \
  -p 5432:5432 postgres:16

# Wait for Postgres to be ready
sleep 3

# Create the schema and seed data
docker exec pg-demo psql -U postgres -d testdb -c "
  CREATE SCHEMA IF NOT EXISTS travelcard;
  CREATE TABLE IF NOT EXISTS travelcard.travel_account (
    id SERIAL PRIMARY KEY, member_id INT NOT NULL UNIQUE,
    name TEXT NOT NULL, balance INT NOT NULL DEFAULT 0);
  INSERT INTO travelcard.travel_account (member_id, name, balance) VALUES
    (19, 'Alice', 1000), (23, 'Bob', 2500),
    (31, 'Charlie', 500), (42, 'Diana', 7500)
  ON CONFLICT (member_id) DO NOTHING;"

# Build
mvn package -DskipTests -q
```

### Record and replay

```bash
# Record
sudo keploy record -c "java -jar target/kotlin-app-1.0.0.jar"

# In another terminal, hit endpoints in order:
curl "http://localhost:8080/account?member=19"   # Alice  (warms PS cache on Connection A)
curl "http://localhost:8080/account?member=23"   # Bob    (cached PS, Bind-only)
curl "http://localhost:8080/evict"                # Force HikariCP to evict connections
curl "http://localhost:8080/account?member=31"   # Charlie (new Connection B, cold PS cache)
curl "http://localhost:8080/account?member=42"   # Diana   (cached PS on Connection B)

# Or run the traffic script:
# bash test.sh

# Stop recording (Ctrl+C), then replay:
sudo keploy test -c "java -jar target/kotlin-app-1.0.0.jar" --skip-coverage
```

**Expected failure (without fix):** The post-eviction `/account?member=31` test fails:
```
EXPECTED: {"id":3, "memberId":31, "name":"Charlie", "balance":500}
ACTUAL:   {"id":1, "memberId":19, "name":"Alice",   "balance":1000}  <- WRONG PERSON
```

> **Note:** The exact test number that fails depends on how many health-check
> requests keploy captures during recording (typically test-5 through test-7).

**With obfuscation enabled (worse):**
```
Post-eviction member=31: EXPECTED Charlie -> ACTUAL Alice
Post-eviction member=42: EXPECTED Diana  -> ACTUAL Bob     <- TWO wrong results
```

### With the FIXED keploy binary
```bash
# Same steps -> all tests pass, correct data for each member
```

## What the Fix Does

The fix adds **recording-connection affinity** to the Postgres mock matcher (see [keploy/integrations#121](https://github.com/keploy/integrations/pull/121)):

1. When the first `Bind` mock is consumed on a replay connection, its recording `connID` is stored
2. Subsequent scoring applies a small tiebreaker bonus to prefer mocks from the same recording connection
3. Only activates when 2+ distinct recording connections exist (zero impact on single-connection apps)

## Configuration

### application.properties
| Property | Value | Purpose |
|----------|-------|---------|
| `server.port` | `8080` | HTTP server port |
| `spring.datasource.hikari.maximum-pool-size` | `1` | Forces all requests through one connection |
| `prepareThreshold=1` | JDBC URL param | Caches PS after first use |
| `spring.sql.init.mode` | `never` | Schema created externally (via init.sql) |

### Environment variables (defaults in parentheses)
| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `testdb` | Database name |
| `DB_USER` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |

### Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /health` | Health check |
| `GET /account?member=N` | Query travel_account by member_id (BEGIN -> SELECT -> COMMIT) |
| `GET /evict` | Soft-evict HikariCP connections (forces new PG connection) |
