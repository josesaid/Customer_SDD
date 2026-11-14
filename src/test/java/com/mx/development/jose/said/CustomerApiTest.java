package com.mx.development.jose.said;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests que verifican los criterios de docs/specs/customer.md.
 * El ID de la operación en cada @DisplayName es la trazabilidad con la spec.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CustomerApiTest {

    private static final String VALID_BODY = """
            {"firstName": "Ana", "lastName": "Lopez", "email": "ana@example.com", "phone": "+5213312345678"}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CustomerRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    // ---- CUST-01 Crear ----

    @Test
    @DisplayName("CUST-01: crear cliente responde 201 con Location y normaliza el email a minúsculas")
    void createReturns201() throws Exception {
        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Ana", "lastName": "Lopez", "email": "ANA@Example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("CUST-01: body inválido responde 400 con errores por campo")
    void createInvalidReturns400() throws Exception {
        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "A", "lastName": "", "email": "no-es-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("CUST-01: email duplicado responde 409")
    void createDuplicateEmailReturns409() throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    // ---- CUST-02 Obtener ----

    @Test
    @DisplayName("CUST-02: obtener cliente existente responde 200")
    void getExistingReturns200() throws Exception {
        long id = createAndGetId(VALID_BODY);

        mvc.perform(get("/api/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.lastName").value("Lopez"));
    }

    @Test
    @DisplayName("CUST-02: obtener cliente inexistente responde 404")
    void getMissingReturns404() throws Exception {
        mvc.perform(get("/api/customers/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Customer not found"));
    }

    // ---- CUST-03 Listar ----

    @Test
    @DisplayName("CUST-03: listar con active=true excluye clientes inactivos")
    void listActiveFilterExcludesInactive() throws Exception {
        long activeId = createAndGetId(VALID_BODY);
        long inactiveId = createAndGetId("""
                {"firstName": "Luis", "lastName": "Perez", "email": "luis@example.com"}
                """);
        mvc.perform(delete("/api/customers/{id}", inactiveId)).andExpect(status().isNoContent());

        mvc.perform(get("/api/customers").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(activeId));
    }

    @Test
    @DisplayName("CUST-03: listar sin filtro devuelve todos ordenados por apellido")
    void listWithoutFilterReturnsAllSorted() throws Exception {
        createAndGetId(VALID_BODY); // Lopez
        createAndGetId("""
                {"firstName": "Luis", "lastName": "Atencio", "email": "luis@example.com"}
                """);

        mvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].lastName").value("Atencio"))
                .andExpect(jsonPath("$[1].lastName").value("Lopez"));
    }

    // ---- CUST-04 Actualizar ----

    @Test
    @DisplayName("CUST-04: actualizar cliente responde 200 con los datos nuevos")
    void updateReturns200() throws Exception {
        long id = createAndGetId(VALID_BODY);

        mvc.perform(put("/api/customers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Ana", "lastName": "Ramirez", "email": "ana@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Ramirez"));
    }

    @Test
    @DisplayName("CUST-04: actualizar con body inválido responde 400")
    void updateInvalidReturns400() throws Exception {
        long id = createAndGetId(VALID_BODY);

        mvc.perform(put("/api/customers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "", "lastName": "Ramirez", "email": "ana@example.com"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CUST-04: actualizar cliente inexistente responde 404")
    void updateMissingReturns404() throws Exception {
        mvc.perform(put("/api/customers/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CUST-04: usar el email de otro cliente responde 409")
    void updateWithOtherCustomersEmailReturns409() throws Exception {
        createAndGetId(VALID_BODY); // ana@example.com
        long otherId = createAndGetId("""
                {"firstName": "Luis", "lastName": "Perez", "email": "luis@example.com"}
                """);

        mvc.perform(put("/api/customers/{id}", otherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    // ---- CUST-05 Borrado lógico ----

    @Test
    @DisplayName("CUST-05: borrar cliente responde 204 y lo marca inactivo sin eliminarlo")
    void deleteIsLogical() throws Exception {
        long id = createAndGetId(VALID_BODY);

        mvc.perform(delete("/api/customers/{id}", id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("CUST-05: borrar cliente inexistente responde 404")
    void deleteMissingReturns404() throws Exception {
        mvc.perform(delete("/api/customers/{id}", 999999L)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CUST-05: borrar un cliente ya inactivo responde 204 (idempotente)")
    void deleteIsIdempotent() throws Exception {
        long id = createAndGetId(VALID_BODY);
        mvc.perform(delete("/api/customers/{id}", id)).andExpect(status().isNoContent());

        mvc.perform(delete("/api/customers/{id}", id)).andExpect(status().isNoContent());
    }

    // ---- helpers ----

    private long createAndGetId(String body) throws Exception {
        String json = mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.parse(json).read("$.id", Long.class);
    }
}
