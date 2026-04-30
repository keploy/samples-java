# Dropwizard Dynamic Deduplication Sample

This sample validates that Keploy Java dynamic deduplication works for a non-Spring Java service. The app is a Dropwizard/Jersey HTTP service and does not import or depend on the Keploy SDK at compile time.

CI does not record this sample. The `keploy/` directory contains checked-in fixtures, so Enterprise CI only builds the app and runs replay with `--dedup`. When the sample behavior changes, record the fixtures locally and push the updated `keploy/` files.

Build without Keploy on the compile classpath:

```bash
mvn -B -DskipTests clean package
```

Build with the runtime Java agent copied into `target/keploy-sdk.jar`:

```bash
mvn -B -DskipTests -Dkeploy.agent.version=2.0.6 clean package
```

Run with the agent:

```bash
java \
  -javaagent:target/keploy-sdk.jar \
  -javaagent:target/jacocoagent.jar=destfile=/tmp/jacoco.exec \
  -jar target/dropwizard-dedup.jar server config.yml
```
