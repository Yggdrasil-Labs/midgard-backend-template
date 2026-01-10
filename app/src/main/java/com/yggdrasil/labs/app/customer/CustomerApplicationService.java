package com.yggdrasil.labs.app.customer;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;
import com.yggdrasil.labs.app.customer.dto.cmd.CreateCustomerCmd;
import com.yggdrasil.labs.app.customer.dto.co.CustomerCO;
import com.yggdrasil.labs.app.customer.dto.query.ListCustomerQuery;

/** ApplicationService：对 Adapter 暴露的客户应用服务接口 */
public interface CustomerApplicationService {

    /** 创建客户 */
    Response createCustomer(CreateCustomerCmd cmd);

    /** 按名称列表查询客户 */
    MultiResponse<CustomerCO> listCustomerByName(ListCustomerQuery query);
}
