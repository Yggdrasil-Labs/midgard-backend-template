package com.yggdrasil.labs.domain.customer.repository;

import com.yggdrasil.labs.domain.customer.model.Customer;

/**
 * Customer 领域仓储接口
 * 
 * <p>负责 Customer 领域对象的持久化操作
 */
public interface CustomerRepository {
  Customer getById(String customerId);
}

