package com.yggdrasil.labs.api;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;
import com.yggdrasil.labs.dto.CustomerAddCmd;
import com.yggdrasil.labs.dto.CustomerListByNameQry;
import com.yggdrasil.labs.dto.data.CustomerDTO;

public interface CustomerServiceI {

    Response addCustomer(CustomerAddCmd customerAddCmd);

    MultiResponse<CustomerDTO> listByName(CustomerListByNameQry customerListByNameQry);
}
