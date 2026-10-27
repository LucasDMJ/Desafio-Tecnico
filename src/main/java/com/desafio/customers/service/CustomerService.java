package com.desafio.customers.service;

import com.desafio.customers.domain.Customer;
import com.desafio.customers.domain.CustomerStatus;
import com.desafio.customers.dto.CustomerRequest;
import com.desafio.customers.dto.CustomerResponse;
import com.desafio.customers.exception.CustomerNotFoundException;
import com.desafio.customers.exception.DuplicateCpfException;
import com.desafio.customers.repository.CustomerRepository;
import com.desafio.customers.repository.CustomerStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerStatsRepository statsRepository;

    public CustomerService(CustomerRepository repository, CustomerStatsRepository statsRepository) {
        this.repository = repository;
        this.statsRepository = statsRepository;
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (repository.existsByCpf(request.cpf())) {
            throw new DuplicateCpfException();
        }
        Customer customer = new Customer();
        customer.setName(request.name().trim());
        customer.setCpf(request.cpf());
        customer.setEmail(request.email().trim());
        customer.setStatus(request.status() != null ? request.status() : CustomerStatus.ACTIVE);
        return CustomerResponse.from(repository.saveAndFlush(customer));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = findOrThrow(id);
        if (repository.existsByCpfAndIdNot(request.cpf(), id)) {
            throw new DuplicateCpfException();
        }
        customer.setName(request.name().trim());
        customer.setCpf(request.cpf());
        customer.setEmail(request.email().trim());
        if (request.status() != null) {
            customer.setStatus(request.status());
        }
        return CustomerResponse.from(repository.saveAndFlush(customer));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new CustomerNotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return CustomerResponse.from(findOrThrow(id));
    }

    /** Retorna a entidade (uso interno, ex.: consulta de score). */
    @Transactional(readOnly = true)
    public Customer getEntity(Long id) {
        return findOrThrow(id);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll(CustomerStatus status) {
        List<Customer> customers = (status == null)
                ? repository.findAll()
                : repository.findByStatusOrderByNameAsc(status);
        return customers.stream().map(CustomerResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> searchByName(String name) {
        String term = name == null ? "" : name.trim();
        return repository.searchByName(term).stream().map(CustomerResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> countByStatus() {
        return statsRepository.countByStatus();
    }

    private Customer findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
    }
}
