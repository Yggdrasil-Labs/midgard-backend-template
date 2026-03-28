package io.yggdrasil.labs.midgard.adapter.web.customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;

import io.yggdrasil.labs.midgard.adapter.web.customer.convertor.CustomerWebConvertor;
import io.yggdrasil.labs.midgard.adapter.web.customer.dto.CreateCustomerRequest;
import io.yggdrasil.labs.midgard.app.customer.application.CustomerAppService;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.app.customer.dto.qry.ListCustomerQry;

/**
 * 客户管理 Controller
 *
 * <p>提供客户相关的 REST API
 */
@Validated
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerAppService customerApplicationService;
    private final CustomerWebConvertor customerWebConvertor;

    public CustomerController(
            CustomerAppService customerApplicationService,
            CustomerWebConvertor customerWebConvertor) {
        this.customerApplicationService = customerApplicationService;
        this.customerWebConvertor = customerWebConvertor;
    }

    /**
     * 按名称查询客户列表
     *
     * @param name 客户名称（支持模糊查询）
     * @return 客户列表
     */
    @GetMapping
    public MultiResponse<CustomerCO> listCustomerByName(
            @RequestParam(required = false) @Size(max = 100, message = "客户名称长度不能超过100个字符")
                    String name) {
        ListCustomerQry query = new ListCustomerQry();
        query.setName(name);
        return customerApplicationService.listCustomerByName(query);
    }

    /**
     * 创建客户
     *
     * @param body 创建客户请求
     * @return 创建结果
     */
    @PostMapping
    public Response createCustomer(@Valid @RequestBody CreateCustomerRequest body) {
        CreateCustomerCmd cmd = customerWebConvertor.toCreateCustomerCmd(body);
        return customerApplicationService.createCustomer(cmd);
    }
}
