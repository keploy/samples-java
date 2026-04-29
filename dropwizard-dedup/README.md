# Dropwizard Dynamic Deduplication Sample

This sample validates that Keploy Java dynamic deduplication works for a non-Spring Java service. The app is a Dropwizard/Jersey HTTP service and does not import or depend on the Keploy SDK at compile time.

Build without Keploy on the compile classpath:

```bash
mvn -B -DskipTests clean package
```

Build with the runtime Java agent copied into `target/keploy-sdk.jar`:

```bash
mvn -B -DskipTests -Dkeploy.agent.version=2.0.1 clean package
```

Run with the agent:

```bash
java \
  -javaagent:target/keploy-sdk.jar \
  -javaagent:target/jacocoagent.jar=destfile=/tmp/jacoco.exec \
  -jar target/dropwizard-dedup-0.0.1-SNAPSHOT.jar server config.yml
```
