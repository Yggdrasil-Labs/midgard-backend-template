package io.yggdrasil.labs.midgard.infrastructure.persistence.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yggdrasil.labs.common.exception.BizException;

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

    @ParameterizedTest
    @MethodSource("emailConflicts")
    void write_translatesOnlyConfirmedEmailConflictAndPreservesCause(
            boolean update, DataIntegrityViolationException failure) {
        stubWriteFailure(update, failure);

        BizException translated = assertThrows(BizException.class, () -> write(update));

        assertThat(translated.getCode()).isEqualTo("21001");
        assertThat(translated.getMessage()).isEqualTo("邮箱已被占用");
        assertThat(translated.getCause()).isSameAs(failure);
    }

    @ParameterizedTest
    @MethodSource("otherFailures")
    void write_preservesUnrelatedOrUnidentifiableFailure(boolean update, RuntimeException failure) {
        stubWriteFailure(update, failure);

        assertThatThrownBy(() -> write(update)).isSameAs(failure);
    }

    private void stubWriteFailure(boolean update, RuntimeException failure) {
        if (update) {
            when(customerMapper.updateById(any(CustomerDO.class))).thenThrow(failure);
        } else {
            when(customerMapper.insert(any(CustomerDO.class))).thenThrow(failure);
        }
    }

    private void write(boolean update) {
        Customer customer = customer(8L, "Lin", "lin@example.com", CustomerStatus.ACTIVE);
        if (update) {
            repository.update(customer);
        } else {
            repository.save(customer);
        }
    }

    private static Stream<Arguments> emailConflicts() {
        return bothWriteOperations(
                new DataIntegrityViolationException(
                        "Duplicate entry 'taken@example.com' for key 'idx_email'"),
                new DataIntegrityViolationException(
                        "wrapper SQL mentions idx_other",
                        new SQLException(
                                "Duplicate entry 'taken@example.com' for key 'customer.IDX_EMAIL'",
                                "23000",
                                1062)),
                new DataIntegrityViolationException(
                        null,
                        new IllegalStateException(
                                new SQLException(
                                        "Duplicate entry 'taken@example.com' for key"
                                                + " `customer`.`idx_email`",
                                        "23000",
                                        0))),
                new DataIntegrityViolationException(
                        null,
                        new SQLException(
                                "Constraint violation for key 'idx_email'", "23000", 1062)),
                new DataIntegrityViolationException("dUpLiCaTe EnTrY 'value' FoR KeY idx_email"),
                new DataIntegrityViolationException(
                        "Duplicate entry 'value' for key `customer.idx_email`"));
    }

    private static Stream<Arguments> otherFailures() {
        return bothWriteOperations(
                new DataIntegrityViolationException("Duplicate entry 'value' for key 'idx_name'"),
                new DataIntegrityViolationException(
                        "Duplicate entry 'value' for key 'idx_email_backup'"),
                new DataIntegrityViolationException(
                        "Duplicate entry 'value' for key 'idx_email.idx_name'"),
                new DataIntegrityViolationException(
                        "Duplicate entry 'value' for key 'customer_idx_email'"),
                new DataIntegrityViolationException("Duplicate entry 'value' for key 'idx_email\""),
                new DataIntegrityViolationException(
                        "Duplicate entry 'value' for key 'idx_email'backup"),
                new DataIntegrityViolationException(
                        "Duplicate entry 'idx_email' for key 'idx_name'"),
                new DataIntegrityViolationException(
                        "Duplicate entry 'value' for key 'idx_email'",
                        new SQLException(
                                "Duplicate entry 'value' for key 'idx_name'", "23000", 1062)),
                new DataIntegrityViolationException(
                        "### SQL: INSERT INTO customer VALUES ('Duplicate entry value for key"
                                + " idx_email')"),
                new DataIntegrityViolationException(
                        "### SQL: INSERT INTO idx_email VALUES (1)",
                        new SQLException(
                                "Duplicate entry 'value' for key 'idx_name'", "23000", 1062)),
                new DataIntegrityViolationException(null, new SQLException(null, "23000", 1062)),
                new DataIntegrityViolationException(
                        null,
                        new SQLException(
                                "Column cannot be null for key 'idx_email'", "23000", 1048)),
                new DataIntegrityViolationException("idx_email"),
                new DataIntegrityViolationException((String) null),
                new DataAccessResourceFailureException(
                        "Duplicate entry 'value' for key 'idx_email'"));
    }

    private static Stream<Arguments> bothWriteOperations(RuntimeException... failures) {
        return Stream.of(failures)
                .flatMap(
                        failure ->
                                Stream.of(
                                        Arguments.of(false, failure), Arguments.of(true, failure)));
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
