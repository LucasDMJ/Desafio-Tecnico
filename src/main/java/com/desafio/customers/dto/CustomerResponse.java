package com.desafio.customers.dto;

import com.desafio.customers.domain.Customer;
import com.desafio.customers.domain.CustomerStatus;

import java.time.Instant;

public record CustomerResponse(
        Long id,
        String name,
        String cpf,
        String email,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getCpf(), c.getEmail(),
                c.getStatus(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
