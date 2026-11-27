FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /src
COPY PaymentsServer.java .
RUN javac PaymentsServer.java

FROM eclipse-temurin:21.0.13_9-jre
WORKDIR /app
COPY --from=build /src/PaymentsServer*.class /app/
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "PaymentsServer"]
