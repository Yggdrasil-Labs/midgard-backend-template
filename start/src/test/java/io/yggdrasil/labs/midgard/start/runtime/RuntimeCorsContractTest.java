package io.yggdrasil.labs.midgard.start.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;

import com.yggdrasil.labs.web.config.CorsConfig;
import com.yggdrasil.labs.web.config.WebProperties;

class RuntimeCorsContractTest {

    private static final String DEVELOPMENT_ORIGIN = "http://localhost:5173";

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "DELETE", "OPTIONS"})
    void allowsOnlyTheFiveDeclaredPreflightMethods(String method) throws Exception {
        Endpoint endpoint = new Endpoint();
        MockMvc mvc = enabledMvc(RuntimeYaml.profiles("local"), endpoint);

        mvc.perform(
                        options("/api/customers")
                                .header(HttpHeaders.ORIGIN, DEVELOPMENT_ORIGIN)
                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method)
                                .header(
                                        HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                        "Content-Type, X-Trace-Id"))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                                        DEVELOPMENT_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(
                        header().string(
                                        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                                        "GET,POST,PUT,DELETE,OPTIONS"))
                .andExpect(
                        header().string(
                                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                                        "Content-Type, X-Trace-Id"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
        assertThat(endpoint.calls.get()).isZero();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "http://localhost:5174",
                "http://127.0.0.1:5173",
                "https://untrusted.example"
            })
    void rejectsUnlistedOriginsBeforeReachingTheEndpoint(String origin) throws Exception {
        Endpoint endpoint = new Endpoint();
        MockMvc mvc = enabledMvc(RuntimeYaml.profiles("dev"), endpoint);

        mvc.perform(get("/api/customers").header(HttpHeaders.ORIGIN, origin))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        mvc.perform(
                        options("/api/customers")
                                .header(HttpHeaders.ORIGIN, origin)
                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"))
                .andExpect(status().isForbidden());
        assertThat(endpoint.calls.get()).isZero();
    }

    @Test
    void rejectsPatchPreflightBeforeReachingTheEndpoint() throws Exception {
        Endpoint endpoint = new Endpoint();
        MockMvc mvc = enabledMvc(RuntimeYaml.profiles("local"), endpoint);

        mvc.perform(
                        options("/api/customers")
                                .header(HttpHeaders.ORIGIN, DEVELOPMENT_ORIGIN)
                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertThat(endpoint.calls.get()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/customers", "/swagger-ui.html", "/actuator/health"})
    void realFilterAppliesToApiDocumentationAndMonitoringPaths(String path) throws Exception {
        Endpoint endpoint = new Endpoint();
        MockMvc mvc = enabledMvc(RuntimeYaml.profiles("dev"), endpoint);

        mvc.perform(get(path).header(HttpHeaders.ORIGIN, DEVELOPMENT_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(content().string("runtime-ok"))
                .andExpect(
                        header().string(
                                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                                        DEVELOPMENT_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(
                        header().string(
                                        HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                                        "Content-Disposition, X-Trace-Id"));
        assertThat(endpoint.calls.get()).isEqualTo(1);
    }

    @Test
    void acceptsRequestsWithoutOriginAndSameOriginRequests() throws Exception {
        Endpoint endpoint = new Endpoint();
        MockMvc mvc = enabledMvc(RuntimeYaml.profiles("local"), endpoint);

        mvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        mvc.perform(get("/api/customers").header(HttpHeaders.ORIGIN, "http://localhost"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertThat(endpoint.calls.get()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"test", "prod", "dev+prod", "local+test"})
    void disabledPolicyFixtureDoesNotSupplyDevelopmentCorsHeaders(String profiles)
            throws Exception {
        WebProperties properties = bind(RuntimeYaml.profiles(profiles.split("\\+")));
        assertThat(properties.getCors().isEnabled()).isFalse();
        // Standalone MVC deliberately omits the filter for a disabled policy. This does not
        // exercise Spring's conditional bean registration, which requires runtime acceptance.
        Endpoint endpoint = new Endpoint();
        MockMvc mvc =
                new StandaloneMockMvcBuilder(endpoint) {
                    @Override
                    protected Map<String, Object> extendMvcSingletons(
                            ServletContext servletContext) {
                        // Spring 6.1 standalone omits the adapter used by MVC's preflight handler.
                        return Map.of("httpRequestHandlerAdapter", new HttpRequestHandlerAdapter());
                    }
                }.build();

        mvc.perform(
                        options("/api/customers")
                                .header(HttpHeaders.ORIGIN, DEVELOPMENT_ORIGIN)
                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        mvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(content().string("runtime-ok"));
    }

    @Test
    void rejectsEmptyOriginListWhenConstructingTheRealFilter() throws IOException {
        StandardEnvironment environment = RuntimeYaml.profiles("local");
        RuntimeYaml.override(environment, Map.of("mimir.boot.web.cors.allowed-origins", ""));
        WebProperties properties = bind(environment);
        assertThat(properties.getCors().getAllowedOrigins()).isEmpty();

        assertThatThrownBy(() -> new CorsConfig(properties).corsFilter())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Origin");
    }

    @Test
    void rejectsWildcardOriginWithCredentialsWhenConstructingTheRealFilter() throws IOException {
        StandardEnvironment environment = RuntimeYaml.profiles("local");
        RuntimeYaml.override(environment, Map.of("mimir.boot.web.cors.allowed-origins[0]", "*"));

        assertThatThrownBy(() -> new CorsConfig(bind(environment)).corsFilter())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allowCredentials");
    }

    private static WebProperties bind(StandardEnvironment environment) {
        return Binder.get(environment).bindOrCreate("mimir.boot.web", WebProperties.class);
    }

    private static MockMvc enabledMvc(StandardEnvironment environment, Endpoint endpoint) {
        WebProperties properties = bind(environment);
        assertThat(properties.getCors().isEnabled()).isTrue();
        return MockMvcBuilders.standaloneSetup(endpoint)
                .addFilters(new CorsConfig(properties).corsFilter())
                .build();
    }

    @RestController
    static class Endpoint {
        private final AtomicInteger calls = new AtomicInteger();

        @RequestMapping({"/api/customers", "/swagger-ui.html", "/actuator/health"})
        String respond() {
            calls.incrementAndGet();
            return "runtime-ok";
        }
    }
}
