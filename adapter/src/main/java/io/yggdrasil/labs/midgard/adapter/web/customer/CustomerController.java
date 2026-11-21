package io.yggdrasil.labs.midgard.adapter.web.customer;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.yggdrasil.labs.common.page.PageResult;
import com.yggdrasil.labs.common.response.R;

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

    /** 创建客户并返回统一客户响应。 */
    @PostMapping
    public R<CustomerCO> create(@Valid @RequestBody CreateCustomerRequest request) {
        return R.success(customerAppService.create(CONVERTOR.toCmd(request)));
    }

    /** 按 ID 查询客户并返回客户视图。 */
    @GetMapping("/{id}")
    public R<CustomerCO> getById(@PathVariable Long id) {
        return R.success(customerAppService.getById(id));
    }

    /** 按条件分页查询客户，并返回总数和分页信息。 */
    @GetMapping
    public R<PageResult<CustomerCO>> list(@Valid @ModelAttribute ListCustomerQry qry) {
        return R.success(customerAppService.list(qry));
    }

    /** 按路径 ID 更新客户并返回更新后的客户视图。 */
    @PutMapping("/{id}")
    public R<CustomerCO> update(
            @PathVariable Long id, @Valid @RequestBody UpdateCustomerRequest request) {
        UpdateCustomerCmd cmd = CONVERTOR.toCmd(request);
        cmd.setId(id);
        return R.success(customerAppService.update(cmd));
    }

    /** 删除指定 ID 的客户并返回空数据的成功信封。 */
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        customerAppService.delete(id);
        return R.success();
    }
}
