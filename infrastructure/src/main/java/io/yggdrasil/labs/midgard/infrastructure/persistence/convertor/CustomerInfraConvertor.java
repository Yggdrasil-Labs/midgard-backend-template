package io.yggdrasil.labs.midgard.infrastructure.persistence.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import io.yggdrasil.labs.midgard.domain.customer.model.Customer;
import io.yggdrasil.labs.midgard.domain.customer.model.CustomerStatus;
import io.yggdrasil.labs.midgard.infrastructure.persistence.dataobject.CustomerDO;

@Mapper
public interface CustomerInfraConvertor {

    CustomerInfraConvertor INSTANCE = Mappers.getMapper(CustomerInfraConvertor.class);

    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    CustomerDO toDataObject(Customer entity);

    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    Customer toEntity(CustomerDO dataObject);

    @Named("statusToString")
    default String statusToString(CustomerStatus status) {
        return status == null ? null : status.name();
    }

    @Named("stringToStatus")
    default CustomerStatus stringToStatus(String status) {
        return status == null ? null : CustomerStatus.valueOf(status);
    }
}
