# Customer_SDD

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.4-6DB33F?logo=springboot&logoColor=white) ![H2](https://img.shields.io/badge/H2-in--memory-1F6FEB?logoColor=white)

Customer CRUD REST API built with Spring Boot 3.5 and Java 21, following **Spec-Driven Development (SDD)**: every operation is defined first in a written spec, then covered by tests that carry the spec ID, and only then implemented.

Project page: https://josesaid.github.io/Customer_SDD/

## Overview

- The spec in [`docs/specs/customer.md`](docs/specs/customer.md) is the source of truth. It defines the entity, operations `CUST-01` to `CUST-05`, the response codes and the error format.
- Each test in `CustomerApiTest` names the operation it verifies (`CUST-0X`) in its `@DisplayName`, which gives traceability from spec to test.
- Behavior changes start in the spec, then the tests, then the code.

## Stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 (Web, Data JPA, Validation) |
| Build | Maven |
| Database | H2 in-memory (`jdbc:h2:mem:customersdb`) |
| Errors | RFC 7807 `ProblemDetail` |
| Tests | JUnit 5, Spring Boot Test, MockMvc |
| Spring AI | Not used |
| Messaging | Not used |
| Cloud | Not used |
| Docker | Not used |

## Architecture

```
HTTP client
   │
   ▼
CustomerController  ──►  CustomerService  ──►  CustomerRepository  ──►  H2
   (REST, validation)     (rules, transactions)   (Spring Data JPA)
   │
   └── GlobalExceptionHandler (404 / 409 / 400 as ProblemDetail)
```

## Components and classes

All classes live in `com.mx.development.jose.said`.

| Class | Role |
|---|---|
| `CustomersApplication` | Spring Boot entry point |
| `Customer` | JPA entity (`customers` table); `update(...)` and `deactivate()` for CUST-04 and CUST-05 |
| `CustomerRequest` | Request body record with Bean Validation rules |
| `CustomerResponse` | Response record built from the entity |
| `CustomerRepository` | Spring Data JPA repository (email uniqueness checks, sorting by last name, active filter) |
| `CustomerService` | Business rules: email normalization to lowercase, duplicate checks, logical delete |
| `CustomerController` | REST endpoints under `/api/customers` |
| `GlobalExceptionHandler` | Maps exceptions to RFC 7807 responses, with per-field `errors` on validation failures |
| `CustomerNotFoundException`, `DuplicateEmailException` | Domain exceptions (404 and 409) |

## Persistence

- H2 in-memory database, schema created by Hibernate (`ddl-auto: create-drop`), so data is lost on restart.
- Entity fields: `id`, `firstName` (2–50), `lastName` (2–50), `email` (unique, stored lowercase, max 255), `phone` (optional, `+?[0-9]{7,15}`), `active` (default `true`), `createdAt`.
- Delete is logical: it sets `active = false` and keeps the row.
- H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:customersdb`, user `sa`, empty password; local development only).

## Configuration

No environment variables are required. All settings are in `src/main/resources/application.yml`.

| Property | Value | Purpose |
|---|---|---|
| `spring.datasource.url` | `jdbc:h2:mem:customersdb;DB_CLOSE_DELAY=-1` | In-memory database |
| `spring.h2.console.enabled` | `true` | H2 web console |
| `spring.jpa.hibernate.ddl-auto` | `create-drop` | Schema generated at startup |
| `spring.jpa.open-in-view` | `false` | No lazy loading in the web layer |

## Endpoints

| ID | Method | Path | Success | Errors |
|---|---|---|---|---|
| CUST-01 | `POST` | `/api/customers` | 201 + `Location` | 400 validation, 409 duplicate email |
| CUST-02 | `GET` | `/api/customers/{id}` | 200 (also for inactive customers) | 404 |
| CUST-03 | `GET` | `/api/customers?active=true\|false` | 200, sorted by `lastName` | — |
| CUST-04 | `PUT` | `/api/customers/{id}` | 200 | 400, 404, 409 email used by another customer |
| CUST-05 | `DELETE` | `/api/customers/{id}` | 204, logical and idempotent | 404 |

Example:

```bash
curl -i -X POST localhost:8080/api/customers \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Ana","lastName":"Lopez","email":"ana@example.com","phone":"+5213312345678"}'
```

## Run

```bash
mvn test                  # runs the spec tests
mvn spring-boot:run       # starts the app on http://localhost:8080
```

## Tests

`CustomerApiTest` is a `@SpringBootTest` with MockMvc and 14 tests covering every operation in the spec, including validation errors, duplicate emails, filtering, sorting and idempotent logical delete.

## SDD workflow

1. Change `docs/specs/customer.md` first.
2. Add or adjust tests with the operation ID in `@DisplayName`.
3. Implement until `mvn test` passes.

## Known limitations

- In-memory database only; no persistent storage or migrations.
- No authentication or authorization.
- No pagination on the list endpoint.
- No Docker image or deployment configuration.
