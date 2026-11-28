package io.yggdrasil.labs.midgard.app.customer.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yggdrasil.labs.common.exception.BizException;
import com.yggdrasil.labs.common.exception.ErrorCode;
import com.yggdrasil.labs.common.page.PageRequest;
import com.yggdrasil.labs.common.page.PageResult;

import io.yggdrasil.labs.midgard.app.customer.assembler.CustomerAssembler;
import io.yggdrasil.labs.midgard.app.customer.convertor.CustomerConvertor;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.UpdateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.app.customer.dto.qry.ListCustomerQry;
import io.yggdrasil.labs.midgard.domain.customer.exception.CustomerErrorCode;
import io.yggdrasil.labs.midgard.domain.customer.model.Customer;
import io.yggdrasil.labs.midgard.domain.customer.repo.CustomerRepository;
import lombok.RequiredArgsConstructor;

/** 实现客户用例并协调领域仓储与对象转换。 */
@Service
@RequiredArgsConstructor
public class CustomerAppServiceImpl implements CustomerAppService {

    private final CustomerRepository customerRepository;
    private static final CustomerConvertor CONVERTOR = CustomerConvertor.INSTANCE;
    private static final CustomerAssembler ASSEMBLER = CustomerAssembler.INSTANCE;

    @Override
    @Transactional
    public CustomerCO create(CreateCustomerCmd cmd) {
        if (customerRepository.existsByEmail(cmd.getEmail())) {
            throw BizException.fromError(CustomerErrorCode.EMAIL_ALREADY_EXISTS);
        }
        Customer customer = CONVERTOR.toEntity(cmd);
        customer.initForCreate();
        customer.validate();
        customerRepository.save(customer);
        return ASSEMBLER.toClientObject(customer);
    }

    @Override
    public CustomerCO getById(Long id) {
        Customer customer =
                customerRepository
                        .findById(id)
                        .orElseThrow(() -> BizException.fromError(ErrorCode.DATA_NOT_FOUND));
        return ASSEMBLER.toClientObject(customer);
    }

    @Override
    public PageResult<CustomerCO> list(ListCustomerQry qry) {
        PageRequest page = qry.toPageRequest();
        if (page.getPageIndex() > Integer.MAX_VALUE) {
            throw BizException.fromError(ErrorCode.PARAM_INVALID, "pageIndex超出支持范围");
        }
        return customerRepository
                .findPage(page, qry.getKeyword(), qry.getStatus())
                .map(ASSEMBLER::toClientObject);
    }

    @Override
    @Transactional
    public CustomerCO update(UpdateCustomerCmd cmd) {
        validateUpdate(cmd);
        Customer customer =
                customerRepository
                        .findById(cmd.getId())
                        .orElseThrow(() -> BizException.fromError(ErrorCode.DATA_NOT_FOUND));
        if (!cmd.getVersion().equals(customer.getVersion())) {
            throw BizException.fromError(CustomerErrorCode.VERSION_CONFLICT);
        }
        if (cmd.getName() != null) customer.setName(cmd.getName());
        if (cmd.getEmail() != null) {
            if (!cmd.getEmail().equals(customer.getEmail())
                    && customerRepository.existsByEmail(cmd.getEmail())) {
                throw BizException.fromError(CustomerErrorCode.EMAIL_ALREADY_EXISTS);
            }
            customer.setEmail(cmd.getEmail());
        }
        if (cmd.getPhone() != null) customer.setPhone(cmd.getPhone());
        customer.validate();
        return ASSEMBLER.toClientObject(customerRepository.update(customer));
    }

    private void validateUpdate(UpdateCustomerCmd cmd) {
        if (cmd.getVersion() == null || cmd.getVersion() < 0 || cmd.getVersion() > 2147483646) {
            throw BizException.fromError(ErrorCode.PARAM_INVALID, "客户版本必须在0-2147483646之间");
        }
        if (cmd.getName() == null && cmd.getEmail() == null && cmd.getPhone() == null) {
            throw BizException.fromError(ErrorCode.PARAM_INVALID, "至少一个字段不能为空");
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        customerRepository
                .findById(id)
                .orElseThrow(() -> BizException.fromError(ErrorCode.DATA_NOT_FOUND));
        customerRepository.deleteById(id);
    }
}
