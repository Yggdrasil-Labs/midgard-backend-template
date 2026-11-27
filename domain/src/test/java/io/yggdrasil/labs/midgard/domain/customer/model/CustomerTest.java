package io.yggdrasil.labs.midgard.domain.customer.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.Serializable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.yggdrasil.labs.common.exception.BizException;

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

    @Test
    void validate_shouldRejectEmailLongerThanColumnCapacity() {
        customer.setEmail("a".repeat(50) + "@" + "d".repeat(46) + ".com");

        assertThatThrownBy(() -> customer.validate())
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo("00004");
    }

    @Test
    void validate_shouldAcceptEmailAtColumnCapacity() {
        customer.setEmail("a".repeat(50) + "@" + "d".repeat(45) + ".com");

        assertThatCode(() -> customer.validate()).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldRejectPhoneLongerThanColumnCapacity() {
        customer.setPhone("1".repeat(21));

        assertThatThrownBy(() -> customer.validate())
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo("00004");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12345678901234567890"})
    void validate_shouldAcceptOptionalPhoneWithinColumnCapacity(String phone) {
        customer.setPhone(phone);

        assertThatCode(() -> customer.validate()).doesNotThrowAnyException();
    }

    // ===== initForCreate() =====

    @Test
    void customer_shouldSupportSerializablePageResults() {
        assertThat(customer).isInstanceOf(Serializable.class);
    }

    @Test
    void version_shouldRemainNullUntilInitialized() {
        assertThat(customer.getVersion()).isNull();
    }

    @Test
    void initForCreate_shouldInitializeVersionToZero() {
        customer.setVersion(5);

        customer.initForCreate();

        assertThat(customer.getVersion()).isZero();
    }

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
