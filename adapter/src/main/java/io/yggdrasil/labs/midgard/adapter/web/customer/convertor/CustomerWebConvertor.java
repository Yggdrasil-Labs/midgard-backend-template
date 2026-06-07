package io.yggdrasil.labs.midgard.adapter.web.customer.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import io.yggdrasil.labs.midgard.adapter.web.customer.dto.CreateCustomerRequest;
import io.yggdrasil.labs.midgard.adapter.web.customer.dto.UpdateCustomerRequest;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.UpdateCustomerCmd;

@Mapper
public interface CustomerWebConvertor {

    CustomerWebConvertor INSTANCE = Mappers.getMapper(CustomerWebConvertor.class);

    CreateCustomerCmd toCmd(CreateCustomerRequest request);

    UpdateCustomerCmd toCmd(UpdateCustomerRequest request);
}
