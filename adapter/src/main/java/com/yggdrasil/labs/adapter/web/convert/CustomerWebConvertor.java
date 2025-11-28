package com.yggdrasil.labs.adapter.web.convert;

import com.yggdrasil.labs.adapter.web.request.CreateCustomerRequest;
import com.yggdrasil.labs.client.dto.cmd.CreateCustomerCmd;
import org.mapstruct.Mapper;

/**
 * CustomerWebConvertor：适配 Web Request 到 Client Cmd 的对象转换
 */
@Mapper(componentModel = "spring")
public interface CustomerWebConvertor {

    /**
     * CustomerRequest -> CreateCustomerCmd
     */
    CreateCustomerCmd toCreateCustomerCmd(CreateCustomerRequest request);
}


