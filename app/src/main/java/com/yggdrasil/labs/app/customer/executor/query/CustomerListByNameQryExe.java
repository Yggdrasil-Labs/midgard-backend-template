package com.yggdrasil.labs.app.customer.executor.query;

import com.alibaba.cola.dto.MultiResponse;
import com.yggdrasil.labs.client.dto.query.ListCustomerQuery;
import com.yggdrasil.labs.client.dto.co.CustomerCO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CustomerListByNameQryExe {
  public MultiResponse<CustomerCO> execute(ListCustomerQuery cmd) {
    List<CustomerCO> customerList = new ArrayList<>();
    CustomerCO customer = new CustomerCO();
    customer.setCustomerName("Frank");
    customerList.add(customer);
    return MultiResponse.of(customerList);
  }
}

