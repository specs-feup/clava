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

import com.google.gson.Gson;
import pt.up.fe.specs.util.SpecsLogs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Immutable, dependency-aware cache for one Clang AST dumper invocation.
 *
 * <p>The cache deliberately knows nothing about the dump format. The caller supplies the stream parser and tells the
 * cache which files the parsed result depends on. A published entry is a complete immutable directory containing a
 * gzip stream and its dependency manifest.</p>
 */
public final class AstDumpCache {

    /** Cache namespace and manifest schema version. */
    public static final String FORMAT_VERSION = "v1";

    private static final String KEY_FORMAT = "ast-dump-key-v1";
    private static final String MANIFEST_FILENAME = "manifest.json";
    private static final String DUMP_FILENAME = "dump.gz";
    private static final Duration STALE_ENTRY_AGE = Duration.ofDays(60);
    private static final Gson GSON = new Gson();
    private static final byte[][] VOLATILE_MACROS = {
            "__TIME__".getBytes(StandardCharsets.US_ASCII),
            "__DATE__".getBytes(StandardCharsets.US_ASCII),
            "__TIMESTAMP__".getBytes(StandardCharsets.US_ASCII)
    };

    private final Path cacheRoot;
    private final Path entriesRoot;
    private final Path canonicalSource;
    private final List<String> command;

    /**
     * Creates a cache for an exact source and dumper command.
     *
     * @param cacheRoot common Clava cache root
     * @param sourceFile source file being dumped
     * @param command exact ordered dumper command, including executable and arguments
     */
    public AstDumpCache(File cacheRoot, File sourceFile, List<String> command) {
        this(cacheRoot.toPath(), sourceFile.toPath(), command);
    }

    /**
     * Creates a cache for an exact source and dumper command.
     *
     * @param cacheRoot common Clava cache root
     * @param sourceFile source file being dumped
     * @param command exact ordered dumper command, including executable and arguments
     */
    public AstDumpCache(Path cacheRoot, Path sourceFile, List<String> command) {
        this.cacheRoot = Objects.requireNonNull(cacheRoot, "cacheRoot").toAbsolutePath().normalize();
        this.entriesRoot = this.cacheRoot.resolve("ast-dumps").resolve(FORMAT_VERSION).resolve("entries");
        this.canonicalSource = canonicalizeAtConstruction(Objects.requireNonNull(sourceFile, "sourceFile"));
        this.command = List.copyOf(Objects.requireNonNull(command, "command"));
    }

    /**
     * Attempts to load a valid cached dump. Any cache or parser failure is an ordinary miss.
     *
     * @param parser parser for the gzip stream; it must not close the supplied stream
     * @return the parsed value on a valid hit, or an empty optional on a miss
     */
    public <T> Optional<T> load(InputStreamParser<T> parser) {
        Objects.requireNonNull(parser, "parser");

        String key = calculateKey();
        if (key == null) {
            return Optional.empty();
        }

        Path entry = entriesRoot.resolve(key);
        if (!claimValidEntry(entry)) {
            return Optional.empty();
        }

        try (InputStream compressed = Files.newInputStream(entry.resolve(DUMP_FILENAME));
                InputStream input = new GZIPInputStream(compressed)) {
            T result = parser.parse(input);

            // Force the gzip stream to EOF so a truncated stream or invalid trailer cannot be accepted merely because
            // a parser stopped after the first record.
            input.transferTo(OutputStream.nullOutputStream());
            if (result == null) {
                throw new RuntimeException("AST dump parser returned null");
            }

            return Optional.of(result);
        } catch (Exception e) {
            reportCacheFailure("Could not read cached AST dump entry '" + entry + "'", e);
            deleteEntryQuietly(entry);
            return Optional.empty();
        }
    }

    /**
     * Runs a producer and publishes its exact gzip output only when the caller accepts the result.
     *
     * <p>The producer is always run. Cache setup, output, manifest, and publication failures are swallowed as cache
     * misses so that parsing can continue. Producer failures themselves are propagated to preserve dumper semantics.</p>
     *
     * @param producer callback which writes the exact stderr bytes to the supplied stream and returns the parsed value
     * @param dependencyPaths extracts named dependency paths from the parsed value
     * @param publishDecision says whether the dumper and stream parse succeeded
     * @return the producer's parsed value
     */
    public <T> T capture(DumpProducer<T> producer,
                         Function<? super T, ? extends Collection<Path>> dependencyPaths,
                         Predicate<? super T> publishDecision) {
        Objects.requireNonNull(producer, "producer");
        Objects.requireNonNull(dependencyPaths, "dependencyPaths");
        Objects.requireNonNull(publishDecision, "publishDecision");

        String key = calculateKey();
        if (key == null) {
            return runWithoutCache(producer);
        }

        Path entry = entriesRoot.resolve(key);
        try {
            prepareEntriesDirectory(entry);
        } catch (RuntimeException e) {
            reportCacheFailure("Could not prepare AST dump cache", e);
            return runWithoutCache(producer);
        }

        CacheFiles.StagingDirectory staging;
        try {
            staging = CacheFiles.createStagingDirectory(cacheRoot, entriesRoot, "." + key + ".tmp-");
        } catch (RuntimeException e) {
            reportCacheFailure("Could not create AST dump cache staging directory", e);
            return runWithoutCache(producer);
        }

        T result = null;
        boolean producerStarted = false;
        boolean outputUsable = true;
        try {
            Path stagedDump = staging.path().resolve(DUMP_FILENAME);
            try (OutputStream fileOutput = Files.newOutputStream(stagedDump, StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE)) {
                var bestEffortOutput = new BestEffortOutputStream(fileOutput);
                try (var gzipOutput = new GZIPOutputStream(bestEffortOutput)) {
                    producerStarted = true;
                    result = runProducer(producer, gzipOutput);
                }
                outputUsable = !bestEffortOutput.failed();
            } catch (IOException e) {
                // A failed cache stream must not turn a successful dumper run into a parse failure. The producer has
                // not necessarily run if opening the cache stream itself failed, so fall back to the producer with a
                // null sink in that case.
                outputUsable = false;
                reportCacheFailure("Could not write cached AST dump", e);
                discardStagingQuietly(staging);
                return producerStarted ? result : runWithoutCache(producer);
            }
        } catch (RuntimeException e) {
            discardStagingQuietly(staging);
            throw e;
        }

        if (!outputUsable) {
            discardStagingQuietly(staging);
            return result;
        }

        if (!shouldPublish(result, publishDecision)) {
            discardStagingQuietly(staging);
            return result;
        }

        try {
            Manifest manifest = buildManifest(dependencyPaths.apply(result));
            Path stagedManifest = staging.path().resolve(MANIFEST_FILENAME);
            Files.writeString(stagedManifest, GSON.toJson(manifest), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

            // The directory move is the publication point; readers cannot observe only one of dump.gz/manifest.json.
            CacheFiles.publish(staging.path(), entry);
        } catch (RuntimeException | IOException e) {
            reportCacheFailure("Could not publish AST dump cache entry '" + entry + "'", e);
        } finally {
            // publish() can report an existing destination without moving this writer's staging directory. Always
            // remove it before releasing the lock, including the losing-writer case.
            CacheFiles.deleteQuietly(staging.path());
            closeStagingQuietly(staging);
        }

        return result;
    }

    /** Parses a cached gzip stream. */
    @FunctionalInterface
    public interface InputStreamParser<T> {
        T parse(InputStream inputStream) throws Exception;
    }

    /** Produces and parses one dumper invocation while writing exact stderr bytes to the supplied stream. */
    @FunctionalInterface
    public interface DumpProducer<T> {
        T produce(OutputStream outputStream) throws Exception;
    }

    private static final class Manifest {
        private String schema;
        private List<Dependency> dependencies;

        private Manifest(String schema, List<Dependency> dependencies) {
            this.schema = schema;
            this.dependencies = dependencies;
        }
    }

    private static final class Dependency {
        private String path;
        private String sha256;

        private Dependency(String path, String sha256) {
            this.path = path;
            this.sha256 = sha256;
        }
    }

    /** Swallows writes after a cache I/O failure so the producer can still complete its real parse. */
    private static final class BestEffortOutputStream extends OutputStream {
        private final OutputStream delegate;
        private boolean failed;

        private BestEffortOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int value) {
            if (failed) {
                return;
            }

            try {
                delegate.write(value);
            } catch (IOException e) {
                failed = true;
            }
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            if (failed) {
                return;
            }

            try {
                delegate.write(bytes, offset, length);
            } catch (IOException e) {
                failed = true;
            }
        }

        @Override
        public void flush() {
            if (failed) {
                return;
            }

            try {
                delegate.flush();
            } catch (IOException e) {
                failed = true;
            }
        }

        @Override
        public void close() {
            try {
                delegate.close();
            } catch (IOException e) {
                failed = true;
            }
        }

        private boolean failed() {
            return failed;
        }
    }

    private static Path canonicalizeAtConstruction(Path sourceFile) {
        Path absolute = sourceFile.toAbsolutePath().normalize();
        try {
            return absolute.toRealPath();
        } catch (IOException e) {
            // Source disappearance is handled as a cache miss when the key or manifest is next needed.
            return absolute;
        }
    }

    private String calculateKey() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            putString(digest, FORMAT_VERSION);
            putString(digest, KEY_FORMAT);
            putString(digest, canonicalSource.toString());
            putLong(digest, command.size());
            for (String argument : command) {
                putString(digest, argument);
            }

            long sourceSize = Files.size(canonicalSource);
            putLong(digest, sourceSize);
            try (InputStream input = Files.newInputStream(canonicalSource)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }

            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            reportCacheFailure("Could not calculate AST dump cache key for '" + canonicalSource + "'", e);
            return null;
        }
    }

    private static void putString(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        putLong(digest, bytes.length);
        digest.update(bytes);
    }

    private static void putLong(MessageDigest digest, long value) {
        for (int shift = Long.SIZE - Byte.SIZE; shift >= 0; shift -= Byte.SIZE) {
            digest.update((byte) (value >>> shift));
        }
    }

    private boolean claimValidEntry(Path entry) {
        boolean claimed;
        try {
            claimed = CacheFiles.withMaintenanceLock(cacheRoot, () -> {
                try {
                    Files.createDirectories(entriesRoot);
                } catch (IOException e) {
                    throw new UncheckedIOException("Could not create AST dump cache entries directory", e);
                }

                // Claim the entry before cleanup. A concurrent cleanup therefore cannot remove an entry that this
                // reader is about to validate and use.
                if (Files.isDirectory(entry)) {
                    CacheFiles.touch(entry);
                }

                CacheFiles.deleteStaleDirectoriesLocked(entriesRoot, Instant.now().minus(STALE_ENTRY_AGE), entry);
                CacheFiles.deleteUnlockedStagingLocksLocked(entriesRoot);

                if (!Files.isDirectory(entry)) {
                    return false;
                }
                return true;
            });
        } catch (RuntimeException e) {
            reportCacheFailure("Could not validate AST dump cache entry '" + entry + "'", e);
            return false;
        }

        if (!claimed) {
            return false;
        }

        // Hashing dependency files can be expensive. The entry was touched and excluded from cleanup above, so the
        // full manifest validation can safely run without holding the process-wide maintenance lock.
        boolean valid;
        try {
            valid = isManifestValid(entry);
        } catch (RuntimeException e) {
            reportCacheFailure("Could not validate AST dump cache entry '" + entry + "'", e);
            valid = false;
        }

        if (!valid) {
            // Reacquire the maintenance lock before deleting. This keeps invalid-entry removal ordered with cleanup
            // and with another reader claiming the same entry.
            deleteEntryQuietly(entry);
            return false;
        }

        return true;
    }

    private void prepareEntriesDirectory(Path currentEntry) {
        CacheFiles.withMaintenanceLock(cacheRoot, () -> {
            try {
                Files.createDirectories(entriesRoot);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not create AST dump cache entries directory", e);
            }

            CacheFiles.deleteStaleDirectoriesLocked(entriesRoot, Instant.now().minus(STALE_ENTRY_AGE), currentEntry);
            CacheFiles.deleteUnlockedStagingLocksLocked(entriesRoot);
        });
    }

    private boolean isManifestValid(Path entry) {
        Path manifestPath = entry.resolve(MANIFEST_FILENAME);
        Path dumpPath = entry.resolve(DUMP_FILENAME);
        if (!Files.isRegularFile(manifestPath) || !Files.isRegularFile(dumpPath)) {
            return false;
        }

        Manifest manifest = GSON.fromJson(readManifest(manifestPath), Manifest.class);
        if (manifest == null || !FORMAT_VERSION.equals(manifest.schema) || manifest.dependencies == null
                || manifest.dependencies.isEmpty()) {
            return false;
        }

        Set<String> paths = new HashSet<>();
        boolean sourceFound = false;
        for (Dependency dependency : manifest.dependencies) {
            if (dependency == null || dependency.path == null || dependency.sha256 == null
                    || !dependency.sha256.matches("[0-9a-fA-F]{64}") || !paths.add(dependency.path)) {
                return false;
            }

            Path path;
            try {
                path = Path.of(dependency.path);
            } catch (InvalidPathException e) {
                return false;
            }

            if (!path.isAbsolute() || !Files.isRegularFile(path)) {
                return false;
            }

            try {
                if (!path.toRealPath().equals(path.normalize())) {
                    return false;
                }
            } catch (IOException e) {
                return false;
            }

            if (!dependency.sha256.equalsIgnoreCase(CacheFiles.calculateSha256(path))) {
                return false;
            }

            sourceFound |= path.equals(canonicalSource);
        }

        return sourceFound;
    }

    private static String readManifest(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read AST dump cache manifest '" + path + "'", e);
        }
    }

    private Manifest buildManifest(Collection<Path> namedDependencies) {
        Map<String, Path> dependencies = new TreeMap<>();
        addRequiredDependency(dependencies, canonicalSource);

        if (namedDependencies != null) {
            for (Path dependency : namedDependencies) {
                if (dependency == null) {
                    continue;
                }

                addOptionalDependency(dependencies, dependency);
            }
        }

        var entries = new ArrayList<Dependency>();
        for (Map.Entry<String, Path> dependency : dependencies.entrySet()) {
            if (containsVolatileMacro(dependency.getValue())) {
                throw new RuntimeException("AST dump depends on volatile preprocessor macro in '"
                        + dependency.getKey() + "'");
            }

            entries.add(new Dependency(dependency.getKey(), CacheFiles.calculateSha256(dependency.getValue())));
        }

        return new Manifest(FORMAT_VERSION, List.copyOf(entries));
    }

    private static void addRequiredDependency(Map<String, Path> dependencies, Path path) {
        try {
            Path canonical = path.toRealPath();
            if (!Files.isRegularFile(canonical)) {
                throw new IOException("Source is not a regular file: " + path);
            }

            dependencies.put(canonical.toString(), canonical);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not include source dependency '" + path + "'", e);
        }
    }

    private static void addOptionalDependency(Map<String, Path> dependencies, Path path) {
        try {
            Path canonical = path.toRealPath();
            if (!Files.isRegularFile(canonical)) {
                return;
            }

            dependencies.put(canonical.toString(), canonical);
        } catch (IOException e) {
            // Headers can disappear between the dumper's report and manifest creation. Such a path simply cannot
            // participate in invalidation and is intentionally omitted.
        }
    }

    private static boolean containsVolatileMacro(Path path) {
        int maximumMacroLength = 0;
        for (byte[] macro : VOLATILE_MACROS) {
            maximumMacroLength = Math.max(maximumMacroLength, macro.length);
        }

        byte[] window = new byte[maximumMacroLength];
        int windowSize = 0;
        boolean macroFound = false;
        boolean binaryFound = false;
        try (InputStream input = new BufferedInputStream(Files.newInputStream(path))) {
            int next;
            while ((next = input.read()) != -1) {
                if (next == 0) {
                    binaryFound = true;
                }

                if (windowSize < window.length) {
                    window[windowSize++] = (byte) next;
                } else {
                    System.arraycopy(window, 1, window, 0, window.length - 1);
                    window[window.length - 1] = (byte) next;
                }

                for (byte[] macro : VOLATILE_MACROS) {
                    if (windowSize < macro.length) {
                        continue;
                    }

                    int start = windowSize - macro.length;
                    boolean matches = true;
                    for (int index = 0; index < macro.length; index++) {
                        if (window[start + index] != macro[index]) {
                            matches = false;
                            break;
                        }
                    }

                    if (matches) {
                        macroFound = true;
                    }
                }
            }

            return macroFound && !binaryFound;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not scan AST dump dependency '" + path + "'", e);
        }
    }

    private static <T> boolean shouldPublish(T result, Predicate<? super T> publishDecision) {
        try {
            return publishDecision.test(result);
        } catch (RuntimeException e) {
            reportCacheFailure("Could not decide whether to publish AST dump cache result", e);
            return false;
        }
    }

    private static <T> T runWithoutCache(DumpProducer<T> producer) {
        return runProducer(producer, OutputStream.nullOutputStream());
    }

    private static <T> T runProducer(DumpProducer<T> producer, OutputStream output) {
        try {
            return producer.produce(output);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("AST dumper producer failed", e);
        }
    }

    private static void closeStagingQuietly(CacheFiles.StagingDirectory staging) {
        try {
            staging.close();
        } catch (RuntimeException e) {
            reportCacheFailure("Could not close AST dump cache staging lock", e);
        }
    }

    private static void discardStagingQuietly(CacheFiles.StagingDirectory staging) {
        CacheFiles.deleteQuietly(staging.path());
        closeStagingQuietly(staging);
    }

    private void deleteEntryQuietly(Path entry) {
        try {
            CacheFiles.withMaintenanceLock(cacheRoot, () -> CacheFiles.deleteQuietly(entry));
        } catch (RuntimeException e) {
            reportCacheFailure("Could not remove invalid AST dump cache entry '" + entry + "'", e);
        }
    }

    private static void reportCacheFailure(String message, Throwable cause) {
        SpecsLogs.debug(() -> message + ": " + cause.getMessage());
    }
}
