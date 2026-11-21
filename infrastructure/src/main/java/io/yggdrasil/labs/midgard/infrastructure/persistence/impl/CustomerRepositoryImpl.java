package io.yggdrasil.labs.midgard.infrastructure.persistence.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yggdrasil.labs.common.exception.BizException;

import io.yggdrasil.labs.midgard.domain.customer.exception.CustomerErrorCode;
import io.yggdrasil.labs.midgard.domain.customer.model.Customer;
import io.yggdrasil.labs.midgard.domain.customer.repo.CustomerRepository;
import io.yggdrasil.labs.midgard.infrastructure.persistence.convertor.CustomerInfraConvertor;
import io.yggdrasil.labs.midgard.infrastructure.persistence.dataobject.CustomerDO;
import io.yggdrasil.labs.midgard.infrastructure.persistence.dataobject.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;

/** 使用 MyBatis-Plus 实现客户仓储。 */
@Repository
@RequiredArgsConstructor
public class CustomerRepositoryImpl implements CustomerRepository {

    private final CustomerMapper customerMapper;
    private static final CustomerInfraConvertor CONVERTOR = CustomerInfraConvertor.INSTANCE;

    @Override
    public Customer save(Customer customer) {
        CustomerDO dataObject = CONVERTOR.toDataObject(customer);
        try {
            customerMapper.insert(dataObject);
        } catch (DataIntegrityViolationException exception) {
            throw translateWriteFailure(exception);
        }
        customer.setId(dataObject.getId());
        return customer;
    }

    @Override
    public Optional<Customer> findById(Long id) {
        CustomerDO dataObject = customerMapper.selectById(id);
        return Optional.ofNullable(dataObject).map(CONVERTOR::toEntity);
    }

    @Override
    public List<Customer> findAll(int page, int size, String keyword, String status) {
        LambdaQueryWrapper<CustomerDO> query =
                buildQueryWrapper(keyword, status).orderByDesc(CustomerDO::getId);
        Page<CustomerDO> pageResult = customerMapper.selectPage(new Page<>(page, size), query);
        return pageResult.getRecords().stream()
                .map(CONVERTOR::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long count(String keyword, String status) {
        return customerMapper.selectCount(buildQueryWrapper(keyword, status));
    }

    private LambdaQueryWrapper<CustomerDO> buildQueryWrapper(String keyword, String status) {
        LambdaQueryWrapper<CustomerDO> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            query.and(
                    wrapper ->
                            wrapper.like(CustomerDO::getName, keyword)
                                    .or()
                                    .like(CustomerDO::getEmail, keyword));
        }
        if (status != null && !status.isBlank()) {
            query.eq(CustomerDO::getStatus, status);
        }
        return query;
    }

    @Override
    public Customer update(Customer customer) {
        CustomerDO dataObject = CONVERTOR.toDataObject(customer);
        try {
            customerMapper.updateById(dataObject);
        } catch (DataIntegrityViolationException exception) {
            throw translateWriteFailure(exception);
        }
        return customer;
    }

    private RuntimeException translateWriteFailure(DataIntegrityViolationException exception) {
        if (EmailUniqueConstraint.matches(exception)) {
            return BizException.fromError(CustomerErrorCode.EMAIL_ALREADY_EXISTS, null, exception);
        }
        return exception;
    }

    @Override
    public void deleteById(Long id) {
        customerMapper.deleteById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return customerMapper.selectCount(
                        new LambdaQueryWrapper<CustomerDO>().eq(CustomerDO::getEmail, email))
                > 0;
    }
}
