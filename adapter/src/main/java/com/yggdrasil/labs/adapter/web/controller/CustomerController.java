package com.yggdrasil.labs.adapter.web.controller;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;
import com.yggdrasil.labs.adapter.web.convert.CustomerWebConvertor;
import com.yggdrasil.labs.adapter.web.request.CreateCustomerRequest;
import com.yggdrasil.labs.client.api.CustomerFacade;
import com.yggdrasil.labs.client.dto.cmd.CreateCustomerCmd;
import com.yggdrasil.labs.client.dto.co.CustomerCO;
import com.yggdrasil.labs.client.dto.query.ListCustomerQuery;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    @Resource
    private CustomerFacade customerFacade;
    @Resource
    private CustomerWebConvertor customerWebConvertor;

    @GetMapping(value = "/list")
    public MultiResponse<CustomerCO> listCustomerByName(@RequestParam(required = false) String name) {
        ListCustomerQuery query = new ListCustomerQuery();
        query.setName(name);
        return customerFacade.listCustomerByName(query);
    }

    @PostMapping(value = "/add")
    public Response createCustomer(@RequestBody CreateCustomerRequest body) {
        CreateCustomerCmd cmd = customerWebConvertor.toCreateCustomerCmd(body);
        return customerFacade.createCustomer(cmd);
    }
}

