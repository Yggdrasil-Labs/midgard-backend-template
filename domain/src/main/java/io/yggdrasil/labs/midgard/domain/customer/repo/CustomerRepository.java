package io.yggdrasil.labs.midgard.domain.customer.repo;

import java.util.List;
import java.util.Optional;

import io.yggdrasil.labs.midgard.domain.customer.model.Customer;

public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(Long id);

    /**
     * 分页查询全部客户（向后兼容）。
     *
     * @param page 页码（从1开始）
     * @param size 每页条数
     * @return 客户列表
     */
    default List<Customer> findAll(int page, int size) {
        return findAll(page, size, null, null);
    }

    /**
     * 分页查询客户，支持关键词和状态过滤。
     *
     * @param page 页码（从1开始）
     * @param size 每页条数
     * @param keyword 关键词（模糊匹配姓名或邮箱），可为null
     * @param status 状态过滤，可为null
     * @return 客户列表
     */
    List<Customer> findAll(int page, int size, String keyword, String status);

    /**
     * 统计全部客户数量（向后兼容）。
     *
     * @return 客户总数
     */
    default long count() {
        return count(null, null);
    }

    /**
     * 统计符合条件的客户数量。
     *
     * @param keyword 关键词（模糊匹配姓名或邮箱），可为null
     * @param status 状态过滤，可为null
     * @return 符合条件的客户数量
     */
    long count(String keyword, String status);

    Customer update(Customer customer);

    void deleteById(Long id);

    boolean existsByEmail(String email);
}
