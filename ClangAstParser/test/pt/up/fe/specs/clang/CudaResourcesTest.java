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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pt.up.fe.specs.util.providers.FileResourceProvider;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CudaResourcesTest {

    private static final String PLATFORM = "linux-x86_64";
    private static final String RELEASE = "12.3.2";
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(10);

    @TempDir
    Path tempFolder;

    @Test
    public void manifestSelectsRequiredComponentsAndValidatesMetadata() {
        var manifest = CudaResources.parseManifest(manifestJson());

        assertEquals(RELEASE, ClangAstWebResource.getCudaReleaseTag());
        assertEquals(RELEASE, manifest.releaseLabel());
        assertEquals("cuda", manifest.releaseProduct());
        assertEquals(List.of("cuda_cudart", "cuda_nvcc", "libcurand", "cuda_cccl"),
                manifest.getRequiredPackages(PLATFORM).stream()
                        .map(CudaResources.CudaPackage::component)
                        .toList());
        assertEquals(CudaResources.NVIDIA_REDIST_ROOT + "cuda_cudart/linux-x86_64/cuda_cudart.tar.xz",
                CudaResources.getArchiveResource(manifest.getRequiredPackages(PLATFORM).get(0)).getUrlString());
        assertEquals("redistrib_12.3.2.json", CudaResources.getManifestFilename(RELEASE));

        assertThrows(RuntimeException.class, () -> CudaResources.parseManifest(""));
        assertThrows(RuntimeException.class,
                () -> CudaResources.parseManifest(manifestJson().replace(SHA256, "not-a-sha")));
        assertThrows(RuntimeException.class,
                () -> CudaResources.parseManifest(manifestJson().replace(
                        "cuda_cudart/linux-x86_64/cuda_cudart.tar.xz", "../cuda_cudart.tar.xz")));
        assertThrows(RuntimeException.class,
                () -> CudaResources.parseManifest(manifestJson().replace("\"size\": \"1\"", "\"size\": \"-1\"")));

        var wrongRelease = CudaResources.parseManifest(manifestJson()
                .replace("\"release_label\": \"12.3.2\"", "\"release_label\": \"12.3.1\""));
        var releaseError = assertThrows(RuntimeException.class, () -> CudaResources.install(
                tempFolder, RELEASE, manifestResource(manifestJson(wrongRelease, wrongRelease.releaseLabel())), ignored -> {
                    throw new AssertionError("Archive downloads must not start for an invalid manifest");
                }));
        assertTrue(releaseError.getMessage().contains("release label"));

        var wrongProduct = CudaResources.parseManifest(manifestJson()
                .replace("\"release_product\": \"cuda\"", "\"release_product\": \"other\""));
        var productError = assertThrows(RuntimeException.class, () -> CudaResources.install(
                tempFolder, RELEASE, manifestResource(manifestJson(wrongProduct, RELEASE)), ignored -> {
                    throw new AssertionError("Archive downloads must not start for an invalid manifest");
                }));
        assertTrue(productError.getMessage().contains("not a CUDA manifest"));

        var missingComponents = new LinkedHashMap<>(manifest.components());
        missingComponents.remove("cuda_cccl");
        var missingComponent = new CudaResources.NvidiaCudaManifest(
                manifest.releaseDate(), manifest.releaseLabel(), manifest.releaseProduct(), missingComponents);
        assertThrows(RuntimeException.class, () -> missingComponent.getRequiredPackages(PLATFORM));
    }

    @Test
    public void manifestAcceptsHostWhenAllRequiredComponentsExposeACompatiblePlatform() {
        var manifest = CudaResources.parseManifest(manifestJson());

        assertEquals(PLATFORM,
                CudaResources.getManifestPlatform(manifest, SupportedPlatform.LINUX, "amd64"));
    }

    @Test
    public void manifestRejectsHostWhenARequiredComponentLacksThePlatform() {
        var manifest = CudaResources.parseManifest(manifestJson());
        var missingPlatformComponent = manifest.components().get("cuda_cccl");
        var archives = new LinkedHashMap<>(missingPlatformComponent.archives());
        archives.remove(PLATFORM);
        var components = new LinkedHashMap<>(manifest.components());
        components.put("cuda_cccl", new CudaResources.NvidiaCudaComponent(
                missingPlatformComponent.name(), missingPlatformComponent.version(), archives));
        var incompleteManifest = new CudaResources.NvidiaCudaManifest(manifest.releaseDate(), manifest.releaseLabel(),
                manifest.releaseProduct(), components);

        var error = assertThrows(RuntimeException.class,
                () -> CudaResources.getManifestPlatform(incompleteManifest, SupportedPlatform.LINUX, "amd64"));
        assertTrue(error.getMessage().contains("linux (amd64)"));
        assertTrue(error.getMessage().contains(PLATFORM));
        assertTrue(error.getMessage().contains("cuda_cccl"));
    }

    @Test
    public void supportDetectionReturnsFalseOnlyForAValidatedUnsupportedHost() throws IOException {
        var unsupportedPlatform = SupportedPlatform.getCurrentPlatform().isWindows()
                ? "linux-riscv64"
                : "windows-x86_64";
        var manifestResource = manifestResource(manifestJson(unsupportedPlatform));

        assertFalse(CudaResources.isSupportedPlatform(tempFolder, manifestResource));
        assertFalse(Files.exists(tempFolder.resolve("cuda").resolve(RELEASE)));
        assertNoCudaStaging();
    }

    @Test
    public void supportDetectionPropagatesManifestAndCacheFailures() throws IOException {
        assertThrows(RuntimeException.class,
                () -> CudaResources.isSupportedPlatform(tempFolder, manifestResource("not-json")));
        assertNoCudaStaging();

        var invalidManifestRoot = Files.createDirectory(tempFolder.resolve("invalid")).toAbsolutePath();
        assertThrows(RuntimeException.class, () -> CudaResources.isSupportedPlatform(invalidManifestRoot,
                manifestResource(manifestJson()
                        .replace("\"release_product\": \"cuda\"", "\"release_product\": \"other\""))));

        var missingComponentRoot = Files.createDirectory(tempFolder.resolve("missing")).toAbsolutePath();
        assertThrows(RuntimeException.class, () -> CudaResources.isSupportedPlatform(missingComponentRoot,
                manifestResource(manifestJson().replace("\"cuda_cccl\":", "\"missing\":"))));

        var cacheFile = tempFolder.resolve("cache-file");
        Files.writeString(cacheFile, "not-a-directory");
        assertThrows(RuntimeException.class, () -> CudaResources.isSupportedPlatform(cacheFile,
                manifestResource(manifestJson())));
    }

    @Test
    public void additionalCompatibleManifestPlatformNeedsNoJavaSupportWhitelist() {
        var additionalPlatform = "linux-riscv64";
        var manifest = CudaResources.parseManifest(manifestJson(additionalPlatform));

        assertEquals(additionalPlatform,
                CudaResources.getManifestPlatform(manifest, SupportedPlatform.LINUX, "riscv64"));
    }

    @Test
    public void installationFetchesOnlyTheSelectedPlatformArchives() throws IOException {
        var archives = createArchives();
        var manifest = addUnusedPlatforms(archives.manifest());
        var writes = new AtomicInteger();

        var installation = CudaResources.install(tempFolder, RELEASE, manifestResource(manifestJson(manifest, RELEASE)),
                cudaPackage -> {
                    assertTrue(cudaPackage.archive().relativePath().contains("/" + PLATFORM + "/"));
                    var source = archives.files().get(cudaPackage.component());
                    return copyingResource(source, source.getFileName().toString(), writes);
                });

        assertEquals(tempFolder.resolve("cuda").resolve(RELEASE).toFile().getAbsoluteFile(), installation.getAbsoluteFile());
        assertTrue(CudaResources.isCudaInstallation(installation, RELEASE, PLATFORM));
        assertEquals(CudaResources.REQUIRED_COMPONENTS.size(), writes.get());
        assertTrue(Files.isRegularFile(installation.toPath().resolve(CudaResources.getManifestFilename(RELEASE))));
        assertTrue(Files.isRegularFile(installation.toPath().resolve(CudaResources.PLATFORM_FILENAME)));
        assertTrue(Files.isDirectory(installation.toPath().resolve("bin")));
        assertTrue(Files.isDirectory(installation.toPath().resolve("include")));
        assertTrue(Files.isDirectory(installation.toPath().resolve("nvvm")));
        assertFalse(Files.exists(installation.toPath().resolve("archives")));
        assertFalse(Files.exists(installation.toPath().resolve("cudalib")));
        assertFalse(Files.exists(installation.toPath().resolve(PLATFORM)));
        assertNoCudaStaging();
    }

    @Test
    public void archiveDownloadsRequireBothExpectedSizeAndSha256() throws IOException {
        var source = Files.writeString(tempFolder.resolve("cuda_cudart.tar.xz"), "archive");
        var actualSize = Files.size(source);
        var actualSha = sha256(source);

        var wrongSize = manifestForSingleArchive(source, actualSha, actualSize + 1);
        var sizeError = assertThrows(RuntimeException.class,
                () -> install(wrongSize, RELEASE, source, new AtomicInteger()));
        assertTrue(sizeError.getMessage().contains("expected size"));
        assertFalse(Files.exists(tempFolder.resolve("cuda").resolve(RELEASE)));
        assertNoCudaStaging();

        var wrongSha = manifestForSingleArchive(source, "0".repeat(64), actualSize);
        var shaError = assertThrows(RuntimeException.class,
                () -> install(wrongSha, RELEASE, source, new AtomicInteger()));
        assertTrue(shaError.getMessage().contains("expected SHA-256"));
        assertFalse(Files.exists(tempFolder.resolve("cuda").resolve(RELEASE)));
        assertNoCudaStaging();
    }

    @Test
    public void failedInstallationRemovesStagingAndDoesNotPublish() throws Exception {
        var archives = createArchives();
        var error = assertThrows(RuntimeException.class, () -> CudaResources.install(
                tempFolder, RELEASE, manifestResource(manifestJson(archives.manifest(), RELEASE)), cudaPackage -> {
                    if (cudaPackage.component().equals("libcurand")) {
                        throw new RuntimeException("download failed");
                    }

                    var source = archives.files().get(cudaPackage.component());
                    return copyingResource(source, source.getFileName().toString(), new AtomicInteger());
                }));

        assertEquals("download failed", error.getMessage());
        assertFalse(Files.exists(tempFolder.resolve("cuda").resolve(RELEASE)));
        assertNoCudaStaging();
    }

    @Test
    public void assembleSupportsTarXzAndZipPackages() throws IOException {
        var archives = createArchives();
        var stagingFolder = Files.createDirectory(tempFolder.resolve("cuda-staging"));
        Files.writeString(stagingFolder.resolve(CudaResources.getManifestFilename(RELEASE)),
                manifestJson(archives.manifest(), RELEASE));

        CudaResources.assemble(stagingFolder.toFile(), new CudaResources.CudaPlatform(PLATFORM),
                downloadedPackages(archives));

        assertEquals(PLATFORM, Files.readString(stagingFolder.resolve(CudaResources.PLATFORM_FILENAME)));
        assertTrue(Files.isDirectory(stagingFolder.resolve("bin")));
        assertEquals("cuda.h", Files.readString(stagingFolder.resolve("include/cuda.h")));
        assertEquals("cuda_runtime.h", Files.readString(stagingFolder.resolve("include/cuda_runtime.h")));
        assertEquals("texture", Files.readString(stagingFolder.resolve("include/texture_fetch_functions.h")));
        assertEquals("curand", Files.readString(stagingFolder.resolve("include/curand_mtgp32_kernel.h")));
        assertEquals("target", Files.readString(stagingFolder.resolve("include/nv/target")));
        assertEquals("host_config", Files.readString(stagingFolder.resolve("include/crt/host_config.h")));
        assertEquals("libdevice", Files.readString(stagingFolder.resolve("nvvm/libdevice/libdevice.10.bc")));
        assertFalse(Files.exists(stagingFolder.resolve("bin/discarded")));
        assertFalse(Files.exists(stagingFolder.resolve("bin/discarded.exe")));
        assertTrue(CudaResources.isCudaInstallation(stagingFolder.toFile(), RELEASE, PLATFORM));
    }

    @Test
    public void extractionRejectsTraversalEntries() throws IOException {
        var traversalArchive = tempFolder.resolve("traversal.tar.xz");
        writeTarXz(traversalArchive, Map.of(
                "cuda_cudart/include/../escape.h", bytes("escape")));
        var traversalPackage = new CudaResources.DownloadedPackage(
                new CudaResources.CudaPackage("cuda_cudart",
                        new CudaResources.CudaArchive("cuda_cudart/linux-x86_64/cuda_cudart.tar.xz", SHA256, 1)),
                traversalArchive.toFile());

        assertThrows(RuntimeException.class, () -> CudaResources.assemble(
                Files.createDirectory(tempFolder.resolve("traversal-out")).toFile(),
                new CudaResources.CudaPlatform(PLATFORM), List.of(traversalPackage)));
        assertFalse(Files.exists(tempFolder.resolve("escape.h")));

        var driveArchive = tempFolder.resolve("drive.tar.xz");
        writeTarXz(driveArchive, Map.of("C:/escape.h", bytes("escape")));
        var drivePackage = new CudaResources.DownloadedPackage(
                new CudaResources.CudaPackage("cuda_cudart",
                        new CudaResources.CudaArchive("cuda_cudart/linux-x86_64/cuda_cudart.tar.xz", SHA256, 1)),
                driveArchive.toFile());
        assertThrows(RuntimeException.class, () -> CudaResources.assemble(
                Files.createDirectory(tempFolder.resolve("drive-out")).toFile(),
                new CudaResources.CudaPlatform(PLATFORM), List.of(drivePackage)));
    }

    @Test
    public void requiredComponentsAreStoredPerReleaseWithoutDeduplication() throws IOException {
        var archives = createArchives();
        var firstRelease = tempFolder.resolve("cuda").resolve(RELEASE);
        var secondRelease = tempFolder.resolve("cuda").resolve("13.3.1");

        var first = install(archives, RELEASE, new AtomicInteger());
        var second = install(archives, "13.3.1", new AtomicInteger());

        assertEquals(firstRelease.toFile().getAbsoluteFile(), first.getAbsoluteFile());
        assertEquals(secondRelease.toFile().getAbsoluteFile(), second.getAbsoluteFile());
        assertNotEquals(first.getAbsoluteFile(), second.getAbsoluteFile());
        assertTrue(CudaResources.isCudaInstallation(first, RELEASE, PLATFORM));
        assertTrue(CudaResources.isCudaInstallation(second, "13.3.1", PLATFORM));
        assertFalse(Files.exists(first.toPath().resolve("archives")));
        assertFalse(Files.exists(second.toPath().resolve("archives")));
    }

    @Test
    public void existingValidInstallationIsReusedAndUsageIsRefreshed() throws IOException {
        var releaseFolder = tempFolder.resolve("cuda").resolve(RELEASE);
        writeValidInstallation(releaseFolder, PLATFORM);
        var old = FileTime.from(Instant.now().minus(Duration.ofDays(61)));
        Files.setLastModifiedTime(releaseFolder, old);

        var result = CudaResources.getBuiltinCudaLib(tempFolder);

        assertEquals(releaseFolder.toFile().getAbsoluteFile(), result.getAbsoluteFile());
        assertTrue(Files.getLastModifiedTime(releaseFolder).toInstant()
                .isAfter(Instant.now().minus(Duration.ofDays(1))));
    }

    @Test
    public void invalidPublishedInstallationFailsWithoutRepair() throws IOException {
        var releaseFolder = tempFolder.resolve("cuda").resolve(RELEASE);
        Files.createDirectories(releaseFolder);
        Files.writeString(releaseFolder.resolve(CudaResources.getManifestFilename(RELEASE)), manifestJson());
        Files.writeString(releaseFolder.resolve("sentinel"), "do not repair");

        var error = assertThrows(RuntimeException.class, () -> CudaResources.getBuiltinCudaLib(tempFolder));

        assertTrue(error.getMessage().contains(releaseFolder.toAbsolutePath().toString()));
        assertTrue(error.getMessage().contains("delete this directory manually to regenerate"));
        assertEquals("do not repair", Files.readString(releaseFolder.resolve("sentinel")));
    }

    @Test
    public void abandonedStagingDirectoriesAreReclaimable() throws Exception {
        var cudaRoot = Files.createDirectories(tempFolder.resolve("cuda"));
        var stagingPath = Files.createDirectory(cudaRoot.resolve("." + RELEASE + ".tmp-123"));
        var lockPath = cudaRoot.resolve("." + RELEASE + ".tmp-123.lock");
        Files.writeString(stagingPath.resolve("partial"), "in progress");
        Files.createFile(lockPath);

        CacheFiles.cleanupDirectories(tempFolder, cudaRoot, Instant.EPOCH, null);

        assertFalse(Files.exists(stagingPath));
        assertFalse(Files.exists(lockPath));
    }

    @Test
    public void concurrentPublicationLeavesOneValidInstallation() throws Exception {
        var archives = createArchives();
        var writes = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(4);
        var futures = new ArrayList<Future<File>>();

        try {
            for (int i = 0; i < 4; i++) {
                futures.add(executor.submit(() -> install(archives, RELEASE, writes)));
            }

            for (var future : futures) {
                assertEquals(tempFolder.resolve("cuda").resolve(RELEASE).toFile().getAbsoluteFile(),
                        future.get(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).getAbsoluteFile());
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }

        var releaseFolder = tempFolder.resolve("cuda").resolve(RELEASE);
        assertTrue(CudaResources.isCudaInstallation(releaseFolder.toFile(), RELEASE, PLATFORM));
        assertNoCudaStaging();
        assertTrue(writes.get() >= 4);
    }

    @Test
    public void staleCudaReleasesAreRemovedAfterSixtyDays() throws IOException {
        var currentFolder = tempFolder.resolve("cuda").resolve(RELEASE);
        writeValidInstallation(currentFolder, PLATFORM);

        var staleFolder = tempFolder.resolve("cuda").resolve("11.8.0");
        Files.createDirectories(staleFolder);
        Files.setLastModifiedTime(staleFolder,
                FileTime.from(Instant.now().minus(Duration.ofDays(61))));

        CudaResources.getBuiltinCudaLib(tempFolder);

        assertTrue(Files.isDirectory(currentFolder));
        assertFalse(Files.exists(staleFolder));
    }

    private File install(CudaResources.NvidiaCudaManifest manifest, String releaseTag, Path source,
                         AtomicInteger writes) throws IOException {
        return CudaResources.install(tempFolder, releaseTag, manifestResource(manifestJson(manifest, releaseTag)),
                cudaPackage -> copyingResource(source, source.getFileName().toString(), writes));
    }

    private File install(ArchiveSet archives, String releaseTag, AtomicInteger writes) throws IOException {
        return CudaResources.install(tempFolder, releaseTag, manifestResource(manifestJson(archives.manifest(), releaseTag)),
                cudaPackage -> {
                    var source = archives.files().get(cudaPackage.component());
                    return copyingResource(source, source.getFileName().toString(), writes);
                });
    }

    private ArchiveSet createArchives() throws IOException {
        var files = new LinkedHashMap<String, Path>();
        var cudart = tempFolder.resolve("cuda_cudart.tar.xz");
        writeTarXz(cudart, Map.of(
                "cuda_cudart/include/cuda.h", bytes("cuda.h"),
                "cuda_cudart/include/cuda_runtime.h", bytes("cuda_runtime.h"),
                "cuda_cudart/include/texture_fetch_functions.h", bytes("texture"),
                "cuda_cudart/bin/discarded", bytes("discarded")));
        files.put("cuda_cudart", cudart);

        var nvcc = tempFolder.resolve("cuda_nvcc.zip");
        writeZip(nvcc, Map.of(
                "cuda_nvcc/include/crt/host_config.h", bytes("host_config"),
                "cuda_nvcc/nvvm/libdevice/libdevice.10.bc", bytes("libdevice"),
                "cuda_nvcc/bin/discarded.exe", bytes("discarded")));
        files.put("cuda_nvcc", nvcc);

        var curand = tempFolder.resolve("libcurand.tar.xz");
        writeTarXz(curand, Map.of(
                "libcurand/include/curand_mtgp32_kernel.h", bytes("curand")));
        files.put("libcurand", curand);

        var cccl = tempFolder.resolve("cuda_cccl.zip");
        writeZip(cccl, Map.of("cuda_cccl/include/nv/target", bytes("target")));
        files.put("cuda_cccl", cccl);

        var components = new LinkedHashMap<String, CudaResources.NvidiaCudaComponent>();
        for (var component : CudaResources.REQUIRED_COMPONENTS) {
            var archive = files.get(component);
            var archivePath = component + "/linux-x86_64/" + archive.getFileName();
            var cudaArchive = new CudaResources.CudaArchive(archivePath, sha256(archive), Files.size(archive));
            components.put(component, new CudaResources.NvidiaCudaComponent(component, RELEASE,
                    Map.of(PLATFORM, cudaArchive)));
        }

        return new ArchiveSet(new CudaResources.NvidiaCudaManifest("2024-01-02", RELEASE, "cuda", components), files);
    }

    private List<CudaResources.DownloadedPackage> downloadedPackages(ArchiveSet archives) {
        return archives.manifest().getRequiredPackages(PLATFORM).stream()
                .map(cudaPackage -> new CudaResources.DownloadedPackage(cudaPackage,
                        archives.files().get(cudaPackage.component()).toFile()))
                .toList();
    }

    private CudaResources.NvidiaCudaManifest manifestForSingleArchive(Path source, String sha256, long size) {
        var components = new LinkedHashMap<String, CudaResources.NvidiaCudaComponent>();
        for (var component : CudaResources.REQUIRED_COMPONENTS) {
            var archiveName = component.equals("cuda_cudart") ? "cuda_cudart.tar.xz" : source.getFileName().toString();
            var relativePath = component + "/linux-x86_64/" + archiveName;
            components.put(component, new CudaResources.NvidiaCudaComponent(component, RELEASE,
                    Map.of(PLATFORM, new CudaResources.CudaArchive(relativePath, sha256, size))));
        }

        return new CudaResources.NvidiaCudaManifest("2024-01-02", RELEASE, "cuda", components);
    }

    private CudaResources.NvidiaCudaManifest addUnusedPlatforms(CudaResources.NvidiaCudaManifest manifest) {
        var components = new LinkedHashMap<String, CudaResources.NvidiaCudaComponent>();
        for (var entry : manifest.components().entrySet()) {
            var selectedArchive = entry.getValue().archives().get(PLATFORM);
            var archives = new LinkedHashMap<>(entry.getValue().archives());
            for (var unusedPlatform : List.of("linux-riscv64", "windows-x86_64")) {
                archives.put(unusedPlatform, new CudaResources.CudaArchive(
                        entry.getKey() + "/" + unusedPlatform + "/unused.tar.xz",
                        selectedArchive.sha256(), selectedArchive.size()));
            }
            components.put(entry.getKey(), new CudaResources.NvidiaCudaComponent(
                    entry.getValue().name(), entry.getValue().version(), archives));
        }

        return new CudaResources.NvidiaCudaManifest(manifest.releaseDate(), manifest.releaseLabel(),
                manifest.releaseProduct(), components);
    }

    private void writeValidInstallation(Path installation, String platform) throws IOException {
        Files.createDirectories(installation.resolve("bin"));
        Files.writeString(installation.resolve(CudaResources.getManifestFilename(RELEASE)), manifestJson());
        Files.writeString(installation.resolve(CudaResources.PLATFORM_FILENAME), platform);
        for (var requiredFile : List.of(
                "include/cuda.h",
                "include/cuda_runtime.h",
                "include/texture_fetch_functions.h",
                "include/curand_mtgp32_kernel.h",
                "include/nv/target",
                "include/crt/host_config.h",
                "nvvm/libdevice/libdevice.10.bc")) {
            var file = installation.resolve(requiredFile);
            Files.createDirectories(file.getParent());
            Files.writeString(file, requiredFile);
        }
    }

    private FileResourceProvider manifestResource(String json) {
        try {
            var source = Files.createTempFile(tempFolder, "manifest-", ".json");
            Files.writeString(source, json);
            return copyingResource(source, source.getFileName().toString(), new AtomicInteger());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertNoCudaStaging() throws IOException {
        var cudaRoot = tempFolder.resolve("cuda");
        if (!Files.isDirectory(cudaRoot)) {
            return;
        }

        try (var children = Files.list(cudaRoot)) {
            assertTrue(children.noneMatch(path -> path.getFileName().toString().contains(".tmp-")));
        }
    }

    private FileResourceProvider copyingResource(Path source, String filename, AtomicInteger writes) {
        return new FileResourceProvider() {
            @Override
            public File write(java.io.File folder) {
                writes.incrementAndGet();
                try {
                    var destination = folder.toPath().resolve(filename);
                    Files.copy(source, destination);
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
                return filename;
            }
        };
    }

    private static void writeTarXz(Path archive, Map<String, byte[]> files) throws IOException {
        try (OutputStream output = Files.newOutputStream(archive);
                var xzOutput = new XZCompressorOutputStream(output);
                var tarOutput = new TarArchiveOutputStream(xzOutput)) {
            tarOutput.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            for (var file : files.entrySet()) {
                var entry = new TarArchiveEntry(file.getKey());
                entry.setSize(file.getValue().length);
                tarOutput.putArchiveEntry(entry);
                tarOutput.write(file.getValue());
                tarOutput.closeArchiveEntry();
            }
        }
    }

    private static void writeZip(Path archive, Map<String, byte[]> files) throws IOException {
        try (OutputStream output = Files.newOutputStream(archive);
                var zipOutput = new ZipArchiveOutputStream(output)) {
            for (var file : files.entrySet()) {
                var entry = new ZipArchiveEntry(file.getKey());
                zipOutput.putArchiveEntry(entry);
                zipOutput.write(file.getValue());
                zipOutput.closeArchiveEntry();
            }
        }
    }

    private static String manifestJson(CudaResources.NvidiaCudaManifest manifest, String releaseTag) {
        return """
                {
                  "release_date": "%s",
                  "release_label": "%s",
                  "release_product": "%s",
                  "cuda_cudart": %s,
                  "cuda_nvcc": %s,
                  "libcurand": %s,
                  "cuda_cccl": %s
                }
                """.formatted(
                manifest.releaseDate(), releaseTag, manifest.releaseProduct(),
                componentJson(manifest.components().get("cuda_cudart")),
                componentJson(manifest.components().get("cuda_nvcc")),
                componentJson(manifest.components().get("libcurand")),
                componentJson(manifest.components().get("cuda_cccl")));
    }

    private static String componentJson(CudaResources.NvidiaCudaComponent component) {
        var platforms = new ArrayList<String>();
        for (var entry : component.archives().entrySet()) {
            var archive = entry.getValue();
            platforms.add("\"%s\": {\"relative_path\": \"%s\", \"sha256\": \"%s\", \"size\": \"%d\"}"
                    .formatted(entry.getKey(), archive.relativePath(), archive.sha256(), archive.size()));
        }

        return "{\"name\": \"%s\", \"version\": \"%s\", %s}"
                .formatted(component.name(), component.version(), String.join(", ", platforms));
    }

    private static String manifestJson() {
        return manifestJson(PLATFORM);
    }

    private static String manifestJson(String platform) {
        return """
                {
                  "release_date": "2024-01-02",
                  "release_label": "12.3.2",
                  "release_product": "cuda",
                  "cuda_cudart": %s,
                  "cuda_nvcc": %s,
                  "libcurand": %s,
                  "cuda_cccl": %s
                }
                """.formatted(
                component("CUDA Runtime", platform, "cuda_cudart/" + platform + "/cuda_cudart.tar.xz"),
                component("CUDA NVCC", platform, "cuda_nvcc/" + platform + "/cuda_nvcc.tar.xz"),
                component("cuRAND", platform, "libcurand/" + platform + "/libcurand.tar.xz"),
                component("CCCL", platform, "cuda_cccl/" + platform + "/cuda_cccl.tar.xz"));
    }

    private static String component(String name, String platform, String relativePath) {
        return """
                {
                  "name": "%s",
                  "version": "12.3.101",
                  "%s": {
                    "relative_path": "%s",
                    "sha256": "%s",
                    "size": "1"
                  }
                }
                """.formatted(name, platform, relativePath, SHA256);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String sha256(Path file) throws IOException {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private static final String SHA256 = "0".repeat(64);

    private record ArchiveSet(CudaResources.NvidiaCudaManifest manifest, Map<String, Path> files) {
    }
}
