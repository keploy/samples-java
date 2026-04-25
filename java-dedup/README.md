# Java Dynamic Deduplication Sample

A Spring Boot application used by Keploy CI to validate Java dynamic deduplication. It mirrors the Go dedup sample by exposing a broad set of endpoints and committing 1000 replay fixtures across four testsets.

CI does not record this sample. The `keploy/` directory is checked in so the pipeline only builds the app and runs replay with `--dedup`.

The Java SDK reads JaCoCo coverage in-process via `org.jacoco.agent.rt.RT.getAgent().getExecutionData(...)`, so attaching the JaCoCo Java agent is enough — no TCP server, no port choice, no `--pass-through-ports`.

## Build

```bash
mvn -B -DskipTests package
```

This produces `target/java-dedup-0.0.1-SNAPSHOT.jar` and copies `target/jacocoagent.jar` next to it.

## Run dedup natively

```bash
keploy test \
  -c "java -javaagent:target/jacocoagent.jar -jar target/java-dedup-0.0.1-SNAPSHOT.jar" \
  --dedup --language java --delay 20
```

## Run dedup with Docker

Build the image (the Dockerfile already attaches the JaCoCo agent):

```bash
docker compose build
```

Replay with dedup:

```bash
keploy test \
  -c "docker compose up" \
  --container-name "dedup-java" \
  --dedup --language java --delay 20
```

The Compose file bind-mounts host `/tmp` into the container so Keploy and the Java SDK share `/tmp/coverage_control.sock` and `/tmp/coverage_data.sock`.

### Restricted Docker

For a read-only root filesystem with dropped capabilities and `no-new-privileges`, overlay the restricted compose file:

```bash
docker compose -f docker-compose.yml -f docker-compose.restricted.yml build

keploy test \
  -c "docker compose -f docker-compose.yml -f docker-compose.restricted.yml up" \
  --container-name "dedup-java" \
  --dedup --language java --delay 20
```

Host `/tmp` is still bind-mounted so the SDK and Keploy share the dedup Unix sockets.
