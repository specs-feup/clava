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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.ClangAstWebResource.LocalBuild;
import pt.up.fe.specs.clang.ClangAstWebResource.Release;
import pt.up.fe.specs.clang.codeparser.CodeParser;

public class ClangResourcesTest {

    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(10);

    @TempDir
    Path tempFolder;

    @Test
    public void releaseTagIsParsedAsRelease() {
        var release = assertInstanceOf(Release.class, ClangAstWebResource.parseDumperSource("v16.0.5_3"));

        assertEquals("v16.0.5_3", release.tag());
    }

    @Test
    public void absolutePathIsParsedAsLocalBuild() {
        var localBuild = assertInstanceOf(LocalBuild.class,
                ClangAstWebResource.parseDumperSource(tempFolder.toString()));

        assertEquals(tempFolder.toFile(), localBuild.folder());
    }

    @Test
    public void relativePathIsRejected() {
        assertThrows(RuntimeException.class,
                () -> ClangAstWebResource.parseDumperSource("../clang-dumper/build"));
    }

    @Test
    public void localBuildSelectsExpectedTool() throws IOException {
        var toolName = SupportedPlatform.getCurrentPlatform().isWindows() ? "tool.exe" : "tool";
        var tool = tempFolder.resolve(toolName).toFile();
        assertTrue(tool.createNewFile());

        assertEquals(tool, ClangResources.getLocalExecutable(tempFolder.toFile()));
    }

    @Test
    public void localBuildRequiresExpectedTool() {
        assertThrows(RuntimeException.class, () -> ClangResources.getLocalExecutable(tempFolder.toFile()));
    }

    @Test
    public void includesCacheValidationOnlyChecksRequiredFiles() throws IOException {
        var includesFolder = tempFolder.resolve("includes");
        assertFalse(ClangResources.isIncludesCacheValid(includesFolder.toFile()));

        Files.createDirectory(includesFolder);
        assertFalse(ClangResources.isIncludesCacheValid(includesFolder.toFile()));

        Files.createDirectories(includesFolder.resolve("builtin"));
        Files.writeString(includesFolder.resolve("entrypoints.txt"), "builtin\n");
        Files.writeString(includesFolder.resolve("builtin/header.h"), "original");
        Files.writeString(includesFolder.resolve("unexpected.txt"), "extra");

        assertTrue(ClangResources.isIncludesCacheValid(includesFolder.toFile()));

        Files.writeString(includesFolder.resolve("builtin/header.h"), "modified");
        assertTrue(ClangResources.isIncludesCacheValid(includesFolder.toFile()));
    }

    @Test
    public void cudaInstallationIsFoundFromConfiguredRoot() throws IOException {
        var cudaFolder = Files.createDirectories(tempFolder.resolve("cuda"));
        Files.createDirectories(cudaFolder.resolve("include"));
        Files.createFile(cudaFolder.resolve("include/cuda_runtime.h"));

        assertEquals(cudaFolder.toFile().getAbsoluteFile(),
                ClangResources.findCudaInstallation(List.of(cudaFolder.toString()), "", List.of()).orElseThrow());
    }

    @Test
    public void cudaInstallationIsFoundFromNvccPath() throws IOException {
        var cudaFolder = Files.createDirectories(tempFolder.resolve("cuda"));
        var binFolder = Files.createDirectories(cudaFolder.resolve("bin"));
        Files.createDirectories(cudaFolder.resolve("include"));
        Files.createFile(cudaFolder.resolve("include/cuda_runtime.h"));
        Files.createFile(binFolder.resolve("nvcc"));

        assertEquals(cudaFolder.toFile().getAbsoluteFile(),
                ClangResources.findCudaInstallation(List.of(), binFolder.toString(), List.of()).orElseThrow());
    }

    @Test
    public void staleCacheCleanupSkipsLockedVersions() throws IOException {
        var currentVersion = Files.createDirectory(tempFolder.resolve("current")).toFile();
        var staleVersion = Files.createDirectory(tempFolder.resolve("stale")).toFile();
        Files.writeString(staleVersion.toPath().resolve("last-used.txt"),
                Instant.now().minus(Duration.ofDays(61)).toString());

        var parser = CodeParser.newInstance();
        parser.set(CodeParser.DUMPER_FOLDER, tempFolder.toFile());
        var resources = new ClangResources(parser);

        try (var ignored = ClangResources.acquireCacheLock(staleVersion)) {

            resources.deleteStaleVersions(Instant.now(), currentVersion);
            assertTrue(staleVersion.isDirectory());
        }

        assertFalse(ClangResources.getCacheLockFolder(staleVersion).exists());

        resources.deleteStaleVersions(Instant.now(), currentVersion);
        assertFalse(staleVersion.exists());
    }

    @Test
    public void cacheLockSerializesConcurrentAcquisition() throws Exception {
        var versionFolder = Files.createDirectory(tempFolder.resolve("version")).toFile();
        var executor = Executors.newSingleThreadExecutor();
        ClangResources.CacheLock firstLock = ClangResources.acquireCacheLock(versionFolder);

        try {
            var secondLock = executor.submit(() -> ClangResources.acquireCacheLock(versionFolder));
            assertFalse(secondLock.isDone());

            firstLock.close();
            firstLock = null;
            try (var ignored = secondLock.get(10, TimeUnit.SECONDS)) {
                assertTrue(versionFolder.isDirectory());
            }
        } finally {
            if (firstLock != null) {
                firstLock.close();
            }
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        assertFalse(ClangResources.getCacheLockFolder(versionFolder).exists());
    }

    @Test
    public void cacheLockSerializesAcquisitionAcrossJvms() throws Exception {
        var versionFolder = Files.createDirectory(tempFolder.resolve("version")).toFile();
        var firstAcquired = tempFolder.resolve("first.acquired");
        var firstRelease = tempFolder.resolve("first.release");
        var secondAcquired = tempFolder.resolve("second.acquired");
        var secondRelease = tempFolder.resolve("second.release");
        var firstLog = tempFolder.resolve("first.log");
        var secondLog = tempFolder.resolve("second.log");

        Process first = startCacheLockProcess(versionFolder, firstAcquired, firstRelease, firstLog);
        Process second = null;
        try {
            assertTrue(waitForFile(firstAcquired, PROCESS_TIMEOUT));

            second = startCacheLockProcess(versionFolder, secondAcquired, secondRelease, secondLog);
            assertFalse(waitForFile(secondAcquired, Duration.ofMillis(750)),
                    "A second JVM acquired a cache lock that was still held");

            Files.createFile(firstRelease);
            assertTrue(waitForFile(secondAcquired, PROCESS_TIMEOUT));

            Files.createFile(secondRelease);
            waitForProcess(first, firstLog);
            waitForProcess(second, secondLog);
        } finally {
            releaseProcess(firstRelease);
            releaseProcess(secondRelease);
            stopProcess(first);
            stopProcess(second);
        }

        assertFalse(ClangResources.getCacheLockFolder(versionFolder).exists());
    }

    @Test
    public void cacheLockAllowsDifferentVersionsToProceedAcrossJvms() throws Exception {
        var firstVersion = Files.createDirectory(tempFolder.resolve("version-1")).toFile();
        var secondVersion = Files.createDirectory(tempFolder.resolve("version-2")).toFile();
        var firstAcquired = tempFolder.resolve("first.acquired");
        var firstRelease = tempFolder.resolve("first.release");
        var secondAcquired = tempFolder.resolve("second.acquired");
        var secondRelease = tempFolder.resolve("second.release");
        var firstLog = tempFolder.resolve("first.log");
        var secondLog = tempFolder.resolve("second.log");

        Process first = startCacheLockProcess(firstVersion, firstAcquired, firstRelease, firstLog);
        Process second = null;
        try {
            assertTrue(waitForFile(firstAcquired, PROCESS_TIMEOUT));

            second = startCacheLockProcess(secondVersion, secondAcquired, secondRelease, secondLog);
            assertTrue(waitForFile(secondAcquired, PROCESS_TIMEOUT),
                    "A different dumper version was blocked by an unrelated cache lock");

            Files.createFile(firstRelease);
            Files.createFile(secondRelease);
            waitForProcess(first, firstLog);
            waitForProcess(second, secondLog);
        } finally {
            releaseProcess(firstRelease);
            releaseProcess(secondRelease);
            stopProcess(first);
            stopProcess(second);
        }

        assertFalse(ClangResources.getCacheLockFolder(firstVersion).exists());
        assertFalse(ClangResources.getCacheLockFolder(secondVersion).exists());
    }

    @Test
    public void cacheLockRecoversAfterOwningJvmIsTerminated() throws Exception {
        var versionFolder = Files.createDirectory(tempFolder.resolve("version")).toFile();
        var firstAcquired = tempFolder.resolve("first.acquired");
        var firstRelease = tempFolder.resolve("first.release");
        var secondAcquired = tempFolder.resolve("second.acquired");
        var secondRelease = tempFolder.resolve("second.release");
        var firstLog = tempFolder.resolve("first.log");
        var secondLog = tempFolder.resolve("second.log");

        Process first = startCacheLockProcess(versionFolder, firstAcquired, firstRelease, firstLog);
        Process second = null;
        try {
            assertTrue(waitForFile(firstAcquired, PROCESS_TIMEOUT));
            first.destroyForcibly();
            assertTrue(first.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));

            second = startCacheLockProcess(versionFolder, secondAcquired, secondRelease, secondLog);
            assertTrue(waitForFile(secondAcquired, PROCESS_TIMEOUT),
                    "A cache lock left by a terminated JVM was not recovered");

            Files.createFile(secondRelease);
            waitForProcess(second, secondLog);
        } finally {
            releaseProcess(firstRelease);
            releaseProcess(secondRelease);
            stopProcess(first);
            stopProcess(second);
        }

        assertFalse(ClangResources.getCacheLockFolder(versionFolder).exists());
    }

    @Test
    public void cacheLockDoesNotTreatLiveOwnerAsStaleWhenDirectoryIsOld() throws Exception {
        var versionFolder = Files.createDirectory(tempFolder.resolve("version")).toFile();
        var lockFolder = ClangResources.getCacheLockFolder(versionFolder).toPath();
        var secondAcquired = tempFolder.resolve("second.acquired");
        var secondRelease = tempFolder.resolve("second.release");
        var secondLog = tempFolder.resolve("second.log");

        ClangResources.CacheLock first = ClangResources.acquireCacheLock(versionFolder);
        Process second = null;
        try {
            Files.setLastModifiedTime(lockFolder, FileTime.from(Instant.now().minus(Duration.ofHours(1))));

            second = startCacheLockProcess(versionFolder, secondAcquired, secondRelease, secondLog);
            assertFalse(waitForFile(secondAcquired, Duration.ofMillis(750)),
                    "A live cache-lock owner was incorrectly treated as stale");

            first.close();
            first = null;
            assertTrue(waitForFile(secondAcquired, PROCESS_TIMEOUT));
            Files.createFile(secondRelease);
            waitForProcess(second, secondLog);
        } finally {
            if (first != null) {
                first.close();
            }
            releaseProcess(secondRelease);
            stopProcess(second);
        }

        assertFalse(lockFolder.toFile().exists());
    }

    @Test
    public void cacheLockUsesProcessStartTimeWhenPidIsStillAlive() throws Exception {
        var versionFolder = Files.createDirectory(tempFolder.resolve("version")).toFile();
        var lockFolder = ClangResources.getCacheLockFolder(versionFolder).toPath();
        Files.createDirectory(lockFolder);
        assumeTrue(ProcessHandle.current().info().startInstant().isPresent(),
                "The current platform does not expose process start times");
        Files.writeString(lockFolder.resolve("owner"),
                ProcessHandle.current().pid() + System.lineSeparator() + Instant.EPOCH + System.lineSeparator());

        var acquired = tempFolder.resolve("acquired");
        var release = tempFolder.resolve("release");
        var log = tempFolder.resolve("child.log");
        Process child = startCacheLockProcess(versionFolder, acquired, release, log);
        try {
            assertTrue(waitForFile(acquired, PROCESS_TIMEOUT),
                    "A lock with a reused PID and a different process start time was not recovered");
            Files.createFile(release);
            waitForProcess(child, log);
        } finally {
            releaseProcess(release);
            stopProcess(child);
        }

        assertFalse(lockFolder.toFile().exists());
    }

    @Test
    public void cacheLockRecoversAnOldOwnerlessLock() throws Exception {
        var versionFolder = Files.createDirectory(tempFolder.resolve("version")).toFile();
        var lockFolder = ClangResources.getCacheLockFolder(versionFolder).toPath();
        Files.createDirectory(lockFolder);
        Files.setLastModifiedTime(lockFolder, FileTime.from(Instant.now().minus(Duration.ofHours(1))));

        var acquired = tempFolder.resolve("acquired");
        var release = tempFolder.resolve("release");
        var log = tempFolder.resolve("child.log");
        Process child = startCacheLockProcess(versionFolder, acquired, release, log);
        try {
            assertTrue(waitForFile(acquired, PROCESS_TIMEOUT), "An old ownerless cache lock was not recovered");
            Files.createFile(release);
            waitForProcess(child, log);
        } finally {
            releaseProcess(release);
            stopProcess(child);
        }

        assertFalse(lockFolder.toFile().exists());
    }

    @Test
    public void cacheLockDoesNotStealARecentOwnerlessLock() throws Exception {
        var versionFolder = Files.createDirectory(tempFolder.resolve("version")).toFile();
        var lockFolder = ClangResources.getCacheLockFolder(versionFolder).toPath();
        Files.createDirectory(lockFolder);

        var acquired = tempFolder.resolve("acquired");
        var release = tempFolder.resolve("release");
        var log = tempFolder.resolve("child.log");
        Process child = startCacheLockProcess(versionFolder, acquired, release, log);
        try {
            assertFalse(waitForFile(acquired, Duration.ofMillis(750)),
                    "A lock without owner metadata was stolen before its claim was old enough");

            Files.delete(lockFolder);
            assertTrue(waitForFile(acquired, PROCESS_TIMEOUT));
            Files.createFile(release);
            waitForProcess(child, log);
        } finally {
            releaseProcess(release);
            stopProcess(child);
        }

        assertFalse(lockFolder.toFile().exists());
    }

    @Test
    public void staleCacheCleanupSkipsLockedVersionAcrossJvms() throws Exception {
        var currentVersion = Files.createDirectory(tempFolder.resolve("current")).toFile();
        var staleVersion = Files.createDirectory(tempFolder.resolve("stale")).toFile();
        Files.writeString(staleVersion.toPath().resolve("last-used.txt"),
                Instant.now().minus(Duration.ofDays(61)).toString());

        var acquired = tempFolder.resolve("acquired");
        var release = tempFolder.resolve("release");
        var log = tempFolder.resolve("child.log");
        Process child = startCacheLockProcess(staleVersion, acquired, release, log);
        try {
            assertTrue(waitForFile(acquired, PROCESS_TIMEOUT));

            var parser = CodeParser.newInstance();
            parser.set(CodeParser.DUMPER_FOLDER, tempFolder.toFile());
            new ClangResources(parser).deleteStaleVersions(Instant.now(), currentVersion);
            assertTrue(staleVersion.isDirectory(), "Stale cleanup deleted a version locked by another JVM");

            Files.createFile(release);
            waitForProcess(child, log);
        } finally {
            releaseProcess(release);
            stopProcess(child);
        }

        var parser = CodeParser.newInstance();
        parser.set(CodeParser.DUMPER_FOLDER, tempFolder.toFile());
        new ClangResources(parser).deleteStaleVersions(Instant.now(), currentVersion);
        assertFalse(staleVersion.exists());
    }

    @Test
    public void releaseResourcesCanBeInitializedBySeparateJvms() throws Exception {
        var cacheFolder = Files.createDirectory(tempFolder.resolve("cache")).toFile();
        var firstDone = tempFolder.resolve("first.done");
        var secondDone = tempFolder.resolve("second.done");
        var firstLog = tempFolder.resolve("first.log");
        var secondLog = tempFolder.resolve("second.log");

        Process first = startResourceProcess(cacheFolder, firstDone, firstLog);
        Process second = startResourceProcess(cacheFolder, secondDone, secondLog);
        try {
            waitForProcess(first, firstLog);
            waitForProcess(second, secondLog);
        } finally {
            stopProcess(first);
            stopProcess(second);
        }

        var firstExecutable = new File(Files.readString(firstDone).trim());
        var secondExecutable = new File(Files.readString(secondDone).trim());
        assertEquals(firstExecutable.getAbsoluteFile(), secondExecutable.getAbsoluteFile());
        assertTrue(firstExecutable.isFile());
        assertFalse(cacheFolder.toPath().resolve(ClangAstWebResource.getReleaseTag() + ".cache.lock").toFile().exists());
    }

    @Test
    public void cacheLockReleaseDoesNotRemoveAReclaimedLock() throws Exception {
        var versionFolder = Files.createDirectory(tempFolder.resolve("version")).toFile();
        var lockFolder = ClangResources.getCacheLockFolder(versionFolder).toPath();
        ClangResources.CacheLock first = ClangResources.acquireCacheLock(versionFolder);

        Files.writeString(first.ownerFile().toPath(), Long.MAX_VALUE + System.lineSeparator());
        ClangResources.CacheLock second = ClangResources.acquireCacheLock(versionFolder);

        try {
            first.close();
            assertTrue(Files.isDirectory(lockFolder),
                    "A stale lock owner released and deleted a replacement owner's lock");
        } finally {
            second.close();
        }
    }

    @Test
    public void cachedUseDoesNotBypassTheCacheLockBeforeUpdatingLastUsed() throws Exception {
        var parser = CodeParser.newInstance();
        parser.set(CodeParser.DUMPER_FOLDER, tempFolder.toFile());
        var resources = new ClangResources(parser);
        var versionFolder = resources.getClangResourceFolder();
        var fakeExecutable = Files.createFile(tempFolder.resolve("fake-tool")).toFile();
        var cache = getClangFilesCache();
        var cacheKey = getClangFilesCacheKey(resources, LibcMode.SYSTEM);
        var cachedFiles = new ClangFiles(fakeExecutable, List.of());
        cache.put(cacheKey, cachedFiles);

        ClangResources.CacheLock lock = ClangResources.acquireCacheLock(versionFolder);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var future = executor.submit(() -> resources.getClangFiles(LibcMode.SYSTEM));
            assertThrows(TimeoutException.class, () -> future.get(750, TimeUnit.MILLISECONDS),
                    "A cached use updated last-used without coordinating with the cache lock");

            lock.close();
            lock = null;
            assertSame(cachedFiles, future.get(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        } finally {
            if (lock != null) {
                lock.close();
            }
            executor.shutdownNow();
            executor.awaitTermination(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            cache.remove(cacheKey);
        }
    }

    @Test
    public void sameJvmInstancesShareReleaseCacheInitialization() throws Exception {
        var firstParser = CodeParser.newInstance();
        firstParser.set(CodeParser.DUMPER_FOLDER, tempFolder.toFile());
        firstParser.set(CodeParser.CUDA_PATH, CodeParser.getBuiltinOption());

        var secondParser = CodeParser.newInstance();
        secondParser.set(CodeParser.DUMPER_FOLDER, tempFolder.toFile());
        secondParser.set(CodeParser.CUDA_PATH, CodeParser.getBuiltinOption());

        var thirdParser = CodeParser.newInstance();
        thirdParser.set(CodeParser.DUMPER_FOLDER, tempFolder.toFile());
        thirdParser.set(CodeParser.CUDA_PATH, CodeParser.getBuiltinOption());

        var executor = Executors.newFixedThreadPool(3);
        try {
            var first = executor.submit(() -> new ClangResources(firstParser).getClangFiles(LibcMode.SYSTEM));
            var second = executor.submit(() -> new ClangResources(secondParser).getClangFiles(LibcMode.SYSTEM));
            var third = executor.submit(
                    () -> new ClangResources(thirdParser).getClangFiles(LibcMode.BUILTIN_AND_LIBC));

            var firstFiles = first.get(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            var secondFiles = second.get(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            var thirdFiles = third.get(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            assertSame(firstFiles, secondFiles, "Same-JVM instances did not share the ClangFiles cache entry");
            assertEquals(firstFiles.clangExecutable().getAbsoluteFile(), thirdFiles.clangExecutable().getAbsoluteFile());
            assertTrue(firstFiles.clangExecutable().isFile());
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private Process startCacheLockProcess(File versionFolder, Path acquired, Path release, Path log)
            throws IOException {

        var javaExecutable = Path.of(System.getProperty("java.home"), "bin",
                SupportedPlatform.getCurrentPlatform().isWindows() ? "java.exe" : "java");

        return new ProcessBuilder(
                javaExecutable.toString(),
                "-cp",
                System.getProperty("java.class.path"),
                CacheLockProcess.class.getName(),
                versionFolder.getAbsolutePath(),
                acquired.toAbsolutePath().toString(),
                release.toAbsolutePath().toString())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
    }

    private Process startResourceProcess(File cacheFolder, Path done, Path log) throws IOException {
        var javaExecutable = Path.of(System.getProperty("java.home"), "bin",
                SupportedPlatform.getCurrentPlatform().isWindows() ? "java.exe" : "java");

        return new ProcessBuilder(
                javaExecutable.toString(),
                "-cp",
                System.getProperty("java.class.path"),
                CacheLockProcess.class.getName(),
                "resources",
                cacheFolder.getAbsolutePath(),
                done.toAbsolutePath().toString())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
    }

    @SuppressWarnings("unchecked")
    private Map<String, ClangFiles> getClangFilesCache() throws ReflectiveOperationException {
        var cacheField = ClangResources.class.getDeclaredField("CLANG_FILES_CACHE");
        cacheField.setAccessible(true);
        return (Map<String, ClangFiles>) cacheField.get(null);
    }

    private String getClangFilesCacheKey(ClangResources resources, LibcMode libcMode) {
        var source = ClangAstWebResource.getDumperSource();
        var sourceKey = source instanceof Release
                ? source + "_" + resources.getClangResourceFolder().getAbsolutePath()
                : source.toString();
        return libcMode.name() + "_false_" + sourceKey;
    }

    private boolean waitForFile(Path file, Duration timeout) throws InterruptedException {
        var deadline = System.nanoTime() + timeout.toNanos();
        do {
            if (Files.isRegularFile(file)) {
                return true;
            }

            Thread.sleep(10);
        } while (System.nanoTime() < deadline);

        return Files.isRegularFile(file);
    }

    private void waitForProcess(Process process, Path log) throws Exception {
        assertTrue(process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS),
                () -> "Child JVM did not finish. Output: " + readLog(log));
        assertEquals(0, process.exitValue(), () -> "Child JVM failed. Output: " + readLog(log));
    }

    private String readLog(Path log) {
        try {
            return Files.readString(log);
        } catch (IOException e) {
            return "<could not read child log: " + e + ">";
        }
    }

    private void releaseProcess(Path release) throws IOException {
        if (release != null && !Files.exists(release)) {
            Files.createFile(release);
        }
    }

    private void stopProcess(Process process) throws InterruptedException {
        if (process == null || !process.isAlive()) {
            return;
        }

        process.destroyForcibly();
        process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }

    public static final class CacheLockProcess {

        private CacheLockProcess() {
        }

        public static void main(String[] args) throws Exception {
            if (args[0].equals("resources")) {
                var parser = CodeParser.newInstance();
                parser.set(CodeParser.DUMPER_FOLDER, new File(args[1]));
                var clangFiles = new ClangResources(parser).getClangFiles(LibcMode.SYSTEM);
                Files.writeString(Path.of(args[2]), clangFiles.clangExecutable().getAbsolutePath());
                return;
            }

            var versionFolder = new File(args[0]);
            var acquired = Path.of(args[1]);
            var release = Path.of(args[2]);

            try (var ignored = ClangResources.acquireCacheLock(versionFolder)) {
                Files.writeString(acquired, Long.toString(ProcessHandle.current().pid()));
                while (!Files.exists(release)) {
                    Thread.sleep(10);
                }
            }
        }
    }

    @Test
    public void builtinCudaIncludesAreAvailableWithSystemLibc() {
        var parser = CodeParser.newInstance();
        parser.set(CodeParser.CUDA_PATH, CodeParser.getBuiltinOption());

        var clangFiles = new ClangResources(parser).getClangFiles(LibcMode.SYSTEM);
        var hasCudaWrapper = clangFiles.builtinIncludes().stream()
                .map(folder -> new File(folder, "__clang_cuda_runtime_wrapper.h"))
                .anyMatch(File::isFile);

        assertTrue(hasCudaWrapper, "Built-in CUDA must provide Clang's CUDA runtime wrapper independently of libc");
    }
}
