package com.yggdrasil.labs.domain.customer.gateway;

import com.yggdrasil.labs.domain.customer.Customer;

public interface CustomerGateway {
    Customer getByById(String customerId);
}
