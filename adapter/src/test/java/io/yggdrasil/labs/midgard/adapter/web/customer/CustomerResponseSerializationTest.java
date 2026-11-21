package io.yggdrasil.labs.midgard.adapter.web.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yggdrasil.labs.common.exception.BizException;
import com.yggdrasil.labs.common.exception.ErrorCode;
import com.yggdrasil.labs.common.exception.ErrorDefinition;
import com.yggdrasil.labs.common.page.PageResult;
import com.yggdrasil.labs.common.response.R;
import com.yggdrasil.labs.exception.handler.MimirExceptionHandler;
import com.yggdrasil.labs.web.advice.ResponseBodyEnhancer;
import com.yggdrasil.labs.web.config.WebProperties;
import com.yggdrasil.labs.web.interceptor.TraceInterceptor;

import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.domain.customer.exception.CustomerErrorCode;

class CustomerResponseSerializationTest {

    private final ObjectMapper mapper = CustomerContractFixture.managedMapper();

    @Test
    void emptySuccess_hasSixPublicFieldsAndExplicitNullData() throws Exception {
        JsonNode json = mapper.readTree(mapper.writeValueAsString(R.success()));

        CustomerContractFixture.assertEnvelope(json, "00000", true);
        CustomerContractFixture.assertNullData(json);
    }

    @Test
    void managedDefaults_keepLargeIdsNumericAndIncludeNullCustomerPhone() throws Exception {
        WebProperties defaults = new WebProperties();
        assertThat(defaults.getJson().isIncludeLegacyFail()).isFalse();
        assertThat(defaults.getSerialization().isWriteLargeIntegersAsString()).isFalse();
        CustomerCO customer = CustomerContractFixture.customer();
        customer.setId(Long.MAX_VALUE);

        JsonNode json = mapper.readTree(mapper.writeValueAsString(R.success(customer)));

        CustomerContractFixture.assertEnvelope(json, "00000", true);
        assertThat(json.path("data").size()).isEqualTo(5);
        assertThat(json.path("data").path("id").isIntegralNumber()).isTrue();
        assertThat(json.path("data").path("id").asLong()).isEqualTo(Long.MAX_VALUE);
        assertThat(json.path("data").has("phone")).isTrue();
        assertThat(json.path("data").path("phone").isNull()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("businessErrors")
    void failure_successFlagFollowsCodeAndLegacyFieldsAreAbsent(ErrorDefinition error)
            throws Exception {
        JsonNode json = mapper.readTree(mapper.writeValueAsString(R.failCode(error)));

        CustomerContractFixture.assertEnvelope(json, error.getCode(), false);
        CustomerContractFixture.assertNullData(json);
        assertThat(json.path("message").asText()).isEqualTo(error.getMessage());
        assertThat(json.has("fail")).isFalse();
        assertThat(json.has("errCode")).isFalse();
        assertThat(json.has("errMessage")).isFalse();
    }

    private static Stream<ErrorDefinition> businessErrors() {
        return Stream.of(
                ErrorCode.DATA_NOT_FOUND,
                CustomerErrorCode.EMAIL_ALREADY_EXISTS,
                CustomerErrorCode.VERSION_CONFLICT);
    }

    @Test
    void pageResult_keepsNestedArrayAndAllSevenMetadataFields() throws Exception {
        PageResult<CustomerCO> page =
                PageResult.of(List.of(CustomerContractFixture.customer()), 4294967296L, 2L, 10L);

        JsonNode json = mapper.readTree(mapper.writeValueAsString(R.success(page)));

        CustomerContractFixture.assertEnvelope(json, "00000", true);
        JsonNode data = json.path("data");
        assertThat(data.size()).isEqualTo(7);
        assertThat(data.path("data").isArray()).isTrue();
        assertThat(data.path("data").size()).isEqualTo(1);
        assertThat(data.path("totalCount").asLong()).isEqualTo(4294967296L);
        assertThat(data.path("pageIndex").asLong()).isEqualTo(2L);
        assertThat(data.path("pageSize").asLong()).isEqualTo(10L);
        assertThat(data.path("totalPages").asLong()).isEqualTo(429496730L);
        assertThat(data.path("hasNext").asBoolean()).isTrue();
        assertThat(data.path("hasPrevious").asBoolean()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void publicTraceComponents_fillSuccessOrExceptionBodyAndRestoreCallerMdc(
            boolean exceptionBody, boolean existingMdc) throws Exception {
        Map<String, String> original = MDC.getCopyOfContextMap();
        try {
            MDC.clear();
            MDC.put("tenant", "unchanged");
            if (existingMdc) {
                MDC.put("traceId", "outer-trace");
                MDC.put("requestId", "outer-request");
            }
            Map<String, String> beforeRequest = MDC.getCopyOfContextMap();
            exerciseTraceComponents(exceptionBody);
            assertThat(MDC.getCopyOfContextMap()).isEqualTo(beforeRequest);
        } finally {
            if (original == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(original);
            }
        }
    }

    private void exerciseTraceComponents(boolean exceptionBody) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers/42");
        request.addHeader("X-Trace-Id", "contract-42");
        request.addHeader("X-Request-Id", "contract-request");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TraceInterceptor interceptor = new TraceInterceptor();
        ResponseBodyEnhancer enhancer = new ResponseBodyEnhancer(new WebProperties());
        MethodParameter returnType =
                exceptionBody
                        ? new MethodParameter(
                                MimirExceptionHandler.class.getMethod(
                                        "handleBizException",
                                        BizException.class,
                                        HttpServletRequest.class),
                                -1)
                        : new MethodParameter(
                                CustomerController.class.getMethod("getById", Long.class), -1);
        assertThat(enhancer.supports(returnType, MappingJackson2HttpMessageConverter.class))
                .isTrue();
        assertThat(interceptor.preHandle(request, response, this)).isTrue();
        try {
            R<?> body =
                    exceptionBody
                            ? exceptionResponse(request)
                            : R.success(CustomerContractFixture.customer());
            R<?> enhanced =
                    enhancer.beforeBodyWrite(
                            body,
                            returnType,
                            MediaType.APPLICATION_JSON,
                            MappingJackson2HttpMessageConverter.class,
                            new ServletServerHttpRequest(request),
                            new ServletServerHttpResponse(response));
            JsonNode json = mapper.readTree(mapper.writeValueAsString(enhanced));

            assertThat(enhanced).isSameAs(body);
            CustomerContractFixture.assertEnvelope(
                    json, exceptionBody ? "21001" : "00000", !exceptionBody);
            assertThat(response.getHeader("X-Trace-Id")).isEqualTo("contract-42");
            assertThat(json.path("traceId").asText()).isEqualTo(response.getHeader("X-Trace-Id"));
            assertThat(response.getHeader("X-Request-Id")).isEqualTo("contract-request");
        } finally {
            interceptor.afterCompletion(request, response, this, null);
        }
    }

    private static R<?> exceptionResponse(MockHttpServletRequest request) {
        Object handled =
                CustomerContractFixture.exceptionHandler()
                        .handleBizException(
                                BizException.fromError(CustomerErrorCode.EMAIL_ALREADY_EXISTS),
                                request);
        assertThat(handled).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> entity = (ResponseEntity<?>) handled;
        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody()).isInstanceOf(R.class);
        return (R<?>) entity.getBody();
    }
}
