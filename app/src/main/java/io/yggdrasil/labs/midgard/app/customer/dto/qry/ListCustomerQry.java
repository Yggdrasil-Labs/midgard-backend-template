package io.yggdrasil.labs.midgard.app.customer.dto.qry;

import jakarta.validation.constraints.Size;

import com.alibaba.cola.dto.Query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** ListCustomerQry：按名称列表查询客户（应用层 DTO） */
@Data
@EqualsAndHashCode(callSuper = true)
public class ListCustomerQry extends Query {

    /** 客户名称（支持模糊查询） */
    @Size(max = 100, message = "客户名称长度不能超过100个字符")
    private String name;
}
