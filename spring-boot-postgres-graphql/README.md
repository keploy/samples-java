# Library DEMO

### Requirements

- JDK 17
- Maven 3.9.5
- Docker or PostgreSQL

### About the DEMO

This is a Spring Boot application implementing a GraphQL service to handle requests related to books and authors.

The controller provides four queries:

- `getBookByName`: Returns a book based on the specified name.
- `getAllBooks`: Retrieves all books present in the repository.
- `getAuthorById`: Fetches an author based on the provided ID.
- `getAllAuthors`: Retrieves all authors available in the repository.

# Getting Started

Follow the steps to start the application:

## Clone
- First, clone the repository:

   ```bash
   git clone https://github.com/keploy/samples-java
   ```
  Navigate to the project folder:

   ```bash
   cd samples-java/spring-boot-postgres-graphql
   ```

## Quick Keploy Installation

- Depending on your OS and preference (Docker/Native), you can set up Keploy using the one-click installation method:
   ```bash
   curl -O https://raw.githubusercontent.com/keploy/keploy/main/keploy.sh && source keploy.sh
   ```

## Setup DB

### Use postgres docker image

If you have Docker installed and prefer a PostgreSQL instance in a container, follow these steps:
1. Build the Docker image:

    ```bash
       docker build -t postgres_library:demo ./postgres_demo_docker
    ```
   
2. Run a container from the generated image:

    ```bash
       docker run -d -p 5432:5432 --name postgres postgres_library:demo
    ```

## Use of Keploy

1. Your application is ready to be executed!!
   To start Keploy in record mode, in the root directory, run:

   ```bash
   keploy record -c "mvn spring-boot:run"
   ```

### Query from GraphQL GUI

Now, go to `localhost:8081/graphiql` and access the GraphQL interface to make requests to the application.
   - Make a `getAllBooks` query:

      ```graphql
         query {
           getAllBooks {
               name
               author {
                  id
                  firstName
                  lastName
               }
           }
         }
      ```

   -  Make a `getBookByName` query:

      ```graphql
         query {
           getBookByName(name: "The Secret of the Moon") {
               id
               name
               pageCount
               author {
                  firstName
                  lastName
               }
           }
         }
      ```

   -  Make a `getAllAuthors` query:

      ```graphql
         query {
            getAllAuthors {
               id
               firstName
               lastName 
            }
         }
      ```

   -  Make a `getAuthorById` query:

      ```graphql
         query {
            getAuthorById(id: 2) {
               firstName
            }
         }
      ```

You can experiment with the data you want to retrieve from the query by removing or rearranging fields.

   The generated tests and mocks are stored in the `Keploy` directory in the current working directory.


### Execute tests

To test the app, start Keploy in test mode. In the root directory, run:

   ```bash
      keploy test -c "mvn spring-boot:run" --delay 15
   ```

   This will run the tests and generate the report in the `Keploy/reports` directory in the current working directory.

