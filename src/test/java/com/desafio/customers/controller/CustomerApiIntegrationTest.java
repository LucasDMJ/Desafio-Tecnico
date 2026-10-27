package com.desafio.customers.controller;

import com.desafio.customers.domain.Customer;
import com.desafio.customers.domain.CustomerStatus;
import com.desafio.customers.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerApiIntegrationTest {

    private static final String VALID_BODY = """
            {"name":"Joao da Silva","cpf":"12345678901","email":"joao@email.com","status":"ACTIVE"}
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    CustomerRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    private Customer save(String name, String cpf, CustomerStatus status) {
        Customer c = new Customer();
        c.setName(name);
        c.setCpf(cpf);
        c.setEmail(name.replace(" ", ".").toLowerCase() + "@email.com");
        c.setStatus(status);
        return repository.save(c);
    }

    // ---------- Seguranca ----------

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mvc.perform(get("/customers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        mvc.perform(get("/customers").with(httpBasic("admin", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userShouldBeAbleToQuery() throws Exception {
        mvc.perform(get("/customers").with(httpBasic("user", "user123")))
                .andExpect(status().isOk());
    }

    @Test
    void userShouldNotCreateUpdateOrDelete() throws Exception {
        Customer c = save("Maria", "11122233344", CustomerStatus.ACTIVE);

        mvc.perform(post("/customers").with(httpBasic("user", "user123"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mvc.perform(put("/customers/{id}", c.getId()).with(httpBasic("user", "user123"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/customers/{id}", c.getId()).with(httpBasic("user", "user123")))
                .andExpect(status().isForbidden());
    }

    // ---------- Criacao ----------

    @Test
    void adminShouldCreateCustomer() throws Exception {
        mvc.perform(post("/customers").with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/customers/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Joao da Silva"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldDefaultStatusToActive() throws Exception {
        String body = """
                {"name":"Ana","cpf":"99988877766","email":"ana@email.com"}
                """;
        mvc.perform(post("/customers").with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturn400WhenRequiredFieldsAreMissingOrInvalid() throws Exception {
        String body = """
                {"name":"","cpf":"123","email":"not-an-email"}
                """;
        mvc.perform(post("/customers").with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasSize(3)))
                .andExpect(jsonPath("$.details", hasItem(containsString("cpf"))))
                .andExpect(jsonPath("$.details", hasItem(containsString("email"))))
                .andExpect(jsonPath("$.details", hasItem(containsString("name"))));
    }

    @Test
    void shouldReturn400WhenBodyIsMalformed() throws Exception {
        mvc.perform(post("/customers").with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenStatusIsInvalid() throws Exception {
        String body = """
                {"name":"Ana","cpf":"99988877766","email":"ana@email.com","status":"WHATEVER"}
                """;
        mvc.perform(post("/customers").with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409WhenCpfAlreadyRegistered() throws Exception {
        save("Existing", "12345678901", CustomerStatus.ACTIVE);

        mvc.perform(post("/customers").with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ---------- Alteracao ----------

    @Test
    void adminShouldUpdateCustomer() throws Exception {
        Customer c = save("Old Name", "12345678901", CustomerStatus.ACTIVE);
        String body = """
                {"name":"New Name","cpf":"12345678901","email":"new@email.com","status":"INACTIVE"}
                """;
        mvc.perform(put("/customers/{id}", c.getId()).with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.email").value("new@email.com"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void shouldReturn404WhenUpdatingUnknownCustomer() throws Exception {
        mvc.perform(put("/customers/{id}", 9999).with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenUpdatingToCpfOfAnotherCustomer() throws Exception {
        save("First", "11111111111", CustomerStatus.ACTIVE);
        Customer second = save("Second", "22222222222", CustomerStatus.ACTIVE);
        String body = """
                {"name":"Second","cpf":"11111111111","email":"second@email.com"}
                """;
        mvc.perform(put("/customers/{id}", second.getId()).with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    // ---------- Exclusao ----------

    @Test
    void adminShouldDeleteCustomer() throws Exception {
        Customer c = save("To Delete", "12345678901", CustomerStatus.ACTIVE);

        mvc.perform(delete("/customers/{id}", c.getId()).with(httpBasic("admin", "admin123")))
                .andExpect(status().isNoContent());

        mvc.perform(get("/customers/{id}", c.getId()).with(httpBasic("admin", "admin123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDeletingUnknownCustomer() throws Exception {
        mvc.perform(delete("/customers/{id}", 9999).with(httpBasic("admin", "admin123")))
                .andExpect(status().isNotFound());
    }

    // ---------- Consultas ----------

    @Test
    void shouldGetCustomerById() throws Exception {
        Customer c = save("Maria Souza", "12345678901", CustomerStatus.ACTIVE);

        mvc.perform(get("/customers/{id}", c.getId()).with(httpBasic("user", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(c.getId()))
                .andExpect(jsonPath("$.cpf").value("12345678901"));
    }

    @Test
    void shouldReturn404WhenCustomerDoesNotExist() throws Exception {
        mvc.perform(get("/customers/{id}", 9999).with(httpBasic("user", "user123")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldListAllCustomers() throws Exception {
        save("A", "11111111111", CustomerStatus.ACTIVE);
        save("B", "22222222222", CustomerStatus.INACTIVE);

        mvc.perform(get("/customers").with(httpBasic("user", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldFilterByStatus() throws Exception {
        save("Active One", "11111111111", CustomerStatus.ACTIVE);
        save("Active Two", "22222222222", CustomerStatus.ACTIVE);
        save("Inactive", "33333333333", CustomerStatus.INACTIVE);

        mvc.perform(get("/customers").param("status", "ACTIVE").with(httpBasic("user", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].status", hasItem("ACTIVE")));
    }

    @Test
    void shouldReturn400WhenStatusFilterIsInvalid() throws Exception {
        mvc.perform(get("/customers").param("status", "NOPE").with(httpBasic("user", "user123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("ACTIVE")));
    }

    @Test
    void shouldSearchByNameIgnoringCase() throws Exception {
        save("Joao da Silva", "11111111111", CustomerStatus.ACTIVE);
        save("Maria Joaquina", "22222222222", CustomerStatus.ACTIVE);
        save("Carlos Pereira", "33333333333", CustomerStatus.ACTIVE);

        mvc.perform(get("/customers/search").param("name", "JOA").with(httpBasic("user", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldReturn400WhenSearchWithoutName() throws Exception {
        mvc.perform(get("/customers/search").with(httpBasic("user", "user123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnSummaryByStatus() throws Exception {
        save("A", "11111111111", CustomerStatus.ACTIVE);
        save("B", "22222222222", CustomerStatus.ACTIVE);
        save("C", "33333333333", CustomerStatus.BLOCKED);

        mvc.perform(get("/customers/summary").with(httpBasic("user", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ACTIVE").value(2))
                .andExpect(jsonPath("$.INACTIVE").value(0))
                .andExpect(jsonPath("$.BLOCKED").value(1));
    }
}
