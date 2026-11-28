package io.yggdrasil.labs.midgard.app.customer.dto.cmd;

import java.io.Serial;
import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

/** 更新客户用例的输入参数。 */
@Data
public class UpdateCustomerCmd implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull(message = "客户版本不能为空")
    @Min(value = 0, message = "客户版本不能为负数")
    @Max(value = 2147483646, message = "客户版本不能超过2147483646")
    private Integer version;

    @Size(min = 2, max = 50, message = "客户名称长度必须在2-50之间")
    private String name;

    @Email(message = "邮箱格式不合法")
    @Size(max = 100, message = "邮箱长度不能超过100")
    private String email;

    @Size(max = 20, message = "电话长度不能超过20")
    private String phone;
}
