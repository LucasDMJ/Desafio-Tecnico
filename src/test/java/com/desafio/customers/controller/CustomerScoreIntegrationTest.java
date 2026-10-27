package com.desafio.customers.controller;

import com.desafio.customers.domain.Customer;
import com.desafio.customers.domain.CustomerStatus;
import com.desafio.customers.repository.CustomerRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testa GET /customers/{id}/score contra um WireMock simulando o servico externo. */
@SpringBootTest(properties = {
        "score-service.connect-timeout=500ms",
        "score-service.read-timeout=500ms"
})
@AutoConfigureMockMvc
class CustomerScoreIntegrationTest {

    private static final String CPF = "12345678901";

    @RegisterExtension
    static WireMockExtension scoreMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("score-service.base-url", scoreMock::baseUrl);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    CustomerRepository repository;

    Long customerId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        Customer c = new Customer();
        c.setName("Joao da Silva");
        c.setCpf(CPF);
        c.setEmail("joao@email.com");
        c.setStatus(CustomerStatus.ACTIVE);
        customerId = repository.save(c).getId();
    }

    @Test
    void shouldReturnScore() throws Exception {
        scoreMock.stubFor(get(urlPathEqualTo("/scores/" + CPF)).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"cpf":"12345678901","score":750,"classification":"LOW_RISK"}
                        """)));

        mvc.perform(get("/customers/{id}/score", customerId).with(httpBasic("user", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.cpf").value(CPF))
                .andExpect(jsonPath("$.score").value(750))
                .andExpect(jsonPath("$.classification").value("LOW_RISK"));
    }

    @Test
    void shouldReturn401WithoutCredentials() throws Exception {
        mvc.perform(get("/customers/{id}/score", customerId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn404WhenCustomerDoesNotExistAndNotCallExternalService() throws Exception {
        mvc.perform(get("/customers/{id}/score", 9999).with(httpBasic("user", "user123")))
                .andExpect(status().isNotFound());

        scoreMock.verify(0, getRequestedFor(anyUrl()));
    }

    @Test
    void shouldReturn404WhenExternalServiceHasNoScoreForCpf() throws Exception {
        scoreMock.stubFor(get(urlPathEqualTo("/scores/" + CPF)).willReturn(aResponse().withStatus(404)));

        mvc.perform(get("/customers/{id}/score", customerId).with(httpBasic("user", "user123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn502WhenExternalServiceFails() throws Exception {
        scoreMock.stubFor(get(urlPathEqualTo("/scores/" + CPF)).willReturn(aResponse().withStatus(500)));

        mvc.perform(get("/customers/{id}/score", customerId).with(httpBasic("user", "user123")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void shouldReturn504WhenExternalServiceIsTooSlow() throws Exception {
        scoreMock.stubFor(get(urlPathEqualTo("/scores/" + CPF)).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"cpf":"12345678901","score":750,"classification":"LOW_RISK"}
                        """)
                .withFixedDelay(3000)));

        mvc.perform(get("/customers/{id}/score", customerId).with(httpBasic("user", "user123")))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.status").value(504));
    }

    @Test
    void shouldReturn502WhenResponseIsNotValidJson() throws Exception {
        scoreMock.stubFor(get(urlPathEqualTo("/scores/" + CPF)).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ this is not json")));

        mvc.perform(get("/customers/{id}/score", customerId).with(httpBasic("user", "user123")))
                .andExpect(status().isBadGateway());
    }

    @Test
    void shouldReturn502WhenResponseHasUnexpectedStructure() throws Exception {
        scoreMock.stubFor(get(urlPathEqualTo("/scores/" + CPF)).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"unexpected":"payload"}
                        """)));

        mvc.perform(get("/customers/{id}/score", customerId).with(httpBasic("user", "user123")))
                .andExpect(status().isBadGateway());
    }

    @Test
    void shouldReturn502WhenResponseCpfDoesNotMatch() throws Exception {
        scoreMock.stubFor(get(urlPathEqualTo("/scores/" + CPF)).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"cpf":"00000000000","score":750,"classification":"LOW_RISK"}
                        """)));

        mvc.perform(get("/customers/{id}/score", customerId).with(httpBasic("user", "user123")))
                .andExpect(status().isBadGateway());
    }
}
