package io.yggdrasil.labs.midgard.adapter.web.customer.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import io.yggdrasil.labs.midgard.adapter.web.customer.dto.UpdateCustomerRequest;

/** 仅校验更新字段是否全部缺省，字段内容交给各字段的约束。 */
public class CustomerUpdateValidator
        implements ConstraintValidator<ValidCustomerUpdate, UpdateCustomerRequest> {

    @Override
    public boolean isValid(UpdateCustomerRequest request, ConstraintValidatorContext context) {
        return request == null
                || request.getName() != null
                || request.getEmail() != null
                || request.getPhone() != null;
    }
}
