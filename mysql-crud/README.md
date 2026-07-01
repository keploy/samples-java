# MySQL CRUD Sample

A minimal Spring Boot + JDBC application used by Keploy Enterprise CI to validate the
self-hosted cloud-replay pipeline: JDBC-manifest secret obfuscation and object-storage
mock upload/download, exercised end-to-end via a real MySQL 8 backend.

## Endpoints

| Method | Path                 | Description                                   |
|--------|----------------------|------------------------------------------------|
| GET    | `/health`            | Runs `SELECT 1` against the configured DB      |
| GET    | `/users`             | Lists users + aggregate order stats            |
| GET    | `/users/{id}`        | Single user, their orders, and order totals    |
| POST   | `/users`             | Creates a user                                 |
| POST   | `/users/{id}/orders` | Creates an order for a user                    |
| GET    | `/stats`             | Aggregate user/order counts and amounts        |

## Configuration

The datasource is fully env-driven so the same jar runs against any MySQL instance:

```
DB_URL   (default: jdbc:mysql://localhost:3306/appdb)
DB_USER  (default: root)
DB_PASS  (default: empty)
```

`schema.sql` / `data.sql` run on every startup (idempotent — `IF NOT EXISTS` / `INSERT IGNORE`).

## MySQL auth-plugin note

MySQL 8's default `caching_sha2_password` auth plugin cannot be captured by Keploy's
MySQL recorder mid-handshake. When running this sample against a container you control,
start MySQL with:

```
--default-authentication-plugin=mysql_native_password
```

## Build & run

```bash
mvn -q -B clean package -DskipTests
DB_URL="jdbc:mysql://localhost:3306/appdb" DB_USER=root java -jar target/app.jar
```

## Docker

```bash
docker build -t mysql-crud .
docker run -p 8080:8080 -e DB_URL="jdbc:mysql://<mysql-host>:3306/appdb" mysql-crud
```
