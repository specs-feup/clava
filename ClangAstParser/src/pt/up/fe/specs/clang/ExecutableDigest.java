/*
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pt.up.fe.specs.clang;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Computes an executable digest once per unchanged file identity. Clang may
 * request the same executable from many translation units in parallel, so the
 * digest is shared across resource validation and cache namespace creation.
 */
public final class ExecutableDigest {

    private static final ConcurrentHashMap<Path, CachedDigest> DIGESTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Path, Object> LOCKS = new ConcurrentHashMap<>();

    private ExecutableDigest() {
    }

    public static String sha256(File executable) {
        if (executable == null || !executable.isFile()) {
            throw new IllegalArgumentException("Cannot hash missing executable: " + executable);
        }

        try {
            Path path = executable.toPath().toRealPath();
            BasicFileAttributes initialAttributes = Files.readAttributes(path, BasicFileAttributes.class);
            CachedDigest cached = DIGESTS.get(path);
            if (cached != null && cached.matches(initialAttributes)) {
                return cached.sha256();
            }

            synchronized (LOCKS.computeIfAbsent(path, ignored -> new Object())) {
                BasicFileAttributes before = Files.readAttributes(path, BasicFileAttributes.class);
                cached = DIGESTS.get(path);
                if (cached != null && cached.matches(before)) {
                    return cached.sha256();
                }

                var digest = MessageDigest.getInstance("SHA-256");
                try (var input = Files.newInputStream(path)) {
                    var buffer = new byte[64 * 1024];
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        digest.update(buffer, 0, read);
                    }
                }

                BasicFileAttributes after = Files.readAttributes(path, BasicFileAttributes.class);
                if (!sameFileVersion(before, after)) {
                    throw new IOException("Executable changed while hashing: " + path);
                }

                String hash = HexFormat.of().formatHex(digest.digest());
                DIGESTS.put(path, new CachedDigest(after.size(), after.lastModifiedTime(), after.fileKey(), hash));
                return hash;
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not hash executable '" + executable + "'", e);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 is required by the JRE", e);
        }
    }

    private static boolean sameFileVersion(BasicFileAttributes left, BasicFileAttributes right) {
        return left.size() == right.size() && left.lastModifiedTime().equals(right.lastModifiedTime())
                && java.util.Objects.equals(left.fileKey(), right.fileKey());
    }

    private record CachedDigest(long size, FileTime modifiedTime, Object fileKey, String sha256) {
        private boolean matches(BasicFileAttributes attributes) {
            return size == attributes.size() && modifiedTime.equals(attributes.lastModifiedTime())
                    && java.util.Objects.equals(fileKey, attributes.fileKey());
        }
    }
}
