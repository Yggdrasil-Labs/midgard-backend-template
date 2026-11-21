package io.yggdrasil.labs.midgard.app.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.yggdrasil.labs.common.exception.BizException;
import com.yggdrasil.labs.common.page.PageResult;

import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.app.customer.dto.qry.ListCustomerQry;
import io.yggdrasil.labs.midgard.domain.customer.model.Customer;
import io.yggdrasil.labs.midgard.domain.customer.repo.CustomerRepository;

class CustomerPaginationTest {

    @Test
    void queriesAndReturnsTheSameCorrectedValuesAndLongTotal() {
        PaginationRepository repository = new PaginationRepository();
        repository.total = 2147483648L;
        ListCustomerQry query = new ListCustomerQry();
        query.setPageIndex(0L);
        query.setPageSize(1001L);
        query.setKeyword("needle");
        query.setStatus("ACTIVE");

        PageResult<CustomerCO> result = new CustomerAppServiceImpl(repository).list(query);

        assertThat(repository.page).isEqualTo(1);
        assertThat(repository.size).isEqualTo(1000);
        assertThat(repository.keyword).isEqualTo("needle");
        assertThat(repository.status).isEqualTo("ACTIVE");
        assertThat(result.getTotalCount()).isEqualTo(2147483648L);
        assertThat(result.getPageIndex()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(1000);
    }

    @Test
    void buildsPageMetadataAndEmptyList() {
        PaginationRepository repository = new PaginationRepository();
        repository.total = 23;
        ListCustomerQry query = new ListCustomerQry();
        query.setPageIndex(2L);
        PageResult<CustomerCO> result = new CustomerAppServiceImpl(repository).list(query);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getHasNext()).isTrue();
        assertThat(result.getHasPrevious()).isTrue();

        repository.total = 0;
        result = new CustomerAppServiceImpl(repository).list(new ListCustomerQry());
        assertThat(result.getData()).isEmpty();
        assertThat(result.getTotalPages()).isZero();
        assertThat(result.getHasNext()).isFalse();
        assertThat(result.getHasPrevious()).isFalse();
    }

    @Test
    void rejectsOverflowBeforeCallingRepository() {
        PaginationRepository repository = new PaginationRepository();
        ListCustomerQry query = new ListCustomerQry();
        query.setPageIndex(2147483648L);

        assertThatThrownBy(() -> new CustomerAppServiceImpl(repository).list(query))
                .isInstanceOfSatisfying(
                        BizException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo("00004"));
        assertThat(repository.calls).isZero();
    }

    private static final class PaginationRepository implements CustomerRepository {
        private int page;
        private int size;
        private long total;
        private int calls;
        private String keyword;
        private String status;

        @Override
        public List<Customer> findAll(int page, int size, String keyword, String status) {
            this.page = page;
            this.size = size;
            this.keyword = keyword;
            this.status = status;
            calls++;
            return List.of();
        }

        @Override
        public long count(String keyword, String status) {
            assertThat(keyword).isEqualTo(this.keyword);
            assertThat(status).isEqualTo(this.status);
            calls++;
            return total;
        }

        @Override
        public Customer save(Customer customer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Customer> findById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Customer update(Customer customer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsByEmail(String email) {
            throw new UnsupportedOperationException();
        }
    }
}
