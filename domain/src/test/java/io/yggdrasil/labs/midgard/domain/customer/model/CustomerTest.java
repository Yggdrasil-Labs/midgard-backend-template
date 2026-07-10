package io.yggdrasil.labs.midgard.domain.customer.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.alibaba.cola.exception.BizException;

class CustomerTest {

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setName("张三");
        customer.setEmail("zhangsan@example.com");
    }

    // ===== validate() 成功 =====

    @Test
    void validate_shouldPass_whenNameAndEmailAreValid() {
        assertThatCode(() -> customer.validate()).doesNotThrowAnyException();
    }

    // ===== validate() 失败：name =====

    @Test
    void validate_shouldThrow_whenNameIsNull() {
        customer.setName(null);

        assertThatThrownBy(() -> customer.validate())
                .isInstanceOf(BizException.class)
                .hasMessageContaining("客户名称长度必须在2-50之间");
    }

    @Test
    void validate_shouldThrow_whenNameIsBlank() {
        customer.setName("  ");

        assertThatThrownBy(() -> customer.validate())
                .isInstanceOf(BizException.class)
                .hasMessageContaining("客户名称长度必须在2-50之间");
    }

    @Test
    void validate_shouldThrow_whenNameIsTooShort() {
        customer.setName("A");

        assertThatThrownBy(() -> customer.validate())
                .isInstanceOf(BizException.class)
                .hasMessageContaining("客户名称长度必须在2-50之间");
    }

    @Test
    void validate_shouldThrow_whenNameIsTooLong() {
        customer.setName("A".repeat(51));

        assertThatThrownBy(() -> customer.validate())
                .isInstanceOf(BizException.class)
                .hasMessageContaining("客户名称长度必须在2-50之间");
    }

    // ===== validate() fail-fast 行为 =====

    @Test
    void validate_shouldReportNameError_whenBothNameAndEmailAreInvalid() {
        customer.setName("");
        customer.setEmail("bad");

        assertThatThrownBy(() -> customer.validate())
                .isInstanceOf(BizException.class)
                .hasMessageContaining("客户名称长度必须在2-50之间");
    }

    // ===== validate() 失败：email =====

    @Test
    void validate_shouldThrow_whenEmailIsNull() {
        customer.setEmail(null);

        assertThatThrownBy(() -> customer.validate())
                .isInstanceOf(BizException.class)
                .hasMessageContaining("邮箱格式不合法");
    }

    @Test
    void validate_shouldThrow_whenEmailIsBlank() {
        customer.setEmail("  ");

        assertThatThrownBy(() -> customer.validate())
                .isInstanceOf(BizException.class)
                .hasMessageContaining("邮箱格式不合法");
    }

    @Test
    void validate_shouldThrow_whenEmailFormatIsInvalid() {
        customer.setEmail("not-an-email");

        assertThatThrownBy(() -> customer.validate())
                .isInstanceOf(BizException.class)
                .hasMessageContaining("邮箱格式不合法");
    }

    // ===== initForCreate() =====

    @Test
    void initForCreate_shouldSetStatusToActive() {
        customer.initForCreate();

        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void initForCreate_shouldResetStatusToActive_whenPreviouslyInactive() {
        customer.setStatus(CustomerStatus.INACTIVE);

        customer.initForCreate();

        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }
}
