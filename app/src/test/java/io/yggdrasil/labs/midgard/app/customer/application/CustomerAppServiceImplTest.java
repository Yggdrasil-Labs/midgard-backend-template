package io.yggdrasil.labs.midgard.app.customer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.alibaba.cola.exception.BizException;

import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.domain.customer.model.Customer;
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
        assertTrue(ex.getErrCode().contains("DUPLICATE"));

        verify(customerRepository, never()).save(any());
    }

    @Test
    void create_invalidName() {
        CreateCustomerCmd cmd = new CreateCustomerCmd();
        cmd.setName("");
        cmd.setEmail("valid@example.com");

        when(customerRepository.existsByEmail("valid@example.com")).thenReturn(false);

        assertThrows(BizException.class, () -> service.create(cmd));

        verify(customerRepository, never()).save(any());
    }
}
