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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AstDumpCacheTest {

    @TempDir
    Path tempFolder;

    @Test
    public void successfulCaptureIsAStableHitWithoutProducerRerun() throws IOException {
        Path source = source("source.cpp", "int value = 1;\n");
        AstDumpCache cache = cache(source, "clang", source.toString(), "-std=c++17");
        AtomicInteger producerRuns = new AtomicInteger();

        String produced = cache.capture(output -> {
            producerRuns.incrementAndGet();
            output.write("dump bytes\n".getBytes(StandardCharsets.UTF_8));
            return "parsed";
        }, ignored -> List.of(source), ignored -> true);

        Optional<String> loaded = cache.load(this::readUtf8);
        Optional<String> loadedAgain = cache.load(this::readUtf8);

        assertEquals("parsed", produced);
        assertEquals(Optional.of("dump bytes\n"), loaded);
        assertEquals(loaded, loadedAgain);
        assertEquals(1, producerRuns.get());
    }

    @Test
    public void sourceContentChangesTheKeyAndMisses() throws IOException {
        Path source = source("source.cpp", "int value = 1;\n");
        AstDumpCache cache = cache(source, "clang", source.toString());
        AtomicInteger producerRuns = new AtomicInteger();

        capture(cache, "first", List.of(source), producerRuns);
        Files.writeString(source, "int value = 2;\n");

        assertTrue(cache.load(this::readUtf8).isEmpty());
        capture(cache, "second", List.of(source), producerRuns);
        assertEquals(2, producerRuns.get());
    }

    @Test
    public void commandOrderAndValueArePartOfTheKey() throws IOException {
        Path source = source("source.cpp", "int value;\n");
        AstDumpCache original = cache(source, "clang", "-a", "-b");
        capture(original, "original", List.of(source), new AtomicInteger());

        AstDumpCache orderChanged = cache(source, "clang", "-b", "-a");
        AstDumpCache valueChanged = cache(source, "clang", "-a", "-c");

        assertTrue(orderChanged.load(this::readUtf8).isEmpty());
        assertTrue(valueChanged.load(this::readUtf8).isEmpty());
    }

    @Test
    public void transitiveDependencyChangeInvalidatesHit() throws IOException {
        Path source = source("source.cpp", "#include \"header.h\"\n");
        Path header = source("header.h", "#define VALUE 1\n");
        AstDumpCache cache = cache(source, "clang", source.toString());

        capture(cache, "with header", List.of(source, header), new AtomicInteger());
        assertTrue(cache.load(this::readUtf8).isPresent());

        Files.writeString(header, "#define VALUE 2\n");

        assertTrue(cache.load(this::readUtf8).isEmpty());
    }

    @Test
    public void corruptManifestAndGzipFailOpen() throws IOException {
        Path source = source("source.cpp", "int value;\n");
        AstDumpCache cache = cache(source, "clang", source.toString());
        capture(cache, "valid", List.of(source), new AtomicInteger());

        Path entry = onlyEntry();
        Files.writeString(entry.resolve("manifest.json"), "not json");
        assertTrue(cache.load(this::readUtf8).isEmpty());
        assertFalse(Files.exists(entry));

        capture(cache, "valid again", List.of(source), new AtomicInteger());
        entry = onlyEntry();
        Files.write(entry.resolve("dump.gz"), new byte[] {1, 2, 3, 4}, StandardOpenOption.TRUNCATE_EXISTING);

        assertTrue(cache.load(this::readUtf8).isEmpty());
        assertFalse(Files.exists(entry));
    }

    @Test
    public void failedProducerIsNeverPublished() throws IOException {
        Path source = source("source.cpp", "int value;\n");
        AstDumpCache cache = cache(source, "clang", source.toString());

        cache.capture(output -> {
            output.write("failed dump".getBytes(StandardCharsets.UTF_8));
            return "failed";
        }, ignored -> List.of(source), ignored -> false);

        assertEquals(List.of(), entryDirectories());
        assertTrue(cache.load(this::readUtf8).isEmpty());
    }

    @Test
    public void concurrentWritersPublishOneCompleteEntry() throws Exception {
        Path source = source("source.cpp", "int value;\n");
        AstDumpCache first = cache(source, "clang", source.toString());
        AstDumpCache second = cache(source, "clang", source.toString());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch producerReady = new CountDownLatch(2);
        CountDownLatch releaseProducers = new CountDownLatch(1);

        try {
            Future<String> firstResult = executor.submit(() -> concurrentCapture(first, "first", producerReady,
                    releaseProducers));
            Future<String> secondResult = executor.submit(() -> concurrentCapture(second, "second", producerReady,
                    releaseProducers));

            assertTrue(producerReady.await(30, TimeUnit.SECONDS));
            releaseProducers.countDown();
            assertNotNull(firstResult.get(30, TimeUnit.SECONDS));
            assertNotNull(secondResult.get(30, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, entryDirectories().size());
        String dump = first.load(this::readUtf8).orElseThrow();
        assertTrue(dump.equals("first") || dump.equals("second"));
    }

    @Test
    public void staleCleanupPreservesCurrentEntryAndRemovesOtherEntry() throws IOException {
        Path currentSource = source("current.cpp", "int current;\n");
        Path staleSource = source("stale.cpp", "int stale;\n");
        AstDumpCache current = cache(currentSource, "clang", currentSource.toString());
        AstDumpCache stale = cache(staleSource, "clang", staleSource.toString());

        capture(current, "current", List.of(currentSource), new AtomicInteger());
        capture(stale, "stale", List.of(staleSource), new AtomicInteger());
        Path currentEntry = entryForSource("current");
        Path staleEntry = entryForSource("stale");
        Instant old = Instant.now().minus(Duration.ofDays(61));
        Files.setLastModifiedTime(currentEntry, FileTime.from(old));
        Files.setLastModifiedTime(staleEntry, FileTime.from(old));

        assertEquals(Optional.of("current"), current.load(this::readUtf8));
        assertTrue(Files.exists(currentEntry));
        assertFalse(Files.exists(staleEntry));
    }

    private String concurrentCapture(AstDumpCache cache, String dump, CountDownLatch producerReady,
                                     CountDownLatch releaseProducers) {
        return cache.capture(output -> {
            producerReady.countDown();
            await(releaseProducers);
            output.write(dump.getBytes(StandardCharsets.UTF_8));
            return dump;
        }, ignored -> List.of(), ignored -> true);
    }

    private void capture(AstDumpCache cache, String dump, Collection<Path> dependencies, AtomicInteger runs) {
        cache.capture(output -> {
            runs.incrementAndGet();
            output.write(dump.getBytes(StandardCharsets.UTF_8));
            return dump;
        }, ignored -> dependencies, ignored -> true);
    }

    private AstDumpCache cache(Path source, String... command) {
        return new AstDumpCache(tempFolder.resolve("cache"), source, List.of(command));
    }

    private Path source(String filename, String contents) throws IOException {
        return Files.writeString(tempFolder.resolve(filename), contents);
    }

    private String readUtf8(InputStream input) throws IOException {
        return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }

    private Path onlyEntry() throws IOException {
        List<Path> entries = entryDirectories();
        assertEquals(1, entries.size());
        return entries.get(0);
    }

    private List<Path> entryDirectories() throws IOException {
        Path entries = tempFolder.resolve("cache/ast-dumps/v1/entries");
        if (!Files.isDirectory(entries)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.list(entries)) {
            return paths.filter(Files::isDirectory)
                    .filter(path -> !path.getFileName().toString().startsWith("."))
                    .toList();
        }
    }

    private Path entryForSource(String dump) throws IOException {
        // Manifest paths are absolute, so the source filename is enough to distinguish these test entries.
        for (Path entry : entryDirectories()) {
            Path manifest = entry.resolve("manifest.json");
            if (Files.readString(manifest).contains("/" + dump + ".cpp")) {
                return entry;
            }
        }
        throw new AssertionError("Could not find entry for " + dump);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(30, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
