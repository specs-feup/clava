/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clang;

import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.providers.FileResourceProvider;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

final class CacheFiles {

    private CacheFiles() {
    }

    static Path createStagingDirectory(Path parent, String prefix) {
        try {
            Files.createDirectories(parent);
            return Files.createTempDirectory(parent, prefix);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create cache staging directory below '" + parent + "'", e);
        }
    }

    static File installFile(File destination, FileResourceProvider resource, String expectedSha256,
                            String description) {
        if (destination.isFile()) {
            return destination;
        }

        Path stagingDirectory = createStagingDirectory(destination.getParentFile().toPath(),
                "." + destination.getName() + ".tmp-");
        try {
            File stagedFile = resource.write(stagingDirectory.toFile());
            if (stagedFile == null || !stagedFile.isFile()) {
                throw new RuntimeException("Could not download " + description);
            }

            if (expectedSha256 != null && !hasExpectedSha256(stagedFile, expectedSha256)) {
                throw new RuntimeException("Downloaded " + description + " does not match expected SHA-256 '"
                        + expectedSha256 + "'");
            }

            return publish(stagedFile.toPath(), destination.toPath()).toFile();
        } finally {
            deleteQuietly(stagingDirectory);
        }
    }

    static Path publish(Path staging, Path destination) {
        try {
            Files.createDirectories(destination.getParent());
            if (Files.exists(destination)) {
                return destination;
            }

            try {
                Files.move(staging, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (FileAlreadyExistsException e) {
                // Another process completed the same object first.
            } catch (AtomicMoveNotSupportedException e) {
                try {
                    Files.move(staging, destination);
                } catch (FileAlreadyExistsException ignored) {
                    // Another process completed the same object first.
                } catch (FileSystemException collision) {
                    if (!Files.exists(destination)) {
                        throw collision;
                    }

                    // Some file systems report a non-empty directory collision as a generic file-system exception.
                }
            } catch (FileSystemException e) {
                if (!Files.exists(destination)) {
                    throw e;
                }

                // Some file systems report a non-empty directory collision as a generic file-system exception.
            }

            return destination;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not publish cache object '" + destination + "'", e);
        }
    }

    static boolean hasExpectedSha256(File file, String expectedSha256) {
        return expectedSha256.equalsIgnoreCase(calculateSha256(file));
    }

    static void touch(Path path) {
        try {
            Files.setLastModifiedTime(path, FileTime.from(Instant.now()));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not update cache use time for '" + path + "'", e);
        }
    }

    static void deleteStaleDirectories(Path parent, Instant cutoff, Path excluded) {
        if (!Files.isDirectory(parent)) {
            return;
        }

        try (DirectoryStream<Path> children = Files.newDirectoryStream(parent)) {
            for (Path child : children) {
                if (!Files.isDirectory(child) || child.getFileName().toString().startsWith(".")) {
                    continue;
                }

                if (excluded != null && child.toAbsolutePath().normalize().equals(excluded.toAbsolutePath().normalize())) {
                    continue;
                }

                if (Files.getLastModifiedTime(child).toInstant().isBefore(cutoff)) {
                    delete(child);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not clean stale cache directories below '" + parent + "'", e);
        }
    }

    static void deleteStaleStagingDirectories(Path parent, Instant cutoff) {
        if (!Files.isDirectory(parent)) {
            return;
        }

        try (DirectoryStream<Path> children = Files.newDirectoryStream(parent)) {
            for (Path child : children) {
                String name = child.getFileName().toString();
                if (!Files.isDirectory(child) || !name.startsWith(".") || !name.contains(".tmp-")) {
                    continue;
                }

                if (Files.getLastModifiedTime(child).toInstant().isBefore(cutoff)) {
                    delete(child);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not clean stale cache staging directories below '" + parent + "'",
                    e);
        }
    }

    static void delete(Path path) {
        if (!Files.exists(path)) {
            return;
        }

        boolean deleted = path.toFile().isDirectory()
                ? SpecsIo.deleteFolder(path.toFile())
                : SpecsIo.delete(path.toFile());
        if (!deleted && Files.exists(path)) {
            throw new RuntimeException("Could not delete cache path '" + path + "'");
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            delete(path);
        } catch (RuntimeException ignored) {
            // A failed best-effort cleanup must not hide the download or extraction result.
        }
    }

    private static String calculateSha256(File file) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            try (var inputStream = new DigestInputStream(Files.newInputStream(file.toPath()), digest)) {
                inputStream.transferTo(OutputStream.nullOutputStream());
            }

            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not calculate SHA-256 for file '" + file + "'", e);
        }
    }
}
