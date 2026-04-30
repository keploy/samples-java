# Java Dynamic Deduplication Sample

A Spring Boot application used by Keploy CI to validate Java dynamic deduplication. It mirrors the Go dedup sample by exposing a broad set of endpoints and committing 400 replay fixtures across four testsets.

CI does not record this sample. The `keploy/` directory is checked in so the pipeline only builds the app and runs replay with `--dedup --skip-app-restart`. When the sample behavior changes, record the fixtures locally and push the updated `keploy/` files.

The Keploy Java SDK is attached as a Java agent at replay time. The sample does not compile against `io.keploy:keploy-sdk` and does not import Keploy classes in application code.

The SDK reads JaCoCo coverage in-process via `org.jacoco.agent.rt.RT.getAgent().getExecutionData(...)`, so attach both agents when running dynamic deduplication: the Keploy agent for the control/data socket protocol, and the JaCoCo agent for runtime coverage.

## Setup

```bash
mvn -B -DskipTests -Dkeploy.agent.version=2.0.6 clean package
```

This builds the runnable application jar, copies `target/keploy-sdk.jar`, and copies `target/jacocoagent.jar` next to it.

## Run dedup natively

```bash
keploy test \
  -c "java -javaagent:target/keploy-sdk.jar -javaagent:target/jacocoagent.jar -jar target/java-dedup.jar" \
  --dedup --skip-app-restart --language java --delay 1 \
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
  --dedup --skip-app-restart --language java --delay 1 \
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
  --dedup --skip-app-restart --language java --delay 1 \
  --health-url "http://127.0.0.1:8080/healthz" \
  --health-poll-timeout 30s \
  --disableMockUpload --disableReportUpload

keploy dedup --path .
```

During direct `docker run`, Enterprise injects the same shared `/tmp` volume into the app container. Do not pass your own `/tmp` mount in the app command.

The CI pipeline also validates additional production-style layouts for the same app, including native classpath, direct Docker run, Docker Compose, exploded classpath images, restricted runtime, restricted classpath, and distroless packaging.
