package com.yggdrasil.labs.client.dto.query;

import com.alibaba.cola.dto.Query;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ListCustomerQuery：按名称列表查询客户
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ListCustomerQuery extends Query {
    private String name;
}


