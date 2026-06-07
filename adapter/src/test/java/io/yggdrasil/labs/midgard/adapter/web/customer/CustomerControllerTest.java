package io.yggdrasil.labs.midgard.adapter.web.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.BizException;

import io.yggdrasil.labs.midgard.adapter.web.customer.dto.CreateCustomerRequest;
import io.yggdrasil.labs.midgard.app.customer.application.CustomerAppService;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock private CustomerAppService customerAppService;

    private CustomerController controller;

    @BeforeEach
    void setUp() {
        controller = new CustomerController(customerAppService);
    }

    @Test
    void create_success() {
        CustomerCO co = new CustomerCO();
        co.setId(1L);
        co.setName("张三");
        co.setEmail("z@example.com");
        co.setStatus("ACTIVE");

        when(customerAppService.create(any(CreateCustomerCmd.class))).thenReturn(co);

        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("张三");
        request.setEmail("z@example.com");

        SingleResponse<CustomerCO> result = controller.create(request);

        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("张三", result.getData().getName());
    }

    @Test
    void getById_success() {
        CustomerCO co = new CustomerCO();
        co.setId(1L);
        co.setName("张三");

        when(customerAppService.getById(1L)).thenReturn(co);

        SingleResponse<CustomerCO> result = controller.getById(1L);

        assertEquals(1L, result.getData().getId());
    }

    @Test
    void getById_notFound() {
        when(customerAppService.getById(999L)).thenThrow(new BizException("NOT_FOUND", "客户不存在"));

        assertThrows(BizException.class, () -> controller.getById(999L));
    }

    @Test
    void list_success() {
        when(customerAppService.list(any())).thenReturn(java.util.List.of());
        when(customerAppService.count(any())).thenReturn(0L);

        PageResponse<CustomerCO> result = controller.list(1, 10);

        assertNotNull(result);
    }

    @Test
    void delete_success() {
        doNothing().when(customerAppService).delete(1L);

        Response result = controller.delete(1L);

        assertNotNull(result);
        verify(customerAppService).delete(1L);
    }

    @Test
    void delete_notFound() {
        doThrow(new BizException("NOT_FOUND", "客户不存在")).when(customerAppService).delete(999L);

        assertThrows(BizException.class, () -> controller.delete(999L));
    }
}
