package io.yggdrasil.labs.midgard.domain.customer.exception;

import com.yggdrasil.labs.common.exception.ErrorDefinition;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 客户领域对外公开的业务错误定义。 */
@Getter
@RequiredArgsConstructor
public enum CustomerErrorCode implements ErrorDefinition {
    EMAIL_ALREADY_EXISTS("21001", "邮箱已被占用"),
    VERSION_CONFLICT("21002", "数据已被他人修改，请刷新后重试");

    private final String code;
    private final String message;
}
