# Build:  docker build -t orders-api:1.0 .
#         docker build --build-arg VARIANT=v2 -t orders-api:2.0 .   (v2: breaks Solace + Gravitee/inventory + Postgres)
#         docker build --build-arg VARIANT=v3 -t orders-api:2.1 .   (v3: upper-cases channel in Solace event for strict-mode demo)
#         docker build --build-arg VARIANT=v4 -t orders-api:2.2 .   (v4: an EXPECTED change: cancel returns status REFUND_PENDING)
FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
ARG VARIANT=v1
COPY src ./src
COPY v2.patch v3.patch v4.patch ./
RUN if [ "$VARIANT" != "v1" ]; then apt-get -qq update >/dev/null && apt-get -qq install -y patch >/dev/null; patch -p1 < $VARIANT.patch; fi \
 && mvn -q -B -DskipTests package

FROM eclipse-temurin:21.0.13_9-jre
WORKDIR /app
COPY --from=build /src/target/orders-api.jar /app/orders-api.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-Djdk.httpclient.allowRestrictedHeaders=host", "-jar", "/app/orders-api.jar"]
