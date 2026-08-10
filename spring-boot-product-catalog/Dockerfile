# syntax=docker/dockerfile:1

# ---- Build stage: compile & package with a pinned JDK 21 (reproducible on any host) ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy the Maven wrapper first so dependency resolution can be cached.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

# Now copy sources and build the fat jar (skip tests — they need a live DB).
COPY src ./src
RUN ./mvnw -B -q -DskipTests clean package

# ---- Run stage: slim JRE image ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# curl is used by the docker-compose healthcheck.
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
