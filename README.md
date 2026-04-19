# price-service

[![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Build](https://img.shields.io/badge/Build-Gradle-blue?logo=gradle)](https://gradle.org/)
[![Architecture](https://img.shields.io/badge/Architecture-Hexagonal-blueviolet)]()
[![License](https://img.shields.io/badge/License-MIT-lightgrey)]()

Microservicio Spring Boot que resuelve el **precio aplicable** para un producto dentro de una cadena, dada una fecha de consulta. Cuando múltiples tarifas se solapan en el tiempo, se aplica la de **mayor prioridad**.

---

## Tabla de Contenidos

- [Arquitectura](#arquitectura)
- [Stack Tecnológico](#stack-tecnológico)
- [Prerequisitos](#prerequisitos)
- [Levantar el servicio](#levantar-el-servicio)
- [Docker](#docker)
- [API Reference](#api-reference)
- [Reglas de Negocio](#reglas-de-negocio)
- [Base de Datos](#base-de-datos)
- [Configuración](#configuración)
- [Tests](#tests)
- [Decisiones de Diseño](#decisiones-de-diseño)
- [Actuator](#actuator)

---

## Arquitectura

El servicio sigue **Arquitectura Hexagonal** (Ports & Adapters). El dominio es completamente agnóstico a Spring, JPA y HTTP. Los adaptadores dependen del dominio; el dominio nunca depende de los adaptadores.

```
com.inditex.price
│
├── domain/                              ← Núcleo — cero dependencias externas
│   ├── model/
│   │   ├── Price.java                   # Modelo de dominio (Java record)
│   │   └── PriceQuery.java              # Value object con los criterios de búsqueda
│   ├── port/
│   │   ├── inbound/
│   │   │   └── GetApplicablePricePort.java   # Puerto de entrada (driven)
│   │   └── outbound/
│   │       └── PriceRepositoryPort.java      # Puerto de salida (driving)
│   └── exception/
│       └── PriceNotFoundException.java
│
├── application/                         ← Orquestación de casos de uso
│   └── usecase/
│       └── GetApplicablePriceUseCase.java
│
└── infrastructure/                      ← Adaptadores — todo lo externo
    ├── adapter/
    │   ├── inbound/
    │   │   └── rest/
    │   │       ├── PriceController.java
    │   │       ├── dto/                 # PriceRequest, PriceResponse (records)
    │   │       └── mapper/              # PriceMapper (MapStruct)
    │   ├── outbound/
    │   │   └── persistence/
    │   │       ├── PriceJpaRepository.java
    │   │       ├── PriceRepositoryAdapter.java
    │   │       ├── entity/              # PriceEntity (@Entity)
    │   │       └── mapper/              # EntityMapper (MapStruct)
    │   └── exception/
    │       └── GlobalExceptionHandler.java  # @RestControllerAdvice + RFC 7807
    └── config/
        ├── OpenApiConfig.java
        └── LoggingAspect.java           # AOP — logging transversal
```

### Flujo de dependencias

```
HTTP Request
    │
    ▼
PriceController          (adapter inbound)
    │  usa puerto
    ▼
GetApplicablePricePort   (inbound port — interfaz de dominio)
    │  implementado por
    ▼
GetApplicablePriceUseCase  (application layer)
    │  usa puerto
    ▼
PriceRepositoryPort      (outbound port — interfaz de dominio)
    │  implementado por
    ▼
PriceRepositoryAdapter   (adapter outbound → JPA → H2)
```

---

## Stack Tecnológico

| Componente       | Tecnología                         | Versión   |
|------------------|------------------------------------|-----------|
| Lenguaje         | Java                               | 21        |
| Framework        | Spring Boot                        | 3.5.x     |
| Persistencia     | Spring Data JPA + H2 (in-memory)   | —         |
| Migraciones DB   | Flyway                             | —         |
| Mapeo            | MapStruct                          | 1.6.3     |
| Boilerplate      | Lombok                             | —         |
| Documentación    | SpringDoc OpenAPI (Swagger UI)     | 2.8.x     |
| Observabilidad   | Spring Actuator + AOP Logging      | —         |
| Testing          | JUnit 5 + Mockito + MockMvc        | —         |
| Build            | Gradle (Kotlin DSL)                | —         |
| Contenedor       | Docker (multi-stage, Alpine)       | —         |

---

## Prerequisitos

- **Java 21** (JDK)
- **Gradle** (o usar el wrapper incluido `./gradlew`)
- **Docker** (opcional, para ejecución en contenedor)

---

## Levantar el servicio

### Compilar y ejecutar localmente

```bash
# Clonar el repositorio
git clone https://github.com/AndresVilla11/price-service.git
cd price-service

# Ejecutar (el wrapper de Gradle descarga todo automáticamente)
./gradlew bootRun
```

El servicio arranca en `http://localhost:8080`.

### Compilar el JAR

```bash
./gradlew bootJar
java -jar build/libs/price-service.jar
```

### Herramientas disponibles en local

| Herramienta      | URL                                      |
|------------------|------------------------------------------|
| Swagger UI       | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON     | http://localhost:8080/v3/api-docs        |
| H2 Console       | http://localhost:8080/h2-console         |
| Health check     | http://localhost:8080/actuator/health    |

> **H2 Console** — JDBC URL: `jdbc:h2:mem:pricing-db` · Usuario: `sa` · Contraseña: _(vacía)_

---

## Docker

El proyecto incluye un `Dockerfile` **multi-stage** optimizado con capas de Spring Boot y usuario no-root.

```bash
# Construir la imagen
docker build -t price-service:latest .

# Ejecutar
docker run -p 8080:8080 price-service:latest
```

### Parámetros de JVM configurados en contenedor

```
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
-Djava.security.egd=file:/dev/./urandom
```

---

## API Reference

### `GET /prices`

Devuelve el precio aplicable para un producto, cadena y fecha dados.

#### Parámetros de entrada

| Parámetro         | Tipo            | Obligatorio | Descripción                                         |
|-------------------|-----------------|-------------|-----------------------------------------------------|
| `productId`       | `Integer`       | ✅          | Identificador del producto (ej. `35455`)            |
| `brandId`         | `Integer`       | ✅          | Identificador de la cadena (`1` = ZARA)             |
| `applicationDate` | `LocalDateTime` | ✅          | Fecha de consulta en ISO 8601 (ej. `2020-06-14T10:00:00`) |

#### Respuestas

| Código | Descripción                                         |
|--------|-----------------------------------------------------|
| `200`  | Precio aplicable encontrado                         |
| `400`  | Parámetros inválidos o ausentes                     |
| `404`  | No existe precio aplicable para los criterios dados |
| `500`  | Error interno inesperado                            |

#### Ejemplo — 200 OK

```bash
curl -X GET "http://localhost:8080/prices?productId=35455&brandId=1&applicationDate=2020-06-14T10:00:00" \
     -H "Accept: application/json"
```

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

#### Ejemplo — 404 Not Found (RFC 7807 / ProblemDetail)

```json
{
  "type":      "https://api.inditex.com/errors/price-not-found",
  "title":     "Price Not Found",
  "status":    404,
  "detail":    "No applicable price found for productId=35455, brandId=1, date=2020-06-14T10:00",
  "timestamp": "2024-01-15T10:23:45.123Z",
  "path":      "/prices"
}
```

#### Ejemplo — 400 Bad Request

```json
{
  "type":       "https://api.inditex.com/errors/validation-error",
  "title":      "Invalid Request",
  "status":     400,
  "detail":     "Request validation failed",
  "timestamp":  "2024-01-15T10:23:45.123Z",
  "path":       "/prices",
  "violations": {
    "productId": "must be greater than 0"
  }
}
```

---

## Reglas de Negocio

La tabla `PRICES` almacena rangos de precios por producto y cadena con ventanas temporales que pueden solaparse.

| Campo        | Descripción                                                                  |
|--------------|------------------------------------------------------------------------------|
| `BRAND_ID`   | Foreign key de la cadena (`1` = ZARA)                                        |
| `START_DATE` | Inicio del rango de validez — inclusivo                                      |
| `END_DATE`   | Fin del rango de validez — inclusivo                                         |
| `PRICE_LIST` | Identificador de la tarifa aplicable                                         |
| `PRODUCT_ID` | Identificador del producto                                                   |
| `PRIORITY`   | Desambiguador: cuando dos tarifas se solapan, gana la de mayor valor numérico |
| `PRICE`      | Precio final de venta                                                        |
| `CURR`       | Código de moneda ISO 4217                                                    |

### Resolución de prioridad

La resolución se delega completamente a la base de datos mediante una query JPQL con `ORDER BY priority DESC LIMIT 1`. Esto evita traer múltiples filas a la aplicación y hace el intent explícito a nivel de consulta, con complejidad O(log n) gracias al índice compuesto.

---

## Base de Datos

### Esquema (`V1__create_prices_table.sql`)

Gestionado mediante **Flyway** (versionado y reproducible).

```sql
CREATE TABLE PRICES (
    ID         BIGINT AUTO_INCREMENT PRIMARY KEY,
    BRAND_ID   INT            NOT NULL,
    PRODUCT_ID INT            NOT NULL,
    PRICE_LIST INT            NOT NULL,
    PRIORITY   INT            NOT NULL,
    START_DATE TIMESTAMP      NOT NULL,
    END_DATE   TIMESTAMP      NOT NULL,
    PRICE      DECIMAL(10, 2) NOT NULL,
    CURR       VARCHAR(3)     NOT NULL,

    CONSTRAINT CHK_PRICE_POSITIVE CHECK (PRICE >= 0),
    CONSTRAINT CHK_DATE_RANGE     CHECK (END_DATE >= START_DATE)
);

-- Índice compuesto para la consulta de lookup (cubre el WHERE y el ORDER BY)
CREATE INDEX IDX_PRICES_LOOKUP ON PRICES (PRODUCT_ID, BRAND_ID, START_DATE, END_DATE, PRIORITY);
CREATE INDEX IDX_PRICES_DATES  ON PRICES (START_DATE, END_DATE);
```

### Datos iniciales (`V2__insert_prices.sql`)

| BRAND_ID | START_DATE          | END_DATE            | PRICE_LIST | PRODUCT_ID | PRIORITY | PRICE | CURR |
|----------|---------------------|---------------------|------------|------------|----------|-------|------|
| 1        | 2020-06-14 00:00:00 | 2020-12-31 23:59:59 | 1          | 35455      | 0        | 35.50 | EUR  |
| 1        | 2020-06-14 15:00:00 | 2020-06-14 18:30:00 | 2          | 35455      | 1        | 25.45 | EUR  |
| 1        | 2020-06-15 00:00:00 | 2020-06-15 11:00:00 | 3          | 35455      | 1        | 30.50 | EUR  |
| 1        | 2020-06-15 16:00:00 | 2020-12-31 23:59:59 | 4          | 35455      | 1        | 38.95 | EUR  |

---

## Configuración

Propiedades relevantes del `application.yml`:

| Propiedad                              | Valor por defecto             | Descripción                                    |
|----------------------------------------|-------------------------------|------------------------------------------------|
| `server.port`                          | `8080`                        | Puerto HTTP                                    |
| `server.shutdown`                      | `graceful`                    | Apagado graceful (drena requests en vuelo)     |
| `spring.threads.virtual.enabled`       | `true`                        | Virtual threads (Project Loom — Java 21)       |
| `spring.jpa.open-in-view`             | `false`                       | Deshabilitado para evitar lazy-load fuera de TX|
| `spring.jpa.hibernate.ddl-auto`       | `validate`                    | Schema gestionado por Flyway                   |
| `spring.flyway.enabled`               | `true`                        | Migraciones activas en arranque                |
| `management.endpoints.web.exposure`   | `health, info, metrics`       | Endpoints de Actuator expuestos                |

> En entornos no locales, las credenciales y URLs del datasource deben proveerse como variables de entorno, nunca commiteadas.

---

## Tests

### Ejecutar todos los tests

```bash
./gradlew test
```

### Estructura de tests

El proyecto cuenta con tres niveles de testing:

#### Tests Unitarios

- `GetApplicablePriceUseCaseTest` — Verifica la lógica del use case de forma aislada con Mockito.
- `PriceRepositoryAdapterTest` — Verifica el adaptador de persistencia en forma unitaria.

#### Tests de Integración

- `PriceControllerIntegrationTest` — Levanta solo la capa web (`@WebMvcTest`) y mockea el puerto de entrada.
- `PriceRepositoryAdapterIntegrationTest` — Verifica la query JPA contra H2 real (`@DataJpaTest`).

#### Tests de Sistema (End-to-End)

- `PriceSystemTest` — Carga el contexto completo de Spring Boot (`@SpringBootTest`) con MockMvc y H2 real, cubriendo los 5 escenarios requeridos:

| Test | Fecha consulta        | Tarifa esperada | Precio esperado |
|------|-----------------------|-----------------|-----------------|
| 1    | 2020-06-14 10:00:00   | 1               | 35.50 €         |
| 2    | 2020-06-14 16:00:00   | 2               | 25.45 €         |
| 3    | 2020-06-14 21:00:00   | 1               | 35.50 €         |
| 4    | 2020-06-15 10:00:00   | 3               | 30.50 €         |
| 5    | 2020-06-16 21:00:00   | 4               | 38.95 €         |

---

## Decisiones de Diseño

**¿Por qué Arquitectura Hexagonal?**  
El dominio es completamente intercambiable. Reemplazar H2 por PostgreSQL, o REST por gRPC, requiere tocar únicamente los adaptadores de infraestructura — ni una sola línea de lógica de negocio.

**¿Por qué resolver prioridad en la base de datos?**  
`ORDER BY priority DESC LIMIT 1` trae una única fila y hace el intent explícito en la query. Traer todos los candidatos y filtrar en Java sería ineficiente y desplazaría la lógica fuera de su capa natural.

**¿Por qué Java 21 Records para DTOs y modelos de dominio?**  
Los records proveen inmutabilidad, igualdad estructural y `toString()` sin boilerplate. `Price`, `PriceQuery`, `PriceRequest` y `PriceResponse` como records hacen imposible la mutación accidental.

**¿Por qué Virtual Threads (Project Loom)?**  
Con `spring.threads.virtual.enabled=true` y `server.shutdown=graceful`, el servicio puede manejar alta concurrencia de I/O con overhead mínimo de hilos, alineado con las capacidades de Java 21.

**¿Por qué `open-in-view: false`?**  
El valor por defecto `true` mantiene la sesión JPA abierta durante todo el request HTTP, lo que puede causar lazy-loading no intencionado fuera de un contexto transaccional y agotamiento del connection pool bajo carga.

**¿Por qué `PriceNotFoundException` es una `RuntimeException`?**  
Representa una violación de regla de negocio que la capa HTTP convierte a un 404 mediante el `@RestControllerAdvice`. Las checked exceptions contaminarían cada capa del call stack innecesariamente.

**¿Por qué RFC 7807 (`ProblemDetail`)?**  
Estándar de la industria para respuestas de error HTTP estructuradas. Spring Boot 3.x lo soporta nativamente y proporciona un contrato claro para los consumidores de la API.

**¿Por qué Flyway para migraciones?**  
Garantiza reproducibilidad total del esquema en cualquier entorno (local, CI, producción). Cada cambio en el schema queda versionado, auditado y es reversible.

---

## Actuator

| Endpoint                  | Descripción               |
|---------------------------|---------------------------|
| `GET /actuator/health`    | Estado de salud del servicio |
| `GET /actuator/info`      | Metadata de la aplicación |
| `GET /actuator/metrics`   | Métricas de runtime       |