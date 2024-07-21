# Use an official OpenJDK runtime as a parent image
FROM openjdk:22-bookworm

# Set the working directory to /app
WORKDIR /app

# Install Maven
RUN brew install maven

# Copy the current directory contents into the container at /app
COPY . /app/

# Build the binary
RUN mvn clean install -Dmaven.test.skip=true

# Expose the port the app runs on
EXPOSE 9966

# Download the ca.crt file
RUN curl -o ca.crt https://raw.githubusercontent.com/keploy/keploy/main/pkg/core/proxy/asset/ca.crt

RUN curl -o setup_ca.sh https://raw.githubusercontent.com/keploy/keploy/main/pkg/core/proxy/asset/setup_ca.sh
ADD https://keploy-enterprise.s3.us-west-2.amazonaws.com/releases/latest/assets/freeze_time_arm64.so /lib/keploy/freeze_time_arm64.so

#set suitable permissions
RUN chmod +x /lib/keploy/freeze_time_arm64.so

# Set LD_PRELOAD environment variable to use freeze_time_arm64.so
ENV LD_PRELOAD=/lib/keploy/freeze_time_arm64.so

# Give execute permission to the setup_ca.sh script
RUN chmod +x setup_ca.sh

# Run the application when the container launches
# CMD ["java", "-jar", ""]
CMD ["/bin/bash", "-c", "source ./setup_ca.sh && java -jar target/spring-petclinic-rest-3.0.2.jar"]
