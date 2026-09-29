/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.dumper;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opt-in identity context for per-translation-unit wire benchmark metrics.
 *
 * <p>The Java parser tests copy fixtures into per-test temporary directories.
 * This registry lets the test helper attach the original resource name to each
 * copy while the parser worker is running. It is empty during normal use.</p>
 */
public final class AstWireBenchmarkIdentity {

    private static final String METRICS_PROPERTY = "clava.astWireMetrics";
    private static final Map<Path, Identity> IDENTITIES = new ConcurrentHashMap<>();

    private AstWireBenchmarkIdentity() {
    }

    /**
     * Registers source identities for one test-parser pass. The returned scope
     * removes exactly these mappings when closed.
     */
    public static Registration register(Map<File, String> sourceToResourceKey,
            String testId, int testerInvocation, String parsePass) {
        if (!isEnabled() || sourceToResourceKey == null || sourceToResourceKey.isEmpty()) {
            return Registration.NO_OP;
        }

        Map<Path, Identity> requested = new HashMap<>();
        sourceToResourceKey.forEach((source, resourceKey) -> {
            if (source != null) {
                Path path = normalizedPath(source);
                Identity identity = new Identity(
                        testId, testerInvocation, parsePass, resourceKey, null);
                Identity previous = requested.putIfAbsent(path, identity);
                if (previous != null && !previous.equals(identity)) {
                    throw new IllegalArgumentException("Conflicting benchmark identities for '" + path + "'");
                }
            }
        });

        Map<Path, Identity> registered = new HashMap<>();
        for (Map.Entry<Path, Identity> entry : requested.entrySet()) {
            Identity previous = IDENTITIES.putIfAbsent(entry.getKey(), entry.getValue());
            if (previous != null) {
                registered.forEach((path, identity) -> IDENTITIES.remove(path, identity));
                throw new IllegalStateException("A benchmark identity is already registered for '"
                        + entry.getKey() + "'");
            }

            registered.put(entry.getKey(), entry.getValue());
        }

        return () -> registered.forEach((path, identity) -> IDENTITIES.remove(path, identity));
    }

    /**
     * Finds the identity registered by the test helper and attaches the parser's
     * batch-local id. Returns {@code null} when metrics are disabled or the file
     * was not registered by a Java test helper.
     */
    public static Identity lookup(File originalSource, String parseId) {
        if (!isEnabled() || originalSource == null) {
            return null;
        }

        Identity identity = IDENTITIES.get(normalizedPath(originalSource));
        if (identity == null) {
            return null;
        }

        return new Identity(identity.testId(), identity.testerInvocation(), identity.parsePass(),
                identity.resourceKey(), parseId);
    }

    public static boolean isEnabled() {
        return Boolean.getBoolean(METRICS_PROPERTY);
    }

    private static Path normalizedPath(File file) {
        return file.toPath().toAbsolutePath().normalize();
    }

    public record Identity(String testId, int testerInvocation, String parsePass,
            String resourceKey, String parseId) {
    }

    @FunctionalInterface
    public interface Registration extends AutoCloseable {
        Registration NO_OP = () -> {
        };

        @Override
        void close();
    }
}
