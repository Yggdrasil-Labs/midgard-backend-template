package io.yggdrasil.labs.midgard.adapter.web.customer.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import io.yggdrasil.labs.midgard.adapter.web.customer.dto.CreateCustomerRequest;
import io.yggdrasil.labs.midgard.adapter.web.customer.dto.UpdateCustomerRequest;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.UpdateCustomerCmd;

/** 将客户 Web 请求转换为应用层命令。 */
@Mapper
public interface CustomerWebConvertor {

    CustomerWebConvertor INSTANCE = Mappers.getMapper(CustomerWebConvertor.class);

    /** 将创建客户请求转换为应用层命令。 */
    CreateCustomerCmd toCmd(CreateCustomerRequest request);

    /** 将更新客户请求转换为应用层命令。 */
    UpdateCustomerCmd toCmd(UpdateCustomerRequest request);
}
