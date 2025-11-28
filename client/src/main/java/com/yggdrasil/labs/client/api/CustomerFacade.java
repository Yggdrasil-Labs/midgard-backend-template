package com.yggdrasil.labs.client.api;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;
import com.yggdrasil.labs.client.dto.cmd.CreateCustomerCmd;
import com.yggdrasil.labs.client.dto.query.ListCustomerQuery;
import com.yggdrasil.labs.client.dto.co.CustomerCO;

/**
 * 客户聚合接口：Facade
 */
public interface CustomerFacade {

    /**
     * 创建客户
     */
    Response createCustomer(CreateCustomerCmd cmd);

    /**
     * 按名称列表查询客户
     */
    MultiResponse<CustomerCO> listCustomerByName(ListCustomerQuery query);
}

