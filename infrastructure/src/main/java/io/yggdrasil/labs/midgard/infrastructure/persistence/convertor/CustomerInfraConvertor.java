package io.yggdrasil.labs.midgard.infrastructure.persistence.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import io.yggdrasil.labs.midgard.domain.customer.model.Customer;
import io.yggdrasil.labs.midgard.domain.customer.model.CustomerStatus;
import io.yggdrasil.labs.midgard.infrastructure.persistence.dataobject.CustomerDO;

/** 客户领域对象与持久化对象之间的转换。 */
@Mapper
public interface CustomerInfraConvertor {

    CustomerInfraConvertor INSTANCE = Mappers.getMapper(CustomerInfraConvertor.class);

    /** 将客户领域对象转换为数据库记录，空对象返回空值。 */
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    CustomerDO toDataObject(Customer entity);

    /** 将数据库记录转换为客户领域对象，空对象返回空值。 */
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    Customer toEntity(CustomerDO dataObject);

    /** 返回客户状态的枚举名称，保留空值。 */
    @Named("statusToString")
    default String statusToString(CustomerStatus status) {
        return status == null ? null : status.name();
    }

    /** 解析状态枚举名称，保留空值；未知名称抛出 {@link IllegalArgumentException}。 */
    @Named("stringToStatus")
    default CustomerStatus stringToStatus(String status) {
        return status == null ? null : CustomerStatus.valueOf(status);
    }
}
