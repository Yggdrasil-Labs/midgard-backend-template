package io.yggdrasil.labs.midgard.domain.customer.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.yggdrasil.labs.common.exception.BizException;

class CustomerErrorCodeTest {

    @Test
    void emailConflict_usesDomainCodeAndSafeDefaultMessage() {
        BizException exception = BizException.fromError(CustomerErrorCode.EMAIL_ALREADY_EXISTS);

        assertThat(exception.getCode()).isEqualTo("21001");
        assertThat(exception.getMessage()).isEqualTo("邮箱已被占用");
    }

    @Test
    void versionConflict_definesReservedCodeWithoutAddingOptimisticLocking() {
        BizException exception = BizException.fromError(CustomerErrorCode.VERSION_CONFLICT);

        assertThat(exception.getCode()).isEqualTo("21002");
        assertThat(exception.getMessage()).isEqualTo("数据已被他人修改，请刷新后重试");
    }
}
