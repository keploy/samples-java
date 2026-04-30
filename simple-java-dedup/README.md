# Simple Java Dynamic Deduplication Sample

This is a small plain-Java HTTP service used to smoke-test Keploy Enterprise Java dynamic deduplication on Java 8 and Java 17. It uses only JDK APIs (`com.sun.net.httpserver.HttpServer`) and does not compile against the Keploy SDK.

The checked-in fixtures exercise `/healthz`, `/grade`, `/shipping`, `/inventory`, and `/invoice`. They intentionally contain duplicate coverage paths:

- `/grade?score=95` and `/grade?score=98` execute the same high-score branch.
- `/shipping?country=US&total=150` and `/shipping?country=US&total=175` execute the same free-shipping branch.
- `/inventory?sku=BOOK-1&quantity=3` and `/inventory?sku=BOOK-2&quantity=4` execute the same priority-reservation branch.
- `/invoice?customer=vip&subtotal=200` and `/invoice?customer=vip&subtotal=250` execute the same VIP-large discount branch.

## Build

```bash
mvn -B -DskipTests -Dkeploy.agent.version=2.0.6 clean package
```

## Native Dedup

```bash
keploy test \
  -c "java -javaagent:target/keploy-sdk.jar -javaagent:target/jacocoagent.jar -jar target/simple-java-dedup.jar" \
  --path . --dedup --language java --delay 1 \
  --health-url "http://127.0.0.1:8080/healthz" \
  --health-poll-timeout 30s \
  --disableMockUpload --disableReportUpload

keploy dedup --path .
```

## Docker Dedup

```bash
JAVA_VERSION=17 docker compose build
keploy test \
  -c "docker compose up" \
  --container-name simple-java-dedup \
  --path . --dedup --language java --delay 10 \
  --health-url "http://127.0.0.1:8080/healthz" \
  --health-poll-timeout 30s \
  --disableMockUpload --disableReportUpload

keploy dedup --path .
```

Keep the `-c` value as `docker compose up` so Keploy detects Docker Compose and mounts the shared `/tmp` socket volume used by Java dynamic deduplication. Pass `JAVA_VERSION`, `JAVA_DEDUP_IMAGE`, or port overrides in the shell environment instead of prefixing them inside the `-c` command.

## Expected Result

All 14 replayed fixtures should pass. Dedup should retain 10 test cases and mark 4 as duplicate, with exactly one duplicate from each intentional pair listed above.
