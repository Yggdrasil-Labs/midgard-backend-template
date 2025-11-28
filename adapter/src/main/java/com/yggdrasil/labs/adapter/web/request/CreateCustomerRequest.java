package com.yggdrasil.labs.adapter.web.request;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * Client DTO：Create/Update Customer 请求体
 */
@Data
public class CreateCustomerRequest {
    private String customerId;
    private String memberId;
    private String customerName;
    private String customerType;
    @NotEmpty
    private String companyName;
    @NotEmpty
    private String source;
}


