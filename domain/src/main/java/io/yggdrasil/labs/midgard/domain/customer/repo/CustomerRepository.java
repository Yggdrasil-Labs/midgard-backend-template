package io.yggdrasil.labs.midgard.domain.customer.repo;

import java.util.List;
import java.util.Optional;

import io.yggdrasil.labs.midgard.domain.customer.model.Customer;

public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(Long id);

    List<Customer> findAll(int page, int size);

    long count();

    Customer update(Customer customer);

    void deleteById(Long id);

    boolean existsByEmail(String email);
}
