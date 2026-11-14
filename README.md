# Customer_SDD

CRUD de clientes en Spring Boot 3.5 / Java 21, con Maven. Las operaciones se definen primero en la spec y el código y los tests siguen a la spec.

## Estructura

- `docs/specs/customer.md`: spec del módulo (fuente de verdad). Define la entidad, las operaciones CUST-01 a CUST-05 y los códigos de respuesta.
- `src/main/java/com/example/customers/`: implementación.
- `src/test/java/com/example/customers/CustomerApiTest.java`: un test (o más) por operación, con el ID de la spec en `@DisplayName`.

## Flujo SDD

1. Cambiar primero `docs/specs/customer.md`.
2. Ajustar o agregar tests con el ID de la operación.
3. Implementar hasta que `mvn test` pase.

## Ejecutar

```bash
mvn test                  # ejecuta los tests de la spec
mvn spring-boot:run       # levanta la app en http://localhost:8080
```

La base de datos es H2 en memoria. La consola está en `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:customersdb`, usuario `sa`, sin contraseña).

## Ejemplo

```bash
curl -i -X POST localhost:8080/api/customers \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Ana","lastName":"Lopez","email":"ana@example.com","phone":"+5213312345678"}'
```
