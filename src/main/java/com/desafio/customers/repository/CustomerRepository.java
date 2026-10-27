package com.desafio.customers.repository;

import com.desafio.customers.domain.Customer;
import com.desafio.customers.domain.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    List<Customer> findByStatusOrderByNameAsc(CustomerStatus status);

    /** Native Query: busca por parte do nome, sem diferenciar maiusculas/minusculas. */
    @Query(value = """
            SELECT * FROM customers
             WHERE LOWER(name) LIKE LOWER(CONCAT('%', :name, '%'))
             ORDER BY name
            """, nativeQuery = true)
    List<Customer> searchByName(@Param("name") String name);
}
