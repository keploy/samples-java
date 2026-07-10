Here’s a README for the Product application you developed using Spring Boot and Redis:

```markdown
# Product Spring Application

This is a simple Product application built with Spring Boot and Redis. The application provides RESTful APIs to manage Product items.

## Prerequisites

- Java 8
- Maven
- Docker

## Installation

### 1. Clone the Repository

```sh
git clone https://github.com/yourusername/product-spring-app.git
cd product-spring-app
```

### 2. Build the Application

```sh
mvn clean install
```

### 3. Set Up Docker Containers

#### Redis Container

1. Pull the Redis Docker Image:

    ```sh
    docker pull redis:latest
    ```

2. Run the Redis Container:

    ```sh
    docker run --name redis-product -p 6379:6379 -d redis:latest
    ```

### 4. Configure Application Properties

Update the `src/main/resources/application.properties` file with your Redis settings:

```properties
spring.redis.host=localhost
spring.redis.port=6379
```

### 5. Run the Application

```sh
mvn spring-boot:run
```

## Usage

### API Endpoints

#### Create a Product Item

```sh
curl -X POST http://localhost:8080/api/products \
    -H "Content-Type: application/json" \
    -d '{
        "name": "Sample Product",
        "description": "This is a sample product description",
        "price": 29.99
    }'
```

#### Get All Product Items

```sh
curl -X GET http://localhost:8080/api/products
```

#### Get a Product Item by ID

```sh
curl -X GET http://localhost:8080/api/products/{id}
```
Replace `{id}` with the actual ID of the Product item you want to retrieve.

#### Update a Product Item

```sh
curl -X PUT http://localhost:8080/api/products/{id} \
    -H "Content-Type: application/json" \
    -d '{
        "name": "Updated Product",
        "description": "This is an updated product description",
        "price": 39.99
    }'
```
Replace `{id}` with the actual ID of the Product item you want to update.

#### Delete a Product Item

```sh
curl -X DELETE http://localhost:8080/api/products/{id}
```
Replace `{id}` with the actual ID of the Product item you want to delete.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
```

Feel free to adjust any details specific to your project, such as the repository link, product attributes, or additional instructions!