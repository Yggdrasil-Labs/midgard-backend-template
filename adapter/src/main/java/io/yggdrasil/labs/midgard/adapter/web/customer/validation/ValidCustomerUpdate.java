package io.yggdrasil.labs.midgard.adapter.web.customer.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** 要求客户更新请求至少提供一个非 null 的可更新字段。 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CustomerUpdateValidator.class)
public @interface ValidCustomerUpdate {

    /** 默认校验消息。 */
    String message() default "至少一个字段不能为空";

    /** 校验分组。 */
    Class<?>[] groups() default {};

    /** 校验附加信息。 */
    Class<? extends Payload>[] payload() default {};
}
