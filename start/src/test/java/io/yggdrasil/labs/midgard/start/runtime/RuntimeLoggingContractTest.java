package io.yggdrasil.labs.midgard.start.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.logging.impl.NoOpLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.yggdrasil.labs.log.config.LogPresetEnvironmentPostProcessor;
import com.yggdrasil.labs.log.web.AccessLogFilter;
import com.yggdrasil.labs.log.web.AccessLogProperties;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.read.ListAppender;

@ResourceLock("midgard-runtime-logback")
class RuntimeLoggingContractTest {

    private static final String SYNTHETIC_CREDENTIALS =
            "password=alpha token=beta secret=gamma api_key=delta";

    @Test
    void realEnvironmentProcessorRecognizesTheExplicitTextPresetAndMaskRules() throws Exception {
        StandardEnvironment environment = RuntimeYaml.defaultProfiles();

        process(environment);

        assertThat(environment.getPropertySources().contains("mimirLogPresetResolved")).isTrue();
        assertThat(environment.getProperty("mimir.boot.log.internal.format")).isEqualTo("text");
        assertThat(environment.getProperty("mimir.boot.log.internal.mask.enabled-patterns"))
                .isEqualTo("password,token,secret,api_key");
        assertThat(environment.getProperty("mimir.boot.log.internal.mask.replacement"))
                .isEqualTo("****");
        assertThat(environment.getProperty("mimir.boot.log.internal.json.capture-active")).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "mimir.boot.log.format, invalid-format",
        "mimir.boot.log.mask.enabled-patterns[0], unknown-mask"
    })
    void realEnvironmentProcessorRejectsInvalidRuntimeSettings(String property, String value)
            throws Exception {
        StandardEnvironment environment = RuntimeYaml.defaultProfiles();
        RuntimeYaml.override(environment, Map.of(property, value));

        assertThatThrownBy(() -> process(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(property);
        assertThat(environment.getPropertySources().contains("mimirLogPresetResolved")).isFalse();
    }

    @Test
    void realConvertersMaskMessageAndCauseChainWithoutChangingGlobalRulesOrProperties()
            throws Exception {
        StandardEnvironment environment = RuntimeYaml.defaultProfiles();
        process(environment);
        LoggerContext global = (LoggerContext) LoggerFactory.getILoggerFactory();
        Map<String, String> originalProperties = global.getCopyOfPropertyMap();
        Object originalConversionRules = global.getObject("PATTERN_RULE_REGISTRY");
        Map<String, String> originalMdc = MDC.getCopyOfContextMap();
        LoggingEvent event = new LoggingEvent();
        event.setMessage("operation " + SYNTHETIC_CREDENTIALS + " completed");
        event.setThrowableProxy(
                new ThrowableProxy(
                        new IllegalStateException(
                                "outer " + SYNTHETIC_CREDENTIALS,
                                new IllegalArgumentException("cause " + SYNTHETIC_CREDENTIALS))));

        // A separate loader owns the real converters' static snapshots, so unrelated tests'
        // programmatically published mask rules remain intact without reflective reset hooks.
        try (IsolatedMaskConverters converters = new IsolatedMaskConverters(environment)) {
            String message = converters.message().convert(event);
            String throwable = converters.throwable().convert(event);

            assertThat(message)
                    .isEqualTo(
                            "operation password=**** token=**** secret=**** api_key=****"
                                    + " completed");
            assertThat(throwable)
                    .contains(
                            "IllegalStateException: outer password=**** token=**** secret=****"
                                    + " api_key=****")
                    .contains(
                            "Caused by: java.lang.IllegalArgumentException: cause password=****"
                                    + " token=**** secret=**** api_key=****")
                    .doesNotContain("alpha", "beta", "gamma", "delta");
            assertThat(converters.message().getClass().getClassLoader())
                    .isNotSameAs(RuntimeLoggingContractTest.class.getClassLoader());
        }
        assertThat(global.getCopyOfPropertyMap()).isEqualTo(originalProperties);
        assertThat(global.getObject("PATTERN_RULE_REGISTRY")).isSameAs(originalConversionRules);
        assertThat(MDC.getCopyOfContextMap()).isEqualTo(originalMdc);
    }

    @ParameterizedTest
    @CsvSource({"200, INFO", "400, WARN", "500, ERROR"})
    void realAccessFilterRecordsOneTerminalEventAtTheFinalHttpLevel(int status, String level)
            throws Exception {
        AccessLogProperties properties = accessProperties();
        AccessLogFilter filter =
                new AccessLogFilter(properties.getSlowThresholdMs(), properties.getExcludePaths());
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/customers/42");
        request.setRemoteAddr("192.0.2.42");
        request.addHeader("User-Agent", "runtime-contract");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (AccessEvents events = new AccessEvents()) {
            filter.doFilter(
                    request,
                    response,
                    (incoming, outgoing) -> ((HttpServletResponse) outgoing).setStatus(status));

            assertThat(response.getStatus()).isEqualTo(status);
            assertThat(events.values())
                    .singleElement()
                    .satisfies(
                            event -> {
                                assertThat(event.getLevel()).isEqualTo(Level.valueOf(level));
                                assertThat(event.getLoggerName()).isEqualTo("access.log");
                                assertThat(event.getFormattedMessage())
                                        .contains(
                                                "IP=[192.0.2.42]",
                                                "Method=[PUT]",
                                                "URI=[/api/customers/42]",
                                                "Status=[" + status + "]",
                                                "Outcome=[COMPLETED]",
                                                "UserAgent=[runtime-contract]")
                                        .containsPattern("Duration=\\[\\d+ms\\]");
                            });
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/health", "/favicon.ico"})
    void realAccessFilterKeepsDefaultExcludedPathsOutOfTheLog(String path) throws Exception {
        AccessLogProperties properties = accessProperties();
        AccessLogFilter filter =
                new AccessLogFilter(properties.getSlowThresholdMs(), properties.getExcludePaths());
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (AccessEvents events = new AccessEvents()) {
            filter.doFilter(
                    new MockHttpServletRequest("GET", path),
                    response,
                    (incoming, outgoing) -> ((HttpServletResponse) outgoing).setStatus(204));

            assertThat(response.getStatus()).isEqualTo(204);
            assertThat(events.values()).isEmpty();
        }
    }

    @Test
    void realAccessEventDoesNotCollectQueryBodyOrAuthenticationData() throws Exception {
        AccessLogProperties properties = accessProperties();
        AccessLogFilter filter =
                new AccessLogFilter(properties.getSlowThresholdMs(), properties.getExcludePaths());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/customers");
        request.setQueryString("token=query-marker");
        request.setContentType("application/json");
        request.setContent("{\"password\":\"body-marker\"}".getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Bearer authorization-marker");
        request.addHeader("Cookie", "session=cookie-marker");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (AccessEvents events = new AccessEvents()) {
            filter.doFilter(
                    request,
                    response,
                    (incoming, outgoing) -> {
                        assertThat(incoming.getInputStream().readAllBytes())
                                .asString(StandardCharsets.UTF_8)
                                .isEqualTo("{\"password\":\"body-marker\"}");
                        ((HttpServletResponse) outgoing).setStatus(200);
                    });

            assertThat(events.values())
                    .singleElement()
                    .satisfies(
                            event -> {
                                assertThat(event.getFormattedMessage())
                                        .contains("URI=[/api/customers]", "Status=[200]");
                                assertThat(
                                                event.getFormattedMessage()
                                                        + Arrays.deepToString(
                                                                event.getArgumentArray())
                                                        + String.valueOf(event.getKeyValuePairs())
                                                        + event.getMDCPropertyMap())
                                        .doesNotContain(
                                                "query-marker",
                                                "body-marker",
                                                "authorization-marker",
                                                "cookie-marker",
                                                "Authorization",
                                                "Cookie");
                            });
        }
    }

    private static AccessLogProperties accessProperties() throws Exception {
        return Binder.get(RuntimeYaml.defaultProfiles())
                .bindOrCreate("mimir.boot.log.access", AccessLogProperties.class);
    }

    private static void process(StandardEnvironment environment) {
        new LogPresetEnvironmentPostProcessor(supplier -> new NoOpLog())
                .postProcessEnvironment(environment, null);
    }

    /** Temporarily captures only access.log and restores its original logger and MDC state. */
    private static final class AccessEvents implements AutoCloseable {
        private final Logger logger = (Logger) LoggerFactory.getLogger("access.log");
        private final Level level = logger.getLevel();
        private final boolean additive = logger.isAdditive();
        private final List<Appender<ILoggingEvent>> appenders = new ArrayList<>();
        private final Map<String, String> mdc = MDC.getCopyOfContextMap();
        private final ListAppender<ILoggingEvent> memory =
                new ListAppender<>() {
                    @Override
                    protected void append(ILoggingEvent event) {
                        event.prepareForDeferredProcessing();
                        super.append(event);
                    }
                };

        private AccessEvents() {
            logger.iteratorForAppenders().forEachRemaining(appenders::add);
            appenders.forEach(logger::detachAppender);
            logger.setLevel(Level.INFO);
            logger.setAdditive(false);
            memory.setContext(logger.getLoggerContext());
            memory.start();
            logger.addAppender(memory);
            MDC.clear();
        }

        private List<ILoggingEvent> values() {
            return memory.list;
        }

        @Override
        public void close() {
            logger.detachAppender(memory);
            memory.stop();
            appenders.forEach(logger::addAppender);
            logger.setLevel(level);
            logger.setAdditive(additive);
            if (mdc == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(mdc);
            }
        }
    }
}
