package com.desafio.customers.service;

import com.desafio.customers.client.ScoreClient;
import com.desafio.customers.domain.Customer;
import com.desafio.customers.dto.CustomerScoreResponse;
import com.desafio.customers.dto.ScoreResponse;
import org.springframework.stereotype.Service;

@Service
public class ScoreService {

    private final CustomerService customerService;
    private final ScoreClient scoreClient;

    public ScoreService(CustomerService customerService, ScoreClient scoreClient) {
        this.customerService = customerService;
        this.scoreClient = scoreClient;
    }

    /** Sem @Transactional de proposito: nao segura conexao do banco durante a chamada HTTP externa. */
    public CustomerScoreResponse getScore(Long customerId) {
        Customer customer = customerService.getEntity(customerId);
        ScoreResponse score = scoreClient.getScore(customer.getCpf());
        return new CustomerScoreResponse(customer.getId(), customer.getCpf(), score.score(), score.classification());
    }
}
