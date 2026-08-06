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
import pt.up.fe.specs.clang.codeparser.CodeParser;
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

    private static final String PLATFORM = CudaResources.LINUX_X86_64_PLATFORM;
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
                tempFolder, tempFolder.resolve("wrong-release").toFile(), RELEASE,
                new CudaResources.CudaPlatform(PLATFORM), wrongRelease, ignored -> {
                    throw new AssertionError("Archive downloads must not start for an invalid manifest");
                }));
        assertTrue(releaseError.getMessage().contains("release label"));

        var wrongProduct = CudaResources.parseManifest(manifestJson()
                .replace("\"release_product\": \"cuda\"", "\"release_product\": \"other\""));
        var productError = assertThrows(RuntimeException.class, () -> CudaResources.install(
                tempFolder, tempFolder.resolve("wrong-product").toFile(), RELEASE,
                new CudaResources.CudaPlatform(PLATFORM), wrongProduct, ignored -> {
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
    public void manifestPlatformMatchesNvidiaKeysAndRejectsUnavailablePlatforms() {
        assertEquals(CudaResources.LINUX_X86_64_PLATFORM,
                CudaResources.getManifestPlatform(SupportedPlatform.LINUX, "amd64"));
        assertEquals(CudaResources.LINUX_PPC64LE_PLATFORM,
                CudaResources.getManifestPlatform(SupportedPlatform.LINUX, "ppc64le"));
        assertEquals(CudaResources.LINUX_SBSA_PLATFORM,
                CudaResources.getManifestPlatform(SupportedPlatform.LINUX, "aarch64"));
        assertEquals(CudaResources.WINDOWS_X86_64_PLATFORM,
                CudaResources.getManifestPlatform(SupportedPlatform.WINDOWS, "x86_64"));

        assertThrows(RuntimeException.class,
                () -> CudaResources.getManifestPlatform(SupportedPlatform.MAC_OS, "aarch64"));
        assertThrows(RuntimeException.class,
                () -> CudaResources.getManifestPlatform(SupportedPlatform.WINDOWS, "aarch64"));
    }

    @Test
    public void installationFetchesOnlyTheSelectedPlatformArchives() throws IOException {
        var archives = createArchives();
        var manifest = addUnusedPlatforms(archives.manifest());
        var platform = new CudaResources.CudaPlatform(PLATFORM);
        var platformFolder = CudaResources.getPlatformFolder(tempFolder, RELEASE, platform);
        var writes = new AtomicInteger();

        var installation = CudaResources.install(tempFolder, platformFolder, RELEASE, platform, manifest,
                cudaPackage -> {
                    assertTrue(cudaPackage.archive().relativePath().contains("/" + PLATFORM + "/"));
                    var source = archives.files().get(cudaPackage.component());
                    return copyingResource(source, source.getFileName().toString(), writes);
                });

        assertTrue(CudaResources.isCudaInstallation(installation, PLATFORM));
        assertEquals(CudaResources.REQUIRED_COMPONENTS.size(), writes.get());
    }

    @Test
    public void archiveDownloadsRequireBothExpectedSizeAndSha256() throws IOException {
        var source = Files.writeString(tempFolder.resolve("cuda_cudart.tar.xz"), "archive");
        var actualSize = Files.size(source);
        var actualSha = sha256(source);
        var platformFolder = CudaResources.getPlatformFolder(tempFolder, RELEASE, new CudaResources.CudaPlatform(PLATFORM));

        var wrongSize = manifestForSingleArchive(source, actualSha, actualSize + 1);
        var sizeError = assertThrows(RuntimeException.class,
                () -> install(wrongSize, platformFolder, source, new AtomicInteger()));
        assertTrue(sizeError.getMessage().contains("expected size"));
        assertFalse(CudaResources.getArchiveFile(platformFolder,
                wrongSize.getRequiredPackages(PLATFORM).get(0)).isFile());

        var wrongSha = manifestForSingleArchive(source, "0".repeat(64), actualSize);
        var shaError = assertThrows(RuntimeException.class,
                () -> install(wrongSha, platformFolder, source, new AtomicInteger()));
        assertTrue(shaError.getMessage().contains("expected SHA-256"));
    }

    @Test
    public void assembleSupportsTarXzAndZipPackages() throws IOException {
        var archives = createArchives();
        var stagingFolder = Files.createDirectory(tempFolder.resolve("cudalib"));

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
        assertTrue(CudaResources.isCudaInstallation(stagingFolder.toFile(), PLATFORM));
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
        var firstRelease = CudaResources.getPlatformFolder(tempFolder, RELEASE,
                new CudaResources.CudaPlatform(PLATFORM));
        var secondRelease = CudaResources.getPlatformFolder(tempFolder, "13.3.1",
                new CudaResources.CudaPlatform(PLATFORM));

        var first = install(archives, firstRelease, new AtomicInteger());
        var second = install(archives, secondRelease, new AtomicInteger());

        assertTrue(CudaResources.isCudaInstallation(first, PLATFORM));
        assertTrue(CudaResources.isCudaInstallation(second, PLATFORM));
        assertNotEquals(CudaResources.getArchiveFile(firstRelease,
                        archives.manifest().getRequiredPackages(PLATFORM).get(0)).toPath(),
                CudaResources.getArchiveFile(secondRelease,
                        archives.manifest().getRequiredPackages(PLATFORM).get(0)).toPath());
    }

    @Test
    public void existingValidInstallationIsReusedAndUsageIsRefreshed() throws IOException {
        var platform = new CudaResources.CudaPlatform(hostPlatform());
        var platformFolder = CudaResources.getPlatformFolder(tempFolder, RELEASE, platform);
        var installation = CudaResources.getInstallationFolder(platformFolder);
        writeValidInstallation(installation.toPath(), PLATFORM);
        var old = FileTime.from(Instant.now().minus(Duration.ofDays(61)));
        Files.setLastModifiedTime(platformFolder.toPath().getParent(), old);

        var parser = CodeParser.newInstance();
        parser.set(CodeParser.DUMPER_FOLDER, tempFolder.toFile());
        var result = new ClangResources(parser).getBuiltinCudaLib();

        assertEquals(installation.getAbsoluteFile(), result.getAbsoluteFile());
        assertTrue(Files.getLastModifiedTime(platformFolder.toPath().getParent()).toInstant()
                .isAfter(Instant.now().minus(Duration.ofDays(1))));
    }

    @Test
    public void invalidPublishedInstallationFailsWithoutRepair() throws IOException {
        var platform = new CudaResources.CudaPlatform(hostPlatform());
        var platformFolder = CudaResources.getPlatformFolder(tempFolder, RELEASE, platform);
        var installation = CudaResources.getInstallationFolder(platformFolder);
        Files.createDirectories(installation.toPath());
        Files.writeString(installation.toPath().resolve("sentinel"), "do not repair");

        var parser = CodeParser.newInstance();
        parser.set(CodeParser.DUMPER_FOLDER, tempFolder.toFile());
        var error = assertThrows(RuntimeException.class, () -> new ClangResources(parser).getBuiltinCudaLib());

        assertTrue(error.getMessage().contains(installation.getAbsolutePath()));
        assertTrue(error.getMessage().contains("delete this directory manually to regenerate"));
        assertEquals("do not repair", Files.readString(installation.toPath().resolve("sentinel")));
    }

    @Test
    public void concurrentPublicationLeavesOneValidInstallation() throws Exception {
        var archives = createArchives();
        var platform = new CudaResources.CudaPlatform(PLATFORM);
        var platformFolder = CudaResources.getPlatformFolder(tempFolder, RELEASE, platform);
        var writes = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(4);
        var futures = new ArrayList<Future<File>>();

        try {
            for (int i = 0; i < 4; i++) {
                futures.add(executor.submit(() -> install(archives, platformFolder, writes)));
            }

            for (var future : futures) {
                assertEquals(CudaResources.getInstallationFolder(platformFolder).getAbsoluteFile(),
                        future.get(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).getAbsoluteFile());
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }

        assertTrue(CudaResources.isCudaInstallation(CudaResources.getInstallationFolder(platformFolder), PLATFORM));
        try (var children = Files.list(platformFolder.toPath())) {
            assertTrue(children.noneMatch(path -> path.getFileName().toString().startsWith(".cudalib.tmp-")));
        }
        try (var children = Files.list(platformFolder.toPath().resolve("archives/cuda_cudart"))) {
            assertTrue(children.noneMatch(path -> path.getFileName().toString().startsWith(".cuda_cudart")));
        }
        assertTrue(writes.get() >= 4);
    }

    @Test
    public void staleCudaReleasesAreRemovedAfterSixtyDays() throws IOException {
        var platform = new CudaResources.CudaPlatform(hostPlatform());
        var currentFolder = CudaResources.getPlatformFolder(tempFolder, RELEASE, platform);
        var currentInstallation = CudaResources.getInstallationFolder(currentFolder);
        writeValidInstallation(currentInstallation.toPath(), PLATFORM);

        var staleFolder = CudaResources.getPlatformFolder(tempFolder, "11.8.0", platform);
        Files.createDirectories(staleFolder.toPath());
        Files.setLastModifiedTime(staleFolder.toPath().getParent(),
                FileTime.from(Instant.now().minus(Duration.ofDays(61))));

        var parser = CodeParser.newInstance();
        parser.set(CodeParser.DUMPER_FOLDER, tempFolder.toFile());
        new ClangResources(parser).getBuiltinCudaLib();

        assertTrue(currentInstallation.isDirectory());
        assertFalse(staleFolder.getParentFile().exists());
    }

    private File install(CudaResources.NvidiaCudaManifest manifest, File platformFolder, Path source,
                         AtomicInteger writes) {
        return CudaResources.install(tempFolder, platformFolder, RELEASE,
                new CudaResources.CudaPlatform(PLATFORM), manifest,
                cudaPackage -> copyingResource(source,
                        source.getFileName().toString(), writes));
    }

    private File install(ArchiveSet archives, File platformFolder, AtomicInteger writes) {
        return CudaResources.install(tempFolder, platformFolder, RELEASE,
                new CudaResources.CudaPlatform(PLATFORM), archives.manifest(),
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
            for (var unusedPlatform : List.of(CudaResources.LINUX_PPC64LE_PLATFORM,
                    CudaResources.LINUX_SBSA_PLATFORM, CudaResources.WINDOWS_X86_64_PLATFORM)) {
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

    private static String hostPlatform() {
        return CudaResources.getCurrentPlatform().manifestName();
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

    private static String manifestJson() {
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
                component("CUDA Runtime", "cuda_cudart/linux-x86_64/cuda_cudart.tar.xz"),
                component("CUDA NVCC", "cuda_nvcc/linux-x86_64/cuda_nvcc.tar.xz"),
                component("cuRAND", "libcurand/linux-x86_64/libcurand.tar.xz"),
                component("CCCL", "cuda_cccl/linux-x86_64/cuda_cccl.tar.xz"));
    }

    private static String component(String name, String relativePath) {
        return """
                {
                  "name": "%s",
                  "version": "12.3.101",
                  "linux-x86_64": {
                    "relative_path": "%s",
                    "sha256": "%s",
                    "size": "1"
                  }
                }
                """.formatted(name, relativePath, SHA256);
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
