# Java Dynamic Deduplication Sample

This sample is a Spring Boot application used by Keploy CI to validate Java dynamic deduplication in native and Docker runs. It mirrors the Go dedup sample by exposing a broad set of endpoints and committing 1000 replay fixtures across four testsets.

CI does not record this sample. The `keploy/` directory is checked in so the pipeline only builds the app and runs replay with `--dedup`.

Build the application after installing the Java SDK locally:

```bash
mvn -B -DskipTests package
```

Run it natively with JaCoCo TCP server mode:

```bash
java -javaagent:target/jacocoagent.jar=address=127.0.0.1,port=36320,destfile=target/jacoco-keploy.exec,output=tcpserver \
  -jar target/java-dedup-0.0.1-SNAPSHOT.jar
```

To regenerate the committed fixtures locally, record high-volume traffic against the running app:

```bash
./run_random_1000.sh
```

When replaying with dynamic deduplication, pass the JaCoCo port through Keploy so the SDK can talk to the local JaCoCo TCP server:

```bash
keploy test \
  -c "java -javaagent:target/jacocoagent.jar=address=127.0.0.1,port=36320,destfile=target/jacoco-keploy.exec,output=tcpserver -jar target/java-dedup-0.0.1-SNAPSHOT.jar" \
  --dedup --pass-through-ports 36320
```

Run it with Docker Compose after the Maven package step has created `target/java-dedup-0.0.1-SNAPSHOT.jar`:

```bash
docker compose up --build
```

The Compose app bind-mounts host `/tmp` into the container so Keploy and the Java SDK use the same Unix socket paths for `/tmp/coverage_control.sock` and `/tmp/coverage_data.sock`. The image runs as a non-root user. For a more restricted container run with a read-only root filesystem, dropped capabilities, `no-new-privileges`, and writable host `/tmp` bind-mounted for Keploy's Unix sockets:

```bash
docker compose -f docker-compose.yml -f docker-compose.restricted.yml up --build
```
