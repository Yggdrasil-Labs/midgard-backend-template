package io.yggdrasil.labs.midgard.app.customer.application;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;

import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.app.customer.dto.qry.ListCustomerQry;

/** CustomerAppService：对 Adapter 暴露的客户 AppService 接口 */
public interface CustomerAppService {

    /** 创建客户 */
    Response createCustomer(CreateCustomerCmd cmd);

    /** 按名称列表查询客户 */
    MultiResponse<CustomerCO> listCustomerByName(ListCustomerQry query);
}
