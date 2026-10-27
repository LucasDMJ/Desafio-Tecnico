package com.desafio.customers.client;

import com.desafio.customers.config.ScoreServiceProperties;
import com.desafio.customers.dto.ScoreResponse;
import com.desafio.customers.exception.ScoreServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

/**
 * Cliente HTTP do servico externo de score.
 *
 * Traducao de falhas:
 *  - 404 do servico externo ........ 404 (score nao encontrado)
 *  - timeout (conexao/leitura) ..... 504 Gateway Timeout
 *  - servico fora do ar ............ 503 Service Unavailable
 *  - 5xx / outros erros HTTP ....... 502 Bad Gateway
 *  - resposta invalida/inesperada .. 502 Bad Gateway
 */
@Component
public class ScoreHttpClient implements ScoreClient {

    private static final Logger log = LoggerFactory.getLogger(ScoreHttpClient.class);

    private final RestClient restClient;

    public ScoreHttpClient(ScoreServiceProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public ScoreResponse getScore(String cpf) {
        ScoreResponse body;
        try {
            body = restClient.get()
                    .uri("/scores/{cpf}", cpf)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(ScoreResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ScoreServiceException(HttpStatus.NOT_FOUND, "Score not found for this customer", e);
        } catch (HttpStatusCodeException e) {
            log.error("Score service returned HTTP {}", e.getStatusCode().value());
            throw new ScoreServiceException(HttpStatus.BAD_GATEWAY,
                    "Score service returned an error (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (ResourceAccessException e) {
            if (isTimeout(e)) {
                log.error("Timeout calling score service", e);
                throw new ScoreServiceException(HttpStatus.GATEWAY_TIMEOUT, "Score service took too long to respond", e);
            }
            log.error("Score service unavailable", e);
            throw new ScoreServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Score service is unavailable", e);
        } catch (RestClientException e) {
            // Ex.: corpo que nao e JSON valido
            log.error("Invalid response from score service", e);
            throw new ScoreServiceException(HttpStatus.BAD_GATEWAY, "Invalid response received from score service", e);
        }

        validate(body, cpf);
        return body;
    }

    private void validate(ScoreResponse body, String cpf) {
        boolean invalid = body == null
                || body.score() == null
                || body.classification() == null || body.classification().isBlank()
                || !cpf.equals(body.cpf());
        if (invalid) {
            log.error("Unexpected payload from score service (missing fields or CPF mismatch)");
            throw new ScoreServiceException(HttpStatus.BAD_GATEWAY, "Unexpected response received from score service");
        }
    }

    private boolean isTimeout(Throwable t) {
        while (t != null) {
            if (t instanceof SocketTimeoutException || t instanceof HttpTimeoutException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
