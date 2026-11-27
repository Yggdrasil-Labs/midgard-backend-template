package io.yggdrasil.labs.midgard.start.runtime;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * Loads only the checked-in runtime YAML inputs, with explicit profile document selection. This is
 * a controlled Binder fixture, not Spring ConfigData loading or auto-configuration. In particular
 * it never reads the user's ignored application-local.yml.
 */
final class RuntimeYaml {

    private static final Set<String> PROFILE_FILES = Set.of("dev", "test", "prod");

    private RuntimeYaml() {}

    static StandardEnvironment base() throws IOException {
        StandardEnvironment environment = isolatedEnvironment();
        environment.getPropertySources().addLast(documents("application.yml").get(0));
        return environment;
    }

    static StandardEnvironment defaultProfiles() throws IOException {
        String[] profiles = Binder.get(base()).bind("spring.profiles.active", String[].class).get();
        return profiles(profiles);
    }

    static StandardEnvironment profiles(String... profiles) throws IOException {
        StandardEnvironment environment = isolatedEnvironment();
        environment.setActiveProfiles(profiles);
        for (PropertySource<?> document : documents("application.yml")) {
            Object expression = document.getProperty("spring.config.activate.on-profile");
            if (expression == null
                    || Profiles.of(expression.toString())
                            .matches(profile -> Arrays.asList(profiles).contains(profile))) {
                environment.getPropertySources().addFirst(document);
            }
        }
        for (String profile : profiles) {
            if (PROFILE_FILES.contains(profile)) {
                for (PropertySource<?> document : documents("application-" + profile + ".yml")) {
                    environment.getPropertySources().addFirst(document);
                }
            }
        }
        return environment;
    }

    static void override(StandardEnvironment environment, Map<String, Object> properties) {
        environment
                .getPropertySources()
                .addFirst(new MapPropertySource("explicit-test-overrides", properties));
    }

    private static StandardEnvironment isolatedEnvironment() {
        StandardEnvironment environment = new StandardEnvironment();
        environment
                .getPropertySources()
                .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment
                .getPropertySources()
                .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        return environment;
    }

    private static List<PropertySource<?>> documents(String resource) throws IOException {
        return new YamlPropertySourceLoader().load(resource, new ClassPathResource(resource));
    }
}
