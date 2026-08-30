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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.providers.FileResourceProvider;
import pt.up.fe.specs.util.providers.WebResourceProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Downloads the NVIDIA redistribution packages required by Clang and assembles them into the CUDA root expected by
 * the bundled dumper.
 *
 * <p>CUDA resources are release-addressed. The published release directory is the complete CUDA installation;
 * manifests and downloaded archives used to build it live in staging until that directory is published.</p>
 */
final class CudaResources {

    static final String NVIDIA_REDIST_ROOT = "https://developer.download.nvidia.com/compute/cuda/redist/";
    static final List<String> REQUIRED_COMPONENTS = List.of("cuda_cudart", "cuda_nvcc", "libcurand", "cuda_cccl");
    static final String PLATFORM_FILENAME = ".platform";

    private static final String CUDA_FOLDERNAME = "cuda";
    private static final String MANIFEST_FILENAME_PREFIX = "redistrib_";
    private static final String MANIFEST_FILENAME_SUFFIX = ".json";
    private static final Set<String> MANIFEST_FIELDS = Set.of("release_date", "release_label", "release_product");
    private static final Set<String> COMPONENT_FIELDS = Set.of("name", "license", "license_path", "version");
    private static final Pattern SHA256_PATTERN = Pattern.compile("[0-9a-fA-F]{64}");
    private static final List<String> REQUIRED_FILES = List.of(
            "include/cuda.h",
            "include/cuda_runtime.h",
            "include/texture_fetch_functions.h",
            "include/curand_mtgp32_kernel.h",
            "include/nv/target",
            "include/crt/host_config.h",
            "nvvm/libdevice/libdevice.10.bc");

    private CudaResources() {
    }

    static File getBuiltinCudaLib(Path cacheRoot) {
        var releaseTag = ClangAstWebResource.getCudaReleaseTag();
        var releaseFolder = getReleaseFolder(cacheRoot, releaseTag);

        // A published installation is immutable. A malformed one is an operator error, not an invitation to repair it
        // in place, because doing so could race with a reader that already selected this release.
        var existing = useExistingInstallation(cacheRoot, releaseFolder, releaseTag);
        if (existing != null) {
            return existing;
        }

        return install(cacheRoot, releaseTag, getManifestResource(releaseTag), CudaResources::getArchiveResource);
    }

    static CudaPlatform requireSupportedPlatform() {
        return getCurrentPlatform();
    }

    static CudaPlatform requireSupportedPlatform(NvidiaCudaManifest manifest) {
        var platform = SupportedPlatform.getCurrentPlatform();
        var architecture = System.getProperty("os.arch");
        return findSupportedPlatform(manifest)
                .orElseThrow(() -> unsupportedPlatform(manifest, platform, architecture));
    }

    static boolean isSupportedPlatform() {
        return isSupportedPlatform(ClangResources.getDefaultTempFolder().toPath(),
                getManifestResource(ClangAstWebResource.getCudaReleaseTag()));
    }

    static boolean isSupportedPlatform(Path cacheRoot) {
        return isSupportedPlatform(cacheRoot, getManifestResource(ClangAstWebResource.getCudaReleaseTag()));
    }

    static boolean isSupportedPlatform(Path cacheRoot, FileResourceProvider manifestResource) {
        return findSupportedPlatform(getCurrentManifest(cacheRoot, manifestResource)).isPresent();
    }

    static CudaPlatform getCurrentPlatform() {
        return getCurrentPlatform(ClangResources.getDefaultTempFolder().toPath());
    }

    static CudaPlatform getCurrentPlatform(Path cacheRoot) {
        return requireSupportedPlatform(getCurrentManifest(cacheRoot));
    }

    static CudaPlatform getCurrentPlatform(NvidiaCudaManifest manifest) {
        return requireSupportedPlatform(manifest);
    }

    static String getManifestPlatform(NvidiaCudaManifest manifest, SupportedPlatform platform, String architecture) {
        return findManifestPlatform(manifest, platform, architecture)
                .orElseThrow(() -> unsupportedPlatform(manifest, platform, architecture));
    }

    private static Optional<CudaPlatform> findSupportedPlatform(NvidiaCudaManifest manifest) {
        return findManifestPlatform(manifest, SupportedPlatform.getCurrentPlatform(),
                System.getProperty("os.arch")).map(CudaPlatform::new);
    }

    private static Optional<String> findManifestPlatform(NvidiaCudaManifest manifest, SupportedPlatform platform,
                                                         String architecture) {
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(architecture, "architecture");

        var commonPlatforms = new LinkedHashSet<String>();
        var missingComponents = new ArrayList<String>();
        var firstComponent = true;
        for (var componentName : REQUIRED_COMPONENTS) {
            var component = manifest.components().get(componentName);
            if (component == null) {
                missingComponents.add(componentName);
                continue;
            }

            if (firstComponent) {
                commonPlatforms.addAll(component.archives().keySet());
                firstComponent = false;
            } else {
                commonPlatforms.retainAll(component.archives().keySet());
            }
        }

        if (!missingComponents.isEmpty()) {
            throw new RuntimeException("NVIDIA CUDA manifest is missing required components " + missingComponents
                    + ". Available manifest platform keys: " + getAvailablePlatformKeys(manifest));
        }

        return commonPlatforms.stream()
                .filter(candidate -> isCompatiblePlatform(candidate, platform, architecture))
                .findFirst();
    }

    private static RuntimeException unsupportedPlatform(NvidiaCudaManifest manifest, SupportedPlatform platform,
                                                        String architecture) {
        return new RuntimeException("Built-in CUDA is unsupported for host '" + platform + " (" + architecture
                + ")': no platform key is present in all required components and is compatible with this host"
                + ". Available manifest platform keys: " + getAvailablePlatformKeys(manifest));
    }

    private static NvidiaCudaManifest getCurrentManifest(Path cacheRoot) {
        var releaseTag = ClangAstWebResource.getCudaReleaseTag();
        return getCurrentManifest(cacheRoot, releaseTag, getManifestResource(releaseTag));
    }

    private static NvidiaCudaManifest getCurrentManifest(Path cacheRoot, FileResourceProvider manifestResource) {
        var releaseTag = ClangAstWebResource.getCudaReleaseTag();
        return getCurrentManifest(cacheRoot, releaseTag, manifestResource);
    }

    private static NvidiaCudaManifest getCurrentManifest(Path cacheRoot, String releaseTag,
                                                          FileResourceProvider manifestResource) {
        var cudaRoot = cacheRoot.resolve(CUDA_FOLDERNAME);
        try (var stagingDirectory = CacheFiles.createStagingDirectory(cacheRoot, cudaRoot,
                "." + releaseTag + ".tmp-")) {
            return downloadManifest(cacheRoot, stagingDirectory.path(), releaseTag, manifestResource);
        }
    }

    private static boolean isCompatiblePlatform(String manifestPlatform, SupportedPlatform hostPlatform,
                                                String hostArchitecture) {
        var separator = manifestPlatform.indexOf('-');
        if (separator <= 0 || separator == manifestPlatform.length() - 1) {
            return false;
        }

        var manifestOs = normalizeOs(manifestPlatform.substring(0, separator));
        var manifestArchitecture = normalizeArchitecture(manifestPlatform.substring(separator + 1));
        return manifestOs.equals(normalizeOs(hostPlatform))
                && manifestArchitecture.equals(normalizeArchitecture(hostArchitecture));
    }

    private static String normalizeOs(SupportedPlatform platform) {
        return switch (platform) {
            case WINDOWS -> "windows";
            case LINUX -> "linux";
            case MAC_OS -> "macos";
        };
    }

    private static String normalizeOs(String os) {
        var normalized = os.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        return switch (normalized) {
            case "darwin", "mac" -> "macos";
            default -> normalized;
        };
    }

    private static String normalizeArchitecture(String architecture) {
        var normalized = architecture.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        return switch (normalized) {
            case "amd64", "x8664", "x64" -> "x8664";
            case "aarch64", "arm64", "armv8", "armv8l", "sbsa" -> "arm64";
            default -> normalized;
        };
    }

    private static Map<String, List<String>> getAvailablePlatformKeys(NvidiaCudaManifest manifest) {
        var available = new LinkedHashMap<String, List<String>>();
        for (var componentName : REQUIRED_COMPONENTS) {
            var component = manifest.components().get(componentName);
            var platforms = component == null ? List.<String>of() : component.archives().keySet().stream().sorted().toList();
            available.put(componentName, platforms);
        }

        return available;
    }

    private static File getReleaseFolder(Path cacheRoot, String releaseTag) {
        return cacheRoot.resolve(CUDA_FOLDERNAME).resolve(releaseTag).toFile();
    }

    private static FileResourceProvider getManifestResource(String releaseTag) {
        var manifestFilename = getManifestFilename(releaseTag);
        return WebResourceProvider.newInstance(NVIDIA_REDIST_ROOT, manifestFilename, releaseTag);
    }

    private static NvidiaCudaManifest downloadManifest(Path cacheRoot, Path stagingRoot, String releaseTag,
                                                        FileResourceProvider resource) {
        var manifestFilename = getManifestFilename(releaseTag);
        var manifestFile = CacheFiles.installFile(cacheRoot, stagingRoot.resolve(manifestFilename).toFile(), resource, null,
                "NVIDIA CUDA redistribution manifest");
        var manifest = parseManifest(SpecsIo.read(manifestFile));
        validateManifest(manifest, releaseTag);
        return manifest;
    }

    private static NvidiaCudaManifest readPublishedManifest(File releaseFolder, String releaseTag) {
        var manifestPath = releaseFolder.toPath().resolve(getManifestFilename(releaseTag));
        if (!Files.isRegularFile(manifestPath, LinkOption.NOFOLLOW_LINKS)) {
            throw invalidInstallation(releaseFolder, "published manifest");
        }

        try {
            var manifest = parseManifest(SpecsIo.read(manifestPath.toFile()));
            validateManifest(manifest, releaseTag);
            return manifest;
        } catch (RuntimeException e) {
            var invalid = invalidInstallation(releaseFolder, "published manifest");
            invalid.initCause(e);
            throw invalid;
        }
    }

    static String getManifestFilename(String releaseTag) {
        return MANIFEST_FILENAME_PREFIX + releaseTag + MANIFEST_FILENAME_SUFFIX;
    }

    static WebResourceProvider getArchiveResource(CudaPackage cudaPackage) {
        var archive = cudaPackage.archive();
        return WebResourceProvider.newInstance(NVIDIA_REDIST_ROOT, archive.relativePath(),
                "cuda-" + cudaPackage.component() + "-" + archive.sha256());
    }

    static NvidiaCudaManifest parseManifest(String json) {
        if (json == null || json.isBlank()) {
            throw new RuntimeException("NVIDIA CUDA redistribution manifest is empty");
        }

        final JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new RuntimeException("Could not parse NVIDIA CUDA redistribution manifest", e);
        }

        var releaseDate = getRequiredString(root, "release_date", "manifest");
        var releaseLabel = getRequiredString(root, "release_label", "manifest");
        var releaseProduct = getRequiredString(root, "release_product", "manifest");
        var components = new LinkedHashMap<String, NvidiaCudaComponent>();

        for (var entry : root.entrySet()) {
            if (MANIFEST_FIELDS.contains(entry.getKey())) {
                continue;
            }

            if (!entry.getValue().isJsonObject()) {
                throw new RuntimeException("NVIDIA CUDA manifest component '" + entry.getKey()
                        + "' is not an object");
            }

            components.put(entry.getKey(), parseComponent(entry.getKey(), entry.getValue().getAsJsonObject()));
        }

        if (components.isEmpty()) {
            throw new RuntimeException("NVIDIA CUDA redistribution manifest does not contain components");
        }

        return new NvidiaCudaManifest(releaseDate, releaseLabel, releaseProduct, components);
    }

    static File install(Path cacheRoot, String releaseTag, FileResourceProvider manifestResource,
                        Function<CudaPackage, FileResourceProvider> archiveResourceFactory) {
        Objects.requireNonNull(manifestResource, "manifestResource");
        Objects.requireNonNull(archiveResourceFactory, "archiveResourceFactory");

        var cudaRoot = cacheRoot.resolve(CUDA_FOLDERNAME);
        var releaseFolder = getReleaseFolder(cacheRoot, releaseTag);
        try (var stagingDirectory = CacheFiles.createStagingDirectory(cacheRoot, cudaRoot,
                "." + releaseTag + ".tmp-")) {
            var manifest = downloadManifest(cacheRoot, stagingDirectory.path(), releaseTag, manifestResource);
            var platform = requireSupportedPlatform(manifest);

            try {
                prepareInstallation(stagingDirectory.path(), platform);
                for (var cudaPackage : manifest.getRequiredPackages(platform.manifestName())) {
                    downloadAndAssemble(stagingDirectory.path(), cudaPackage, archiveResourceFactory);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Could not assemble CUDA resources in '"
                        + stagingDirectory.path() + "'", e);
            }

            if (!isCudaInstallation(stagingDirectory.path().toFile(), releaseTag, platform.manifestName())) {
                throw new RuntimeException("Assembled CUDA resources failed structural validation in '"
                        + stagingDirectory.path() + "'");
            }

            CacheFiles.publish(stagingDirectory.path(), releaseFolder.toPath());
            return useExistingInstallation(cacheRoot, releaseFolder, releaseTag);
        }
    }

    private static void downloadAndAssemble(Path stagingRoot, CudaPackage cudaPackage,
                                             Function<CudaPackage, FileResourceProvider> factory) throws IOException {
        var downloadFolder = CacheFiles.createTemporaryDirectory(stagingRoot, ".download-");
        try {
            var archive = factory.apply(cudaPackage).write(downloadFolder.toFile());
            var archiveName = getArchiveName(cudaPackage);
            if (archive == null || !archive.isFile()) {
                throw new RuntimeException("Could not download NVIDIA CUDA archive '" + archiveName + "'");
            }

            var expectedSize = cudaPackage.archive().size();
            if (Files.size(archive.toPath()) != expectedSize) {
                throw new RuntimeException("Downloaded NVIDIA CUDA archive '" + archiveName
                        + "' does not match expected size '" + expectedSize + "' (actual: "
                        + Files.size(archive.toPath()) + ")");
            }

            var expectedSha256 = cudaPackage.archive().sha256();
            if (!CacheFiles.hasExpectedSha256(archive, expectedSha256)) {
                throw new RuntimeException("Downloaded NVIDIA CUDA archive '" + archiveName
                        + "' does not match expected SHA-256 '" + expectedSha256 + "'");
            }

            assemblePackage(stagingRoot.toFile(), cudaPackage, archive);
        } finally {
            CacheFiles.delete(downloadFolder);
        }
    }

    private static String getArchiveName(CudaPackage cudaPackage) {
        var relativePath = cudaPackage.archive().relativePath();
        return relativePath.substring(relativePath.lastIndexOf('/') + 1);
    }

    private static File useExistingInstallation(Path cacheRoot, File releaseFolder, String releaseTag) {
        var validInstallation = CacheFiles.useDirectory(cacheRoot, releaseFolder.toPath(), path -> {
            var platform = requireSupportedPlatform(readPublishedManifest(releaseFolder, releaseTag));
            if (!isCudaInstallation(releaseFolder, releaseTag, platform.manifestName())) {
                throw invalidInstallation(releaseFolder, platform.manifestName());
            }

            return Optional.of(releaseFolder);
        }).orElse(null);

        if (validInstallation == null) {
            return null;
        }

        cleanup(cacheRoot, releaseFolder.toPath());
        SpecsLogs.debug(() -> "Using cached CUDA resources: " + validInstallation);
        return validInstallation;
    }

    private static void cleanup(Path cacheRoot, Path releaseFolder) {
        var cudaRoot = cacheRoot.resolve(CUDA_FOLDERNAME);
        var cutoff = Instant.now().minus(Duration.ofDays(60));
        try {
            CacheFiles.cleanupDirectories(cacheRoot, cudaRoot, cutoff, releaseFolder);
        } catch (RuntimeException e) {
            SpecsLogs.warn("Could not clean stale CUDA cache resources", e);
        }
    }

    static boolean isCudaInstallation(File folder, String releaseTag, String platform) {
        Path root = folder.toPath().toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
                || !Files.isDirectory(root.resolve("bin"), LinkOption.NOFOLLOW_LINKS)
                || !Files.isDirectory(root.resolve("include"), LinkOption.NOFOLLOW_LINKS)
                || !Files.isDirectory(root.resolve("nvvm"), LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }

        var manifestFilename = getManifestFilename(releaseTag);
        if (!Files.isRegularFile(root.resolve(manifestFilename), LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }

        for (var requiredFile : REQUIRED_FILES) {
            if (!Files.isRegularFile(root.resolve(requiredFile), LinkOption.NOFOLLOW_LINKS)) {
                return false;
            }
        }

        var platformFile = root.resolve(PLATFORM_FILENAME);
        if (!Files.isRegularFile(platformFile, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }

        try {
            if (!platform.equals(Files.readString(platformFile).trim())) {
                return false;
            }

            var expectedEntries = Set.of("bin", "include", "nvvm", PLATFORM_FILENAME, manifestFilename);
            try (var entries = Files.list(root)) {
                return entries.allMatch(entry -> expectedEntries.contains(entry.getFileName().toString()));
            }
        } catch (IOException e) {
            return false;
        }
    }

    private static RuntimeException invalidInstallation(File folder, String platform) {
        return new RuntimeException("Invalid published CUDA installation '" + folder.getAbsolutePath()
                + "' for platform '" + platform + "'; delete this directory manually to regenerate");
    }

    private static void validateManifest(NvidiaCudaManifest manifest, String releaseTag) {
        Objects.requireNonNull(manifest, "manifest");
        if (!releaseTag.equals(manifest.releaseLabel())) {
            throw new RuntimeException("NVIDIA CUDA manifest release label '" + manifest.releaseLabel()
                    + "' does not match the configured CUDA release tag '" + releaseTag + "'");
        }

        if (!"cuda".equals(manifest.releaseProduct())) {
            throw new RuntimeException("NVIDIA redistribution manifest is not a CUDA manifest: '"
                    + manifest.releaseProduct() + "'");
        }
    }

    private static NvidiaCudaComponent parseComponent(String componentName, JsonObject component) {
        var name = getRequiredString(component, "name", "component '" + componentName + "'");
        var version = getRequiredString(component, "version", "component '" + componentName + "'");
        var archives = new LinkedHashMap<String, CudaArchive>();

        for (var entry : component.entrySet()) {
            if (COMPONENT_FIELDS.contains(entry.getKey())) {
                continue;
            }

            if (!entry.getValue().isJsonObject()) {
                continue;
            }

            var platform = entry.getKey();
            var platformObject = entry.getValue().getAsJsonObject();
            var relativePath = getRequiredString(platformObject, "relative_path",
                    "component '" + componentName + "', platform '" + platform + "'");
            var sha256 = getRequiredString(platformObject, "sha256",
                    "component '" + componentName + "', platform '" + platform + "'");
            validateSha256(sha256, componentName, platform);
            var size = getRequiredLong(platformObject, "size",
                    "component '" + componentName + "', platform '" + platform + "'");
            validateRelativePath(relativePath, "component '" + componentName + "', platform '" + platform + "'");
            if (size < 0) {
                throw new RuntimeException("NVIDIA CUDA archive size must not be negative for component '"
                        + componentName + "', platform '" + platform + "'");
            }

            archives.put(platform, new CudaArchive(relativePath, sha256, size));
        }

        if (archives.isEmpty()) {
            throw new RuntimeException("NVIDIA CUDA component '" + componentName + "' has no platform archives");
        }

        return new NvidiaCudaComponent(name, version, archives);
    }

    private static String getRequiredString(JsonObject object, String field, String owner) {
        JsonElement value = object.get(field);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            throw new RuntimeException("NVIDIA CUDA " + owner + " is missing string field '" + field + "'");
        }

        var stringValue = value.getAsString().trim();
        if (stringValue.isEmpty()) {
            throw new RuntimeException("NVIDIA CUDA " + owner + " has an empty field '" + field + "'");
        }

        return stringValue;
    }

    private static long getRequiredLong(JsonObject object, String field, String owner) {
        JsonElement value = object.get(field);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            throw new RuntimeException("NVIDIA CUDA " + owner + " is missing numeric field '" + field + "'");
        }

        try {
            return value.getAsLong();
        } catch (RuntimeException e) {
            throw new RuntimeException("NVIDIA CUDA " + owner + " has an invalid numeric field '" + field + "'", e);
        }
    }

    private static void validateSha256(String sha256, String component, String platform) {
        if (!SHA256_PATTERN.matcher(sha256).matches()) {
            throw new RuntimeException("NVIDIA CUDA archive for component '" + component + "', platform '"
                    + platform + "' has an invalid SHA-256: '" + sha256 + "'");
        }
    }

    private static void validateRelativePath(String relativePath, String owner) {
        if (relativePath.startsWith("/") || relativePath.startsWith("\\") || relativePath.contains("\\")
                || hasWindowsDrivePrefix(relativePath)) {
            throw new RuntimeException("NVIDIA CUDA " + owner + " has an unsafe relative path: '" + relativePath + "'");
        }

        for (var segment : relativePath.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new RuntimeException("NVIDIA CUDA " + owner + " has an unsafe relative path: '" + relativePath + "'");
            }
        }
    }

    private static void prepareInstallation(Path stagingFolder, CudaPlatform platform) throws IOException {
        Files.writeString(stagingFolder.resolve(PLATFORM_FILENAME), platform.manifestName());
        Files.createDirectories(stagingFolder.resolve("bin"));
    }

    static void assemble(File stagingFolder, CudaPlatform platform, List<DownloadedPackage> packages) throws IOException {
        prepareInstallation(stagingFolder.toPath(), platform);

        for (var downloadedPackage : packages) {
            assemblePackage(stagingFolder, downloadedPackage.cudaPackage(), downloadedPackage.archiveFile());
        }
    }

    private static void assemblePackage(File stagingFolder, CudaPackage cudaPackage, File archive) throws IOException {
        var sourceRoots = switch (cudaPackage.component()) {
            case "cuda_cudart", "libcurand", "cuda_cccl" -> List.of("include");
            case "cuda_nvcc" -> List.of("include/crt", "nvvm/libdevice/libdevice.10.bc");
            default -> throw new RuntimeException("Unsupported NVIDIA CUDA component '"
                    + cudaPackage.component() + "'");
        };

        extractArchive(archive, stagingFolder, sourceRoots);
    }

    private static void extractArchive(File archive, File destination, List<String> sourceRoots) throws IOException {
        if (archive.getName().endsWith(".zip")) {
            try (InputStream input = Files.newInputStream(archive.toPath());
                    var archiveInput = new ZipArchiveInputStream(input)) {
                extractArchiveEntries(archiveInput, archive, destination, sourceRoots,
                        entry -> entry instanceof ZipArchiveEntry zipEntry && isRegularZipEntry(zipEntry));
            }
            return;
        }

        if (!archive.getName().endsWith(".tar.xz")) {
            throw new RuntimeException("Unsupported NVIDIA CUDA archive format: '" + archive + "'");
        }

        try (InputStream input = Files.newInputStream(archive.toPath());
                var xzInput = new XZCompressorInputStream(input);
                var tarInput = new TarArchiveInputStream(xzInput)) {
            extractArchiveEntries(tarInput, archive, destination, sourceRoots,
                    entry -> entry instanceof TarArchiveEntry tarEntry && tarEntry.isFile());
        }
    }

    private static void extractArchiveEntries(ArchiveInputStream<?> archiveInput, File archive, File destination,
                                              List<String> sourceRoots, ArchiveEntryPolicy entryPolicy) throws IOException {
        var foundRoots = new HashSet<String>();
        String archiveRoot = null;
        ArchiveEntry entry;
        while ((entry = archiveInput.getNextEntry()) != null) {
            var entryName = validateArchiveEntryName(entry.getName(), archive);
            var topLevel = getTopLevelPath(entryName);

            if (archiveRoot == null) {
                archiveRoot = topLevel;
            } else if (!archiveRoot.equals(topLevel)) {
                throw new RuntimeException("NVIDIA CUDA archive contains multiple top-level folders: '"
                        + archiveRoot + "' and '" + topLevel + "'");
            }

            var relativeName = entryName.length() == archiveRoot.length()
                    ? ""
                    : entryName.substring(archiveRoot.length() + 1);
            var sourceRoot = findSourceRoot(relativeName, sourceRoots);
            if (sourceRoot == null) {
                continue;
            }

            if (!entry.isDirectory() && !entryPolicy.isRegular(entry)) {
                throw new RuntimeException("NVIDIA CUDA archive contains a non-regular selected entry: '"
                        + entryName + "'");
            }

            if (entry.isDirectory()) {
                Files.createDirectories(destination.toPath().resolve(relativeName));
                continue;
            }

            foundRoots.add(sourceRoot);
            copyArchiveFile(archiveInput, destination.toPath().resolve(relativeName), entryName);
        }

        if (!foundRoots.containsAll(sourceRoots)) {
            var missingRoots = new ArrayList<>(sourceRoots);
            missingRoots.removeAll(foundRoots);
            throw new RuntimeException("NVIDIA CUDA archive '" + archive + "' is missing selected paths: " + missingRoots);
        }
    }

    private static boolean isRegularZipEntry(ZipArchiveEntry entry) {
        if (entry.isUnixSymlink()) {
            return false;
        }

        var unixMode = entry.getUnixMode();
        return unixMode == 0 || (unixMode & 0170000) == 0100000;
    }

    private static String validateArchiveEntryName(String entryName, File archive) {
        if (entryName == null || entryName.isBlank() || entryName.startsWith("/") || entryName.contains("\\")
                || hasWindowsDrivePrefix(entryName)) {
            throw new RuntimeException("NVIDIA CUDA archive '" + archive + "' contains an unsafe path: '" + entryName + "'");
        }

        var normalizedName = entryName.endsWith("/") ? entryName.substring(0, entryName.length() - 1) : entryName;
        if (normalizedName.isEmpty()) {
            throw new RuntimeException("NVIDIA CUDA archive '" + archive + "' contains an empty path");
        }

        for (var segment : normalizedName.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new RuntimeException("NVIDIA CUDA archive '" + archive + "' contains an unsafe path: '" + entryName + "'");
            }
        }

        return normalizedName;
    }

    private static boolean hasWindowsDrivePrefix(String path) {
        return path.length() >= 2 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':';
    }

    private static String getTopLevelPath(String entryName) {
        var separator = entryName.indexOf('/');
        return separator == -1 ? entryName : entryName.substring(0, separator);
    }

    private static String findSourceRoot(String relativeName, List<String> sourceRoots) {
        for (var sourceRoot : sourceRoots) {
            if (relativeName.equals(sourceRoot) || relativeName.startsWith(sourceRoot + "/")) {
                return sourceRoot;
            }
        }

        return null;
    }

    private static void copyArchiveFile(InputStream input, Path destination, String entryName) throws IOException {
        Files.createDirectories(destination.getParent());
        var temporaryFile = Files.createTempFile(destination.getParent(), ".cuda-entry-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporaryFile)) {
                input.transferTo(output);
            }

            if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS)
                        || Files.mismatch(destination, temporaryFile) != -1) {
                    throw new RuntimeException("NVIDIA CUDA archives contain conflicting files at '" + entryName + "'");
                }
                return;
            }

            try {
                Files.move(temporaryFile, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, destination);
            } catch (java.nio.file.FileAlreadyExistsException e) {
                if (!Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS)
                        || Files.mismatch(destination, temporaryFile) != -1) {
                    throw new RuntimeException("NVIDIA CUDA archives contain conflicting files at '" + entryName + "'");
                }
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    record NvidiaCudaManifest(String releaseDate, String releaseLabel, String releaseProduct,
                              Map<String, NvidiaCudaComponent> components) {

        NvidiaCudaManifest {
            components = Map.copyOf(components);
        }

        List<CudaPackage> getRequiredPackages(String platform) {
            return REQUIRED_COMPONENTS.stream()
                    .map(component -> new CudaPackage(component, getComponent(component).getArchive(platform)))
                    .toList();
        }

        private NvidiaCudaComponent getComponent(String component) {
            var value = components.get(component);
            if (value == null) {
                throw new RuntimeException("NVIDIA CUDA manifest is missing required component '" + component + "'");
            }

            return value;
        }
    }

    record NvidiaCudaComponent(String name, String version, Map<String, CudaArchive> archives) {

        NvidiaCudaComponent {
            archives = Map.copyOf(archives);
        }

        CudaArchive getArchive(String platform) {
            var archive = archives.get(platform);
            if (archive == null) {
                throw new RuntimeException("NVIDIA CUDA component '" + name + "' has no archive for platform '"
                        + platform + "'");
            }

            return archive;
        }
    }

    record CudaPackage(String component, CudaArchive archive) {
    }

    record CudaArchive(String relativePath, String sha256, long size) {
    }

    record DownloadedPackage(CudaPackage cudaPackage, File archiveFile) {
    }

    record CudaPlatform(String manifestName) {
    }

    @FunctionalInterface
    private interface ArchiveEntryPolicy {

        boolean isRegular(ArchiveEntry entry);
    }
}
