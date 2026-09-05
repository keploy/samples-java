# Two-stage build so the runtime image does not carry Maven, the Maven
# local repo, or the source tree. The runtime image is what k8s pulls.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
# Copy the POM first so dependency resolution caches when only source
# changes. Maven would otherwise re-fetch the entire dep set on every
# source edit, which makes iteration painful.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
# Spring Boot's maven plugin emits the jar to target/. Pinning the jar
# name keeps the COPY deterministic (no wildcard depending on artifact
# version expansion).
COPY --from=build /src/target/tidb-stmt-cache-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
