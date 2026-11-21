package io.yggdrasil.labs.midgard.app.customer.dto.qry;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.yggdrasil.labs.common.page.PageRequest;

class ListCustomerQryTest {

    @ParameterizedTest
    @CsvSource(
            value = {
                "null,null,1,10",
                "0,0,1,10",
                "-1,-20,1,10",
                "2,1000,2,1000",
                "2,1001,2,1000",
                "2147483647,9223372036854775807,2147483647,1000"
            },
            nullValues = "null")
    void correctsPaginationWithoutChangingRawInput(
            Long index, Long size, long expectedIndex, long expectedSize) {
        ListCustomerQry query = new ListCustomerQry();
        query.setPageIndex(index);
        query.setPageSize(size);

        PageRequest result = query.toPageRequest();

        assertThat(result.getPageIndex()).isEqualTo(expectedIndex);
        assertThat(result.getPageSize()).isEqualTo(expectedSize);
        assertThat(query.getPageIndex()).isEqualTo(index);
        assertThat(query.getPageSize()).isEqualTo(size);
    }

    @Test
    void defaultsToFirstPageWithTenRows() {
        PageRequest result = new ListCustomerQry().toPageRequest();
        assertThat(result.getPageIndex()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
    }

    @Test
    void rejectsUnsupportedPageIndexAndStatusAtRequestBoundary() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            ListCustomerQry query = new ListCustomerQry();
            query.setPageIndex(2147483648L);
            query.setStatus("UNKNOWN");
            assertThat(factory.getValidator().validate(query))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("pageIndex", "status");
        }
    }
}
