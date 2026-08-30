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
import pt.up.fe.specs.clang.ClangAstWebResource.ClangDumperManifest;
import pt.up.fe.specs.clang.ClangAstWebResource.ClangDumperManifestAsset;
import pt.up.fe.specs.clang.ClangAstWebResource.LocalBuild;
import pt.up.fe.specs.clang.ClangAstWebResource.Release;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.dumper.ClangAstDumper;
import pt.up.fe.specs.clang.parsers.TopLevelNodesParser;
import pt.up.fe.specs.util.providers.FileResourceProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class ClangResourcesTest {

    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(30);
    private static final String HELLO_SHA256 = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

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
        var toolName = ClangAstDumper.usePlugin()
                ? System.mapLibraryName("plugin")
                : SupportedPlatform.getCurrentPlatform().isWindows() ? "tool.exe" : "tool";
        var tool = tempFolder.resolve(toolName).toFile();
        assertTrue(tool.createNewFile());

        assertEquals(tool, ClangResources.getLocalExecutable(tempFolder.toFile()));
    }

    @Test
    public void localBuildRequiresExpectedTool() {
        assertThrows(RuntimeException.class, () -> ClangResources.getLocalExecutable(tempFolder.toFile()));
    }

    @Test
    public void manifestValidationAndAssetSelectionArePreserved() {
        var tool = asset("tool", "tool", "linux", "x64");
        var plugin = asset("plugin", "plugin", "linux", "x64");
        var manifest = new ClangDumperManifest(1, List.of(tool, plugin));

        assertDoesNotThrow(manifest::validate);
        assertEquals(tool, manifest.getAsset("linux", "x64", "tool"));
        assertEquals(plugin, manifest.getAsset("linux", "x64", "plugin"));
        assertEquals(HELLO_SHA256, tool.sha256());
        assertThrows(RuntimeException.class, () -> manifest.getAsset("windows", "x64", "tool"));
        assertThrows(RuntimeException.class, () -> new ClangDumperManifest(2, List.of(tool)).validate());
        assertThrows(RuntimeException.class, () -> new ClangDumperManifest(1, List.of()).validate());
    }

    @Test
    public void includesCacheValidationChecksEntrypointsButNotEveryFile() throws IOException {
        var includesFolder = tempFolder.resolve("includes");
        assertFalse(ClangResources.isIncludesCacheValid(includesFolder.toFile()));

        Files.createDirectories(includesFolder.resolve("builtin"));
        Files.writeString(includesFolder.resolve("entrypoints.txt"), "builtin\n");
        Files.writeString(includesFolder.resolve("builtin/header.h"), "original");
        Files.writeString(includesFolder.resolve("unexpected.txt"), "extra");

        assertTrue(ClangResources.isIncludesCacheValid(includesFolder.toFile()));

        Files.writeString(includesFolder.resolve("builtin/header.h"), "modified");
        assertTrue(ClangResources.isIncludesCacheValid(includesFolder.toFile()));
        Files.writeString(includesFolder.resolve("entrypoints.txt"), "missing\n");
        assertFalse(ClangResources.isIncludesCacheValid(includesFolder.toFile()));
    }

    @Test
    public void entrypointsPreserveDeclaredIncludeOrder() throws IOException {
        var includesFolder = Files.createDirectories(tempFolder.resolve("includes"));
        var first = Files.createDirectories(includesFolder.resolve("first"));
        var second = Files.createDirectories(includesFolder.resolve("second"));
        Files.writeString(includesFolder.resolve("entrypoints.txt"), "second\nfirst\n");

        assertEquals(List.of(second.toFile(), first.toFile()),
                ClangResources.getIncludeFolders(includesFolder.toFile()));
    }

    @Test
    public void releasesWithTheSameIncludesShaShareOneExtraction() throws Exception {
        var archive = createIncludesArchive();
        var sha = sha256(archive);
        var firstAsset = new ClangDumperManifestAsset("v1-includes.zip", "includes", "linux", "x64", 18, sha);
        var secondAsset = new ClangDumperManifestAsset("v2-includes.zip", "includes", "linux", "x64", 18, sha);
        var firstManifest = new ClangDumperManifest(1, List.of(firstAsset));
        var secondManifest = new ClangDumperManifest(1, List.of(secondAsset));
        var firstRelease = Files.createDirectories(tempFolder.resolve("releases/v1"));
        var secondRelease = Files.createDirectories(tempFolder.resolve("releases/v2"));
        var firstWrites = new AtomicInteger();
        var secondWrites = new AtomicInteger();

        firstManifest.validate();
        secondManifest.validate();
        assertNotEquals(firstRelease, secondRelease);

        var firstIncludes = ClangResources.resolveIncludes(tempFolder.toFile(), firstAsset,
                copyingResource(archive, firstWrites));
        var secondIncludes = ClangResources.resolveIncludes(tempFolder.toFile(), secondAsset,
                copyingResource(archive, secondWrites));

        assertEquals(firstAsset, firstManifest.getAsset("linux", "x64", "includes"));
        assertEquals(secondAsset, secondManifest.getAsset("linux", "x64", "includes"));
        assertEquals(firstIncludes, secondIncludes);
        assertEquals(1, firstWrites.get());
        assertEquals(0, secondWrites.get());
        assertTrue(ClangResources.isIncludesCacheValid(firstIncludes));
        try (var children = Files.list(tempFolder.resolve("includes"))) {
            assertEquals(1, children.filter(Files::isDirectory).count());
        }
    }

    @Test
    public void invalidPublishedIncludesFailWithoutRepair() throws IOException {
        var sha = "a".repeat(64);
        var invalidFolder = ClangResources.getSharedIncludesFolder(tempFolder.toFile(), sha);
        Files.createDirectories(invalidFolder.toPath());
        Files.writeString(invalidFolder.toPath().resolve("entrypoints.txt"), "missing\n");
        var writes = new AtomicInteger();
        var unusedArchive = tempFolder.resolve("unused.zip");

        var error = assertThrows(RuntimeException.class,
                () -> ClangResources.resolveIncludes(tempFolder.toFile(),
                        new ClangDumperManifestAsset("includes.zip", "includes", "linux", "x64", 18, sha),
                        copyingResource(unusedArchive, writes)));

        assertTrue(error.getMessage().contains(invalidFolder.getAbsolutePath()));
        assertTrue(error.getMessage().contains(sha));
        assertTrue(error.getMessage().contains("delete this directory manually to regenerate"));
        assertTrue(invalidFolder.isDirectory());
        assertEquals("missing\n", Files.readString(invalidFolder.toPath().resolve("entrypoints.txt")));
        assertEquals(0, writes.get());
    }

    @Test
    public void corruptNewDownloadIsRejectedWithoutRetry() throws IOException {
        var source = Files.writeString(tempFolder.resolve("source"), "bad");
        var writes = new AtomicInteger();
        var destination = tempFolder.resolve("release/tool").toFile();

        assertThrows(RuntimeException.class,
                () -> CacheFiles.installFile(tempFolder, destination, copyingResource(source, writes), HELLO_SHA256,
                        "test asset"));

        assertEquals(1, writes.get());
        assertFalse(destination.exists());
        try (var children = Files.list(destination.getParentFile().toPath())) {
            assertTrue(children.noneMatch(path -> path.getFileName().toString().startsWith(".tool.tmp-")));
        }
    }

    @Test
    public void existingReleaseResourceIsReused() throws IOException {
        var destination = tempFolder.resolve("release/tool").toFile();
        Files.createDirectories(destination.toPath().getParent());
        Files.writeString(destination.toPath(), "cached");
        var writes = new AtomicInteger();
        var source = Files.writeString(tempFolder.resolve("source"), "new");

        assertEquals(destination,
                CacheFiles.installFile(tempFolder, destination, copyingResource(source, writes), HELLO_SHA256,
                        "test asset"));
        assertEquals(0, writes.get());
        assertEquals("cached", Files.readString(destination.toPath()));
    }

    @Test
    public void concurrentInitializationLeavesOneValidIncludesTree() throws Exception {
        var archive = createIncludesArchive();
        var sha = sha256(archive);
        var asset = new ClangDumperManifestAsset("includes.zip", "includes", "linux", "x64", 18, sha);
        var writes = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(4);
        var futures = new ArrayList<Future<Path>>();

        try {
            for (int i = 0; i < 4; i++) {
                futures.add(executor.submit(() -> ClangResources.resolveIncludes(tempFolder.toFile(), asset,
                        copyingResource(archive, writes)).toPath()));
            }

            for (var future : futures) {
                assertEquals(ClangResources.getSharedIncludesFolder(tempFolder.toFile(), sha).toPath(),
                        future.get(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }

        var finalFolder = ClangResources.getSharedIncludesFolder(tempFolder.toFile(), sha);
        assertTrue(ClangResources.isIncludesCacheValid(finalFolder));
        assertTrue(writes.get() >= 1);
        try (var children = Files.list(finalFolder.toPath().getParent())) {
            var childPaths = children.toList();
            assertEquals(1, childPaths.stream().filter(Files::isDirectory).count());
            assertTrue(childPaths.stream()
                    .noneMatch(path -> path.getFileName().toString().startsWith("." + sha + ".tmp-")));
        }
    }

    @Test
    public void activelyLockedStagingDirectoriesArePreserved() throws Exception {
        var includesRoot = Files.createDirectories(tempFolder.resolve("includes"));
        Path stagingPath;
        Path lockPath;
        try (var staging = CacheFiles.createStagingDirectory(tempFolder, includesRoot, ".sha.tmp-")) {
            stagingPath = staging.path();
            lockPath = staging.lockPath();
            CacheFiles.cleanupDirectories(tempFolder, includesRoot, Instant.EPOCH, null);
            assertTrue(Files.exists(stagingPath));
        }

        assertFalse(Files.exists(stagingPath));
        assertFalse(Files.exists(lockPath));
    }

    @Test
    public void unlockedStagingDirectoriesAreCleaned() throws Exception {
        var includesRoot = Files.createDirectories(tempFolder.resolve("includes"));
        var stagingPath = Files.createDirectory(includesRoot.resolve(".sha.tmp-123"));
        var lockPath = includesRoot.resolve(".sha.tmp-123.lock");
        Files.createFile(lockPath);

        assertTrue(Files.exists(lockPath));
        CacheFiles.cleanupStagingDirectories(tempFolder, includesRoot);

        assertFalse(Files.exists(stagingPath));
        assertFalse(Files.exists(lockPath));
    }

    @Test
    public void orphanedStagingLocksAreCleaned() throws IOException {
        var includesRoot = Files.createDirectories(tempFolder.resolve("includes"));
        var lockPath = includesRoot.resolve(".orphan.tmp-123.lock");
        Files.createFile(lockPath);

        CacheFiles.cleanupDirectories(tempFolder, includesRoot, Instant.EPOCH, null);

        assertFalse(Files.exists(lockPath));
    }

    @Test
    public void staleReleaseAndSharedIncludesAreRemovedAfterSixtyDays() throws IOException {
        var clangCacheRoot = clangCacheRoot();
        var releases = Files.createDirectories(clangCacheRoot.resolve("releases"));
        var current = Files.createDirectories(releases.resolve("current"));
        var staleRelease = Files.createDirectories(releases.resolve("stale"));
        var staleIncludes = Files.createDirectories(
                ClangResources.getSharedIncludesFolder(clangCacheRoot.toFile(), "c".repeat(64)).toPath()
                        .resolve("builtin"));
        Files.writeString(staleIncludes.getParent().resolve("entrypoints.txt"), "builtin\n");
        var old = FileTime.from(Instant.now().minus(Duration.ofDays(61)));
        Files.setLastModifiedTime(staleRelease, old);
        Files.setLastModifiedTime(staleIncludes.getParent(), old);

        var parser = CodeParser.newInstance();
        parser.set(CodeParser.DUMPER_FOLDER, tempFolder.toFile());
        new ClangResources(parser).deleteStaleVersions(Instant.now(), current.toFile());

        assertTrue(Files.exists(current));
        assertFalse(Files.exists(staleRelease));
        assertFalse(Files.exists(staleIncludes.getParent()));
    }

    @Test
    public void usingSharedIncludesRefreshesItsLastUsedTime() throws IOException {
        var sha = "d".repeat(64);
        var clangCacheRoot = clangCacheRoot();
        var shared = Files.createDirectories(
                ClangResources.getSharedIncludesFolder(clangCacheRoot.toFile(), sha).toPath());
        Files.createDirectories(shared.resolve("builtin"));
        Files.writeString(shared.resolve("entrypoints.txt"), "builtin\n");
        Files.setLastModifiedTime(shared, FileTime.from(Instant.now().minus(Duration.ofDays(61))));
        var writes = new AtomicInteger();
        var unusedArchive = tempFolder.resolve("unused.zip");
        var asset = new ClangDumperManifestAsset("includes.zip", "includes", "linux", "x64", 18, sha);
        assertEquals(shared.toFile(), ClangResources.resolveIncludes(clangCacheRoot.toFile(), asset,
                copyingResource(unusedArchive, writes)));
        assertEquals(0, writes.get());

        var parser = CodeParser.newInstance();
        parser.set(CodeParser.DUMPER_FOLDER, tempFolder.toFile());
        new ClangResources(parser).deleteStaleVersions(Instant.now(),
                Files.createDirectories(clangCacheRoot.resolve("releases/current")).toFile());

        assertTrue(Files.exists(shared));
    }

    @Test
    public void claimedDirectoryRemainsUsableDuringContendingCleanup() throws Exception {
        var releases = Files.createDirectories(tempFolder.resolve("releases"));
        var stale = Files.createDirectories(releases.resolve("stale"));
        Files.setLastModifiedTime(stale, FileTime.from(Instant.now().minus(Duration.ofDays(61))));
        var usageStarted = new CountDownLatch(1);
        var allowUsageToFinish = new CountDownLatch(1);
        var cleanupStarted = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            var usage = executor.submit(() -> CacheFiles.useDirectory(tempFolder, stale, path -> {
                usageStarted.countDown();
                awaitLatch(allowUsageToFinish);
                return Optional.of(path);
            }));
            assertTrue(usageStarted.await(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));

            var cleanup = executor.submit(() -> {
                cleanupStarted.countDown();
                CacheFiles.cleanupDirectories(tempFolder, releases,
                        Instant.now().minus(Duration.ofDays(60)), null);
            });
            assertTrue(cleanupStarted.await(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
            cleanup.get(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            allowUsageToFinish.countDown();
            usage.get(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            assertTrue(Files.exists(stale));
        } finally {
            allowUsageToFinish.countDown();
            executor.shutdownNow();
            executor.awaitTermination(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    @Test
    public void cleanupWinnerMakesSubsequentIncludesResolutionAcknowledgeTheMiss() throws Exception {
        var archive = createIncludesArchive();
        var sha = sha256(archive);
        var asset = new ClangDumperManifestAsset("includes.zip", "includes", "linux", "x64", 18, sha);
        var shared = ClangResources.getSharedIncludesFolder(tempFolder.toFile(), sha);
        Files.createDirectories(shared.toPath().resolve("builtin"));
        Files.writeString(shared.toPath().resolve("entrypoints.txt"), "builtin\n");
        Files.setLastModifiedTime(shared.toPath(), FileTime.from(Instant.now().minus(Duration.ofDays(61))));

        CacheFiles.cleanupDirectories(tempFolder, shared.toPath().getParent(),
                Instant.now().minus(Duration.ofDays(60)), null);
        assertFalse(shared.exists());

        var writes = new AtomicInteger();
        assertEquals(shared, ClangResources.resolveIncludes(tempFolder.toFile(), asset,
                copyingResource(archive, writes)));
        assertEquals(1, writes.get());
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
        assertTrue(cacheFolder.toPath().resolve("clang-dumper").resolve("releases")
                .resolve(ClangAstWebResource.getReleaseTag()).toFile().isDirectory());
    }

    @Test
    public void maintenanceLockIsSharedAcrossJvmProcesses() throws Exception {
        var holder = startMaintenanceProcess(MaintenanceLockHolderProcess.class, tempFolder);
        var contender = (Process) null;
        try (var holderOutput = new BufferedReader(
                new InputStreamReader(holder.getInputStream(), StandardCharsets.UTF_8))) {
            assertEquals("READY", holderOutput.readLine());

            contender = startMaintenanceProcess(MaintenanceLockProbeProcess.class, tempFolder);
            try (var contenderOutput = new BufferedReader(
                    new InputStreamReader(contender.getInputStream(), StandardCharsets.UTF_8))) {
                assertEquals("BLOCKED", contenderOutput.readLine());

                holder.getOutputStream().write('\n');
                holder.getOutputStream().flush();
                assertEquals("DONE", holderOutput.readLine());

                contender.getOutputStream().write('\n');
                contender.getOutputStream().flush();
                assertEquals("ENTERED", contenderOutput.readLine());
                assertEquals("DONE", contenderOutput.readLine());
                assertTrue(contender.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
                assertEquals(0, contender.exitValue());
            }
        } finally {
            try {
                holder.getOutputStream().write('\n');
                holder.getOutputStream().flush();
            } catch (IOException ignored) {
                // The holder may already have exited after the assertion path.
            }
            if (contender != null) {
                try {
                    contender.getOutputStream().write('\n');
                    contender.getOutputStream().flush();
                } catch (IOException ignored) {
                    // The contender may already have exited after the assertion path.
                }
            }
            stopProcess(contender);
            stopProcess(holder);
        }
    }

    @Test
    public void sameJvmInstancesReuseReleaseFilesAndPrepareIncludesOnlyForBuiltinLibc() throws Exception {
        var firstParser = newParser("");
        var secondParser = newParser("");
        var thirdParser = newParser("");
        var firstResources = new ClangResources(firstParser);
        var secondResources = new ClangResources(secondParser);
        var thirdResources = new ClangResources(thirdParser);

        var executor = Executors.newFixedThreadPool(3);
        try {
            var first = executor.submit(() -> firstResources.getClangFiles(LibcMode.SYSTEM));
            var second = executor.submit(() -> secondResources.getClangFiles(LibcMode.SYSTEM));
            var third = executor.submit(() -> thirdResources.getClangFiles(LibcMode.BUILTIN_AND_LIBC));

            var firstFiles = first.get(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            var secondFiles = second.get(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            var thirdFiles = third.get(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            assertEquals(firstFiles, secondFiles);
            assertEquals(firstFiles.clangExecutable().getAbsoluteFile(), thirdFiles.clangExecutable().getAbsoluteFile());
            assertTrue(firstFiles.clangExecutable().isFile());
            assertTrue(firstFiles.builtinIncludes().isEmpty());
            assertTrue(secondFiles.builtinIncludes().isEmpty());
            assertFalse(thirdFiles.builtinIncludes().isEmpty());

            var shared = new File(thirdFiles.builtinIncludes().get(0)).toPath();
            while (!Files.isRegularFile(shared.resolve("entrypoints.txt"))) {
                shared = shared.getParent();
            }
            Files.setLastModifiedTime(shared, FileTime.from(Instant.now().minus(Duration.ofDays(61))));
            thirdResources.getClangFiles(LibcMode.BUILTIN_AND_LIBC);
            assertTrue(Files.getLastModifiedTime(shared).toInstant().isAfter(Instant.now().minus(Duration.ofDays(1))));
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    @Test
    public void builtinCudaAutoSystemLibcUsesSameResourcePathAsExplicitSystem() {
        var parser = newParser(CodeParser.getBuiltinOption());
        var resources = new ClangResources(parser);
        var autoFiles = resources.getClangFiles(LibcMode.AUTO);
        var systemFiles = resources.getClangFiles(LibcMode.SYSTEM);

        assertEquals(LibcMode.SYSTEM, autoFiles.libcMode());
        assertEquals(LibcMode.SYSTEM, systemFiles.libcMode());
        assertTrue(autoFiles.builtinIncludes().isEmpty());
        assertEquals(systemFiles.systemResourceDir(), autoFiles.systemResourceDir());
        assertNotNull(autoFiles.systemResourceDir());
        assertTrue(autoFiles.systemResourceDir().isDirectory());
        assertFalse(Files.exists(clangCacheRoot().resolve("includes")));
    }

    @Test
    public void builtinCudaInstallationHasCanonicalLayout() {
        var parser = newParser(CodeParser.getBuiltinOption());
        var cudaFolder = new ClangResources(parser).getBuiltinCudaLib();

        assertEquals(tempFolder.resolve("cuda").resolve(ClangAstWebResource.getCudaReleaseTag())
                .toFile().getAbsolutePath(), cudaFolder.getAbsolutePath());
        assertTrue(new File(cudaFolder, CudaResources.getManifestFilename(ClangAstWebResource.getCudaReleaseTag()))
                .isFile());
        assertTrue(new File(cudaFolder, CudaResources.PLATFORM_FILENAME).isFile());
        assertFalse(new File(cudaFolder, "archives").exists());
        assertFalse(new File(cudaFolder, "cudalib").exists());
        assertFalse(new File(cudaFolder, "linux-x86_64").exists());
        assertTrue(new File(cudaFolder, "include/cuda.h").isFile());
        assertTrue(new File(cudaFolder, "include/cuda_runtime.h").isFile());
        assertTrue(new File(cudaFolder, "nvvm/libdevice/libdevice.10.bc").isFile());
    }

    @Test
    public void libcDetectionIsScopedToTheExecutable() throws IOException {
        assumeTrue(!SupportedPlatform.getCurrentPlatform().isWindows(), "Shell fixtures require a Unix executable");

        var systemLibcDumper = tempFolder.resolve("system-libc-dumper");
        Files.writeString(systemLibcDumper,
                "#!/bin/sh\nprintf '%s\\n' '" + TopLevelNodesParser.getTopLevelNodesHeader() + "'\n");
        assertTrue(systemLibcDumper.toFile().setExecutable(true));

        var builtinLibcDumper = tempFolder.resolve("builtin-libc-dumper");
        Files.writeString(builtinLibcDumper, "#!/bin/sh\nexit 1\n");
        assertTrue(builtinLibcDumper.toFile().setExecutable(true));

        assertEquals(LibcMode.SYSTEM,
                ClangResources.resolveLibcMode(systemLibcDumper.toFile(), LibcMode.AUTO, false));
        assertEquals(LibcMode.BUILTIN_AND_LIBC,
                ClangResources.resolveLibcMode(builtinLibcDumper.toFile(), LibcMode.AUTO, false));
        assertEquals(LibcMode.SYSTEM,
                ClangResources.resolveLibcMode(systemLibcDumper.toFile(), LibcMode.SYSTEM, false));
        assertEquals(LibcMode.BUILTIN_AND_LIBC,
                ClangResources.resolveLibcMode(builtinLibcDumper.toFile(), LibcMode.BUILTIN_AND_LIBC, false));
    }

    @Test
    public void forcedBuildAndPluginModesResolveToSystemWithoutAutoState() throws IOException {
        assumeTrue(!SupportedPlatform.getCurrentPlatform().isWindows(), "Shell fixtures require a Unix executable");

        var dumper = tempFolder.resolve("dumper");
        Files.writeString(dumper, "#!/bin/sh\nexit 1\n");
        assertTrue(dumper.toFile().setExecutable(true));

        assertEquals(LibcMode.SYSTEM,
                ClangResources.resolveLibcMode(dumper.toFile(), LibcMode.BUILTIN_AND_LIBC, true));
        assertEquals(LibcMode.SYSTEM,
                ClangResources.resolveLibcMode(dumper.toFile(), LibcMode.AUTO, true));
        assertThrows(IllegalArgumentException.class,
                () -> new ClangFiles(dumper.toFile(), List.of(), null, LibcMode.AUTO));
    }

    private CodeParser newParser(String cudaPath) {
        var parser = CodeParser.newInstance();
        parser.set(CodeParser.DUMPER_FOLDER, tempFolder.toFile());
        parser.set(CodeParser.CUDA_PATH, cudaPath);
        return parser;
    }

    private Path clangCacheRoot() {
        return tempFolder.resolve("clang-dumper");
    }

    private static ClangDumperManifestAsset asset(String filename, String kind, String platform, String arch) {
        return new ClangDumperManifestAsset(filename, kind, platform, arch, 18, HELLO_SHA256);
    }

    private static FileResourceProvider copyingResource(Path source, AtomicInteger writes) {
        return new FileResourceProvider() {
            @Override
            public File write(File folder) {
                writes.incrementAndGet();
                try {
                    var destination = folder.toPath().resolve(getFilename());
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                    return destination.toFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String version() {
                return "test";
            }

            @Override
            public String getFilename() {
                return source.getFileName().toString();
            }
        };
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Timed out waiting for maintenance-lock test coordination");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private Path createIncludesArchive() throws IOException {
        var archive = tempFolder.resolve("includes.zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("builtin/"));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("entrypoints.txt"));
            zip.write("builtin\n".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("builtin/header.h"));
            zip.write("header\n".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        return archive;
    }

    private static String sha256(Path file) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private Process startResourceProcess(File cacheFolder, Path done, Path log) throws IOException {
        var javaExecutable = Path.of(System.getProperty("java.home"), "bin",
                SupportedPlatform.getCurrentPlatform().isWindows() ? "java.exe" : "java");

        return new ProcessBuilder(
                javaExecutable.toString(),
                "-cp",
                System.getProperty("java.class.path"),
                ResourceProcess.class.getName(),
                cacheFolder.getAbsolutePath(),
                done.toAbsolutePath().toString())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
    }

    private Process startMaintenanceProcess(Class<?> processClass, Path cacheFolder) throws IOException {
        var javaExecutable = Path.of(System.getProperty("java.home"), "bin",
                SupportedPlatform.getCurrentPlatform().isWindows() ? "java.exe" : "java");

        return new ProcessBuilder(
                javaExecutable.toString(),
                "-cp",
                System.getProperty("java.class.path"),
                processClass.getName(),
                cacheFolder.toAbsolutePath().toString())
                .start();
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

    private void stopProcess(Process process) throws InterruptedException {
        if (process == null || !process.isAlive()) {
            return;
        }

        process.destroyForcibly();
        process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }

    public static final class ResourceProcess {

        private ResourceProcess() {
        }

        public static void main(String[] args) throws Exception {
            var parser = CodeParser.newInstance();
            parser.set(CodeParser.DUMPER_FOLDER, new File(args[0]));
            var clangFiles = new ClangResources(parser).getClangFiles(LibcMode.SYSTEM);
            Files.writeString(Path.of(args[1]), clangFiles.clangExecutable().getAbsolutePath());
        }
    }

    public static final class MaintenanceLockHolderProcess {

        private MaintenanceLockHolderProcess() {
        }

        public static void main(String[] args) {
            var lockPath = Path.of(args[0], ".maintenance.lock");
            try {
                Files.createDirectories(lockPath.getParent());
                try (var channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                        var ignored = channel.lock()) {
                    System.out.println("READY");
                    System.out.flush();
                    System.in.read();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("DONE");
            System.out.flush();
        }
    }

    public static final class MaintenanceLockProbeProcess {

        private MaintenanceLockProbeProcess() {
        }

        public static void main(String[] args) {
            var lockPath = Path.of(args[0], ".maintenance.lock");
            try (var channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                var lock = channel.tryLock();
                if (lock != null) {
                    try (lock) {
                        System.out.println("ACQUIRED");
                        System.out.flush();
                    }
                    return;
                }

                System.out.println("BLOCKED");
                System.out.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                System.in.read();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            var cacheRoot = Path.of(args[0]);
            CacheFiles.touch(cacheRoot, cacheRoot);
            System.out.println("ENTERED");
            System.out.flush();
            System.out.println("DONE");
            System.out.flush();
        }
    }
}
