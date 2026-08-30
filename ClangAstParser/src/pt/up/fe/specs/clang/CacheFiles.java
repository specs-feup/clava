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
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

final class CacheFiles {

    // FileChannel rejects overlapping locks in one JVM; this monitor serializes the small critical section.
    private static final Object MAINTENANCE_MONITOR = new Object();
    private static final String MAINTENANCE_LOCK_FILENAME = ".maintenance.lock";

    private CacheFiles() {
    }

    private static <T> T withMaintenanceLock(Path cacheRoot, Supplier<T> action) {
        var lockPath = cacheRoot.resolve(MAINTENANCE_LOCK_FILENAME);
        synchronized (MAINTENANCE_MONITOR) {
            try {
                Files.createDirectories(cacheRoot);
                try (var channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                        var ignored = channel.lock()) {
                    return action.get();
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Could not access cache maintenance lock '" + lockPath + "'", e);
            }
        }
    }

    private static void withMaintenanceLock(Path cacheRoot, Runnable action) {
        withMaintenanceLock(cacheRoot, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Claims a published directory, refreshes its use time, and uses it outside the maintenance lock.
     *
     * <p>An empty result means the directory was invalid and removes it before returning. Exceptions leave the
     * directory untouched so callers can report malformed published resources instead of silently repairing them.</p>
     */
    static <T> Optional<T> useDirectory(Path cacheRoot, Path directory,
                                        Function<Path, Optional<T>> use) {
        boolean claimed = withMaintenanceLock(cacheRoot, () -> {
            if (!Files.isDirectory(directory)) {
                return false;
            }

            touchLocked(directory);
            return true;
        });

        if (!claimed) {
            return Optional.empty();
        }

        var result = use.apply(directory);
        if (result.isEmpty()) {
            withMaintenanceLock(cacheRoot, () -> deleteQuietly(directory));
        }

        return result;
    }

    /** Refreshes existing cache paths as one maintenance operation. */
    static void touch(Path cacheRoot, Path... paths) {
        withMaintenanceLock(cacheRoot, () -> {
            for (var path : paths) {
                if (Files.exists(path)) {
                    touchLocked(path);
                }
            }
        });
    }

    static StagingDirectory createStagingDirectory(Path cacheRoot, Path parent, String prefix) {
        return withMaintenanceLock(cacheRoot, () -> createStagingDirectoryLocked(parent, prefix));
    }

    private static StagingDirectory createStagingDirectoryLocked(Path parent, String prefix) {
        Path lockPath;
        try {
            Files.createDirectories(parent);
            lockPath = Files.createTempFile(parent, prefix, ".lock");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create cache staging directory below '" + parent + "'", e);
        }

        FileChannel channel = null;
        Path stagingPath = null;
        try {
            channel = FileChannel.open(lockPath, StandardOpenOption.WRITE);
            channel.lock();
            stagingPath = lockPath.resolveSibling(removeLockSuffix(lockPath.getFileName().toString()));
            Files.createDirectory(stagingPath);
            return new StagingDirectory(stagingPath, lockPath, channel);
        } catch (IOException e) {
            cleanupStagingCreation(stagingPath, lockPath, channel);
            throw new UncheckedIOException("Could not create cache staging directory below '" + parent + "'", e);
        } catch (RuntimeException e) {
            cleanupStagingCreation(stagingPath, lockPath, channel);
            throw e;
        }
    }

    static Path createTemporaryDirectory(Path parent, String prefix) {
        try {
            Files.createDirectories(parent);
            return Files.createTempDirectory(parent, prefix);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create cache temporary directory below '" + parent + "'", e);
        }
    }

    private static String removeLockSuffix(String filename) {
        return filename.substring(0, filename.length() - ".lock".length());
    }

    private static void cleanupStagingCreation(Path stagingPath, Path lockPath, FileChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ignored) {
                // Best-effort cleanup after staging creation failed.
            }
        }

        if (stagingPath != null) {
            deleteQuietly(stagingPath);
        }

        try {
            Files.deleteIfExists(lockPath);
        } catch (IOException ignored) {
            // Best-effort cleanup after staging creation failed.
        }
    }

    record StagingDirectory(Path path, Path lockPath, FileChannel channel) implements AutoCloseable {

        @Override
        public void close() {
            deleteQuietly(path);

            try {
                channel.close();
                Files.deleteIfExists(lockPath);
            } catch (IOException ignored) {
                // Staging cleanup is best-effort. A remaining lock lets a later cache cleanup safely retry.
            }
        }
    }

    static File installFile(Path cacheRoot, File destination, FileResourceProvider resource, String expectedSha256,
                            String description) {
        return installFile(cacheRoot, destination, resource, expectedSha256, -1, description);
    }

    static File installFile(Path cacheRoot, File destination, FileResourceProvider resource, String expectedSha256,
                            long expectedSize, String description) {
        if (destination.isFile()) {
            return destination;
        }

        try (var stagingDirectory = createStagingDirectory(cacheRoot, destination.getParentFile().toPath(),
                "." + destination.getName() + ".tmp-")) {
            File stagedFile = resource.write(stagingDirectory.path().toFile());
            if (stagedFile == null || !stagedFile.isFile()) {
                throw new RuntimeException("Could not download " + description);
            }

            if (expectedSize >= 0 && stagedFile.length() != expectedSize) {
                throw new RuntimeException("Downloaded " + description + " does not match expected size '"
                        + expectedSize + "' (actual: " + stagedFile.length() + ")");
            }

            if (expectedSha256 != null && !hasExpectedSha256(stagedFile, expectedSha256)) {
                throw new RuntimeException("Downloaded " + description + " does not match expected SHA-256 '"
                        + expectedSha256 + "'");
            }

            return publish(stagedFile.toPath(), destination.toPath()).toFile();
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

    private static void touchLocked(Path path) {
        try {
            Files.setLastModifiedTime(path, FileTime.from(Instant.now()));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not update cache use time for '" + path + "'", e);
        }
    }

    /** Removes stale published directories and abandoned staging directories in one locked pass. */
    static void cleanupDirectories(Path cacheRoot, Path parent, Instant cutoff, Path excluded) {
        withMaintenanceLock(cacheRoot, () -> {
            deleteStaleDirectories(parent, cutoff, excluded);
            deleteUnlockedStagingDirectories(parent);
        });
    }

    /** Removes abandoned staging directories without treating other child directories as cache entries. */
    static void cleanupStagingDirectories(Path cacheRoot, Path parent) {
        withMaintenanceLock(cacheRoot, () -> deleteUnlockedStagingDirectories(parent));
    }

    private static void deleteStaleDirectories(Path parent, Instant cutoff, Path excluded) {
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

                deleteIfStale(child, cutoff);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not clean stale cache directories below '" + parent + "'", e);
        }
    }

    private static void deleteIfStale(Path path, Instant cutoff) {
        try {
            if (Files.isDirectory(path)
                    && Files.getLastModifiedTime(path).toInstant().isBefore(cutoff)) {
                delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not inspect cache path '" + path + "'", e);
        }
    }

    private static void deleteUnlockedStagingDirectories(Path parent) {
        if (!Files.isDirectory(parent)) {
            return;
        }

        try (DirectoryStream<Path> locks = Files.newDirectoryStream(parent, ".*.tmp-*.lock")) {
            for (Path lock : locks) {
                deleteIfUnlockedStagingLock(lock);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not clean cache staging directories below '" + parent + "'",
                    e);
        }
    }

    private static void deleteIfUnlockedStagingLock(Path lockPath) {
        try {
            try (var channel = FileChannel.open(lockPath, StandardOpenOption.WRITE)) {
                FileLock lock;
                try {
                    lock = channel.tryLock();
                } catch (OverlappingFileLockException e) {
                    return;
                }

                if (lock == null) {
                    return;
                }

                try (lock) {
                    delete(lockPath.resolveSibling(removeLockSuffix(lockPath.getFileName().toString())));
                }
            }
            Files.deleteIfExists(lockPath);
        } catch (NoSuchFileException e) {
            // Another cleanup or publisher already removed the candidate.
        } catch (IOException e) {
            throw new UncheckedIOException("Could not inspect cache staging lock '" + lockPath + "'", e);
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

    static String calculateSha256(Path path) {
        return calculateSha256(path.toFile());
    }
}
