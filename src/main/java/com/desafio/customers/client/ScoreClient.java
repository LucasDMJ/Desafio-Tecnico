package com.desafio.customers.client;

import com.desafio.customers.dto.ScoreResponse;

public interface ScoreClient {

    /**
     * Consulta o score no servico externo.
     *
     * @throws com.desafio.customers.exception.ScoreServiceException em qualquer falha de integracao
     */
    ScoreResponse getScore(String cpf);
}
