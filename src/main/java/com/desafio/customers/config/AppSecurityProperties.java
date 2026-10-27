package com.desafio.customers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(List<UserCredentials> users) {

    public record UserCredentials(String username, String password, String role) {
    }
}
