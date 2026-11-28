package io.yggdrasil.labs.midgard.app.customer.assembler;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.domain.customer.model.Customer;
import io.yggdrasil.labs.midgard.domain.customer.model.CustomerStatus;

/** 将领域客户对象组装为应用层客户视图。 */
@Mapper
public interface CustomerAssembler {

    CustomerAssembler INSTANCE = Mappers.getMapper(CustomerAssembler.class);

    /** 将客户实体转换为客户端视图对象。 */
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    CustomerCO toClientObject(Customer entity);

    /** 将客户状态转换为名称，保留空状态为 {@code null}。 */
    @Named("statusToString")
    default String statusToString(CustomerStatus status) {
        return status == null ? null : status.name();
    }
}
