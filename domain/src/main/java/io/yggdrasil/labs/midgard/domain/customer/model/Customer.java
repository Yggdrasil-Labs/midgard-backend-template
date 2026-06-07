package io.yggdrasil.labs.midgard.domain.customer.model;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.cola.exception.BizException;

import lombok.Data;

@Data
public class Customer {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private CustomerStatus status;

    public void validate() {
        if (StringUtils.isBlank(name) || name.length() < 2 || name.length() > 50) {
            throw new BizException("VALIDATION_ERROR", "客户名称长度必须在2-50之间");
        }
        if (StringUtils.isBlank(email) || !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new BizException("VALIDATION_ERROR", "邮箱格式不合法");
        }
    }

    public void initForCreate() {
        this.status = CustomerStatus.ACTIVE;
    }
}
