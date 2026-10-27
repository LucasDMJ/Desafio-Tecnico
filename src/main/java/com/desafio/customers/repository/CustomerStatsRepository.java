package com.desafio.customers.repository;

import com.desafio.customers.domain.CustomerStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

/** Consultas agregadas feitas com JdbcTemplate (SQL puro). */
@Repository
public class CustomerStatsRepository {

    private final JdbcTemplate jdbcTemplate;

    public CustomerStatsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Quantidade de clientes por status (status sem clientes retornam 0). */
    public Map<String, Long> countByStatus() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (CustomerStatus s : CustomerStatus.values()) {
            result.put(s.name(), 0L);
        }
        jdbcTemplate.query("SELECT status, COUNT(*) AS total FROM customers GROUP BY status",
                rs -> {
                    result.put(rs.getString("status"), rs.getLong("total"));
                });
        return result;
    }
}
