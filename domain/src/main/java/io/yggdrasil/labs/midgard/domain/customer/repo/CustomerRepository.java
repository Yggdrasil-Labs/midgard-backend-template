package io.yggdrasil.labs.midgard.domain.customer.repo;

import java.util.Optional;

import com.yggdrasil.labs.common.page.PageRequest;
import com.yggdrasil.labs.common.page.PageResult;

import io.yggdrasil.labs.midgard.domain.customer.model.Customer;

/** 定义客户领域对象的持久化操作。 */
public interface CustomerRepository {

    /** 保存客户并返回持久化后的对象。 */
    Customer save(Customer customer);

    /** 按标识查询客户。 */
    Optional<Customer> findById(Long id);

    /**
     * 从同一次分页调用取得客户记录及总数，支持关键词和状态过滤。
     *
     * @param page 已纠正的分页请求
     * @param keyword 关键词（模糊匹配姓名或邮箱），可为null
     * @param status 状态过滤，可为null
     * @return 客户及分页元数据
     */
    PageResult<Customer> findPage(PageRequest page, String keyword, String status);

    /** 按客户的预期版本更新；成功返回新版本，缺失或冲突抛出业务异常。调用方须提供事务边界。 */
    Customer update(Customer customer);

    /** 按标识删除客户。 */
    void deleteById(Long id);

    /** 判断指定邮箱是否已被客户使用。 */
    boolean existsByEmail(String email);
}
