package io.yggdrasil.labs.midgard.adapter.web.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yggdrasil.labs.common.exception.BizException;
import com.yggdrasil.labs.common.exception.ErrorCode;
import com.yggdrasil.labs.common.exception.ErrorDefinition;
import com.yggdrasil.labs.common.page.PageResult;

import io.yggdrasil.labs.midgard.domain.customer.exception.CustomerErrorCode;

class CustomerHttpContractTest {

    private CustomerContractFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new CustomerContractFixture();
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    @Test
    void create_returnsHttp200AndManagedEnvelope() throws Exception {
        MvcResult result =
                fixture.mvc
                        .perform(
                                post("/api/customers")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(fixture.body("张三", "zhangsan@example.com", null)))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode json = fixture.json(result);
        CustomerContractFixture.assertEnvelope(json, "00000", true);
        assertThat(json.path("data").path("id").isIntegralNumber()).isTrue();
        assertThat(json.path("data").path("id").asLong()).isEqualTo(42L);
        assertThat(json.path("data").path("name").asText()).isEqualTo("张三");
        assertThat(json.path("data").path("email").asText()).isEqualTo("zhangsan@example.com");
        assertThat(json.path("data").path("status").asText()).isEqualTo("ACTIVE");
        assertThat(json.path("data").path("version").isIntegralNumber()).isTrue();
        assertThat(json.path("data").path("version").asInt()).isZero();
        assertThat(fixture.service.calls).isEqualTo(1);
    }

    @Test
    void detail_preservesNumericIdAndExplicitNullPhone() throws Exception {
        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(get("/api/customers/42"))
                                .andExpect(status().isOk())
                                .andReturn());

        CustomerContractFixture.assertEnvelope(json, "00000", true);
        assertThat(json.path("data").path("id").isIntegralNumber()).isTrue();
        assertThat(json.path("data").path("id").asLong()).isEqualTo(42L);
        assertThat(json.path("data").has("phone")).isTrue();
        assertThat(json.path("data").path("phone").isNull()).isTrue();
        assertThat(json.path("data").path("version").isIntegralNumber()).isTrue();
        assertThat(json.path("data").path("version").asInt()).isZero();
        assertThat(fixture.service.id).isEqualTo(42L);
    }

    @Test
    void missingRoute_returnsProtocol404InsteadOfBusinessHttp200() throws Exception {
        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(get("/api/missing-contract-route"))
                                .andExpect(status().isNotFound())
                                .andReturn());

        CustomerContractFixture.assertEnvelope(json, "20001", false);
        CustomerContractFixture.assertNullData(json);
        assertThat(fixture.service.calls).isZero();
    }

    @Test
    void update_rejectsMissingVersionWithoutCallingApp() throws Exception {
        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(
                                        put("/api/customers/42")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"phone\":\"\"}"))
                                .andExpect(status().isBadRequest())
                                .andReturn());

        CustomerContractFixture.validationError(json, "FIELD", "version", "NotNull");
        assertThat(fixture.service.calls).isZero();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "0.5",
                "3.5",
                "3.0",
                "1e0",
                "\"3\"",
                "true",
                "false",
                "2147483648",
                "-2147483649",
                "9999999999999999999999999999",
                "[]",
                "{}"
            })
    void update_rejectsNonIntegerVersionBeforeCallingApp(String version) throws Exception {
        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(
                                        put("/api/customers/42")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                        "{\"version\":"
                                                                + version
                                                                + ",\"phone\":\"\"}"))
                                .andExpect(status().isBadRequest())
                                .andReturn());

        CustomerContractFixture.assertEnvelope(json, "00004", false);
        CustomerContractFixture.assertNullData(json);
        assertThat(json.path("message").asText()).isEqualTo("请求体格式错误");
        assertThat(fixture.service.calls).isZero();
    }

    @ParameterizedTest
    @CsvSource({"null, NotNull", "-1, Min", "2147483647, Max"})
    void update_rejectsInvalidVersionAsStructuredFieldError(String version, String constraint)
            throws Exception {
        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(
                                        put("/api/customers/42")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                        "{\"version\":"
                                                                + version
                                                                + ",\"phone\":\"\"}"))
                                .andExpect(status().isBadRequest())
                                .andReturn());

        CustomerContractFixture.validationError(json, "FIELD", "version", constraint);
        assertThat(fixture.service.calls).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2147483646})
    void update_acceptsVersionBoundariesAndReturnsIntegerVersion(int version) throws Exception {
        fixture.service.result.setVersion(version + 1);

        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(
                                        put("/api/customers/42")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                        "{\"version\":"
                                                                + version
                                                                + ",\"phone\":\"\"}"))
                                .andExpect(status().isOk())
                                .andReturn());

        CustomerContractFixture.assertEnvelope(json, "00000", true);
        assertThat(fixture.service.updated.getVersion()).isEqualTo(version);
        assertThat(json.path("data").path("version").isIntegralNumber()).isTrue();
        assertThat(json.path("data").path("version").asInt()).isEqualTo(version + 1);
    }

    @Test
    void update_partialRequestBindsPathIdAndLeavesMissingFieldsNull() throws Exception {
        fixture.service.result.setName("李四");
        fixture.service.result.setVersion(1);
        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(
                                        put("/api/customers/42")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"version\":0,\"name\":\"李四\"}"))
                                .andExpect(status().isOk())
                                .andReturn());

        CustomerContractFixture.assertEnvelope(json, "00000", true);
        assertThat(json.path("data").path("name").asText()).isEqualTo("李四");
        assertThat(json.path("data").path("version").isIntegralNumber()).isTrue();
        assertThat(json.path("data").path("version").asInt()).isEqualTo(1);
        assertThat(fixture.service.updated.getId()).isEqualTo(42L);
        assertThat(fixture.service.updated.getName()).isEqualTo("李四");
        assertThat(fixture.service.updated.getEmail()).isNull();
        assertThat(fixture.service.updated.getPhone()).isNull();
        assertThat(fixture.service.updated.getVersion()).isZero();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"version\":0,\"phone\":\"\"}",
                "{\"version\":0,\"name\":null,\"email\":null,\"phone\":\"\"}"
            })
    void update_emptyPhoneAloneIsValidClearRequest(String body) throws Exception {
        fixture.service.result.setPhone("");
        fixture.service.result.setVersion(1);
        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(
                                        put("/api/customers/42")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(body))
                                .andExpect(status().isOk())
                                .andReturn());

        CustomerContractFixture.assertEnvelope(json, "00000", true);
        assertThat(json.path("data").path("phone").asText()).isEmpty();
        assertThat(fixture.service.updated.getPhone()).isEmpty();
        assertThat(fixture.service.updated.getName()).isNull();
        assertThat(fixture.service.updated.getEmail()).isNull();
        assertThat(fixture.service.updated.getVersion()).isZero();
    }

    @Test
    void delete_returnsHttp200WithExplicitNullData() throws Exception {
        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(delete("/api/customers/42"))
                                .andExpect(status().isOk())
                                .andReturn());

        CustomerContractFixture.assertEnvelope(json, "00000", true);
        CustomerContractFixture.assertNullData(json);
        assertThat(fixture.service.id).isEqualTo(42L);
        assertThat(fixture.service.calls).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(longs = {23L, 4294967296L})
    void list_serializesAppPageAndLongTotalWithoutTruncation(long total) throws Exception {
        fixture.service.page =
                PageResult.of(List.of(CustomerContractFixture.customer()), total, 2L, 10L);

        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(
                                        get("/api/customers")
                                                .param("pageIndex", "2")
                                                .param("pageSize", "10")
                                                .param("keyword", "needle")
                                                .param("status", "ACTIVE"))
                                .andExpect(status().isOk())
                                .andReturn());

        CustomerContractFixture.assertEnvelope(json, "00000", true);
        JsonNode page = json.path("data");
        assertThat(page.path("data").isArray()).isTrue();
        assertThat(page.path("data").size()).isEqualTo(1);
        assertThat(page.path("data").get(0).path("id").asLong()).isEqualTo(42L);
        assertThat(page.path("data").get(0).path("version").isIntegralNumber()).isTrue();
        assertThat(page.path("data").get(0).path("version").asInt()).isZero();
        assertThat(page.path("totalCount").isIntegralNumber()).isTrue();
        assertThat(page.path("totalCount").asLong()).isEqualTo(total);
        assertThat(page.path("pageIndex").asLong()).isEqualTo(2L);
        assertThat(page.path("pageSize").asLong()).isEqualTo(10L);
        assertThat(page.path("totalPages").asLong())
                .isEqualTo(total / 10 + (total % 10 == 0 ? 0 : 1));
        assertThat(page.path("hasNext").asBoolean()).isTrue();
        assertThat(page.path("hasPrevious").asBoolean()).isTrue();
        assertThat(fixture.service.query.getPageIndex()).isEqualTo(2L);
        assertThat(fixture.service.query.getPageSize()).isEqualTo(10L);
        assertThat(fixture.service.query.getKeyword()).isEqualTo("needle");
        assertThat(fixture.service.query.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void list_emptyPageUsesArrayAndDefaultBindingIgnoresOldAliases() throws Exception {
        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(
                                        get("/api/customers")
                                                .param("page", "9")
                                                .param("size", "99"))
                                .andExpect(status().isOk())
                                .andReturn());

        CustomerContractFixture.assertEnvelope(json, "00000", true);
        JsonNode page = json.path("data");
        assertThat(page.path("data").isArray()).isTrue();
        assertThat(page.path("data").isEmpty()).isTrue();
        assertThat(page.path("totalCount").asLong()).isZero();
        assertThat(page.path("totalPages").asLong()).isZero();
        assertThat(page.path("hasNext").asBoolean()).isFalse();
        assertThat(page.path("hasPrevious").asBoolean()).isFalse();
        assertThat(fixture.service.query.getPageIndex()).isEqualTo(1L);
        assertThat(fixture.service.query.getPageSize()).isEqualTo(10L);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, 2147483647L})
    void list_bindsRawParametersBeforeAppNormalization(long index) throws Exception {
        fixture.mvc
                .perform(
                        get("/api/customers")
                                .param("pageIndex", Long.toString(index))
                                .param("pageSize", "1001"))
                .andExpect(status().isOk());

        assertThat(fixture.service.query.getPageIndex()).isEqualTo(index);
        assertThat(fixture.service.query.getPageSize()).isEqualTo(1001L);
    }

    @ParameterizedTest
    @CsvSource({"pageIndex, 2147483648", "pageIndex, text", "pageSize, text", "status, UNKNOWN"})
    void list_rejectsInvalidQueryWithoutCallingApp(String field, String value) throws Exception {
        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(get("/api/customers").param(field, value))
                                .andExpect(status().isBadRequest())
                                .andReturn());

        CustomerContractFixture.assertEnvelope(json, "00004", false);
        assertThat(fixture.service.calls).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12345678901234567890"})
    void createAndUpdate_acceptEmail100AndOptionalPhone(String phone) throws Exception {
        String email = "a".repeat(50) + "@" + "d".repeat(45) + ".com";
        String body = fixture.body("张三", email, phone);

        fixture.mvc
                .perform(
                        post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk());
        fixture.mvc
                .perform(
                        put("/api/customers/42")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(fixture.updateBody("张三", email, phone)))
                .andExpect(status().isOk());

        assertThat(fixture.service.created.getEmail()).isEqualTo(email);
        assertThat(fixture.service.created.getPhone()).isEqualTo(phone);
        assertThat(fixture.service.updated.getEmail()).isEqualTo(email);
        assertThat(fixture.service.updated.getPhone()).isEqualTo(phone);
        assertThat(fixture.service.calls).isEqualTo(2);
    }

    @Test
    void create_acceptsMissingOptionalPhone() throws Exception {
        fixture.mvc
                .perform(
                        post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"张三\",\"email\":\"zhangsan@example.com\"}"))
                .andExpect(status().isOk());

        assertThat(fixture.service.created.getPhone()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":null,\"email\":null}"})
    void create_rejectsMissingOrNullRequiredFieldsWithoutCallingApp(String body) throws Exception {
        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(
                                        post("/api/customers")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(body))
                                .andExpect(status().isBadRequest())
                                .andReturn());

        CustomerContractFixture.validationError(json, "FIELD", "name", "NotBlank");
        CustomerContractFixture.validationError(json, "FIELD", "email", "NotBlank");
        assertThat(fixture.service.calls).isZero();
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void invalidField_returnsStructuredErrorWithoutCallingApp(
            String method, String field, String value, String constraint) throws Exception {
        ObjectNode body =
                (ObjectNode) fixture.mapper.readTree(fixture.body("张三", "valid@example.com", null));
        body.put(field, value);
        if (method.equals("PUT")) {
            body.put("version", 0);
        }
        String path = method.equals("POST") ? "/api/customers" : "/api/customers/42";

        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(
                                        request(HttpMethod.valueOf(method), path)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(body.toString()))
                                .andExpect(status().isBadRequest())
                                .andReturn());

        JsonNode error = CustomerContractFixture.validationError(json, "FIELD", field, constraint);
        assertThat(error.path("parameterIndex").asInt()).isEqualTo(method.equals("POST") ? 0 : 1);
        assertThat(error.path("message").asText()).isIn("不能为空", "格式不符合约束", "长度不符合约束");
        assertThat(fixture.service.calls).isZero();
    }

    private static Stream<Arguments> invalidFields() {
        return Stream.of(
                        new String[] {"name", "A", "Size"},
                        new String[] {"name", "A".repeat(51), "Size"},
                        new String[] {"name", "  ", "Pattern"},
                        new String[] {"name", "\u2003\u2003", "Pattern"},
                        new String[] {"email", "", "Pattern"},
                        new String[] {"email", "  ", "Pattern"},
                        new String[] {"email", "user@localhost", "Pattern"},
                        new String[] {"email", "secret-input", "Email"},
                        new String[] {
                            "email", "a".repeat(50) + "@" + "d".repeat(46) + ".com", "Size"
                        },
                        new String[] {"phone", "1".repeat(21), "Size"})
                .flatMap(
                        values ->
                                Stream.of(
                                        Arguments.of("POST", values[0], values[1], values[2]),
                                        Arguments.of("PUT", values[0], values[1], values[2])));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "{\"name\":null,\"email\":null,\"phone\":null}",
                "{\"version\":0}",
                "{\"version\":0,\"name\":null,\"email\":null,\"phone\":null}"
            })
    void update_allMissingOrNullIsObjectError(String body) throws Exception {
        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(
                                        put("/api/customers/42")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(body))
                                .andExpect(status().isBadRequest())
                                .andReturn());

        JsonNode error =
                CustomerContractFixture.validationError(json, "OBJECT", "$", "ValidCustomerUpdate");
        assertThat(error.path("parameterIndex").asInt()).isEqualTo(1);
        assertThat(error.path("message").asText()).isEqualTo("参数不符合约束");
        assertThat(fixture.service.calls).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void malformedJson_returnsSafe400WithoutCallingApp(String method) throws Exception {
        String path = method.equals("POST") ? "/api/customers" : "/api/customers/42";
        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(
                                        request(HttpMethod.valueOf(method), path)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"name\":\"private-payload\","))
                                .andExpect(status().isBadRequest())
                                .andReturn());

        CustomerContractFixture.assertEnvelope(json, "00004", false);
        CustomerContractFixture.assertNullData(json);
        assertThat(json.path("message").asText()).isEqualTo("请求体格式错误");
        assertThat(json.toString()).doesNotContain("private-payload");
        assertThat(fixture.service.calls).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void nonNumericPathId_returns400WithoutCallingApp(String method) throws Exception {
        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(
                                        request(
                                                        HttpMethod.valueOf(method),
                                                        "/api/customers/invalid")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"version\":0,\"name\":\"李四\"}"))
                                .andExpect(status().isBadRequest())
                                .andReturn());

        CustomerContractFixture.assertEnvelope(json, "00004", false);
        CustomerContractFixture.assertNullData(json);
        assertThat(fixture.service.calls).isZero();
    }

    @ParameterizedTest
    @MethodSource("businessErrors")
    void businessException_mapsToHttp200AndSafeDomainEnvelope(
            String method, String path, ErrorDefinition error) throws Exception {
        fixture.service.failure = BizException.fromError(error);

        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(
                                        request(HttpMethod.valueOf(method), path)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                        method.equals("PUT")
                                                                ? fixture.updateBody(
                                                                        "张三",
                                                                        "valid@example.com",
                                                                        null)
                                                                : fixture.body(
                                                                        "张三",
                                                                        "valid@example.com",
                                                                        null)))
                                .andExpect(status().isOk())
                                .andReturn());

        CustomerContractFixture.assertEnvelope(json, error.getCode(), false);
        CustomerContractFixture.assertNullData(json);
        assertThat(json.path("message").asText()).isEqualTo(error.getMessage());
        assertThat(fixture.service.calls).isEqualTo(1);
    }

    private static Stream<Arguments> businessErrors() {
        return Stream.of(
                Arguments.of("POST", "/api/customers", CustomerErrorCode.EMAIL_ALREADY_EXISTS),
                Arguments.of("PUT", "/api/customers/42", CustomerErrorCode.EMAIL_ALREADY_EXISTS),
                Arguments.of("GET", "/api/customers/999", ErrorCode.DATA_NOT_FOUND),
                Arguments.of("PUT", "/api/customers/999", ErrorCode.DATA_NOT_FOUND),
                Arguments.of("DELETE", "/api/customers/999", ErrorCode.DATA_NOT_FOUND),
                Arguments.of("PUT", "/api/customers/42", ErrorCode.PARAM_INVALID),
                Arguments.of("PUT", "/api/customers/42", CustomerErrorCode.VERSION_CONFLICT));
    }

    @ParameterizedTest
    @MethodSource("unknownFailures")
    void unknownFailure_returnsSafe500(String path, RuntimeException failure) throws Exception {
        fixture.service.failure = failure;

        JsonNode json =
                fixture.json(
                        fixture.mvc
                                .perform(get(path))
                                .andExpect(status().isInternalServerError())
                                .andReturn());

        CustomerContractFixture.assertEnvelope(json, "10000", false);
        CustomerContractFixture.assertNullData(json);
        assertThat(json.path("message").asText()).isEqualTo(ErrorCode.SYSTEM_ERROR.getMessage());
        assertThat(json.toString())
                .doesNotContain(
                        "SELECT",
                        "password",
                        "jdbc:",
                        "private-host",
                        "IllegalStateException",
                        "DataIntegrityViolationException");
    }

    private static Stream<Arguments> unknownFailures() {
        String sensitive = "SELECT password FROM customer; jdbc:mysql://private-host/private-db";
        return Stream.of(
                        new IllegalStateException(sensitive),
                        new DataIntegrityViolationException(sensitive),
                        new DataIntegrityViolationException((String) null))
                .flatMap(
                        failure ->
                                Stream.of(
                                        Arguments.of("/api/customers/42", failure),
                                        Arguments.of("/api/customers", failure)));
    }
}
