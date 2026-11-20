package io.yggdrasil.labs.midgard.app.customer.application;

import java.util.List;

import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.UpdateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.app.customer.dto.qry.ListCustomerQry;

/** 提供客户用例的应用服务。 */
public interface CustomerAppService {

    /** 创建客户并返回客户视图。 */
    CustomerCO create(CreateCustomerCmd cmd);

    /** 按 ID 查询客户；客户不存在时由实现抛出业务异常。 */
    CustomerCO getById(Long id);

    /** 按查询条件分页查询客户视图。 */
    List<CustomerCO> list(ListCustomerQry qry);

    /** 统计符合查询条件的客户数量。 */
    long count(ListCustomerQry qry);

    /** 更新客户并返回更新后的客户视图。 */
    CustomerCO update(UpdateCustomerCmd cmd);

    /** 删除指定 ID 的客户；客户不存在时由实现抛出业务异常。 */
    void delete(Long id);
}
