# Use an official OpenJDK runtime as a parent image
FROM openjdk:25-bookworm

# Set the working directory to /app
WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven

# Copy the current directory contents into the container at /app
COPY . /app/

# Build the binary
RUN mvn clean install -Dmaven.test.skip=true

# Expose the port the app runs on
EXPOSE 9966

# Run the application when the container launches

CMD ["java", "-jar", "target/spring-petclinic-rest-3.0.2.jar"]
