package io.yggdrasil.labs.midgard.start.runtime;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.springframework.core.env.Environment;

import com.yggdrasil.labs.log.converter.SensitiveDataConverter;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;

/**
 * Loads the actual dependency converters with their own static rule state. Logback APIs are shared
 * with the parent loader; no upstream private hook or global LoggerContext reset is used. This
 * exercises the converters, not the Spring-aware preset XML initialization.
 */
final class IsolatedMaskConverters implements AutoCloseable {

    private static final String CONVERTER_PACKAGE = "com.yggdrasil.labs.log.converter.";

    private final URLClassLoader loader =
            new URLClassLoader(
                    new URL[] {
                        SensitiveDataConverter.class
                                .getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                    },
                    SensitiveDataConverter.class.getClassLoader()) {
                @Override
                protected Class<?> loadClass(String name, boolean resolve)
                        throws ClassNotFoundException {
                    synchronized (getClassLoadingLock(name)) {
                        if (!name.startsWith(CONVERTER_PACKAGE)) {
                            return super.loadClass(name, resolve);
                        }
                        Class<?> type = findLoadedClass(name);
                        if (type == null) {
                            type = findClass(name);
                        }
                        if (resolve) {
                            resolveClass(type);
                        }
                        return type;
                    }
                }
            };
    private final LoggerContext context = new LoggerContext();
    private final ClassicConverter message;
    private final ThrowableProxyConverter throwable;

    IsolatedMaskConverters(Environment resolved) throws ReflectiveOperationException {
        context.putProperty(
                SensitiveDataConverter.MASK_ENABLED_PATTERNS_PROPERTY,
                resolved.getProperty("mimir.boot.log.internal.mask.enabled-patterns"));
        context.putProperty(
                SensitiveDataConverter.MASK_CUSTOM_PATTERNS_PROPERTY,
                resolved.getProperty("mimir.boot.log.internal.mask.custom-patterns", ""));
        context.putProperty(
                SensitiveDataConverter.MASK_REPLACEMENT_PROPERTY,
                resolved.getProperty("mimir.boot.log.internal.mask.replacement"));
        message =
                loader.loadClass(CONVERTER_PACKAGE + "SensitiveDataConverter")
                        .asSubclass(ClassicConverter.class)
                        .getConstructor()
                        .newInstance();
        throwable =
                loader.loadClass(CONVERTER_PACKAGE + "SensitiveThrowableProxyConverter")
                        .asSubclass(ThrowableProxyConverter.class)
                        .getConstructor()
                        .newInstance();
        message.setContext(context);
        throwable.setContext(context);
        message.start();
        throwable.start();
    }

    ClassicConverter message() {
        return message;
    }

    ThrowableProxyConverter throwable() {
        return throwable;
    }

    @Override
    public void close() throws IOException {
        throwable.stop();
        message.stop();
        context.stop();
        loader.close();
    }
}
