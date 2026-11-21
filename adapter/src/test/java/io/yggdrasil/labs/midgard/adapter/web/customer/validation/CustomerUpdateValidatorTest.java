package io.yggdrasil.labs.midgard.adapter.web.customer.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.yggdrasil.labs.midgard.adapter.web.customer.dto.UpdateCustomerRequest;

class CustomerUpdateValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void initializeValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void rejectsBlankNameAtRequestBoundary() {
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setName("  ");

        assertThat(validator.validate(request))
                .anySatisfy(
                        violation ->
                                assertThat(violation.getPropertyPath().toString())
                                        .isEqualTo("name"));
    }

    @Test
    void rejectsEmptyUpdateAsObjectConstraint() {
        assertThat(validator.validate(new UpdateCustomerRequest()))
                .singleElement()
                .satisfies(
                        violation -> {
                            assertThat(violation.getPropertyPath().toString()).isEmpty();
                            assertThat(
                                            violation
                                                    .getConstraintDescriptor()
                                                    .getAnnotation()
                                                    .annotationType())
                                    .isEqualTo(ValidCustomerUpdate.class);
                        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "\u3000\u3000", "x"})
    void rejectsInvalidProvidedNames(String name) {
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setName(name);
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void allowsNullFieldsAndPhoneClearing() {
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setPhone("");
        assertThat(validator.validate(request)).isEmpty();
        request.setPhone("1".repeat(20));
        assertThat(validator.validate(request)).isEmpty();
        request.setPhone("1".repeat(21));
        assertThat(validator.validate(request))
                .anySatisfy(
                        violation ->
                                assertThat(violation.getPropertyPath().toString())
                                        .isEqualTo("phone"));
        assertThat(new CustomerUpdateValidator().isValid(null, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "a@localhost", "a..b@example.com"})
    void rejectsEmailsOutsideEitherFormatBoundary(String email) {
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setEmail(email);
        assertThat(validator.validate(request))
                .anySatisfy(
                        violation ->
                                assertThat(violation.getPropertyPath().toString())
                                        .isEqualTo("email"));
    }

    @Test
    void acceptsEmailLimitAndRejectsNextCharacter() {
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setEmail("a".repeat(64) + "@" + "b".repeat(31) + ".com");
        assertThat(request.getEmail()).hasSize(100);
        assertThat(validator.validate(request)).isEmpty();
        request.setEmail("a".repeat(64) + "@" + "b".repeat(32) + ".com");
        assertThat(validator.validate(request)).isNotEmpty();
    }
}
