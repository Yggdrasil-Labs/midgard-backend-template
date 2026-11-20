package io.yggdrasil.labs.midgard.adapter.web.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/** 创建客户 HTTP 请求参数。 */
@Data
public class CreateCustomerRequest {

    @NotBlank(message = "客户名称不能为空")
    @Size(min = 2, max = 50, message = "客户名称长度必须在2-50之间")
    private String name;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不合法")
    private String email;

    private String phone;
}
