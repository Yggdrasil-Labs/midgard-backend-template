package io.yggdrasil.labs.midgard.app.customer.application;

import java.util.List;

import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.UpdateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.app.customer.dto.qry.ListCustomerQry;

public interface CustomerAppService {

    CustomerCO create(CreateCustomerCmd cmd);

    CustomerCO getById(Long id);

    List<CustomerCO> list(ListCustomerQry qry);

    long count(ListCustomerQry qry);

    CustomerCO update(UpdateCustomerCmd cmd);

    void delete(Long id);
}
