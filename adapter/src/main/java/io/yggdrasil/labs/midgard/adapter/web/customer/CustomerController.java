package io.yggdrasil.labs.midgard.adapter.web.customer;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;

import io.yggdrasil.labs.midgard.adapter.web.customer.convertor.CustomerWebConvertor;
import io.yggdrasil.labs.midgard.adapter.web.customer.dto.CreateCustomerRequest;
import io.yggdrasil.labs.midgard.adapter.web.customer.dto.UpdateCustomerRequest;
import io.yggdrasil.labs.midgard.app.customer.application.CustomerAppService;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.UpdateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.app.customer.dto.qry.ListCustomerQry;
import lombok.RequiredArgsConstructor;

/** 暴露客户资源的 HTTP 接口。 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerAppService customerAppService;
    private static final CustomerWebConvertor CONVERTOR = CustomerWebConvertor.INSTANCE;

    /** 创建客户并返回 HTTP 201 及客户视图。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SingleResponse<CustomerCO> create(@Valid @RequestBody CreateCustomerRequest request) {
        return SingleResponse.of(customerAppService.create(CONVERTOR.toCmd(request)));
    }

    /** 按 ID 查询客户并返回客户视图。 */
    @GetMapping("/{id}")
    public SingleResponse<CustomerCO> getById(@PathVariable Long id) {
        return SingleResponse.of(customerAppService.getById(id));
    }

    /** 按条件分页查询客户，并返回总数和分页信息。 */
    @GetMapping
    public PageResponse<CustomerCO> list(@Valid @ModelAttribute ListCustomerQry qry) {
        List<CustomerCO> records = customerAppService.list(qry);
        long total = customerAppService.count(qry);
        return PageResponse.of(records, (int) total, qry.getSize(), qry.getPage());
    }

    /** 按路径 ID 更新客户并返回更新后的客户视图。 */
    @PutMapping("/{id}")
    public SingleResponse<CustomerCO> update(
            @PathVariable Long id, @Valid @RequestBody UpdateCustomerRequest request) {
        UpdateCustomerCmd cmd = CONVERTOR.toCmd(request);
        cmd.setId(id);
        return SingleResponse.of(customerAppService.update(cmd));
    }

    /** 删除指定 ID 的客户并返回 HTTP 204。 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Response delete(@PathVariable Long id) {
        customerAppService.delete(id);
        return Response.buildSuccess();
    }
}
