package io.yggdrasil.labs.midgard.adapter.web.customer;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.BizException;

import io.yggdrasil.labs.midgard.adapter.web.customer.convertor.CustomerWebConvertor;
import io.yggdrasil.labs.midgard.adapter.web.customer.dto.CreateCustomerRequest;
import io.yggdrasil.labs.midgard.adapter.web.customer.dto.UpdateCustomerRequest;
import io.yggdrasil.labs.midgard.app.customer.application.CustomerAppService;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.UpdateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.app.customer.dto.qry.ListCustomerQry;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerAppService customerAppService;
    private static final CustomerWebConvertor CONVERTOR = CustomerWebConvertor.INSTANCE;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SingleResponse<CustomerCO> create(@Valid @RequestBody CreateCustomerRequest request) {
        return SingleResponse.of(customerAppService.create(CONVERTOR.toCmd(request)));
    }

    @GetMapping("/{id}")
    public SingleResponse<CustomerCO> getById(@PathVariable Long id) {
        return SingleResponse.of(customerAppService.getById(id));
    }

    @GetMapping
    public PageResponse<CustomerCO> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (page < 1 || size < 1) {
            throw new BizException("VALIDATION_ERROR", "page和size必须大于0");
        }
        int actualSize = Math.min(size, 100);
        ListCustomerQry qry = new ListCustomerQry();
        qry.setPage(page);
        qry.setSize(actualSize);
        List<CustomerCO> records = customerAppService.list(qry);
        long total = customerAppService.count(qry);
        return PageResponse.of(records, (int) total, actualSize, page);
    }

    @PutMapping("/{id}")
    public SingleResponse<CustomerCO> update(
            @PathVariable Long id, @Valid @RequestBody UpdateCustomerRequest request) {
        if (request.getName() == null && request.getEmail() == null && request.getPhone() == null) {
            throw new BizException("VALIDATION_ERROR", "至少一个字段不能为空");
        }
        UpdateCustomerCmd cmd = CONVERTOR.toCmd(request);
        cmd.setId(id);
        return SingleResponse.of(customerAppService.update(cmd));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Response delete(@PathVariable Long id) {
        customerAppService.delete(id);
        return Response.buildSuccess();
    }
}
