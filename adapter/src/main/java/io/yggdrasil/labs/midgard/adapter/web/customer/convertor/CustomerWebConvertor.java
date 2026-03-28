package io.yggdrasil.labs.midgard.adapter.web.customer.convertor;

import org.mapstruct.Mapper;

import io.yggdrasil.labs.midgard.adapter.web.customer.dto.CreateCustomerRequest;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;

/** CustomerWebConvertor：适配 Web Request 到应用层 Cmd 的对象转换 */
@Mapper(componentModel = "spring")
public interface CustomerWebConvertor {

    /** CustomerRequest -> CreateCustomerCmd */
    CreateCustomerCmd toCreateCustomerCmd(CreateCustomerRequest request);
}
