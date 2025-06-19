# Spring Boot Redis CRUD API

This repository provides a simple, well-structured Spring Boot application that integrates with **Redis** as a database. It exposes robust RESTful API endpoints to perform **Create**, **Read**, **Update**, and **Delete (CRUD)** operations on in-memory data objects. This a contribution to keploy/keploy/issues/2333

---

## 🧰 Tech Stack

- Java 17  
- Spring Boot 3.x  
- Spring Data Redis  
- Redis (as in-memory database)  
- Maven  
- Docker (optional for running Redis)  
- Postman (for testing)

---

## 📦 Features

- 🔄 Full CRUD support using Redis as a key-value store  
- ⚡ Fast in-memory access with Redis  
- 🌱 Clean architecture with service/repository separation  
- 🧪 Includes basic unit tests for service layer  
- 📄 Swagger/OpenAPI support (optional)

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven
- Redis server (locally installed or via Docker)

### Running Redis with Docker

```bash
docker run -d -p 6379:6379 --name redis redis:latest
