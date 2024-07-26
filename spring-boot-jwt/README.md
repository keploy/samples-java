# Keploy Sample Java - JWT Token Verification and Spring Boot

This repository contains a sample project that demonstrates the integration of Keploy with JWT (JSON Web Token) authentication in a Spring Boot application.

## Prerequisites

Before getting started, make sure you have the following installed:

- Latest version of JDK
- Install [Keploy](https://keploy.io/docs/server/installation/)
- Postman for testing APIs

## Getting Started

To get started, clone the repository:

```bash
git clone https://github.com/jaiakash/samples-java.git
cd spring-boot-jwt
```

## API Endpoints

The following API endpoints are available:

#### Login

- POST `/users/login`

  Authenticate a user and receive a JWT token.

  Request Body:

  ```json
  {
    "username": "your_username",
    "password": "your_password"
  }
  ```

  Response:

  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
  ```

#### Token Verification

- POST `/users/tokenVerification`

  Verify the validity of a JWT token.

  Request Body:

  ```json
  {
    "token": "your_jwt_token_here"
  }
  ```

  Response:

  ```json
  {
    "isValid": true
  }
  ```

## Integration with Keploy

#### RECORD Mode

1. To run the application, run

   ```
   keploy run -c "./mvnw spring-boot:run" --delay 240
   ```

2. To generate testcases, you can make API calls using Postman or `curl`:

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
  ```

#### TEST mode

To test the application, start Keploy in test mode. In the root directory, run the following command:

```bash
keploy test -c "./mvnw spring-boot:run" --delay 240
```

This command will run the tests and generate the report in the `Keploy/reports` directory in the current working directory.
