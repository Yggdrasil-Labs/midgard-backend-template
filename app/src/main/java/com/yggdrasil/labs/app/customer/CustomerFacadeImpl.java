package com.yggdrasil.labs.app.customer;

import com.alibaba.cola.catchlog.CatchAndLog;
import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;
import com.yggdrasil.labs.app.customer.executor.CustomerAddCmdExe;
import com.yggdrasil.labs.app.customer.query.CustomerListByNameQryExe;
import com.yggdrasil.labs.client.api.CustomerFacade;
import com.yggdrasil.labs.client.dto.cmd.CreateCustomerCmd;
import com.yggdrasil.labs.client.dto.query.ListCustomerQuery;
import com.yggdrasil.labs.client.dto.co.CustomerCO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@CatchAndLog
public class CustomerFacadeImpl implements CustomerFacade {

  @Autowired
  private CustomerAddCmdExe customerAddCmdExe;

  @Autowired
  private CustomerListByNameQryExe customerListByNameQryExe;

  @Override
  public Response createCustomer(CreateCustomerCmd cmd) {
    return customerAddCmdExe.execute(cmd);
  }

  @Override
  public MultiResponse<CustomerCO> listCustomerByName(ListCustomerQuery query) {
    return customerListByNameQryExe.execute(query);
  }
}

