package io.yggdrasil.labs.midgard.app.customer.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.domain.customer.model.Customer;

/** 将应用层命令转换为领域对象。 */
@Mapper
public interface CustomerConvertor {

    CustomerConvertor INSTANCE = Mappers.getMapper(CustomerConvertor.class);

    /** 将创建客户命令转换为领域客户实体。 */
    Customer toEntity(CreateCustomerCmd cmd);
}
