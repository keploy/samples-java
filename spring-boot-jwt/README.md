# Keploy Sample Java - JWT Token Verification and Spring Boot

This repository contains a sample project that demonstrates the integration of Keploy with JWT (JSON Web Token) authentication in a Spring Boot application.

## Prerequisites

Before getting started, ensure you have the following installed:

- Latest version of JDK
- [Keploy](https://keploy.io/docs/server/installation/)
- [Docker](https://docs.docker.com/engine/install/) (if you want to run the application in Docker)
- Postman for testing APIs

## Getting Started

To get started, clone the repository:

```bash
git clone https://github.com/jaiakash/samples-java.git
cd samples-java/spring-boot-jwt
```

## Native Usage

To run the application locally, follow these steps:

1. Build the application:

  ```bash
  mvn clean install
  ```

2. Run the application:

  ```bash
  java -jar target/spring-boot-jwt.jar
  ```

## Running with Docker

To run the application with Docker, follow these steps:

1. Build the Docker image:

  ```bash
  docker build -t spring-boot-jwt .
  ```

2. Run the Docker container:

  ```bash
  docker run -p 8080:8080 spring-boot-jwt
  ```

The application will be accessible at `http://localhost:8080`.

## Integration with Keploy

### RECORD Mode

1. To run the application, use:

  #### Native Usage

  ```bash
  keploy record -c "java -jar target/spring-boot-jwt.jar"
  ```

  #### Docker Usage

  ```bash
  keploy record -c "docker run -p 8080:8080 spring-boot-jwt"
  ```

2. To generate test cases, make API calls using Postman or `curl`:

- Login

  ```bash
  curl --location --request POST 'http://localhost:8080/users/login' \
  --header 'Content-Type: application/json' \
  --data-raw '{
   "username": "akash@example.com",
   "password": "password"
  }'
  ```

- Verify

  ```bash
  curl --location --request POST 'http://localhost:8080/users/tokenVerification' \
  --header 'Authorization: Bearer <your_jwt_token_here>'
  ```

### TEST Mode

To test the application, start Keploy in test mode. In the root directory, run the following command:

#### Native Usage

```bash
keploy test -c "java -jar target/spring-boot-jwt.jar" --delay 30
```

#### Docker Usage

```bash
keploy test -c "docker run -p 8080:8080 spring-boot-jwt" --delay 30
```

This command will run the tests and generate the report in the `Keploy/reports` directory in the current working directory.

