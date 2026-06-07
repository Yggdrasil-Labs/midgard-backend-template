package io.yggdrasil.labs.midgard.app.customer.application;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.cola.exception.BizException;

import io.yggdrasil.labs.midgard.app.customer.assembler.CustomerAssembler;
import io.yggdrasil.labs.midgard.app.customer.convertor.CustomerConvertor;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.UpdateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.app.customer.dto.qry.ListCustomerQry;
import io.yggdrasil.labs.midgard.domain.customer.model.Customer;
import io.yggdrasil.labs.midgard.domain.customer.repo.CustomerRepository;
import lombok.RequiredArgsConstructor;

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
            throw new BizException("DUPLICATE_EMAIL", "邮箱已被占用");
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
                        .orElseThrow(() -> new BizException("NOT_FOUND", "客户不存在"));
        return ASSEMBLER.toClientObject(customer);
    }

    @Override
    public List<CustomerCO> list(ListCustomerQry qry) {
        return customerRepository.findAll(qry.getPage(), qry.getSize()).stream()
                .map(ASSEMBLER::toClientObject)
                .collect(Collectors.toList());
    }

    @Override
    public long count(ListCustomerQry qry) {
        return customerRepository.count();
    }

    @Override
    @Transactional
    public CustomerCO update(UpdateCustomerCmd cmd) {
        Customer customer =
                customerRepository
                        .findById(cmd.getId())
                        .orElseThrow(() -> new BizException("NOT_FOUND", "客户不存在"));
        if (cmd.getName() != null) customer.setName(cmd.getName());
        if (cmd.getEmail() != null) {
            if (!cmd.getEmail().equals(customer.getEmail())
                    && customerRepository.existsByEmail(cmd.getEmail())) {
                throw new BizException("DUPLICATE_EMAIL", "邮箱已被占用");
            }
            customer.setEmail(cmd.getEmail());
        }
        if (cmd.getPhone() != null) customer.setPhone(cmd.getPhone());
        customer.validate();
        customerRepository.update(customer);
        return ASSEMBLER.toClientObject(customer);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        customerRepository.findById(id).orElseThrow(() -> new BizException("NOT_FOUND", "客户不存在"));
        customerRepository.deleteById(id);
    }
}
