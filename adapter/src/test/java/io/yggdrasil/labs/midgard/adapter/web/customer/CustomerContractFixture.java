package io.yggdrasil.labs.midgard.adapter.web.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yggdrasil.labs.exception.config.ExceptionProperties;
import com.yggdrasil.labs.exception.handler.DefaultExceptionResponseFactory;
import com.yggdrasil.labs.exception.handler.MimirExceptionHandler;
import com.yggdrasil.labs.web.config.JacksonConfig;
import com.yggdrasil.labs.web.config.WebProperties;

import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;

/** 受管序列化与 standalone MVC 夹具；不启动应用或证明自动配置已注册。 */
final class CustomerContractFixture implements AutoCloseable {

    final ObjectMapper mapper = managedMapper();
    final StubCustomerAppService service = new StubCustomerAppService();
    final MockMvc mvc;
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

    CustomerContractFixture() {
        validator.afterPropertiesSet();
        mvc =
                MockMvcBuilders.standaloneSetup(new CustomerController(service))
                        .setControllerAdvice(exceptionHandler())
                        .setValidator(validator)
                        .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                        .build();
    }

    static ObjectMapper managedMapper() {
        WebProperties properties = new WebProperties();
        properties.getSerialization().setWriteNulls(true);
        Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
        new JacksonConfig(properties, new StaticListableBeanFactory().getBeanProvider(Module.class))
                .jackson2ObjectMapperBuilderCustomizer()
                .customize(builder);
        return builder.build();
    }

    static MimirExceptionHandler exceptionHandler() {
        ExceptionProperties properties = new ExceptionProperties();
        properties.getValidation().setStructuredDetailsEnabled(true);
        return new MimirExceptionHandler(new DefaultExceptionResponseFactory(), properties);
    }

    static CustomerCO customer() {
        CustomerCO customer = new CustomerCO();
        customer.setId(42L);
        customer.setName("张三");
        customer.setEmail("zhangsan@example.com");
        customer.setStatus("ACTIVE");
        return customer;
    }

    String body(String name, String email, String phone) {
        ObjectNode json = mapper.createObjectNode();
        json.put("name", name);
        json.put("email", email);
        json.put("phone", phone);
        return json.toString();
    }

    JsonNode json(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    static void assertEnvelope(JsonNode json, String code, boolean success) {
        Set<String> fields = new HashSet<>();
        json.fieldNames().forEachRemaining(fields::add);
        assertThat(fields)
                .containsExactlyInAnyOrder(
                        "code", "message", "data", "timestamp", "traceId", "success");
        assertThat(json.path("code").asText()).isEqualTo(code);
        assertThat(json.path("success").asBoolean()).isEqualTo(success);
        assertThat(json.path("message").isTextual()).isTrue();
        assertThat(json.path("timestamp").isIntegralNumber()).isTrue();
        assertThat(json.path("timestamp").asLong()).isPositive();
        assertThat(json.path("traceId").isNull() || json.path("traceId").isTextual()).isTrue();
    }

    static void assertNullData(JsonNode json) {
        assertThat(json.has("data")).isTrue();
        assertThat(json.path("data").isNull()).isTrue();
    }

    static JsonNode validationError(JsonNode json, String source, String path, String constraint) {
        assertEnvelope(json, "00004", false);
        List<JsonNode> errors = new ArrayList<>();
        json.path("data").path("errors").forEach(errors::add);
        assertThat(errors).isNotEmpty();
        assertThat(json.path("data").path("totalCount").asInt()).isEqualTo(errors.size());
        assertThat(json.path("data").path("truncated").asBoolean()).isFalse();
        List<JsonNode> matching =
                errors.stream()
                        .filter(
                                error ->
                                        source.equals(error.path("source").asText())
                                                && path.equals(error.path("path").asText())
                                                && constraint.equals(
                                                        error.path("constraint").asText()))
                        .toList();
        assertThat(matching).hasSize(1);
        return matching.get(0);
    }

    @Override
    public void close() {
        validator.close();
    }
}
