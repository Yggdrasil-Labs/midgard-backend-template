package io.yggdrasil.labs.midgard.app.customer.assembler;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.domain.customer.model.Customer;
import io.yggdrasil.labs.midgard.domain.customer.model.CustomerStatus;

@Mapper
public interface CustomerAssembler {

    CustomerAssembler INSTANCE = Mappers.getMapper(CustomerAssembler.class);

    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    CustomerCO toClientObject(Customer entity);

    @Named("statusToString")
    default String statusToString(CustomerStatus status) {
        return status == null ? null : status.name();
    }
}
