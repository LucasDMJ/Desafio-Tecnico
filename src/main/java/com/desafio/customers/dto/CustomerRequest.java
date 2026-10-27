package com.desafio.customers.dto;

import com.desafio.customers.domain.CustomerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must have at most 150 characters")
        String name,

        @NotBlank(message = "cpf is required")
        @Pattern(regexp = "\\d{11}", message = "cpf must contain exactly 11 digits (numbers only)")
        String cpf,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        @Size(max = 150, message = "email must have at most 150 characters")
        String email,

        /** Opcional: se nao informado, assume ACTIVE na criacao e mantem o valor atual na alteracao. */
        CustomerStatus status
) {
}
