package com.yggdrasil.labs.domain.customer.repository;

import com.yggdrasil.labs.domain.customer.Customer;

public interface CustomerGateway {
  Customer getByById(String customerId);
}

