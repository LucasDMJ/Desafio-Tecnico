package com.desafio.customers.exception;

import org.springframework.http.HttpStatus;

/** Falha na integracao com o servico externo de score, ja traduzida para o status HTTP adequado. */
public class ScoreServiceException extends RuntimeException {

    private final HttpStatus status;

    public ScoreServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public ScoreServiceException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
