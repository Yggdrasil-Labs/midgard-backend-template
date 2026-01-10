package com.yggdrasil.labs.app.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.cola.catchlog.CatchAndLog;
import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;
import com.yggdrasil.labs.app.customer.dto.cmd.CreateCustomerCmd;
import com.yggdrasil.labs.app.customer.dto.co.CustomerCO;
import com.yggdrasil.labs.app.customer.dto.query.ListCustomerQuery;
import com.yggdrasil.labs.app.customer.executor.CustomerCreateCmdExe;
import com.yggdrasil.labs.app.customer.query.CustomerListQryExe;

/** 客户应用服务实现，负责编排调用执行器 */
@Service
@CatchAndLog
public class CustomerApplicationServiceImpl implements CustomerApplicationService {

    @Autowired private CustomerCreateCmdExe customerCreateCmdExe;

    @Autowired private CustomerListQryExe customerListQryExe;

    @Override
    public Response createCustomer(CreateCustomerCmd cmd) {
        return customerCreateCmdExe.execute(cmd);
    }

    @Override
    public MultiResponse<CustomerCO> listCustomerByName(ListCustomerQuery query) {
        return customerListQryExe.execute(query);
    }
}
