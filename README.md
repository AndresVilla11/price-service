# price-service

Microservice responsible for resolving the applicable price for a product within a brand, given an application date. When multiple price lists overlap in time, the one with the highest **priority** value is returned.

---

## Table of Contents

- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Business Rules](#business-rules)
- [Configuration](#configuration)
- [Running Tests](#running-tests)
- [Design Decisions](#design-decisions)

---

## Architecture

This service follows **Hexagonal Architecture** (Ports & Adapters), ensuring the domain is completely isolated from infrastructure concerns.

```
com.inditex.price
├── domain/                     # Pure business logic — zero external dependencies
│   ├── model/
│   │   ├── Price.java          # Domain model (Java 21 record)
│   │   └── PriceQuery.java     # Value object encapsulating query criteria
│   ├── port/
│   │   ├── input/
│   │   │   └── GetApplicablePriceUseCase.java
│   │   └── output/
│   │       └── PriceRepositoryPort.java
│   ├── service/
│   │   └── GetApplicablePriceUseCaseImpl.java
│   └── exception/
│       └── PriceNotFoundException.java
│
├── application/                # Use case orchestration
│   └── usecase/
│       └── GetApplicablePriceUseCaseImpl.java
│
└── infrastructure/             # Adapters — all external concerns
    ├── adapter/
    │   ├── input/rest/         # HTTP adapter (Spring MVC)
    │   │   ├── PriceController.java
    │   │   ├── dto/            # PriceRequest, PriceResponse
    │   │   └── mapper/         # PriceMapper (MapStruct)
    │   └── output/persistence/ # JPA adapter
    │       ├── PriceRepositoryAdapter.java
    │       ├── entity/         # PriceEntity (@Entity)
    │       └── mapper/         # EntityMapper (MapStruct)
    └── config/
        ├── OpenApiConfig.java
        ├── aop/LoggingAspect.java
        └── exception/GlobalExceptionHandler.java
```

### Dependency rule

Dependencies flow strictly inward. The domain knows nothing about Spring, JPA, or HTTP. Adapters depend on ports; ports never depend on adapters.

```
REST Controller → UseCase (port) → Domain Service → RepositoryPort (interface)
                                                             ↑
                                               JPA Adapter implements this
```

---

## Technology Stack

| Component        | Technology                      | Version  |
|------------------|---------------------------------|----------|
| Language         | Java                            | 21       |
| Framework        | Spring Boot                     | 3.3.x    |
| Persistence      | Spring Data JPA + H2            | —        |
| Mapping          | MapStruct                       | 1.5.x    |
| Documentation    | SpringDoc OpenAPI               | 2.x      |
| Testing          | JUnit 5 + Mockito + MockMvc     | —        |
| Build            | Maven                           | 3.9.x    |

---

## Getting Started

### Prerequisites

- Java 21
- Maven 3.9+

### Build and run

```bash
./mvnw clean package
./mvnw spring-boot:run
```

### Run with dev profile (H2 console + verbose SQL logging)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The H2 console is available at `http://localhost:8080/h2-console` in the `dev` profile only.  
JDBC URL: `jdbc:h2:mem:pricedb`

### API documentation

Once running, the OpenAPI UI is available at:

```
http://localhost:8080/swagger-ui.html
http://localhost:8080/api-docs           (raw JSON)
```

---

## API Reference

### `GET /api/v1/prices`

Returns the applicable price for a product given a brand and application date.

#### Query parameters

| Parameter         | Type              | Required | Description                          |
|-------------------|-------------------|----------|--------------------------------------|
| `productId`       | `Long`            | Yes      | Product identifier                   |
| `brandId`         | `Long`            | Yes      | Brand identifier (1 = ZARA)          |
| `applicationDate` | `LocalDateTime`   | Yes      | ISO 8601 date-time (e.g. `2020-06-14T10:00:00`) |

#### Responses

| Status | Description                                      |
|--------|--------------------------------------------------|
| `200`  | Applicable price found                           |
| `400`  | Invalid or missing request parameters            |
| `404`  | No applicable price for the given criteria       |
| `500`  | Unexpected server error                          |

#### Example request

```bash
curl -X GET "http://localhost:8080/api/v1/prices?productId=35455&brandId=1&applicationDate=2020-06-14T10:00:00" \
     -H "Accept: application/json"
```

#### Example response — 200 OK

```json
{
  "productId":  35455,
  "brandId":    1,
  "priceList":  1,
  "startDate":  "2020-06-14T00:00:00",
  "endDate":    "2020-12-31T23:59:59",
  "price":      35.50,
  "currency":   "EUR"
}
```

#### Example response — 404 Not Found (RFC 7807)

```json
{
  "type":      "https://api.inditex.com/errors/price-not-found",
  "title":     "Price Not Found",
  "status":    404,
  "detail":    "No applicable price found for productId=35455, brandId=1, date=2020-06-14T10:00",
  "timestamp": "2024-01-15T10:23:45.123Z",
  "path":      "/api/v1/prices"
}
```

#### Example response — 400 Bad Request (RFC 7807)

```json
{
  "type":   "https://api.inditex.com/errors/validation-error",
  "title":  "Invalid Request",
  "status": 400,
  "detail": "Request validation failed",
  "violations": {
    "productId": "must be greater than 0"
  }
}
```

---

## Business Rules

The `PRICES` table stores price ranges per product and brand with overlapping time windows.

| Field        | Description                                                             |
|--------------|-------------------------------------------------------------------------|
| `BRAND_ID`   | Brand foreign key (1 = ZARA)                                            |
| `START_DATE` | Range start — inclusive                                                  |
| `END_DATE`   | Range end — inclusive                                                    |
| `PRICE_LIST` | Price list identifier                                                    |
| `PRODUCT_ID` | Product identifier                                                       |
| `PRIORITY`   | Disambiguation: when ranges overlap, the highest priority value wins     |
| `PRICE`      | Final applicable price                                                   |
| `CURR`       | ISO 4217 currency code                                                   |

### Priority resolution

Priority is resolved at the database level via `ORDER BY priority DESC LIMIT 1`. This avoids fetching multiple rows and filtering in application memory, keeping the query O(log n) with a proper composite index on `(product_id, brand_id, start_date, end_date)`.

### Seed data

The service initializes with the following dataset on startup:

| brandId | startDate           | endDate             | priceList | productId | priority | price | currency |
|---------|---------------------|---------------------|-----------|-----------|----------|-------|----------|
| 1       | 2020-06-14 00:00:00 | 2020-12-31 23:59:59 | 1         | 35455     | 0        | 35.50 | EUR      |
| 1       | 2020-06-14 15:00:00 | 2020-06-14 18:30:00 | 2         | 35455     | 1        | 25.45 | EUR      |
| 1       | 2020-06-15 00:00:00 | 2020-06-15 11:00:00 | 3         | 35455     | 1        | 30.50 | EUR      |
| 1       | 2020-06-15 16:00:00 | 2020-12-31 23:59:59 | 4         | 35455     | 1        | 38.95 | EUR      |

---

## Configuration

| Property                              | Default                        | Description                             |
|---------------------------------------|--------------------------------|-----------------------------------------|
| `server.port`                         | `8080`                         | HTTP server port                        |
| `spring.jpa.open-in-view`             | `false`                        | Disabled to avoid lazy-load anti-pattern|
| `spring.jpa.hibernate.ddl-auto`       | `none`                         | Schema managed by `schema.sql`          |
| `spring.mvc.problemdetails.enabled`   | `true`                         | RFC 7807 error format                   |
| `springdoc.swagger-ui.path`           | `/swagger-ui.html`             | OpenAPI UI path                         |
| `management.endpoints.web.exposure`   | `health, info, metrics`        | Actuator endpoints                      |

Sensitive overrides (datasource credentials, ports) should be provided via environment variables in non-local environments, never committed to source control.

---

## Running Tests

```bash
# All tests
./mvnw test

# Only integration tests
./mvnw test -Dtest=PriceControllerIntegrationTest

# With coverage report (target/site/jacoco/index.html)
./mvnw verify
```

### Test scenarios covered

The five required scenarios are validated via `@ParameterizedTest`:

| # | Date              | Expected priceList | Expected price |
|---|-------------------|--------------------|----------------|
| 1 | 2020-06-14 10:00  | 1                  | 35.50          |
| 2 | 2020-06-14 16:00  | 2                  | 25.45          |
| 3 | 2020-06-14 21:00  | 1                  | 35.50          |
| 4 | 2020-06-15 10:00  | 3                  | 30.50          |
| 5 | 2020-06-16 21:00  | 4                  | 38.95          |

---

## Design Decisions

**Why Hexagonal Architecture?**  
The domain is completely decoupled from Spring, JPA, and HTTP. Replacing H2 with PostgreSQL, or REST with GraphQL, requires touching only the infrastructure adapters — not a single line of business logic.

**Why resolve priority in the database?**  
Fetching a single row with `ORDER BY priority DESC LIMIT 1` is more efficient than loading all candidates and filtering in Java. It also makes the intent explicit at the query level.

**Why `PriceNotFoundException` as a `RuntimeException`?**  
The caller (use case) is not expected to handle this — it represents a business rule violation that the HTTP layer converts to a 404. Checked exceptions would pollute every layer of the call stack.

**Why `open-in-view: false`?**  
The default `true` keeps the JPA session open for the entire HTTP request lifecycle, which can cause unintended lazy-loading outside a transactional context and connection pool exhaustion under load.

**Why Java 21 Records for DTOs and value objects?**  
Records provide immutability, structural equality, and `toString()` out of the box with zero boilerplate. `PriceQuery` and `Price` as records make accidental mutation impossible.

---

## Actuator endpoints

| Endpoint              | Description            |
|-----------------------|------------------------|
| `GET /actuator/health`  | Service health check   |
| `GET /actuator/info`    | Application metadata   |
| `GET /actuator/metrics` | Runtime metrics        |