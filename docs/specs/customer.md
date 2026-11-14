# Spec: Customer

Fuente de verdad del comportamiento del módulo de clientes.
Cualquier cambio de comportamiento empieza actualizando este archivo.

## Entidad

- `id`: Long, generado por el sistema
- `firstName`: String, obligatorio, 2–50 caracteres
- `lastName`: String, obligatorio, 2–50 caracteres
- `email`: String, obligatorio, formato válido, máximo 255, único (se guarda en minúsculas)
- `phone`: String, opcional, formato `+?[0-9]{7,15}`
- `active`: boolean, por defecto `true`
- `createdAt`: Instant, generado al crear

## Operaciones

### CUST-01 Crear cliente
- `POST /api/customers`
- Body: `firstName`, `lastName`, `email`, `phone` (opcional)
- 201 con header `Location` y el cliente creado
- 400 si la validación falla
- 409 si el email ya existe

### CUST-02 Obtener cliente
- `GET /api/customers/{id}`
- 200 con el cliente (también si está inactivo)
- 404 si no existe

### CUST-03 Listar clientes
- `GET /api/customers?active=true|false` (filtro opcional)
- 200 con la lista ordenada por `lastName`

### CUST-04 Actualizar cliente
- `PUT /api/customers/{id}` (reemplazo completo de los campos editables)
- 200 con el cliente actualizado
- 400 si la validación falla
- 404 si no existe
- 409 si el email pertenece a otro cliente

### CUST-05 Borrado lógico
- `DELETE /api/customers/{id}`
- 204; marca `active = false` y no elimina el registro
- 404 si no existe
- Borrar un cliente ya inactivo también responde 204 (la operación es idempotente)

## Formato de errores

Todas las respuestas de error usan RFC 7807 (`ProblemDetail`).
La validación de campos incluye la propiedad `errors` con el detalle por campo.

## Trazabilidad

Cada test de `CustomerApiTest` incluye el ID de la operación que verifica (`CUST-0X`) en su `@DisplayName`.
Si se agrega una operación a esta spec, debe tener al menos un test con su ID.
