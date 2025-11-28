package com.yggdrasil.labs.domain.customer.repository;

import com.yggdrasil.labs.domain.customer.Credit;

public interface CreditGateway {
  Credit getCredit(String customerId);
}

