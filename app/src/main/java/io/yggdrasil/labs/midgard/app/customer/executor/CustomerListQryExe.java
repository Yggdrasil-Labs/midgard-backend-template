package io.yggdrasil.labs.midgard.app.customer.executor;

import java.util.List;

import org.springframework.stereotype.Component;

import com.alibaba.cola.dto.MultiResponse;

import io.yggdrasil.labs.midgard.app.customer.assembler.CustomerAssembler;
import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.app.customer.dto.qry.ListCustomerQry;
import io.yggdrasil.labs.midgard.domain.customer.model.Customer;
import io.yggdrasil.labs.midgard.domain.customer.repo.CustomerRepository;

/**
 * CustomerListQryExe：查询客户列表执行器
 *
 * <p>负责编排查询客户的流程：查询 → 组装
 */
@Component
public class CustomerListQryExe {

    private final CustomerRepository customerRepository;

    private final CustomerAssembler customerAssembler;

    public CustomerListQryExe(
            CustomerRepository customerRepository, CustomerAssembler customerAssembler) {
        this.customerRepository = customerRepository;
        this.customerAssembler = customerAssembler;
    }

    /**
     * 执行查询客户列表
     *
     * <p>流程：
     *
     * <ol>
     *   <li>调用 Repository 查询
     *   <li>使用 Assembler 转换为 CO
     * </ol>
     *
     * @param query 查询条件
     * @return 客户列表
     */
    public MultiResponse<CustomerCO> execute(ListCustomerQry query) {
        // 1. 查询 Domain Entity
        List<Customer> customerList = customerRepository.findByNameLike(query.getName());

        // 2. Entity → CO
        List<CustomerCO> customerCOList = customerAssembler.toCustomerCOList(customerList);

        return MultiResponse.of(customerCOList);
    }
}
