package io.yggdrasil.labs.midgard.infrastructure.persistence.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.yggdrasil.labs.midgard.domain.customer.model.Customer;
import io.yggdrasil.labs.midgard.domain.customer.repo.CustomerRepository;
import io.yggdrasil.labs.midgard.infrastructure.persistence.convertor.CustomerInfraConvertor;
import io.yggdrasil.labs.midgard.infrastructure.persistence.dataobject.CustomerDO;
import io.yggdrasil.labs.midgard.infrastructure.persistence.dataobject.mapper.CustomerMapper;

/**
 * CustomerRepository 实现类
 *
 * <p>使用 @AutoMybatis 自动生成的 Mapper（CustomerMapper）
 *
 * <p>注意：@AutoMybatis 会在编译期生成 CustomerMapper 和 CustomerService
 */
@Repository
public class CustomerRepositoryImpl implements CustomerRepository {

    private final CustomerMapper customerMapper;

    private final CustomerInfraConvertor customerInfraConvertor;

    public CustomerRepositoryImpl(
            CustomerMapper customerMapper, CustomerInfraConvertor customerInfraConvertor) {
        this.customerMapper = customerMapper;
        this.customerInfraConvertor = customerInfraConvertor;
    }

    @Override
    public Customer findById(String customerId) {
        CustomerDO customerDO = customerMapper.selectById(customerId);
        return customerDO == null ? null : customerInfraConvertor.toEntity(customerDO);
    }

    @Override
    public List<Customer> findByNameLike(String name) {
        LambdaQueryWrapper<CustomerDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(CustomerDO::getCompanyName, name);
        List<CustomerDO> customerDOList = customerMapper.selectList(wrapper);
        return customerInfraConvertor.toEntityList(customerDOList);
    }

    @Override
    public void save(Customer customer) {
        CustomerDO customerDO = customerInfraConvertor.toDO(customer);
        customerMapper.insert(customerDO);
    }

    @Override
    public void update(Customer customer) {
        CustomerDO customerDO = customerInfraConvertor.toDO(customer);
        customerMapper.updateById(customerDO);
    }

    @Override
    public void delete(String customerId) {
        customerMapper.deleteById(customerId);
    }

    @Override
    public boolean existsByCompanyName(String companyName) {
        LambdaQueryWrapper<CustomerDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerDO::getCompanyName, companyName);
        return customerMapper.selectCount(wrapper) > 0;
    }
}
