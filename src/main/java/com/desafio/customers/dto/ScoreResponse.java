package com.desafio.customers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Resposta do servico externo: GET /scores/{cpf}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScoreResponse(String cpf, Integer score, String classification) {
}
