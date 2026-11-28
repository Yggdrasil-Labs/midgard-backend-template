package io.yggdrasil.labs.midgard.start.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;

import com.yggdrasil.labs.log.config.LogMaskProperties;
import com.yggdrasil.labs.log.web.AccessLogProperties;
import com.yggdrasil.labs.web.config.WebProperties;

class RuntimeConfigurationTest {

    @Test
    void selectsMimirConsolePresetExplicitly() throws IOException {
        var base = RuntimeYaml.base();
        assertThat(base.getProperty("logging.config"))
                .isEqualTo("classpath:com/yggdrasil/labs/log/logback/container.xml");
    }

    @Test
    void distinguishesBaseDocumentFromDefaultLocalSelection() throws IOException {
        assertRuntimePolicy(RuntimeYaml.base(), "INFO", "INFO", false);
        StandardEnvironment defaults = RuntimeYaml.defaultProfiles();
        assertThat(defaults.getActiveProfiles()).containsExactly("local");
        assertRuntimePolicy(defaults, "INFO", "DEBUG", true);
    }

    @ParameterizedTest
    @CsvSource({
        "local, INFO, DEBUG, true",
        "dev, INFO, DEBUG, true",
        "test, INFO, INFO, false",
        "prod, WARN, WARN, false",
        "dev+prod, WARN, WARN, false",
        "local+test, INFO, INFO, false"
    })
    void bindsTheExplicitProfileMatrix(
            String profiles, String root, String application, boolean cors) throws IOException {
        assertRuntimePolicy(RuntimeYaml.profiles(profiles.split("\\+")), root, application, cors);
    }

    @Test
    void explicitOriginListReplacesTheDefaultInsteadOfAppending() throws IOException {
        StandardEnvironment environment = RuntimeYaml.profiles("local");
        RuntimeYaml.override(
                environment,
                Map.of("mimir.boot.web.cors.allowed-origins[0]", "https://frontend.example"));

        WebProperties web =
                Binder.get(environment).bindOrCreate("mimir.boot.web", WebProperties.class);

        assertThat(web.getCors().getAllowedOrigins()).containsExactly("https://frontend.example");
        assertThat(web.getCors().isEnabled()).isTrue();
    }

    private static void assertRuntimePolicy(
            StandardEnvironment environment, String root, String application, boolean cors) {
        Binder binder = Binder.get(environment);
        WebProperties web = binder.bindOrCreate("mimir.boot.web", WebProperties.class);
        LogMaskProperties mask =
                binder.bindOrCreate("mimir.boot.log.mask", LogMaskProperties.class);
        AccessLogProperties access =
                binder.bindOrCreate("mimir.boot.log.access", AccessLogProperties.class);
        assertThat(environment.getProperty("logging.config"))
                .isEqualTo("classpath:com/yggdrasil/labs/log/logback/container.xml");
        assertThat(environment.getProperty("mimir.boot.log.format")).isEqualTo("text");
        assertThat(environment.getProperty("logging.level.root")).isEqualToIgnoringCase(root);
        assertThat(environment.getProperty("logging.level.io.yggdrasil.labs.midgard"))
                .isEqualToIgnoringCase(application);
        assertThat(environment.getProperty("logging.level.io.yggdrasil.labs.midgard.*")).isNull();
        assertThat(environment.getProperty("logging.level.access.log")).isEqualTo("INFO");
        assertThat(
                        environment.getProperty(
                                "spring.main.allow-bean-definition-overriding", Boolean.class))
                .isFalse();
        assertThat(mask.getEnabledPatterns())
                .containsExactly("password", "token", "secret", "api_key");
        assertThat(mask.getReplacement()).isEqualTo("****");
        assertThat(access.isEnabled()).isTrue();
        assertThat(access.getSlowThresholdMs()).isEqualTo(3000);
        assertThat(environment.getProperty("mimir.boot.log.access.exclude-paths")).isNull();
        assertThat(access.getExcludePaths()).isEqualTo(new AccessLogProperties().getExcludePaths());
        assertThat(web.getCors().isEnabled()).isEqualTo(cors);
        assertThat(web.getCors().getAllowedOrigins()).containsExactly("http://localhost:5173");
        assertThat(web.getCors().getAllowedMethods())
                .containsExactly("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(web.getCors().getAllowedHeaders()).containsExactly("*");
        assertThat(web.getCors().getExposedHeaders())
                .containsExactly("Content-Disposition", "X-Trace-Id");
        assertThat(web.getCors().isAllowCredentials()).isTrue();
        assertThat(web.getCors().getMaxAge()).isEqualTo(Duration.ofHours(1));
        assertThat(environment.getProperty("mimir.boot.web.cors.allowed-origin-patterns")).isNull();
        assertThat(web.getSerialization().isWriteNulls()).isTrue();
        assertThat(
                        environment.getProperty(
                                "mimir.boot.exception.validation.structured-details-enabled",
                                Boolean.class))
                .isTrue();
        assertThat(environment.getProperty("spring.application.name"))
                .isEqualTo("midgard-backend-template");
        assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
        assertThat(
                        binder.bind(
                                        "spring.flyway.locations",
                                        org.springframework.boot.context.properties.bind.Bindable
                                                .listOf(String.class))
                                .get())
                .isEqualTo(List.of("classpath:db/migration"));
    }
}
