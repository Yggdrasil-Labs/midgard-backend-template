package io.yggdrasil.labs.midgard.app.customer.dto.co;

import lombok.Data;

@Data
public class CustomerCO {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String status;
}
