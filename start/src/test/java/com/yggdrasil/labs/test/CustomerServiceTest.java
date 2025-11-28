package com.yggdrasil.labs.test;

import com.alibaba.cola.dto.Response;
import com.yggdrasil.labs.client.api.CustomerFacade;
import com.yggdrasil.labs.client.dto.cmd.CreateCustomerCmd;
import com.yggdrasil.labs.client.dto.cmd.base.CustomerBaseCmd;
import com.yggdrasil.labs.client.dto.enums.ErrorCode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * This is for integration test.
 *
 * <p>Created by fulan.zjf on 2017/11/29.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class CustomerServiceTest {

  @Autowired private CustomerFacade customerFacade;

  @Before
  public void setUp() {}

  @Test
  public void testCustomerAddSuccess() {
    // 1.prepare
    CreateCustomerCmd cmd = new CreateCustomerCmd();
    CustomerBaseCmd customer = new CustomerBaseCmd();
    customer.setCompanyName("NormalName");
    cmd.setCustomer(customer);

    // 2.execute
    Response response = customerFacade.createCustomer(cmd);

    // 3.assert
    Assert.assertTrue(response.isSuccess());
  }

  @Test
  public void testCustomerAddCompanyNameConflict() {
    // 1.prepare
    CreateCustomerCmd cmd = new CreateCustomerCmd();
    CustomerBaseCmd customer = new CustomerBaseCmd();
    customer.setCompanyName("ConflictCompanyName");
    cmd.setCustomer(customer);

    // 2.execute
    Response response = customerFacade.createCustomer(cmd);

    // 3.assert error
    Assert.assertEquals(
        ErrorCode.B_CUSTOMER_companyNameConflict.getErrCode(), response.getErrCode());
  }
}
