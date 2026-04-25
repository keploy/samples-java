# hibernate-cache-tour-postgres

A Spring Boot 3 + Hibernate 6 + Postgres 16 reproducer that exercises two
specific drift signatures in keploy's Postgres v3 logical codec:

1. **Issue #1 — pgjdbc `prepareThreshold` flip.**
   pgjdbc defaults to `prepareThreshold=5`: the same SQL handle is sent in
   text format for the first 4 calls and switches to binary on the 5th
   execution. Each `WHERE id = ?` JpaRepository finder hit `>= 8` times will
   straddle that boundary — the recorded mocks for one SQL hash carry mixed
   format codes; replay only matches when the v3 codec reconciles them.

2. **Issue #3 — Hibernate StatementCache classification drift.**
   With `hibernate.cache.use_query_cache` and `use_second_level_cache`
   enabled (EHCache 3.x via JCache), per-test cache eviction triggered by
   the `keploy.io/test-name` header changes the cache hit/miss pattern
   between record and replay, which classifies the same SQL differently
   in keploy's StatementCache.

## Endpoints

| Method | Path                       | Notes                              |
|--------|----------------------------|------------------------------------|
| POST   | /customer                  | Create + return id                 |
| GET    | /customer/{id}             | `WHERE id = ?` (single int4 bind)  |
| GET    | /customer/{id}/tags        | `WHERE customer_id = ?`            |
| GET    | /tags?priority={p}         | `WHERE priority = ?`               |
| POST   | /tag                       | Create tag + return id             |

## Schema

`customer(id SERIAL, name, email)` and `customer_tag(id SERIAL,
customer_id REFERENCES customer, tag VARCHAR(64), priority INT)`.

`init.sql` seeds 4 customers (ids 1..4) and 8 tags. The CI exerciser hits
each id-keyed endpoint 8 times against `1 + (i-1) % 4`, so the same id
recurs every 4 calls — the 5th call on a given handle is the
prepareThreshold flip.

## Build / run

```
mvn -DskipTests package
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/hibcache \
SPRING_DATASOURCE_USERNAME=hibcache \
SPRING_DATASOURCE_PASSWORD=hibcache \
java -jar target/hibernate-cache-tour.jar
```

The CI harness lives in `keploy/integrations` at
`.ci/scripts/java/hibernate-cache-tour-postgres/` and drives the full
record → replay regression.
