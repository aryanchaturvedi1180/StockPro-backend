# StockPro Backend

StockPro Backend is a Spring Boot microservices-based inventory management system. It is designed to manage products, warehouses, suppliers, purchase orders, stock movements, alerts, reports, and user authentication through separate backend services.

The project follows a microservices architecture using Eureka Server for service discovery and API Gateway for centralized routing and JWT-based request filtering.

---

## Project Overview

StockPro helps manage inventory operations such as:

- User registration and login
- Role-based backend access
- Product management
- Warehouse and stock management
- Supplier management
- Purchase order management
- Stock movement tracking
- Low-stock and purchase-order alerts
- Inventory reports
- Centralized API routing through API Gateway
- Service discovery using Eureka Server

---

## Tech Stack

- Java 17
- Spring Boot 3.2.5
- Spring Cloud 2023.0.1
- Spring Web
- Spring Security
- JWT Authentication
- Spring Data JPA
- Hibernate
- MySQL
- Maven Multi-Module Project
- Eureka Server / Eureka Client
- Spring Cloud Gateway
- OpenFeign
- RabbitMQ
- Flyway Migration
- Spring Boot Actuator
- Swagger / OpenAPI
- Lombok

---

## Microservices Included

| Service | Port | Purpose |
|---|---:|---|
| Eureka Server | 8761 | Service registry and discovery |
| API Gateway | 8080 | Central routing layer for all services |
| Auth Service | 8081 | User registration, login, JWT generation, user management |
| Product Service | 8082 | Product CRUD and product status management |
| Warehouse Service | 8083 | Warehouse CRUD and stock level management |
| Purchase Service | 8084 | Purchase order creation, approval, cancellation, receiving |
| Supplier Service | 8085 | Supplier CRUD and supplier status management |
| Movement Service | 8086 | Stock movement and transfer tracking |
| Alert Service | 8087 | Alert management for low stock and overdue POs |
| Report Service | 8088 | Stock value and low-stock reports |

---

## Architecture

```text
Angular Frontend / API Client
        |
        v
API Gateway :8080
        |
        v
Eureka Server :8761
        |
        +--> Auth Service :8081
        +--> Product Service :8082
        +--> Warehouse Service :8083
        +--> Purchase Service :8084
        +--> Supplier Service :8085
        +--> Movement Service :8086
        +--> Alert Service :8087
        +--> Report Service :8088

MySQL is used by data-based services.
RabbitMQ is used for event-driven alerts.
OpenFeign is used for inter-service communication.
```

---

## Main Backend Flow

1. User registers or logs in using Auth Service.
2. Auth Service validates credentials and generates a JWT token.
3. Frontend sends the token in every protected request using the `Authorization` header.
4. API Gateway validates the JWT token before forwarding the request.
5. Gateway routes the request to the correct microservice using Eureka service discovery.
6. Individual services handle business logic and persist data in MySQL.
7. Services communicate internally using OpenFeign where required.
8. RabbitMQ is used for event-based alert handling.
9. Reports are generated using data fetched from other services.

---

## API Gateway Routes

All requests can be accessed through API Gateway at:

```text
http://localhost:8080
```

| Route | Target Service | Auth Required |
|---|---|---|
| `/api/v1/auth/**` | auth-service | Login/Register open, protected user APIs require token |
| `/api/v1/products/**` | product-service | Yes |
| `/api/v1/warehouses/**` | warehouse-service | Yes |
| `/api/v1/purchase-orders/**` | purchase-service | Yes |
| `/api/v1/suppliers/**` | supplier-service | Yes |
| `/api/v1/movements/**` | movement-service | Yes |
| `/api/v1/alerts/**` | alert-service | Yes |
| `/api/v1/reports/**` | report-service | Yes |

---

## Service Details

### 1. Eureka Server

Eureka Server is used as the service registry. All microservices register themselves with Eureka so that API Gateway and other services can discover them dynamically.

Access Eureka Dashboard:

```text
http://localhost:8761
```

---

### 2. API Gateway

API Gateway is the single entry point for backend APIs. It handles:

- Request routing
- Load-balanced service discovery
- JWT validation
- CORS configuration
- Forwarding authenticated user details using headers

The gateway forwards user context through headers:

```text
X-Auth-Email
X-Auth-Role
X-Auth-UserId
```

---

### 3. Auth Service

Base URL:

```text
/api/v1/auth
```

Main APIs:

| Method | Endpoint | Description |
|---|---|---|
| POST | `/register` | Register a new user |
| POST | `/login` | Login and generate JWT token |
| GET | `/validate` | Validate JWT token |
| GET | `/users` | Get all users |
| GET | `/users/{id}` | Get user by ID |
| PUT | `/users/{id}/deactivate` | Deactivate user |
| PUT | `/users/{id}/reactivate` | Reactivate user |
| POST | `/logout` | Logout user |

---

### 4. Product Service

Base URL:

```text
/api/v1/products
```

Main APIs:

| Method | Endpoint | Description |
|---|---|---|
| POST | `/` | Create product |
| GET | `/` | Get all products |
| GET | `/{id}` | Get product by ID |
| GET | `/sku/{sku}` | Get product by SKU |
| GET | `/active` | Get active products |
| GET | `/search` | Search products |
| PUT | `/{id}` | Update product |
| PUT | `/{id}/activate` | Activate product |
| PUT | `/{id}/deactivate` | Deactivate product |
| DELETE | `/{id}` | Delete product |

---

### 5. Warehouse Service

Base URL:

```text
/api/v1/warehouses
```

Main APIs:

| Method | Endpoint | Description |
|---|---|---|
| POST | `/` | Create warehouse |
| GET | `/` | Get all warehouses |
| GET | `/{id}` | Get warehouse by ID |
| GET | `/active` | Get active warehouses |
| PUT | `/{id}` | Update warehouse |
| PUT | `/{id}/activate` | Activate warehouse |
| PUT | `/{id}/deactivate` | Deactivate warehouse |
| GET | `/stock` | Get stock data |
| GET | `/{warehouseId}/stock` | Get stock by warehouse |
| GET | `/{warehouseId}/stock/{productId}` | Get product stock in warehouse |
| GET | `/stock/product/{productId}` | Get product stock across warehouses |
| POST | `/stock/add` | Add stock |
| POST | `/stock/deduct` | Deduct stock |
| POST | `/stock/transfer` | Transfer stock |

---

### 6. Supplier Service

Base URL:

```text
/api/v1/suppliers
```

Main APIs:

| Method | Endpoint | Description |
|---|---|---|
| POST | `/` | Create supplier |
| GET | `/` | Get all suppliers |
| GET | `/{id}` | Get supplier by ID |
| GET | `/active` | Get active suppliers |
| PUT | `/{id}` | Update supplier |
| PUT | `/{id}/activate` | Activate supplier |
| PUT | `/{id}/deactivate` | Deactivate supplier |
| DELETE | `/{id}` | Delete supplier |

---

### 7. Purchase Service

Base URL:

```text
/api/v1/purchase-orders
```

Main APIs:

| Method | Endpoint | Description |
|---|---|---|
| POST | `/` | Create purchase order |
| GET | `/` | Get all purchase orders |
| GET | `/{id}` | Get purchase order by ID |
| GET | `/status/{status}` | Get purchase orders by status |
| GET | `/supplier/{supplierId}` | Get purchase orders by supplier |
| PUT | `/{id}/approve` | Approve purchase order |
| PUT | `/{id}/cancel` | Cancel purchase order |
| PUT | `/{id}/receive` | Receive purchase order |

---

### 8. Movement Service

Base URL:

```text
/api/v1/movements
```

Main APIs:

| Method | Endpoint | Description |
|---|---|---|
| POST | `/` | Create stock movement |
| POST | `/transfer` | Transfer stock |
| GET | `/` | Get all movements |
| GET | `/{id}` | Get movement by ID |
| GET | `/warehouse/{warehouseId}` | Get movements by warehouse |
| GET | `/product/{productId}` | Get movements by product |
| GET | `/type/{type}` | Get movements by type |
| GET | `/user/{userId}` | Get movements by user |
| GET | `/reference` | Get movements by reference |
| GET | `/daterange` | Get movements by date range |
| GET | `/warehouse/{warehouseId}/product/{productId}` | Get movements by warehouse and product |

---

### 9. Alert Service

Base URL:

```text
/api/v1/alerts
```

Main APIs:

| Method | Endpoint | Description |
|---|---|---|
| GET | `/` | Get all alerts |
| GET | `/{id}` | Get alert by ID |
| GET | `/unread` | Get unread alerts |
| GET | `/type/{alertType}` | Get alerts by type |
| PUT | `/{id}/read` | Mark alert as read |
| PUT | `/read-all` | Mark all alerts as read |

---

### 10. Report Service

Base URL:

```text
/api/v1/reports
```

Main APIs:

| Method | Endpoint | Description |
|---|---|---|
| GET | `/stock-value` | Generate stock value report |
| GET | `/low-stock` | Generate low-stock report |

---

## Prerequisites

Install these before running the project:

- Java 17
- Maven
- MySQL Server
- RabbitMQ Server
- Git
- Postman or Swagger UI for API testing

---

## Database Setup

Create the following MySQL databases:

```sql
CREATE DATABASE stockpro_auth;
CREATE DATABASE stockpro_product;
CREATE DATABASE stockpro_warehouse;
CREATE DATABASE stockpro_supplier;
CREATE DATABASE stockpro_purchase;
CREATE DATABASE stockpro_movement;
CREATE DATABASE stockpro_alert;
```

Update each service's `application.properties` with your local MySQL username and password.

Example:

```properties
spring.datasource.username=root
spring.datasource.password=your_password
```

---

## RabbitMQ Setup

Warehouse, Purchase, Movement, and Alert services use RabbitMQ/event-based communication.

Default RabbitMQ configuration:

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

RabbitMQ Management Console:

```text
http://localhost:15672
```

Default credentials:

```text
Username: guest
Password: guest
```

---

## How to Run the Project

### 1. Clone the repository

```bash
git clone <repository-url>
cd StockPro-backend
```

### 2. Build the full project

```bash
mvn clean install
```

### 3. Start services in this order

Start Eureka Server first:

```bash
cd eureka-server
mvn spring-boot:run
```

Then start API Gateway:

```bash
cd ../api-gateway
mvn spring-boot:run
```

Then start business services in separate terminals:

```bash
cd auth-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
cd warehouse-service && mvn spring-boot:run
cd supplier-service && mvn spring-boot:run
cd purchase-service && mvn spring-boot:run
cd movement-service && mvn spring-boot:run
cd alert-service && mvn spring-boot:run
cd report-service && mvn spring-boot:run
```

---

## Swagger URLs

Each service exposes Swagger UI separately:

| Service | Swagger URL |
|---|---|
| Auth Service | `http://localhost:8081/swagger-ui.html` |
| Product Service | `http://localhost:8082/swagger-ui.html` |
| Warehouse Service | `http://localhost:8083/swagger-ui.html` |
| Purchase Service | `http://localhost:8084/swagger-ui.html` |
| Supplier Service | `http://localhost:8085/swagger-ui.html` |
| Movement Service | `http://localhost:8086/swagger-ui.html` |
| Alert Service | `http://localhost:8087/swagger-ui.html` |
| Report Service | `http://localhost:8088/swagger-ui.html` |

---

## Authentication Flow

### Register

```http
POST /api/v1/auth/register
```

### Login

```http
POST /api/v1/auth/login
```

After successful login, copy the JWT token from the response.

For protected APIs, pass the token like this:

```http
Authorization: Bearer <jwt-token>
```

---

## Recommended Git Ignore

This project should ignore generated files and OS-specific files:

```gitignore
.DS_Store
/target/
*/target/
.idea/
.vscode/
*.log
```

---

## Suggested Run Order for Testing

1. Start MySQL
2. Start RabbitMQ
3. Start Eureka Server
4. Start API Gateway
5. Start Auth Service
6. Register/Login user
7. Start Product, Warehouse, Supplier services
8. Add product, supplier, warehouse data
9. Test purchase order flow
10. Test stock add/deduct/transfer flow
11. Test movement records
12. Test alert APIs
13. Test report APIs

---

## Project Structure

```text
StockPro-backend/
├── pom.xml
├── eureka-server/
├── api-gateway/
├── auth-service/
├── product-service/
├── warehouse-service/
├── supplier-service/
├── purchase-service/
├── movement-service/
├── alert-service/
└── report-service/
```

---

## Important Notes

- Run Eureka Server before starting other services.
- Run API Gateway before testing APIs through port `8080`.
- Keep JWT secret same across Gateway and secured services.
- Make sure MySQL database credentials are correct in each service.
- Make sure RabbitMQ is running before testing event-based alert features.
- Do not push `target/`, `.DS_Store`, IDE files, or generated files to GitHub.

---

## Author

Aryan Chaturvedi
