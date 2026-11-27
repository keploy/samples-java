FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /src
COPY InventoryServer.java .
RUN javac InventoryServer.java

FROM eclipse-temurin:21.0.13_9-jre
WORKDIR /app
COPY --from=build /src/InventoryServer*.class /app/
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "InventoryServer"]
