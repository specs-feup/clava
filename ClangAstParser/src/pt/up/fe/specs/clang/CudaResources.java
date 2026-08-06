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
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Downloads the NVIDIA redistribution packages required by Clang and assembles them into the CUDA root expected by
 * the bundled dumper.
 *
 * <p>CUDA resources are release-addressed. Archives from different CUDA releases therefore never share a cache
 * destination, even when NVIDIA publishes identical bytes for both releases.</p>
 */
final class CudaResources {

    static final String NVIDIA_REDIST_ROOT = "https://developer.download.nvidia.com/compute/cuda/redist/";
    static final List<String> REQUIRED_COMPONENTS = List.of("cuda_cudart", "cuda_nvcc", "libcurand", "cuda_cccl");
    static final String PLATFORM_FILENAME = ".platform";

    private static final String CUDA_FOLDERNAME = "cuda";
    private static final String CUDA_LIB_FOLDERNAME = "cudalib";
    private static final String ARCHIVES_FOLDERNAME = "archives";
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
        claimReleaseInUse(cacheRoot, releaseFolder);
        var manifest = getManifest(cacheRoot, releaseFolder);
        var platform = requireSupportedPlatform(manifest);
        var platformFolder = getPlatformFolder(cacheRoot, releaseTag, platform);
        var installationFolder = getInstallationFolder(platformFolder);

        // A published installation is immutable. A malformed one is an operator error, not an invitation to repair it
        // in place, because doing so could race with a reader that already selected this release.
        if (Files.exists(installationFolder.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            return useExistingInstallation(cacheRoot, platformFolder, platform, installationFolder);
        }

        claimInUse(cacheRoot, platformFolder);
        return install(cacheRoot, platformFolder, releaseTag, platform, manifest, CudaResources::getArchiveResource);
    }

    private static void claimReleaseInUse(Path cacheRoot, File releaseFolder) {
        CacheFiles.withMaintenanceLock(cacheRoot, () -> {
            try {
                Files.createDirectories(releaseFolder.toPath());
            } catch (IOException e) {
                throw new UncheckedIOException("Could not create CUDA release folder '" + releaseFolder + "'", e);
            }

            CacheFiles.touch(releaseFolder.toPath());
        });
    }

    static void claimInUse(Path cacheRoot, File platformFolder) {
        CacheFiles.withMaintenanceLock(cacheRoot, () -> {
            var platformPath = platformFolder.toPath();
            var releasePath = platformPath.getParent();
            if (releasePath == null) {
                throw new RuntimeException("CUDA platform folder is not below a release folder: '"
                        + platformFolder + "'");
            }

            try {
                Files.createDirectories(platformPath);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not create CUDA platform folder '" + platformPath + "'", e);
            }

            CacheFiles.touch(releasePath);
            CacheFiles.touch(platformPath);
        });
    }

    static CudaPlatform requireSupportedPlatform() {
        return getCurrentPlatform();
    }

    static CudaPlatform requireSupportedPlatform(NvidiaCudaManifest manifest) {
        return getCurrentPlatform(manifest);
    }

    static boolean isSupportedPlatform() {
        return isSupportedPlatform(ClangResources.getDefaultTempFolder().toPath());
    }

    static boolean isSupportedPlatform(Path cacheRoot) {
        try {
            getCurrentPlatform(cacheRoot);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    static CudaPlatform getCurrentPlatform() {
        return getCurrentPlatform(ClangResources.getDefaultTempFolder().toPath());
    }

    static CudaPlatform getCurrentPlatform(Path cacheRoot) {
        var releaseTag = ClangAstWebResource.getCudaReleaseTag();
        var releaseFolder = getReleaseFolder(cacheRoot, releaseTag);
        claimReleaseInUse(cacheRoot, releaseFolder);
        return getCurrentPlatform(getManifest(cacheRoot, releaseFolder));
    }

    static CudaPlatform getCurrentPlatform(NvidiaCudaManifest manifest) {
        return new CudaPlatform(getManifestPlatform(manifest, SupportedPlatform.getCurrentPlatform(),
                System.getProperty("os.arch")));
    }

    static String getManifestPlatform(NvidiaCudaManifest manifest, SupportedPlatform platform, String architecture) {
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

        var selectedPlatform = commonPlatforms.stream()
                .filter(candidate -> isCompatiblePlatform(candidate, platform, architecture))
                .findFirst();
        if (missingComponents.isEmpty() && selectedPlatform.isPresent()) {
            return selectedPlatform.get();
        }

        var reason = missingComponents.isEmpty()
                ? "no platform key is present in all required components and is compatible with this host"
                : "the manifest is missing required components " + missingComponents;
        throw new RuntimeException("Built-in CUDA is unsupported for host '" + platform + " (" + architecture
                + ")': " + reason + ". Available manifest platform keys: "
                + getAvailablePlatformKeys(manifest));
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

    static File getPlatformFolder(Path cacheRoot, String releaseTag, CudaPlatform platform) {
        return getReleaseFolder(cacheRoot, releaseTag).toPath().resolve(platform.manifestName()).toFile();
    }

    static File getInstallationFolder(File platformFolder) {
        return new File(platformFolder, CUDA_LIB_FOLDERNAME);
    }

    static File getArchiveFile(File platformFolder, CudaPackage cudaPackage) {
        var relativePath = cudaPackage.archive().relativePath();
        var archiveName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
        return new File(new File(new File(platformFolder, ARCHIVES_FOLDERNAME), cudaPackage.component()), archiveName);
    }

    static NvidiaCudaManifest getManifest(File resourceFolder) {
        return getManifest(getCacheRoot(resourceFolder), resourceFolder);
    }

    static NvidiaCudaManifest getManifest(Path cacheRoot, File resourceFolder) {
        var releaseTag = ClangAstWebResource.getCudaReleaseTag();
        var manifestFilename = getManifestFilename(releaseTag);
        var resource = WebResourceProvider.newInstance(NVIDIA_REDIST_ROOT, manifestFilename, releaseTag);
        var manifestFile = CacheFiles.installFile(cacheRoot, new File(resourceFolder, manifestFilename), resource, null,
                "NVIDIA CUDA redistribution manifest");
        var manifest = parseManifest(SpecsIo.read(manifestFile));
        validateManifest(manifest, releaseTag);
        return manifest;
    }

    private static Path getCacheRoot(File resourceFolder) {
        var cacheRoot = resourceFolder.toPath();
        for (int i = 0; i < 3; i++) {
            cacheRoot = cacheRoot.getParent();
            if (cacheRoot == null) {
                throw new RuntimeException("CUDA resource folder is not below a cache root: '" + resourceFolder + "'");
            }
        }

        return cacheRoot;
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

    static File install(Path cacheRoot, File resourceFolder, String releaseTag, CudaPlatform platform,
                        NvidiaCudaManifest manifest,
                        Function<CudaPackage, FileResourceProvider> archiveResourceFactory) {
        validateManifest(manifest, releaseTag);
        Objects.requireNonNull(archiveResourceFactory, "archiveResourceFactory");

        var installationFolder = getInstallationFolder(resourceFolder);
        if (Files.exists(installationFolder.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            return useExistingInstallation(cacheRoot, resourceFolder, platform, installationFolder);
        }

        var downloadedPackages = manifest.getRequiredPackages(platform.manifestName()).stream()
                .map(cudaPackage -> downloadPackage(cacheRoot, resourceFolder, cudaPackage, archiveResourceFactory))
                .toList();

        CacheFiles.deleteUnlockedStagingLocks(cacheRoot, resourceFolder.toPath());
        var stagingDirectory = CacheFiles.createStagingDirectory(cacheRoot, resourceFolder.toPath(), ".cudalib.tmp-");
        try {
            try {
                assemble(stagingDirectory.path().toFile(), platform, downloadedPackages);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not assemble CUDA resources in '"
                        + stagingDirectory.path() + "'", e);
            }

            if (!isCudaInstallation(stagingDirectory.path().toFile(), platform.manifestName())) {
                throw new RuntimeException("Assembled CUDA resources failed structural validation in '"
                        + stagingDirectory.path() + "'");
            }

            var publishedFolder = CacheFiles.publish(stagingDirectory.path(), installationFolder.toPath()).toFile();
            return useExistingInstallation(cacheRoot, resourceFolder, platform, publishedFolder);
        } finally {
            try {
                CacheFiles.delete(stagingDirectory.path());
            } finally {
                stagingDirectory.close();
            }
        }
    }

    private static CudaResources.DownloadedPackage downloadPackage(Path cacheRoot, File resourceFolder,
                                                                     CudaPackage cudaPackage,
                                                                     Function<CudaPackage, FileResourceProvider> factory) {
        var destination = getArchiveFile(resourceFolder, cudaPackage);
        var archiveParent = destination.getParentFile().toPath();
        CacheFiles.deleteUnlockedStagingLocks(cacheRoot, archiveParent);
        var archive = CacheFiles.installFile(cacheRoot, destination, factory.apply(cudaPackage),
                cudaPackage.archive().sha256(), cudaPackage.archive().size(),
                "NVIDIA CUDA archive '" + destination.getName() + "'");
        return new DownloadedPackage(cudaPackage, archive);
    }

    private static File useExistingInstallation(Path cacheRoot, File resourceFolder, CudaPlatform platform,
                                                File installationFolder) {
        var validInstallation = CacheFiles.withMaintenanceLock(cacheRoot, () -> {
            if (!isCudaInstallation(installationFolder, platform.manifestName())) {
                throw invalidInstallation(installationFolder, platform.manifestName());
            }

            CacheFiles.touch(resourceFolder.toPath());
            CacheFiles.touch(resourceFolder.getParentFile().toPath());
            return installationFolder;
        });

        cleanup(cacheRoot, resourceFolder);
        SpecsLogs.debug(() -> "Using cached CUDA resources: " + validInstallation);
        return validInstallation;
    }

    private static void cleanup(Path cacheRoot, File resourceFolder) {
        var cudaRoot = cacheRoot.resolve(CUDA_FOLDERNAME);
        var releaseFolder = resourceFolder.toPath().getParent();
        var cutoff = Instant.now().minus(Duration.ofDays(60));
        try {
            CacheFiles.deleteStaleDirectories(cacheRoot, cudaRoot, cutoff, releaseFolder);
            CacheFiles.deleteUnlockedStagingLocks(cacheRoot, releaseFolder);
            CacheFiles.deleteUnlockedStagingLocks(cacheRoot, resourceFolder.toPath());
            for (var component : REQUIRED_COMPONENTS) {
                CacheFiles.deleteUnlockedStagingLocks(cacheRoot,
                        resourceFolder.toPath().resolve(ARCHIVES_FOLDERNAME).resolve(component));
            }
        } catch (RuntimeException e) {
            SpecsLogs.warn("Could not clean stale CUDA cache resources", e);
        }
    }

    static boolean isCudaInstallation(File folder, String platform) {
        Path root = folder.toPath().toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
                || !Files.isDirectory(root.resolve("bin"), LinkOption.NOFOLLOW_LINKS)) {
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
            return platform.equals(Files.readString(platformFile).trim());
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

    static void assemble(File stagingFolder, CudaPlatform platform, List<DownloadedPackage> packages) throws IOException {
        Files.writeString(new File(stagingFolder, PLATFORM_FILENAME).toPath(), platform.manifestName());
        Files.createDirectories(new File(stagingFolder, "bin").toPath());

        for (var downloadedPackage : packages) {
            var component = downloadedPackage.cudaPackage().component();
            var sourceRoots = switch (component) {
                case "cuda_cudart", "libcurand", "cuda_cccl" -> List.of("include");
                case "cuda_nvcc" -> List.of("include/crt", "nvvm/libdevice/libdevice.10.bc");
                default -> throw new RuntimeException("Unsupported NVIDIA CUDA component '" + component + "'");
            };

            extractArchive(downloadedPackage.archiveFile(), stagingFolder, sourceRoots);
        }
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
