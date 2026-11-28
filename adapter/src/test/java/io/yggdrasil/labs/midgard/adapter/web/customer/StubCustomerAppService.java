package io.yggdrasil.labs.midgard.adapter.web.customer;

import com.yggdrasil.labs.common.page.PageResult;

import io.yggdrasil.labs.midgard.app.customer.application.CustomerAppService;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.UpdateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.app.customer.dto.qry.ListCustomerQry;

/** 仅记录 HTTP 边界调用并返回预设结果，不实现业务或持久化行为。 */
final class StubCustomerAppService implements CustomerAppService {

    CustomerCO result = CustomerContractFixture.customer();
    PageResult<CustomerCO> page = PageResult.empty(1L, 10L);
    RuntimeException failure;
    int calls;
    CreateCustomerCmd created;
    UpdateCustomerCmd updated;
    ListCustomerQry query;
    Long id;

    @Override
    public CustomerCO create(CreateCustomerCmd cmd) {
        created = cmd;
        recordCall();
        return result;
    }

    @Override
    public CustomerCO getById(Long customerId) {
        id = customerId;
        recordCall();
        return result;
    }

    @Override
    public PageResult<CustomerCO> list(ListCustomerQry qry) {
        query = qry;
        recordCall();
        return page;
    }

    @Override
    public CustomerCO update(UpdateCustomerCmd cmd) {
        updated = cmd;
        recordCall();
        return result;
    }

    @Override
    public void delete(Long customerId) {
        id = customerId;
        recordCall();
    }

    private void recordCall() {
        calls++;
        if (failure != null) {
            throw failure;
        }
    }
}
