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
 * Tests that verify the criteria in docs/specs/customer.md.
 * The operation ID in each @DisplayName provides traceability to the spec.
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

    // ---- CUST-01 Create ----

    @Test
    @DisplayName("CUST-01: creating a customer returns 201 with Location and lowercases the email")
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
    @DisplayName("CUST-01: invalid body returns 400 with per-field errors")
    void createInvalidReturns400() throws Exception {
        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "A", "lastName": "", "email": "not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("CUST-01: duplicate email returns 409")
    void createDuplicateEmailReturns409() throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    // ---- CUST-02 Get ----

    @Test
    @DisplayName("CUST-02: getting an existing customer returns 200")
    void getExistingReturns200() throws Exception {
        long id = createAndGetId(VALID_BODY);

        mvc.perform(get("/api/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.lastName").value("Lopez"));
    }

    @Test
    @DisplayName("CUST-02: getting a missing customer returns 404")
    void getMissingReturns404() throws Exception {
        mvc.perform(get("/api/customers/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Customer not found"));
    }

    // ---- CUST-03 List ----

    @Test
    @DisplayName("CUST-03: listing with active=true excludes inactive customers")
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
    @DisplayName("CUST-03: listing without a filter returns all customers sorted by last name")
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

    // ---- CUST-04 Update ----

    @Test
    @DisplayName("CUST-04: updating a customer returns 200 with the new data")
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
    @DisplayName("CUST-04: updating with an invalid body returns 400")
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
    @DisplayName("CUST-04: updating a missing customer returns 404")
    void updateMissingReturns404() throws Exception {
        mvc.perform(put("/api/customers/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CUST-04: using another customer's email returns 409")
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

    // ---- CUST-05 Logical delete ----

    @Test
    @DisplayName("CUST-05: deleting a customer returns 204 and marks it inactive without removing it")
    void deleteIsLogical() throws Exception {
        long id = createAndGetId(VALID_BODY);

        mvc.perform(delete("/api/customers/{id}", id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("CUST-05: deleting a missing customer returns 404")
    void deleteMissingReturns404() throws Exception {
        mvc.perform(delete("/api/customers/{id}", 999999L)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CUST-05: deleting an already inactive customer returns 204 (idempotent)")
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
