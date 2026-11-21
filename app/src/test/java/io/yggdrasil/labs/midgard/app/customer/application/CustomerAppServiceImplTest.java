package io.yggdrasil.labs.midgard.app.customer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yggdrasil.labs.common.exception.BizException;

import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.UpdateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.app.customer.dto.qry.ListCustomerQry;
import io.yggdrasil.labs.midgard.domain.customer.model.Customer;
import io.yggdrasil.labs.midgard.domain.customer.model.CustomerStatus;
import io.yggdrasil.labs.midgard.domain.customer.repo.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class CustomerAppServiceImplTest {

    @Mock private CustomerRepository customerRepository;

    private CustomerAppServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CustomerAppServiceImpl(customerRepository);
    }

    @Test
    void create_success() {
        CreateCustomerCmd cmd = new CreateCustomerCmd();
        cmd.setName("张三");
        cmd.setEmail("zhangsan@example.com");
        cmd.setPhone("13800000000");

        when(customerRepository.existsByEmail("zhangsan@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(
                        inv -> {
                            Customer c = inv.getArgument(0);
                            c.setId(1L);
                            return c;
                        });

        CustomerCO result = service.create(cmd);

        verify(customerRepository).save(any(Customer.class));
        assertNotNull(result);
        assertEquals("张三", result.getName());
        assertEquals("zhangsan@example.com", result.getEmail());
        assertEquals("ACTIVE", result.getStatus());
    }

    @Test
    void create_duplicateEmail() {
        CreateCustomerCmd cmd = new CreateCustomerCmd();
        cmd.setName("张三");
        cmd.setEmail("duplicate@example.com");

        when(customerRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        BizException ex = assertThrows(BizException.class, () -> service.create(cmd));
        assertEquals("21001", ex.getCode());

        verify(customerRepository, never()).save(any());
    }

    @Test
    void create_invalidName() {
        CreateCustomerCmd cmd = new CreateCustomerCmd();
        cmd.setName("");
        cmd.setEmail("valid@example.com");

        when(customerRepository.existsByEmail("valid@example.com")).thenReturn(false);

        BizException ex = assertThrows(BizException.class, () -> service.create(cmd));
        assertEquals("00004", ex.getCode());

        verify(customerRepository, never()).save(any());
    }

    @Test
    void getById_success() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("张三");
        customer.setEmail("z@example.com");
        customer.setStatus(CustomerStatus.ACTIVE);
        when(customerRepository.findById(1L)).thenReturn(java.util.Optional.of(customer));

        CustomerCO result = service.getById(1L);
        assertEquals("张三", result.getName());
    }

    @Test
    void getById_notFound() {
        when(customerRepository.findById(999L)).thenReturn(java.util.Optional.empty());
        BizException ex = assertThrows(BizException.class, () -> service.getById(999L));
        assertEquals("20001", ex.getCode());
    }

    @Test
    void list_withFilters_forwardsQueryCriteriaToRepository() {
        ListCustomerQry qry = new ListCustomerQry();
        qry.setPageIndex(2L);
        qry.setPageSize(20L);
        qry.setKeyword("needle");
        qry.setStatus("ACTIVE");
        when(customerRepository.findAll(2, 20, "needle", "ACTIVE")).thenReturn(java.util.List.of());

        service.list(qry);

        verify(customerRepository).findAll(2, 20, "needle", "ACTIVE");
        verify(customerRepository).count("needle", "ACTIVE");
    }

    @Test
    void update_success() {
        Customer existing = existingCustomer();
        when(customerRepository.findById(1L)).thenReturn(java.util.Optional.of(existing));
        when(customerRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateCustomerCmd cmd = new UpdateCustomerCmd();
        cmd.setId(1L);
        cmd.setName("李四");
        cmd.setEmail("new@example.com");
        cmd.setPhone("13900000000");

        CustomerCO result = service.update(cmd);
        assertEquals("李四", result.getName());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("13900000000", result.getPhone());
        verify(customerRepository).update(existing);
        verify(customerRepository).existsByEmail("new@example.com");
    }

    @Test
    void update_emptyCommand_rejectedWithoutRepositoryAccess() {
        UpdateCustomerCmd cmd = updateCommand();

        BizException ex = assertThrows(BizException.class, () -> service.update(cmd));

        assertEquals("00004", ex.getCode());
        verify(customerRepository, never()).findById(any());
        verify(customerRepository, never()).existsByEmail(any());
        verify(customerRepository, never()).update(any());
    }

    @Test
    void update_sameEmailDoesNotCheckEmailUniqueness() {
        Customer existing = existingCustomer();
        when(customerRepository.findById(1L)).thenReturn(java.util.Optional.of(existing));
        when(customerRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));
        UpdateCustomerCmd cmd = updateCommand();
        cmd.setEmail("z@example.com");

        CustomerCO result = service.update(cmd);

        assertEquals("z@example.com", result.getEmail());
        verify(customerRepository, never()).existsByEmail(any());
        verify(customerRepository).update(existing);
    }

    @Test
    void update_emailAlreadyUsed_rejectedWithoutWritingAndKeepsExistingFields() {
        Customer existing = existingCustomer();
        when(customerRepository.findById(1L)).thenReturn(java.util.Optional.of(existing));
        when(customerRepository.existsByEmail("taken@example.com")).thenReturn(true);
        UpdateCustomerCmd cmd = updateCommand();
        cmd.setEmail("taken@example.com");
        cmd.setPhone("13900000000");

        BizException ex = assertThrows(BizException.class, () -> service.update(cmd));

        assertEquals("21001", ex.getCode());
        assertEquals("张三", existing.getName());
        assertEquals("z@example.com", existing.getEmail());
        assertEquals("13800000000", existing.getPhone());
        verify(customerRepository, never()).update(any());
    }

    @Test
    void update_notFound() {
        when(customerRepository.findById(999L)).thenReturn(java.util.Optional.empty());
        UpdateCustomerCmd cmd = new UpdateCustomerCmd();
        cmd.setId(999L);
        cmd.setName("李四");
        BizException ex = assertThrows(BizException.class, () -> service.update(cmd));
        assertEquals("20001", ex.getCode());
        verify(customerRepository, never()).update(any());
    }

    @Test
    void delete_success() {
        Customer customer = new Customer();
        customer.setId(1L);
        when(customerRepository.findById(1L)).thenReturn(java.util.Optional.of(customer));
        service.delete(1L);
        verify(customerRepository).deleteById(1L);
    }

    @Test
    void delete_notFound() {
        when(customerRepository.findById(999L)).thenReturn(java.util.Optional.empty());
        BizException ex = assertThrows(BizException.class, () -> service.delete(999L));
        assertEquals("20001", ex.getCode());
        verify(customerRepository, never()).deleteById(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12345678901234567890"})
    void create_acceptsEmail100AndOptionalPhone(String phone) {
        CreateCustomerCmd cmd = new CreateCustomerCmd();
        cmd.setName("张三");
        cmd.setEmail("a".repeat(50) + "@" + "d".repeat(45) + ".com");
        cmd.setPhone(phone);

        CustomerCO result = service.create(cmd);

        assertEquals(cmd.getEmail(), result.getEmail());
        assertEquals(phone, result.getPhone());
        verify(customerRepository).save(any(Customer.class));
    }

    @ParameterizedTest
    @CsvSource({"101, 20", "100, 21"})
    void create_rejectsOversizedFieldsWithoutWriting(int emailLength, int phoneLength) {
        CreateCustomerCmd cmd = new CreateCustomerCmd();
        cmd.setName("张三");
        cmd.setEmail("a".repeat(50) + "@" + "d".repeat(emailLength - 55) + ".com");
        cmd.setPhone("1".repeat(phoneLength));

        BizException ex = assertThrows(BizException.class, () -> service.create(cmd));

        assertEquals("00004", ex.getCode());
        verify(customerRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12345678901234567890"})
    void update_acceptsEmail100AndPhoneNullEmptyOr20(String phone) {
        Customer existing = existingCustomer();
        when(customerRepository.findById(1L)).thenReturn(java.util.Optional.of(existing));
        UpdateCustomerCmd cmd = updateCommand();
        cmd.setEmail("a".repeat(50) + "@" + "d".repeat(45) + ".com");
        cmd.setPhone(phone);

        CustomerCO result = service.update(cmd);

        assertEquals(cmd.getEmail(), result.getEmail());
        assertEquals(phone == null ? "13800000000" : phone, result.getPhone());
        assertEquals("张三", result.getName());
        verify(customerRepository).update(existing);
    }

    @ParameterizedTest
    @CsvSource({"101, 20", "100, 21"})
    void update_rejectsOversizedFieldsWithoutWriting(int emailLength, int phoneLength) {
        when(customerRepository.findById(1L)).thenReturn(java.util.Optional.of(existingCustomer()));
        UpdateCustomerCmd cmd = updateCommand();
        cmd.setEmail("a".repeat(50) + "@" + "d".repeat(emailLength - 55) + ".com");
        cmd.setPhone("1".repeat(phoneLength));

        BizException ex = assertThrows(BizException.class, () -> service.update(cmd));

        assertEquals("00004", ex.getCode());
        verify(customerRepository, never()).update(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "A", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void update_rejectsInvalidNameWithoutWriting(String name) {
        when(customerRepository.findById(1L)).thenReturn(java.util.Optional.of(existingCustomer()));
        UpdateCustomerCmd cmd = updateCommand();
        cmd.setName(name);

        BizException ex = assertThrows(BizException.class, () -> service.update(cmd));

        assertEquals("00004", ex.getCode());
        verify(customerRepository, never()).update(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "bad", "user@localhost"})
    void update_rejectsInvalidEmailWithoutWriting(String email) {
        when(customerRepository.findById(1L)).thenReturn(java.util.Optional.of(existingCustomer()));
        UpdateCustomerCmd cmd = updateCommand();
        cmd.setEmail(email);

        BizException ex = assertThrows(BizException.class, () -> service.update(cmd));

        assertEquals("00004", ex.getCode());
        verify(customerRepository, never()).update(any());
    }

    @ParameterizedTest
    @CsvSource({"101, 20, email", "100, 21, phone"})
    void commands_declareLengthConstraints(int emailLength, int phoneLength, String field) {
        CreateCustomerCmd create = new CreateCustomerCmd();
        create.setName("张三");
        create.setEmail("a".repeat(50) + "@" + "d".repeat(emailLength - 55) + ".com");
        create.setPhone("1".repeat(phoneLength));
        UpdateCustomerCmd update = updateCommand();
        update.setEmail(create.getEmail());
        update.setPhone(create.getPhone());

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertEquals(1, factory.getValidator().validateProperty(create, field).size());
            assertEquals(1, factory.getValidator().validateProperty(update, field).size());
        }
    }

    private Customer existingCustomer() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("张三");
        customer.setEmail("z@example.com");
        customer.setPhone("13800000000");
        customer.setStatus(CustomerStatus.ACTIVE);
        return customer;
    }

    private UpdateCustomerCmd updateCommand() {
        UpdateCustomerCmd cmd = new UpdateCustomerCmd();
        cmd.setId(1L);
        return cmd;
    }
}
