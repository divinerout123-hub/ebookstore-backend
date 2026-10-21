# E-Commerce Bookstore Backend

A RESTful backend for an online bookstore. It provides user registration and JWT authentication, a browsable book
catalogue with search and recommendations, a shopping cart, order management with stock handling, payments, and
address management. The full API contract is in [`openapi.yaml`](openapi.yaml).

## Table of Contents

- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Setup](#setup)
- [Running Locally](#running-locally)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Quick Walkthrough](#quick-walkthrough)
- [Project Structure](#project-structure)
- [Design Notes](#design-notes)
- [Production Checklist](#production-checklist)

## Technology Stack

| Area | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 (Spring Web, Validation) |
| Persistence | Spring Data JPA, Hibernate 6 |
| Database | PostgreSQL |
| Security | Spring Security, stateless JWT (jjwt 0.12), BCrypt password hashing |
| Build | Maven |
| Utilities | Lombok |

## Prerequisites

- **JDK 17 or newer** (the project compiles to Java 17 bytecode; verified on JDK 17 and JDK 26)
- **Maven 3.9+**
- **PostgreSQL**, running locally on port 5432 (verified on PostgreSQL 18)

Check your versions:

```bash
java -version
mvn -version
psql --version
```

## Setup

### 1. Create the database

```bash
createdb bookstore
# or: psql -d postgres -c "create database bookstore"
```

### 2. Set database credentials

The defaults are in [`application.properties`](src/main/resources/application.properties). Override them with
environment variables to match your PostgreSQL install:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/bookstore
export DB_USERNAME=<your-postgres-user>
export SPRING_DATASOURCE_PASSWORD=<your-password>   # omit for passwordless local setups
```

> The checked-in default username is a local developer account. Always set `DB_USERNAME` / `SPRING_DATASOURCE_PASSWORD` for your own
> environment.

### 3. Build

```bash
mvn clean install
```

## Running Locally

```bash
mvn spring-boot:run
```

The API is available at **http://localhost:8080**. On first start Hibernate creates all tables automatically
(`spring.jpa.hibernate.ddl-auto=update`).

Or run the packaged jar:

```bash
java -jar target/ebookstore-backend-1.0.0.jar
```

### Load sample data (optional)

The application has no endpoints for creating books or categories, so load the sample data to get started. Run these
**in your shell** (not inside the `psql` prompt), after the application has started once so the tables exist:

```bash
psql -d bookstore -f db/sample-data.sql          # 5 categories, 10 books (prices in INR), 1 user
psql -d bookstore -f db/sample-data-orders.sql   # addresses, a cart, 3 orders, 2 payments
```

Both scripts are safe to re-run. The sample user is created with a bcrypt hash generated at load time (pgcrypto), so no hash is stored in the repo. To use a different password: `psql -d bookstore -v demo_pw='your-password' -f db/sample-data.sql`. Default sample login:

| Email | Password |
|---|---|
| `aarav.sharma@example.com` | `Password@123` |

### Verify

```bash
curl http://localhost:8080/api/categories
```

## Configuration

All settings live in [`application.properties`](src/main/resources/application.properties).

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/bookstore` | Override with `DB_URL` |
| `spring.datasource.username` | see file | Override with `DB_USERNAME` |
| `spring.datasource.password` | none | Set `SPRING_DATASOURCE_PASSWORD` |
| `app.jwt.secret` | development key | Base64, at least 256 bits. Override with `JWT_SECRET` |
| `app.jwt.expiration-seconds` | `3600` | Token lifetime |
| `app.cors.allowed-origins` | `http://localhost:3000,http://localhost:4200` | Comma-separated allowed origins |
| `app.order.tax-rate` | `0.08` | Tax applied to the order subtotal |
| `app.order.shipping-cost` | `5.99` | Flat shipping fee |
| `app.order.free-shipping-threshold` | `50.00` | Subtotal at or above which shipping is free |

Pricing values are plain numbers with no currency. If you use INR prices, adjust the shipping settings accordingly
(for example a threshold of `500` and a shipping cost of `49`).

## API Endpoints

Authentication: send `Authorization: Bearer <token>` on every protected endpoint. Get a token from
`/api/auth/login` or `/api/auth/register`.

### Auth

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register a new user and receive a token |
| POST | `/api/auth/login` | Public | Log in and receive a token |

### Users

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/users/{userId}` | Required | Get a user profile |

### Products

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/products` | Public | List products (`page`, `size`, `sort`) |
| GET | `/api/products/{productId}` | Public | Get a product by ID |
| GET | `/api/products/category/{categoryId}` | Public | List products in a category |
| GET | `/api/products/search?keyword=` | Public | Search by title, author, ISBN or description |
| GET | `/api/products/recommended/{userId}?limit=` | Required | Personalised recommendations (default 10, max 50) |

### Categories

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/categories` | Public | List all categories |
| GET | `/api/categories/{categoryId}` | Public | Get a category by ID |

### Cart

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/cart/{userId}` | Required | Get the user's cart |
| POST | `/api/cart/{userId}/items` | Required | Add an item (merges with an existing line) |
| PUT | `/api/cart/{userId}/items/{itemId}` | Required | Update an item's quantity |
| DELETE | `/api/cart/{userId}/items/{itemId}` | Required | Remove an item |

### Orders

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/orders` | Required | Create an order from the user's cart |
| GET | `/api/orders/{orderId}` | Required | Get an order by ID |
| GET | `/api/orders/user/{userId}` | Required | Order history (`page`, `size`, `status`) |
| PUT | `/api/orders/{orderId}/cancel` | Required | Cancel an order (pending, paid or processing) |

### Payments

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/payments` | Required | Process a payment (optional `Idempotency-Key` header) |
| GET | `/api/payments/{paymentId}` | Required | Get payment status |

### Addresses

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/addresses` | Required | Add an address |
| GET | `/api/addresses/user/{userId}` | Required | List a user's addresses |

### Errors

Errors use one consistent JSON shape:

```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register",
  "details": [{ "field": "email", "message": "must be a well-formed email address" }]
}
```

Common statuses: `400` validation, `401` missing or invalid token, `402` payment declined, `403` accessing another
user's data, `404` not found, `409` conflict (duplicate email, insufficient stock, order not payable or cancellable).

## Quick Walkthrough

```bash
# 1. Log in (sample user) and capture the token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"aarav.sharma@example.com","password":"Password@123"}' \
  | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')

# 2. Browse books
curl 'http://localhost:8080/api/products/search?keyword=tolkien'

# 3. Add a book to the cart (user id 1)
curl -X POST http://localhost:8080/api/cart/1/items \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"productId":1,"quantity":1}'

# 4. Place an order using address 1
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"userId":1,"shippingAddressId":1}'
```

## Project Structure

```
src/main/java/com/bookstore
├── BookstoreApplication.java
├── config/        SecurityConfig (JWT filter chain, CORS, password encoder)
├── controller/    REST controllers
├── dto/           request and response records
├── entity/        JPA entities
├── enums/         roles and statuses
├── exception/     GlobalExceptionHandler and domain exceptions
├── repository/    Spring Data repositories
├── security/      JWT service and filter, ownership checks
└── service/       service interfaces and impl/ implementations
db/                sample data scripts
openapi.yaml       API specification
```

## Design Notes

- **Access control:** customers can only read or modify their own user, cart, orders, payments and addresses; admins
  can access all. Product and category reads are public.
- **Orders:** created from the cart. Product rows are locked and stock is decremented at creation, and restored when
  the order is cancelled.
- **Payments:** the payment gateway is **simulated**. Cards are checked only for a valid number (Luhn) and expiry, and
  declined attempts are stored as `FAILED`. Cash on delivery moves the order to `PROCESSING`. The
  `Idempotency-Key` header makes retries safe.

## Production Checklist

- Replace the simulated gateway in `PaymentServiceImpl.authorizeCard` with a real payment provider.
- Set a strong `JWT_SECRET` and real database credentials through environment variables.
- Switch `spring.jpa.hibernate.ddl-auto` to `validate` and manage the schema with migrations (Flyway or Liquibase).
- Restrict `app.cors.allowed-origins` to your real front-end origins.
- Add endpoints for managing products and categories, and automated tests (none are included yet).
