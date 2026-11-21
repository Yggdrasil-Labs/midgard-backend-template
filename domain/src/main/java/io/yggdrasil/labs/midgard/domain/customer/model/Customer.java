package io.yggdrasil.labs.midgard.domain.customer.model;

import org.apache.commons.lang3.StringUtils;

import com.yggdrasil.labs.common.exception.BizException;
import com.yggdrasil.labs.common.exception.ErrorCode;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/** 表示客户的领域实体及其基础校验规则。 */
@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class Customer {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private CustomerStatus status;

    private static final String EMAIL_REGEX = "^[\\w.+%-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";

    /** 校验客户名称、邮箱和可选电话；字段不符合规则时抛出业务异常。 */
    public void validate() {
        if (StringUtils.isBlank(name) || name.length() < 2 || name.length() > 50) {
            throw BizException.fromError(ErrorCode.PARAM_INVALID, "客户名称长度必须在2-50之间");
        }
        if (StringUtils.isBlank(email) || !email.matches(EMAIL_REGEX)) {
            throw BizException.fromError(ErrorCode.PARAM_INVALID, "邮箱格式不合法");
        }
        if (email.length() > 100) {
            throw BizException.fromError(ErrorCode.PARAM_INVALID, "邮箱长度不能超过100");
        }
        if (phone != null && phone.length() > 20) {
            throw BizException.fromError(ErrorCode.PARAM_INVALID, "电话长度不能超过20");
        }
    }

    /** 初始化新建客户的默认状态为启用。 */
    public void initForCreate() {
        this.status = CustomerStatus.ACTIVE;
    }
}
