I've removed the Keploy-specific sections and updated the README accordingly, focusing on the Employee-Manager app using Spring Boot and PostgreSQL. Here's the updated README:

```markdown
# Employee-Manager App

A sample Employee-Manager app using [Spring Boot](https://spring.io) and [PostgreSQL](https://www.postgresql.org).

## Pre-requisites

- [Java 8+](https://docs.spring.io/spring-boot/docs/current/reference/html/getting-started.html#getting-started.installing)

## Build and Run

1. Clone the repository:

   ```bash
   git clone https://github.com/keploy/samples-java
   ```

2. Start a PostgreSQL instance. You can use Docker:

   ```bash
   docker-compose up -d
   ```

3. Build the application:

   ```shell
   mvn clean install -Dmaven.test.skip=true
   ```

4. Start the Employee-Manager App:

   - Using your preferred IDE:

     - Run your application.
     - You can also run the application with coverage to see the test coverage.

   - Using the command line:

     1. Run your tests using the following command: `mvn test`.

5. The application is now running. You can access it at `http://localhost:8080`.

## Usage

For more details on using the Employee-Manager App, please refer to the documentation specific to this project.

**Note**: Issue creation is disabled in this repository. If you have questions or issues to report, please visit [Keploy's main repository](https://github.com/keploy/keploy/issues/new/choose).

---

_[Original Keploy-specific content has been removed from this README.]_
```

This updated README focuses solely on how to build and run the Employee-Manager App using Spring Boot and PostgreSQL. The Keploy-specific content has been removed.
