package com.desafio.customers.client;

import com.desafio.customers.config.ScoreServiceProperties;
import com.desafio.customers.exception.ScoreServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreHttpClientConnectionTest {

    @Test
    void shouldReturnServiceUnavailableWhenNothingIsListening() throws IOException {
        int freePort;
        try (ServerSocket socket = new ServerSocket(0)) {
            freePort = socket.getLocalPort();
        } // porta liberada: nenhum servico escutando

        ScoreHttpClient client = new ScoreHttpClient(new ScoreServiceProperties(
                "http://localhost:" + freePort, Duration.ofMillis(500), Duration.ofMillis(500)));

        ScoreServiceException ex = assertThrows(ScoreServiceException.class, () -> client.getScore("12345678901"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
    }
}
