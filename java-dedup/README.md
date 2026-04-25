# Java Dynamic Deduplication Sample

A Spring Boot application used by Keploy CI to validate Java dynamic deduplication in native and Docker runs. It mirrors the Go dedup sample by exposing a broad set of endpoints and committing 1000 replay fixtures across four testsets.

CI does not record this sample. The `keploy/` directory is checked in so the pipeline only builds the app and runs replay with `--dedup`.

## How dedup works for Java

Keploy's dynamic dedup engine asks the application for per-test code coverage between every replayed test, then skips future tests whose coverage signature it has already seen. The Java SDK (`io.keploy.dedup.KeployDedupAgent`, bundled in the app via the `keploy-sdk` dependency) handles that coverage exchange.

The SDK reads coverage **in-process** through JaCoCo's runtime API (`org.jacoco.agent.rt.RT.getAgent().getExecutionData(...)`). All you have to do is attach the JaCoCo Java agent — no TCP server, no port choice, no `--pass-through-ports`:

```
-javaagent:target/jacocoagent.jar
```

The SDK still falls back to JaCoCo's TCP server mode if the in-process API is unavailable for some reason, which is why the `KEPLOY_JACOCO_HOST` and `KEPLOY_JACOCO_PORT` environment variables are still honoured.

## Build

```bash
mvn -B -DskipTests package
```

This produces `target/java-dedup-0.0.1-SNAPSHOT.jar` and copies `target/jacocoagent.jar` next to it.

## Native run

Start the app with the JaCoCo agent attached:

```bash
java -javaagent:target/jacocoagent.jar \
  -jar target/java-dedup-0.0.1-SNAPSHOT.jar
```

Replay with dynamic dedup:

```bash
keploy test \
  -c "java -javaagent:target/jacocoagent.jar -jar target/java-dedup-0.0.1-SNAPSHOT.jar" \
  --dedup --language java --delay 20
```

To regenerate the committed fixtures locally, record high-volume traffic against the running app:

```bash
./run_random_1000.sh
```

## Docker run

Build the JAR first, then build the image. The Dockerfile already wires the JaCoCo agent into the entrypoint:

```bash
mvn -B -DskipTests package
docker compose build
```

Run with Keploy in dedup mode:

```bash
keploy test \
  -c "docker compose up" \
  --container-name "dedup-java" \
  --dedup --language java --delay 20
```

The Compose file supports `JAVA_DEDUP_IMAGE`, `JAVA_DEDUP_CONTAINER_NAME`, and `JAVA_DEDUP_HOST_PORT` so CI can isolate repeated replay runs without recording new tests.

The Compose app bind-mounts host `/tmp` into the container so Keploy and the Java SDK use the same Unix socket paths for `/tmp/coverage_control.sock` and `/tmp/coverage_data.sock`.

### Restricted Docker

For a more restricted container — read-only root filesystem, dropped capabilities, and `no-new-privileges` — overlay the restricted compose file:

```bash
docker compose -f docker-compose.yml -f docker-compose.restricted.yml build
keploy test \
  -c "docker compose -f docker-compose.yml -f docker-compose.restricted.yml up" \
  --container-name "dedup-java" \
  --dedup --language java --delay 20
```

Host `/tmp` is still bind-mounted so the SDK and Keploy share the dedup Unix sockets.
