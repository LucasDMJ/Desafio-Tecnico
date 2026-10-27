package com.desafio.customers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "score-service")
public record ScoreServiceProperties(
        String baseUrl,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout
) {
}
