package io.yggdrasil.labs.midgard.app.customer.dto.co;

import java.io.Serial;
import java.io.Serializable;

import lombok.Data;

/** 面向调用方返回的客户视图对象。 */
@Data
public class CustomerCO implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String status;
}
