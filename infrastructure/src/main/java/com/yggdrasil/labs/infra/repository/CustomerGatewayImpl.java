package com.yggdrasil.labs.infra.repository;

import com.yggdrasil.labs.infra.db.entity.CustomerDO;
import com.yggdrasil.labs.domain.customer.Customer;
import com.yggdrasil.labs.domain.customer.repository.CustomerGateway;
import com.yggdrasil.labs.infra.db.entity.mapper.CustomerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CustomerGatewayImpl implements CustomerGateway {
  @Autowired
  private CustomerMapper customerMapper;

  public Customer getByById(String customerId) {
    CustomerDO customerDO = customerMapper.selectById(customerId);
    // Convert to Customer
    return null;
  }
}

