package io.yggdrasil.labs.midgard.infrastructure.persistence.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.yggdrasil.labs.midgard.domain.customer.model.Customer;
import io.yggdrasil.labs.midgard.domain.customer.model.CustomerStatus;
import io.yggdrasil.labs.midgard.infrastructure.persistence.dataobject.CustomerDO;
import io.yggdrasil.labs.midgard.infrastructure.persistence.dataobject.mapper.CustomerMapper;

@ExtendWith(MockitoExtension.class)
class CustomerRepositoryImplTest {

    @Mock private CustomerMapper customerMapper;

    private CustomerRepositoryImpl repository;

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "repository-test"),
                CustomerDO.class);
    }

    @BeforeEach
    void setUp() {
        repository = new CustomerRepositoryImpl(customerMapper);
    }

    @Test
    void save_insertsDataObjectAndBackfillsGeneratedId() {
        Customer customer = customer(null, "Lin", "lin@example.com", CustomerStatus.ACTIVE);
        when(customerMapper.insert(any(CustomerDO.class)))
                .thenAnswer(
                        invocation -> {
                            ((CustomerDO) invocation.getArgument(0)).setId(41L);
                            return 1;
                        });

        Customer saved = repository.save(customer);

        assertThat(saved).isSameAs(customer);
        assertThat(saved.getId()).isEqualTo(41L);
        ArgumentCaptor<CustomerDO> inserted = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getName()).isEqualTo("Lin");
        assertThat(inserted.getValue().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void findById_returnsConvertedCustomerWhenPresent() {
        CustomerDO dataObject = dataObject(7L, "Lin", "lin@example.com", "INACTIVE");
        when(customerMapper.selectById(7L)).thenReturn(dataObject);

        Optional<Customer> result = repository.findById(7L);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId()).isEqualTo(7L);
        assertThat(result.orElseThrow().getStatus()).isEqualTo(CustomerStatus.INACTIVE);
    }

    @Test
    void findById_returnsEmptyWhenMissing() {
        when(customerMapper.selectById(404L)).thenReturn(null);

        assertThat(repository.findById(404L)).isEmpty();
    }

    @Test
    void findAll_passesPaginationFiltersAndDescendingIdOrder() {
        when(customerMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(
                        invocation -> {
                            Page<CustomerDO> page = invocation.getArgument(0);
                            page.setRecords(
                                    List.of(dataObject(9L, "Lin", "lin@example.com", "ACTIVE")));
                            return page;
                        });

        List<Customer> result = repository.findAll(2, 15, "lin", "ACTIVE");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(9L);
        ArgumentCaptor<Page<CustomerDO>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<LambdaQueryWrapper<CustomerDO>> queryCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(customerMapper).selectPage(pageCaptor.capture(), queryCaptor.capture());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(2);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(15);
        assertThat(queryCaptor.getValue().getSqlSegment()).contains("ORDER BY id DESC");
        assertThat(queryCaptor.getValue().getParamNameValuePairs().values())
                .containsExactlyInAnyOrder("%lin%", "%lin%", "ACTIVE");
    }

    @Test
    void findAll_ignoresNullAndBlankFilters() {
        when(customerMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(repository.findAll(1, 10, "  ", null)).isEmpty();
        assertThat(repository.findAll(1, 10, null, " ")).isEmpty();

        ArgumentCaptor<LambdaQueryWrapper<CustomerDO>> queryCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(customerMapper, org.mockito.Mockito.times(2))
                .selectPage(any(Page.class), queryCaptor.capture());
        assertThat(queryCaptor.getAllValues())
                .allSatisfy(
                        wrapper -> {
                            assertThat(wrapper.getSqlSegment()).contains("ORDER BY id DESC");
                            assertThat(wrapper.getParamNameValuePairs()).isEmpty();
                        });
    }

    @Test
    void count_passesKeywordAndStatusFilters() {
        when(customerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        long count = repository.count("lin", "INACTIVE");

        assertThat(count).isEqualTo(3L);
        ArgumentCaptor<LambdaQueryWrapper<CustomerDO>> queryCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(customerMapper).selectCount(queryCaptor.capture());
        queryCaptor.getValue().getSqlSegment();
        assertThat(queryCaptor.getValue().getParamNameValuePairs().values())
                .containsExactlyInAnyOrder("%lin%", "%lin%", "INACTIVE");
    }

    @Test
    void update_convertsAndPassesDataObjectToMapper() {
        Customer customer = customer(8L, "Lin", "lin@example.com", CustomerStatus.INACTIVE);

        assertThat(repository.update(customer)).isSameAs(customer);

        ArgumentCaptor<CustomerDO> updated = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).updateById(updated.capture());
        assertThat(updated.getValue().getId()).isEqualTo(8L);
        assertThat(updated.getValue().getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void deleteById_delegatesToMapper() {
        repository.deleteById(8L);

        verify(customerMapper).deleteById(8L);
    }

    @Test
    void existsByEmail_returnsWhetherMapperFoundMatches() {
        when(customerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L, 0L);

        assertThat(repository.existsByEmail("lin@example.com")).isTrue();
        assertThat(repository.existsByEmail("missing@example.com")).isFalse();

        ArgumentCaptor<LambdaQueryWrapper<CustomerDO>> queryCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(customerMapper, org.mockito.Mockito.times(2)).selectCount(queryCaptor.capture());
        queryCaptor.getAllValues().forEach(LambdaQueryWrapper::getSqlSegment);
        assertThat(queryCaptor.getAllValues().get(0).getParamNameValuePairs().values())
                .contains("lin@example.com");
        assertThat(queryCaptor.getAllValues().get(1).getParamNameValuePairs().values())
                .contains("missing@example.com");
    }

    private static Customer customer(Long id, String name, String email, CustomerStatus status) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName(name);
        customer.setEmail(email);
        customer.setStatus(status);
        return customer;
    }

    private static CustomerDO dataObject(Long id, String name, String email, String status) {
        CustomerDO dataObject = new CustomerDO();
        dataObject.setId(id);
        dataObject.setName(name);
        dataObject.setEmail(email);
        dataObject.setStatus(status);
        return dataObject;
    }
}
