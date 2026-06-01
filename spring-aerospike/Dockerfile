FROM eclipse-temurin:17-jdk
WORKDIR /app
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
COPY pom.xml /app/
COPY src /app/src
RUN mvn -q -DskipTests package
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "target/spring-aerospike.jar"]
