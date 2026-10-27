package com.desafio.customers.service;

import com.desafio.customers.domain.Customer;
import com.desafio.customers.domain.CustomerStatus;
import com.desafio.customers.dto.CustomerRequest;
import com.desafio.customers.dto.CustomerResponse;
import com.desafio.customers.exception.CustomerNotFoundException;
import com.desafio.customers.exception.DuplicateCpfException;
import com.desafio.customers.repository.CustomerRepository;
import com.desafio.customers.repository.CustomerStatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    CustomerRepository repository;

    @Mock
    CustomerStatsRepository statsRepository;

    @InjectMocks
    CustomerService service;

    private CustomerRequest request(String cpf, CustomerStatus status) {
        return new CustomerRequest("  Joao da Silva ", cpf, "joao@email.com", status);
    }

    @Test
    void create_shouldDefaultStatusToActiveAndTrimName() {
        when(repository.existsByCpf("12345678901")).thenReturn(false);
        when(repository.saveAndFlush(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponse response = service.create(request("12345678901", null));

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(repository).saveAndFlush(captor.capture());
        assertEquals("Joao da Silva", captor.getValue().getName());
        assertEquals(CustomerStatus.ACTIVE, response.status());
    }

    @Test
    void create_shouldFailWhenCpfAlreadyExists() {
        when(repository.existsByCpf("12345678901")).thenReturn(true);

        assertThrows(DuplicateCpfException.class, () -> service.create(request("12345678901", null)));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void update_shouldFailWhenCustomerDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class,
                () -> service.update(99L, request("12345678901", CustomerStatus.ACTIVE)));
    }

    @Test
    void update_shouldFailWhenCpfBelongsToAnotherCustomer() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Customer()));
        when(repository.existsByCpfAndIdNot("12345678901", 1L)).thenReturn(true);

        assertThrows(DuplicateCpfException.class,
                () -> service.update(1L, request("12345678901", CustomerStatus.ACTIVE)));
    }

    @Test
    void update_shouldKeepCurrentStatusWhenNotInformed() {
        Customer existing = new Customer();
        existing.setStatus(CustomerStatus.BLOCKED);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByCpfAndIdNot("12345678901", 1L)).thenReturn(false);
        when(repository.saveAndFlush(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponse response = service.update(1L, request("12345678901", null));

        assertEquals(CustomerStatus.BLOCKED, response.status());
    }

    @Test
    void delete_shouldFailWhenCustomerDoesNotExist() {
        when(repository.existsById(5L)).thenReturn(false);

        assertThrows(CustomerNotFoundException.class, () -> service.delete(5L));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void findById_shouldFailWhenCustomerDoesNotExist() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> service.findById(7L));
    }
}
