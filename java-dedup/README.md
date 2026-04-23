# Java Dynamic Deduplication Sample

This sample is a small Spring Boot application used by Keploy CI to validate Java dynamic deduplication in native and Docker runs.

Build the application after installing the Java SDK locally:

```bash
mvn -B -DskipTests package
```

Run it natively with JaCoCo TCP server mode:

```bash
java -javaagent:target/jacocoagent.jar=address=127.0.0.1,port=36320,destfile=target/jacoco-keploy.exec,output=tcpserver \
  -jar target/java-dedup-0.0.1-SNAPSHOT.jar
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

For a more restricted container run:

```bash
docker compose -f docker-compose.yml -f docker-compose.restricted.yml up --build
```
