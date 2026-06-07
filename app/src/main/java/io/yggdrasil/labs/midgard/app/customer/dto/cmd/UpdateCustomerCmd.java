package io.yggdrasil.labs.midgard.app.customer.dto.cmd;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class UpdateCustomerCmd {

    private Long id;

    @Size(min = 2, max = 50, message = "客户名称长度必须在2-50之间")
    private String name;

    @Email(message = "邮箱格式不合法")
    private String email;

    private String phone;
}
