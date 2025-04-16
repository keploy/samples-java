# Use a Java 17 runtime (Debian-based, works on Apple Silicon)
FROM eclipse-temurin:17-jdk

# Set the working directory inside the container
WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven

# Copy the current directory contents into the container at /app
COPY . /app/

# Build the binary
RUN mvn clean install -Dmaven.test.skip=true


# Expose the port your Spring Boot app runs on
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "target/springbootapp-0.0.1-SNAPSHOT.jar"]
