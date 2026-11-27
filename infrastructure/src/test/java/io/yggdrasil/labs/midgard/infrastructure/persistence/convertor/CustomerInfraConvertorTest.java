package io.yggdrasil.labs.midgard.infrastructure.persistence.convertor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.yggdrasil.labs.midgard.domain.customer.model.Customer;
import io.yggdrasil.labs.midgard.domain.customer.model.CustomerStatus;
import io.yggdrasil.labs.midgard.infrastructure.persistence.dataobject.CustomerDO;

class CustomerInfraConvertorTest {

    private final CustomerInfraConvertor convertor = CustomerInfraConvertor.INSTANCE;

    @Test
    void toDataObject_mapsCustomerFieldsAndStatus() {
        Customer customer = new Customer();
        customer.setId(12L);
        customer.setName("Lin");
        customer.setEmail("lin@example.com");
        customer.setPhone("13800000000");
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setVersion(3);

        CustomerDO dataObject = convertor.toDataObject(customer);

        assertThat(dataObject.getId()).isEqualTo(12L);
        assertThat(dataObject.getName()).isEqualTo("Lin");
        assertThat(dataObject.getEmail()).isEqualTo("lin@example.com");
        assertThat(dataObject.getPhone()).isEqualTo("13800000000");
        assertThat(dataObject.getStatus()).isEqualTo("ACTIVE");
        assertThat(dataObject.getVersion()).isEqualTo(3);
    }

    @Test
    void toEntity_mapsDataObjectFieldsAndStatus() {
        CustomerDO dataObject = new CustomerDO();
        dataObject.setId(12L);
        dataObject.setName("Lin");
        dataObject.setEmail("lin@example.com");
        dataObject.setPhone("13800000000");
        dataObject.setStatus("INACTIVE");
        dataObject.setVersion(4);

        Customer customer = convertor.toEntity(dataObject);

        assertThat(customer.getId()).isEqualTo(12L);
        assertThat(customer.getName()).isEqualTo("Lin");
        assertThat(customer.getEmail()).isEqualTo("lin@example.com");
        assertThat(customer.getPhone()).isEqualTo("13800000000");
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
        assertThat(customer.getVersion()).isEqualTo(4);
    }

    @Test
    void conversion_preservesNullValues() {
        Customer customer = new Customer();
        CustomerDO dataObject = convertor.toDataObject(customer);

        assertThat(dataObject.getId()).isNull();
        assertThat(dataObject.getStatus()).isNull();
        assertThat(dataObject.getVersion()).isNull();
        Customer converted = convertor.toEntity(dataObject);
        assertThat(converted.getId()).isNull();
        assertThat(converted.getStatus()).isNull();
        assertThat(converted.getVersion()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, Integer.MAX_VALUE})
    void conversion_preservesVersionOnRoundTrip(int version) {
        Customer customer = new Customer();
        customer.setVersion(version);

        Customer converted = convertor.toEntity(convertor.toDataObject(customer));

        assertThat(converted.getVersion()).isEqualTo(version);
    }

    @Test
    void toDataObject_shouldCreateFreshRecordWithAuditFieldsAwaitingFill() {
        Customer customer = new Customer();
        customer.setVersion(2);

        CustomerDO first = convertor.toDataObject(customer);
        CustomerDO second = convertor.toDataObject(customer);

        assertThat(second).isNotSameAs(first);
        assertThat(second.getCreateTime()).isNull();
        assertThat(second.getUpdateTime()).isNull();
        assertThat(second.getCreateBy()).isNull();
        assertThat(second.getUpdateBy()).isNull();
    }

    @Test
    void conversion_returnsNullForNullInput() {
        assertThat(convertor.toDataObject(null)).isNull();
        assertThat(convertor.toEntity(null)).isNull();
    }

    @Test
    void toEntity_rejectsUnknownStatus() {
        CustomerDO dataObject = new CustomerDO();
        dataObject.setStatus("SUSPENDED");

        assertThatThrownBy(() -> convertor.toEntity(dataObject))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
