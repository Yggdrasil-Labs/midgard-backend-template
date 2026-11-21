package io.yggdrasil.labs.midgard.adapter.web.customer.dto;

import java.io.Serial;
import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.yggdrasil.labs.midgard.adapter.web.customer.validation.ValidCustomerUpdate;
import lombok.Data;

/** 更新客户 HTTP 请求参数。 */
@Data
@ValidCustomerUpdate
public class UpdateCustomerRequest implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @Pattern(regexp = "(?s).*[^\\p{javaWhitespace}].*", message = "客户名称不能为空白")
    @Size(min = 2, max = 50, message = "客户名称长度必须在2-50之间")
    private String name;

    @Email(message = "邮箱格式不合法")
    @Pattern(regexp = "^[\\w.+%-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不合法")
    @Size(max = 100, message = "邮箱长度不能超过100")
    private String email;

    @Size(max = 20, message = "电话长度不能超过20")
    private String phone;
}
