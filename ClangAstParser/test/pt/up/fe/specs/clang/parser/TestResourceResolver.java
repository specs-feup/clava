/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.parser;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Resolves parser test resources to their original filesystem files. */
public final class TestResourceResolver {

    private TestResourceResolver() {
    }

    public static File resolve(String resourcePath) {
        URL resourceUrl = TestResourceResolver.class.getClassLoader().getResource(resourcePath);
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Could not find test resource '" + resourcePath + "'");
        }

        return resolve(resourcePath, resourceUrl);
    }

    static File resolve(String resourcePath, URL resourceUrl) {
        if (!"file".equals(resourceUrl.getProtocol())) {
            throw new IllegalArgumentException("Test resource '" + resourcePath
                    + "' must be available as a file URL, but was '" + resourceUrl + "'");
        }

        try {
            Path resourceFile = Paths.get(resourceUrl.toURI()).toRealPath();
            if (!Files.isRegularFile(resourceFile)) {
                throw new IllegalArgumentException("Test resource '" + resourcePath
                        + "' does not resolve to a regular file: '" + resourceFile + "'");
            }

            return resourceFile.toFile();
        } catch (URISyntaxException | IOException e) {
            throw new IllegalArgumentException("Could not resolve test resource '" + resourcePath
                    + "' to a real file at '" + resourceUrl + "'", e);
        }
    }
}
