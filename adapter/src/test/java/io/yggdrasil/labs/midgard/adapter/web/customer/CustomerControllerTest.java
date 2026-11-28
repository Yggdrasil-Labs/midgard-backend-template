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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yggdrasil.labs.common.exception.BizException;
import com.yggdrasil.labs.common.exception.ErrorCode;
import com.yggdrasil.labs.common.page.PageResult;
import com.yggdrasil.labs.common.response.R;

import io.yggdrasil.labs.midgard.adapter.web.customer.dto.CreateCustomerRequest;
import io.yggdrasil.labs.midgard.adapter.web.customer.dto.UpdateCustomerRequest;
import io.yggdrasil.labs.midgard.app.customer.application.CustomerAppService;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.UpdateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.app.customer.dto.qry.ListCustomerQry;

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

        R<CustomerCO> result = controller.create(request);

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

        R<CustomerCO> result = controller.getById(1L);

        assertEquals(1L, result.getData().getId());
    }

    @Test
    void getById_notFound() {
        when(customerAppService.getById(999L))
                .thenThrow(BizException.fromError(ErrorCode.DATA_NOT_FOUND));

        assertThrows(BizException.class, () -> controller.getById(999L));
    }

    @Test
    void update_forwardsVersionAndPathIdAndReturnsPersistedVersion() {
        CustomerCO customer = new CustomerCO();
        customer.setId(42L);
        customer.setVersion(4);
        when(customerAppService.update(any(UpdateCustomerCmd.class))).thenReturn(customer);
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setVersion(3);
        request.setPhone("");

        R<CustomerCO> result = controller.update(42L, request);

        ArgumentCaptor<UpdateCustomerCmd> captor = ArgumentCaptor.forClass(UpdateCustomerCmd.class);
        verify(customerAppService).update(captor.capture());
        assertEquals(42L, captor.getValue().getId());
        assertEquals(3, captor.getValue().getVersion());
        assertEquals("", captor.getValue().getPhone());
        assertEquals(4, result.getData().getVersion());
    }

    @Test
    void list_success() {
        when(customerAppService.list(any())).thenReturn(PageResult.empty(1L, 10L));

        ListCustomerQry qry = new ListCustomerQry();
        qry.setPageIndex(1L);
        qry.setPageSize(10L);
        R<PageResult<CustomerCO>> result = controller.list(qry);

        assertNotNull(result);
    }

    @Test
    void list_withFilters_forwardsQueryCriteria() {
        when(customerAppService.list(any())).thenReturn(PageResult.empty(1L, 10L));

        ListCustomerQry qry = new ListCustomerQry();
        qry.setPageIndex(1L);
        qry.setPageSize(10L);
        qry.setKeyword("needle");
        qry.setStatus("ACTIVE");
        controller.list(qry);

        ArgumentCaptor<ListCustomerQry> captor = ArgumentCaptor.forClass(ListCustomerQry.class);
        verify(customerAppService).list(captor.capture());
        assertEquals("needle", captor.getValue().getKeyword());
        assertEquals("ACTIVE", captor.getValue().getStatus());
    }

    @Test
    void delete_success() {
        doNothing().when(customerAppService).delete(1L);

        R<?> result = controller.delete(1L);

        assertNotNull(result);
        verify(customerAppService).delete(1L);
    }

    @Test
    void delete_notFound() {
        doThrow(BizException.fromError(ErrorCode.DATA_NOT_FOUND))
                .when(customerAppService)
                .delete(999L);

        assertThrows(BizException.class, () -> controller.delete(999L));
    }
}
