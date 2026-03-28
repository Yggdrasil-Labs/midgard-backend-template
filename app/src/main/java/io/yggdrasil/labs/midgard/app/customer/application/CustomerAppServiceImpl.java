package io.yggdrasil.labs.midgard.app.customer.application;

import org.springframework.stereotype.Service;

import com.alibaba.cola.catchlog.CatchAndLog;
import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;

import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.app.customer.dto.qry.ListCustomerQry;
import io.yggdrasil.labs.midgard.app.customer.executor.CustomerCreateCmdExe;
import io.yggdrasil.labs.midgard.app.customer.executor.CustomerListQryExe;

/** 客户应用服务实现，负责编排调用执行器 */
@Service
@CatchAndLog
public class CustomerAppServiceImpl implements CustomerAppService {

    private final CustomerCreateCmdExe customerCreateCmdExe;

    private final CustomerListQryExe customerListQryExe;

    public CustomerAppServiceImpl(
            CustomerCreateCmdExe customerCreateCmdExe, CustomerListQryExe customerListQryExe) {
        this.customerCreateCmdExe = customerCreateCmdExe;
        this.customerListQryExe = customerListQryExe;
    }

    @Override
    public Response createCustomer(CreateCustomerCmd cmd) {
        return customerCreateCmdExe.execute(cmd);
    }

    @Override
    public MultiResponse<CustomerCO> listCustomerByName(ListCustomerQry query) {
        return customerListQryExe.execute(query);
    }
}
