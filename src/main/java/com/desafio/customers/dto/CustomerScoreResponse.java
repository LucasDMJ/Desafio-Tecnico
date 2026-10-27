package com.desafio.customers.dto;

/** Resposta de GET /customers/{id}/score. */
public record CustomerScoreResponse(Long customerId, String cpf, Integer score, String classification) {
}
