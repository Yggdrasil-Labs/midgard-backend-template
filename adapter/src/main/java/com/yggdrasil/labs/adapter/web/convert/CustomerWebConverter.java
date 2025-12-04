package com.yggdrasil.labs.adapter.web.convert;

import com.yggdrasil.labs.adapter.web.request.CreateCustomerRequest;
import com.yggdrasil.labs.client.dto.cmd.CreateCustomerCmd;
import org.mapstruct.Mapper;

/**
 * CustomerWebConverter：适配 Web Request 到 Client Cmd 的对象转换
 */
@Mapper(componentModel = "spring")
public interface CustomerWebConverter {

    /**
     * CustomerRequest -> CreateCustomerCmd
     */
    CreateCustomerCmd toCreateCustomerCmd(CreateCustomerRequest request);
}

