package com.desafio.customers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {
    public static ErrorResponse of(HttpStatus status, String message, String path, List<String> details) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, details);
    }
}
