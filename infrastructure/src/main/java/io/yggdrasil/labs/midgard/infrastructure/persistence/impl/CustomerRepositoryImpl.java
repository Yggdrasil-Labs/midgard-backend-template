package io.yggdrasil.labs.midgard.infrastructure.persistence.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.yggdrasil.labs.midgard.domain.customer.model.Customer;
import io.yggdrasil.labs.midgard.domain.customer.repo.CustomerRepository;
import io.yggdrasil.labs.midgard.infrastructure.persistence.convertor.CustomerInfraConvertor;
import io.yggdrasil.labs.midgard.infrastructure.persistence.dataobject.CustomerDO;
import io.yggdrasil.labs.midgard.infrastructure.persistence.dataobject.CustomerDOMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CustomerRepositoryImpl implements CustomerRepository {

    private final CustomerDOMapper customerMapper;
    private static final CustomerInfraConvertor CONVERTOR = CustomerInfraConvertor.INSTANCE;

    @Override
    public Customer save(Customer customer) {
        CustomerDO dataObject = CONVERTOR.toDataObject(customer);
        customerMapper.insert(dataObject);
        customer.setId(dataObject.getId());
        return customer;
    }

    @Override
    public Optional<Customer> findById(Long id) {
        CustomerDO dataObject = customerMapper.selectById(id);
        return Optional.ofNullable(dataObject).map(CONVERTOR::toEntity);
    }

    @Override
    public List<Customer> findAll(int page, int size) {
        Page<CustomerDO> pageResult = customerMapper.selectPage(
                new Page<>(page, size), new LambdaQueryWrapper<>());
        return pageResult.getRecords().stream()
                .map(CONVERTOR::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return customerMapper.selectCount(new LambdaQueryWrapper<>());
    }

    @Override
    public Customer update(Customer customer) {
        CustomerDO dataObject = CONVERTOR.toDataObject(customer);
        customerMapper.updateById(dataObject);
        return customer;
    }

    @Override
    public void deleteById(Long id) {
        customerMapper.deleteById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return customerMapper.selectCount(
                new LambdaQueryWrapper<CustomerDO>().eq(CustomerDO::getEmail, email)) > 0;
    }
}
