package io.yggdrasil.labs.midgard.client.api;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;

import io.yggdrasil.labs.midgard.client.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.client.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.client.dto.qry.ListCustomerQry;

/** 客户聚合接口：Client */
public interface CustomerClient {

    /** 创建客户 */
    Response createCustomer(CreateCustomerCmd cmd);

    /** 按名称列表查询客户 */
    MultiResponse<CustomerCO> listCustomerByName(ListCustomerQry query);
}
