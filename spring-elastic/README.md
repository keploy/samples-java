# Spring Boot Elasticsearch Example

This project is a simple Spring Boot application that demonstrates how to integrate with Elasticsearch using the Java High-Level REST Client. It provides REST endpoints to create and search books in an Elasticsearch index.

## Prerequisites

- **Java Development Kit (JDK) 8 or higher**
- **Maven** for building the project
- **Docker** for running Elasticsearch
- An API client like **Postman** or **cURL** for testing endpoints

## Setting Up Elasticsearch with Docker

To run Elasticsearch, we'll use Docker to pull and run the official Elasticsearch image.

Execute the following command in your terminal:

```bash
docker run -d \
  -p 9200:9200 \
  -p 9300:9300 \
  --name elasticsearch \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  docker.elastic.co/elasticsearch/elasticsearch:8.15.2
```

## Explanation of the Docker Command:

- `-d`: Run the container in detached mode (in the background).
- `-p 9200:9200`: Map port 9200 of the host to port 9200 of the container (HTTP API).
- `-p 9300:9300`: Map port 9300 of the host to port 9300 of the container (Transport API).
- `--name elasticsearch`: Assign the name "elasticsearch" to the container.
- `-e "discovery.type=single-node"`: Run Elasticsearch in single-node mode.
- `-e "xpack.security.enabled=false"`: Disable X-Pack security features for simplicity. !!! Not recommended for production
- `docker.elastic.co/elasticsearch/elasticsearch:8.15.2`: The Docker image to use.

### Verify Elasticsearch is Running:

After running the Docker command, verify that Elasticsearch is running by accessing [http://localhost:9200](http://localhost:9200) in your browser or using `curl`:

```bash
curl http://localhost:9200
```

You should receive a JSON response with cluster information.

Running the Spring Boot Application
1. Clone the Repository

```bash
git clone https://github.com/yourusername/your-repo-name.git
cd your-repo-name
```

2. Configure Application Properties
   Ensure that your application.properties file is correctly set up. It should be located in src/main/resources/ and contain the following properties:

```bash
spring.application.name=spring-elastic
server.port=8081

spring.elasticsearch.url=localhost
spring.elasticsearch.port=9200

# Credentials are optional if security is disabled
spring.elasticsearch.username=
spring.elasticsearch.password=
```
3. Build the Project
   Use Maven to build the project:

```bash
mvn clean install
```

4. Run the Application
   You can run the application using Maven:


```bash
mvn spring-boot:run
```

Or run the generated JAR file:

```bash
java -jar target/your-app-name.jar
```
The application will start on port 8081.

Project Structure
- Controller
BookController: Handles HTTP requests for creating and searching books.
- Model
Book: Represents the book entity stored in Elasticsearch.
- Client
BookElasticsearchClient: Manages the connection to Elasticsearch.
- Service
BookElasticsearchService: Contains business logic for interacting with Elasticsearch.
Endpoints
1. Create a Book
   - URL: /book/create
   - Method: POST
   - Description: Creates a new book in the Elasticsearch index.
   - Request Body:

```bash
{
  "id": "1",
  "name": "Elasticsearch Basics",
  "description": "An introductory guide to Elasticsearch.",
  "price": 29.99
}
```
Sample cURL Request:

```bash
curl -X POST \
  http://localhost:8081/book/create \
  -H 'Content-Type: application/json' \
  -d '{
        "id": "1",
        "name": "Elasticsearch Basics",
        "description": "An introductory guide to Elasticsearch.",
        "price": 29.99
      }'
```

2. Find Books by Name
   - URL: /book/find_by_name
   - Method: GET
   - Description: Searches for books by name in the Elasticsearch index.
   - Query Parameter:
      - name: The name or partial name of the book to search for.
```bash
curl -X GET "http://localhost:8081/book/find_by_name?name=Elasticsearch"
```

### Conclusion
You have successfully set up an Elasticsearch instance using Docker and run a Spring Boot application that interacts with it. This application demonstrates basic CRUD operations with Elasticsearch, providing a foundation for more complex integrations.

Feel free to extend this application by adding more features like update and delete operations, or by integrating authentication and security features.

Note: In a production environment, you should enable security features in Elasticsearch and handle credentials appropriately.