package io.yggdrasil.labs.midgard.app.customer.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.domain.customer.model.Customer;

@Mapper
public interface CustomerConvertor {

    CustomerConvertor INSTANCE = Mappers.getMapper(CustomerConvertor.class);

    Customer toEntity(CreateCustomerCmd cmd);
}
