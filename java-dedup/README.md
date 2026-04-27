# Java Dynamic Deduplication Sample

A Spring Boot application used by Keploy CI to validate Java dynamic deduplication. It mirrors the Go dedup sample by exposing a broad set of endpoints and committing 1000 replay fixtures across four testsets.

CI does not record this sample. The `keploy/` directory is checked in so the pipeline only builds the app and runs replay with `--dedup`.

The Java SDK reads JaCoCo coverage in-process via `org.jacoco.agent.rt.RT.getAgent().getExecutionData(...)`, so attaching the JaCoCo Java agent is enough — no TCP server, no port choice, no `--pass-through-ports`.

## Setup

```bash
mvn -B -DskipTests clean package
```

This builds the sample against the released Keploy Java SDK, produces `target/java-dedup-0.0.1-SNAPSHOT.jar`, and copies `target/jacocoagent.jar` next to it.

## Run dedup natively

```bash
keploy test \
  -c "java -javaagent:target/jacocoagent.jar -jar target/java-dedup-0.0.1-SNAPSHOT.jar" \
  --dedup --language java --delay 1 \
  --health-url "http://127.0.0.1:8080/healthz" \
  --health-poll-timeout 30s \
  --disableMockUpload --disableReportUpload

keploy dedup --path .
```

## Run dedup with Docker

```bash
docker compose build
keploy test \
  -c "docker compose up" \
  --container-name "dedup-java" \
  --host "127.0.0.1" \
  --dedup --language java --delay 1 \
  --health-url "http://127.0.0.1:8080/healthz" \
  --health-poll-timeout 30s \
  --disableMockUpload --disableReportUpload

keploy dedup --path .
```

During `keploy test`, Enterprise rewrites the Compose file and injects its own shared `/tmp` volume for the dedup control/data sockets. The base sample Compose file does not need a host `/tmp` bind mount.
Re-run `docker compose build` whenever the jar, JaCoCo agent, or Dockerfile changes so replay uses the current image.

## Run dedup with direct Docker

```bash
docker compose build
keploy test \
  -c "docker run --rm --name dedup-java -p 8080:8080 java-dedup:local" \
  --container-name "dedup-java" \
  --host "127.0.0.1" \
  --dedup --language java --delay 1 \
  --health-url "http://127.0.0.1:8080/healthz" \
  --health-poll-timeout 30s \
  --disableMockUpload --disableReportUpload

keploy dedup --path .
```

During direct `docker run`, Enterprise injects the same shared `/tmp` volume into the app container. Do not pass your own `/tmp` mount in the app command.

The CI pipeline also validates additional production-style Docker layouts for the same app, including direct Docker run, exploded classpath, restricted runtime, and distroless packaging.
