package com.yggdrasil.labs.customer;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.catchlog.CatchAndLog;
import com.yggdrasil.labs.api.CustomerServiceI;
import com.yggdrasil.labs.dto.CustomerAddCmd;
import com.yggdrasil.labs.dto.CustomerListByNameQry;
import com.yggdrasil.labs.dto.data.CustomerDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yggdrasil.labs.customer.executor.CustomerAddCmdExe;
import com.yggdrasil.labs.customer.executor.query.CustomerListByNameQryExe;

@Service
@CatchAndLog
public class CustomerServiceImpl implements CustomerServiceI {

    @Autowired
    private CustomerAddCmdExe customerAddCmdExe;

    @Autowired
    private CustomerListByNameQryExe customerListByNameQryExe;

    public Response addCustomer(CustomerAddCmd customerAddCmd) {
        return customerAddCmdExe.execute(customerAddCmd);
    }

    @Override
    public MultiResponse<CustomerDTO> listByName(CustomerListByNameQry customerListByNameQry) {
        return customerListByNameQryExe.execute(customerListByNameQry);
    }

}