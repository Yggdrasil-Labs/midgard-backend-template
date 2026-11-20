package io.yggdrasil.labs.midgard.app.customer.dto.co;

import lombok.Data;

/** 面向调用方返回的客户视图对象。 */
@Data
public class CustomerCO {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String status;
}
