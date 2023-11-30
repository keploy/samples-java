# Use an official OpenJDK runtime as a parent image
FROM openjdk:22-bookworm

# Set the working directory to /app
WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven

# Copy the current directory contents into the container at /app
COPY . /app/

# Expose the port the app runs on
EXPOSE 8080

# Define environment variables
ENV MONGODB_URI=mongodb://localhost:27017/Keploy

# Run the application when the container launches
# CMD ["java", "-jar", "PotionsAPI-0.0.1-SNAPSHOT.jar"]
CMD ["/usr/share/maven/bin/mvn", "spring-boot:run"]
