package com.desafio.customers.controller;

import com.desafio.customers.domain.CustomerStatus;
import com.desafio.customers.dto.CustomerRequest;
import com.desafio.customers.dto.CustomerResponse;
import com.desafio.customers.dto.CustomerScoreResponse;
import com.desafio.customers.service.CustomerService;
import com.desafio.customers.service.ScoreService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final ScoreService scoreService;

    public CustomerController(CustomerService customerService, ScoreService scoreService) {
        this.customerService = customerService;
        this.scoreService = scoreService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse created = customerService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public CustomerResponse findById(@PathVariable Long id) {
        return customerService.findById(id);
    }

    /** GET /customers  ou  GET /customers?status=ACTIVE */
    @GetMapping
    public List<CustomerResponse> findAll(@RequestParam(required = false) CustomerStatus status) {
        return customerService.findAll(status);
    }

    /** GET /customers/search?name=joao */
    @GetMapping("/search")
    public List<CustomerResponse> search(@RequestParam String name) {
        return customerService.searchByName(name);
    }

    /** Extra: quantidade de clientes por status (JdbcTemplate). */
    @GetMapping("/summary")
    public Map<String, Long> summary() {
        return customerService.countByStatus();
    }

    @GetMapping("/{id}/score")
    public CustomerScoreResponse score(@PathVariable Long id) {
        return scoreService.getScore(id);
    }
}
