package io.yggdrasil.labs.midgard.infrastructure.persistence.impl;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yggdrasil.labs.common.exception.BizException;
import com.yggdrasil.labs.common.exception.ErrorCode;
import com.yggdrasil.labs.common.page.PageRequest;
import com.yggdrasil.labs.common.page.PageResult;
import com.yggdrasil.labs.mybatis.page.PageConverters;

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
        dataObject.setVersion(0);
        try {
            if (customerMapper.insert(dataObject) != 1) {
                throw new IllegalStateException("客户插入未影响一行");
            }
        } catch (DataIntegrityViolationException exception) {
            throw translateWriteFailure(exception);
        }
        customer.setId(dataObject.getId());
        customer.setVersion(dataObject.getVersion());
        return customer;
    }

    @Override
    public Optional<Customer> findById(Long id) {
        CustomerDO dataObject = customerMapper.selectById(id);
        return Optional.ofNullable(dataObject).map(CONVERTOR::toEntity);
    }

    @Override
    public PageResult<Customer> findPage(PageRequest page, String keyword, String status) {
        LambdaQueryWrapper<CustomerDO> query =
                buildQueryWrapper(keyword, status).orderByDesc(CustomerDO::getId);
        Page<CustomerDO> pageResult =
                customerMapper.selectPage(PageConverters.toMybatisPage(page), query);
        return PageConverters.toPageResult(pageResult.convert(CONVERTOR::toEntity));
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
        Integer version = customer.getVersion();
        if (version == null || version < 0 || version == Integer.MAX_VALUE) {
            throw BizException.fromError(ErrorCode.PARAM_INVALID, "version必须在0-2147483646之间");
        }
        CustomerDO dataObject = CONVERTOR.toDataObject(customer);
        int affected;
        try {
            affected = customerMapper.updateById(dataObject);
        } catch (DataIntegrityViolationException exception) {
            throw translateWriteFailure(exception);
        }
        if (affected == 0) {
            CustomerDO current =
                    customerMapper.selectOne(
                            new LambdaQueryWrapper<CustomerDO>()
                                    .eq(CustomerDO::getId, customer.getId())
                                    .last("FOR UPDATE"));
            if (current == null) {
                throw BizException.fromError(ErrorCode.DATA_NOT_FOUND);
            }
            throw BizException.fromError(CustomerErrorCode.VERSION_CONFLICT);
        }
        if (affected != 1) {
            throw new IllegalStateException("客户更新未影响一行");
        }
        customer.setVersion(dataObject.getVersion());
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
