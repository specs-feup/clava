/**
 * Copyright 2018 SPeCS.
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

import pt.up.fe.specs.clang.ClangAstWebResource.ClangDumperManifest;
import pt.up.fe.specs.clang.ClangAstWebResource.ClangDumperManifestAsset;
import pt.up.fe.specs.clang.ClangAstWebResource.LocalBuild;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.dumper.ClangAstDumper;
import pt.up.fe.specs.clang.wire.ProtoAstReader;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.providers.FileResourceProvider;
import pt.up.fe.specs.util.system.ProcessOutputAsString;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class ClangResources {

    static final int MAX_METADATA_CACHE_ENTRIES = 128;

    static final BoundedMetadataCache<String, CachedClangFiles> CLANG_FILES_CACHE =
            new BoundedMetadataCache<>(MAX_METADATA_CACHE_ENTRIES);
    private static final String CLANG_FOLDERNAME = "clang_ast_exe";
    private static final String CLANG_CACHE_FOLDERNAME = "clang-dumper";
    private static final String RELEASES_FOLDERNAME = "releases";
    private static final String INCLUDES_FOLDERNAME = "includes";
    private static final Duration STALE_CACHE_MAX_AGE = Duration.ofDays(60);

    static final BoundedMetadataCache<String, Boolean> HAS_LIBC =
            new BoundedMetadataCache<>(MAX_METADATA_CACHE_ENTRIES);
    static final BoundedMetadataCache<String, Boolean> FRAMEWORK_SEARCH_ROOTS =
            new BoundedMetadataCache<>(MAX_METADATA_CACHE_ENTRIES);

    private final CodeParser options;

    public ClangResources(CodeParser options) {
        this.options = options;
    }

    public static boolean isBuiltinCudaSupported() {
        return CudaResources.isSupportedPlatform();
    }

    public File getBuiltinCudaLib() {
        return CudaResources.getBuiltinCudaLib(options.get(CodeParser.DUMPER_FOLDER).toPath());
    }

    public ClangFiles getClangFiles(LibcMode requestedLibcMode) {

        var source = ClangAstWebResource.getDumperSource();
        var useBuiltinCuda = options.get(CodeParser.CUDA_PATH).equalsIgnoreCase(CodeParser.getBuiltinOption());
        var forceSystemLibc = source instanceof LocalBuild || ClangAstDumper.usePlugin();

        if (source instanceof LocalBuild localBuild) {
            var manifest = ClangAstWebResource.getLocalManifest(localBuild.folder());
            var executableKind = ClangAstDumper.usePlugin() ? "plugin" : "tool";
            var localAsset = getCurrentAsset(manifest, executableKind);
            var clangExecutable = getLocalExecutable(localBuild.folder());
            if (!localAsset.filename().equals(clangExecutable.getName())
                    || !localAsset.sha256().equalsIgnoreCase(ExecutableDigest.sha256(clangExecutable))) {
                throw new RuntimeException("Local clang-dumper executable does not match its release manifest: '"
                        + clangExecutable + "'");
            }
            var libcMode = resolveLibcMode(clangExecutable, requestedLibcMode, forceSystemLibc);
            File systemResourceDir = null;
            if (libcMode == LibcMode.SYSTEM && useBuiltinCuda) {
                systemResourceDir = findSystemClangResourceDir(
                        getLocalLLVMMajor(getCurrentAsset(manifest, "tool")));
            }
            return new ClangFiles(clangExecutable, List.of(), systemResourceDir, libcMode);
        }

        var resourceFolder = getClangResourceFolder();
        var manifest = ClangAstWebResource.getManifest(resourceFolder);
        File clangExecutable = prepareResources(manifest, resourceFolder);
        var executableKind = ClangAstDumper.usePlugin() ? "plugin" : "tool";
        var llvmMajor = getLLVMMajor(getCurrentAsset(manifest, executableKind));
        var systemResourceDir = !forceSystemLibc && requestedLibcMode == LibcMode.BUILTIN_AND_LIBC
                ? null
                : tryFindSystemClangResourceDir(llvmMajor);
        var libcMode = requestedLibcMode == LibcMode.AUTO && !forceSystemLibc && systemResourceDir == null
                ? LibcMode.BUILTIN_AND_LIBC
                : resolveLibcMode(clangExecutable, requestedLibcMode, forceSystemLibc, systemResourceDir);

        if (libcMode == LibcMode.SYSTEM && useBuiltinCuda && systemResourceDir == null) {
            systemResourceDir = findSystemClangResourceDir(llvmMajor);
        }

        if (libcMode != LibcMode.SYSTEM) {
            systemResourceDir = null;
        }

        var systemResourceKey = systemResourceDir == null ? "no-system-resource-dir"
                : SpecsIo.getCanonicalPath(systemResourceDir);
        var key = libcMode.name() + "_" + useBuiltinCuda + "_" + source + "_"
                + resourceFolder.getAbsolutePath() + "_" + systemResourceKey;
        var cached = CLANG_FILES_CACHE.get(key);
        if (isUsable(cached)) {
            SpecsLogs.debug(() -> "Using cached version of Clang files: " + cached.files());
            return cached.files();
        }

        if (cached != null) {
            CLANG_FILES_CACHE.remove(key, cached);
        }

        var includes = prepareIncludes(manifest, libcMode);
        if (useBuiltinCuda) {
            getBuiltinCudaLib();
        }

        touchUse(resourceFolder, includes.extractedFolder());
        updateLastUsedAndCleanupStaleVersions(resourceFolder, includes.extractedFolder());

        var newFiles = new CachedClangFiles(new ClangFiles(clangExecutable, includes.folders(), systemResourceDir,
                libcMode),
                includes.extractedFolder());
        var existingFiles = CLANG_FILES_CACHE.putIfAbsent(key, newFiles);
        var selectedFiles = existingFiles == null ? newFiles : existingFiles;
        touchUse(resourceFolder, selectedFiles.includesFolder());
        SpecsLogs.debug(() -> "Using downloaded version of Clang files: " + selectedFiles.files());
        return selectedFiles.files();
    }

    static LibcMode resolveLibcMode(File clangExecutable, LibcMode requestedLibcMode, boolean forceSystem) {
        return resolveLibcMode(clangExecutable, requestedLibcMode, forceSystem, null);
    }

    static LibcMode resolveLibcMode(File clangExecutable, LibcMode requestedLibcMode, boolean forceSystem,
            File systemResourceDir) {
        Objects.requireNonNull(clangExecutable, "clangExecutable");
        Objects.requireNonNull(requestedLibcMode, "requestedLibcMode");

        if (forceSystem) {
            return LibcMode.SYSTEM;
        }

        return switch (requestedLibcMode) {
            case AUTO -> useBuiltinLibc(clangExecutable, requestedLibcMode, systemResourceDir)
                    ? LibcMode.BUILTIN_AND_LIBC
                    : LibcMode.SYSTEM;
            case BUILTIN_AND_LIBC, SYSTEM -> requestedLibcMode;
        };
    }

    private boolean isUsable(CachedClangFiles cached) {
        if (cached == null) {
            return false;
        }

        return CacheFiles.withMaintenanceLock(getClangCacheRoot().toPath(), () -> {
            if (!cached.files().clangExecutable().isFile()) {
                return false;
            }

            if (cached.files().systemResourceDir() != null
                    && !cached.files().systemResourceDir().isDirectory()) {
                return false;
            }

            var includesFolder = cached.includesFolder();
            if (includesFolder == null) {
                return true;
            }

            if (!includesFolder.exists()) {
                return false;
            }

            CacheFiles.touch(includesFolder.toPath());
            if (!isIncludesCacheValid(includesFolder)) {
                throw invalidIncludesCache(includesFolder, includesFolder.getName());
            }

            return true;
        });
    }

    private void touchUse(File resourceFolder, File includesFolder) {
        CacheFiles.withMaintenanceLock(getClangCacheRoot().toPath(), () -> {
            CacheFiles.touch(resourceFolder.toPath());
            if (includesFolder != null) {
                CacheFiles.touch(includesFolder.toPath());
            }
        });
    }

    static File getLocalExecutable(File buildFolder) {
        if (!buildFolder.isDirectory()) {
            throw new RuntimeException("Local clang-dumper build directory does not exist: '" + buildFolder + "'");
        }

        String filename;
        if (ClangAstDumper.usePlugin()) {
            filename = System.mapLibraryName("plugin");
        } else {
            filename = SupportedPlatform.getCurrentPlatform().isWindows() ? "tool.exe" : "tool";
        }

        var executable = new File(buildFolder, filename);
        if (!executable.isFile()) {
            throw new RuntimeException("Could not find local clang-dumper "
                    + (ClangAstDumper.usePlugin() ? "plugin" : "tool") + " '" + executable + "'");
        }

        SpecsLogs.info("Using local clang-dumper build: " + executable);
        return executable;
    }

    private File prepareResources(ClangDumperManifest manifest, File resourceFolder) {
        var executableKind = ClangAstDumper.usePlugin() ? "plugin" : "tool";
        var asset = getCurrentAsset(manifest, executableKind);
        return prepareExecutable(asset, resourceFolder, ClangAstWebResource.getAssetResource(asset));
    }

    File prepareExecutable(ClangDumperManifestAsset asset, File resourceFolder, FileResourceProvider assetResource) {
        SupportedPlatform platform = SupportedPlatform.getCurrentPlatform();

        File executable = CacheFiles.installFile(getClangCacheRoot().toPath(),
                new File(resourceFolder, asset.filename()), assetResource, asset.sha256(),
                "clang-dumper asset '" + asset.filename() + "'");

        if (!asset.sha256().equalsIgnoreCase(ExecutableDigest.sha256(executable))) {
            throw new RuntimeException("Cached clang-dumper executable does not match its release manifest: '"
                    + executable + "'; remove the corrupt asset and retry");
        }
        if (platform.isWindows()) {
            unblockWindowsFile(executable);
        }

        if ((platform.isLinux() || platform.isMacOs()) && !executable.canExecute()
                && !executable.setExecutable(true, false)) {
            throw new RuntimeException("Could not make cached clang-dumper executable runnable: '"
                    + executable + "'");
        }

        return executable;
    }

    private void unblockWindowsFile(File executable) {
        var command = List.of(SpecsSystem.getWindowsPowershell(), "-NoLogo", "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass",
                "-Command",
                "Unblock-File",
                "-Path",
                "\"" + executable.getAbsolutePath() + "\"",
                "-ErrorAction",
                "Stop"
        );

        var output = SpecsSystem.runProcess(command, true, true);
        if (output.getReturnValue() == 0) {
            SpecsLogs.info("Successfully unblocked dumper executable");
        } else {
            SpecsLogs.info("Could not unblock dumper executable");
        }
    }

    public File getClangResourceFolder() {
        var cacheFolder = getClangCacheRoot();
        return CacheFiles.withMaintenanceLock(cacheFolder.toPath(), () -> {
            var releaseFolder = SpecsIo.mkdir(getReleasesFolder(), ClangAstWebResource.getReleaseTag());
            CacheFiles.touch(releaseFolder.toPath());
            return releaseFolder;
        });
    }

    public static File getDefaultTempFolder() {
        return SpecsIo.getTempFolder(CLANG_FOLDERNAME);
    }

    private File getReleasesFolder() {
        return SpecsIo.mkdir(getClangCacheRoot(), RELEASES_FOLDERNAME);
    }

    private File getIncludesRoot() {
        return new File(getClangCacheRoot(), INCLUDES_FOLDERNAME);
    }

    private File getClangCacheRoot() {
        return new File(options.get(CodeParser.DUMPER_FOLDER), CLANG_CACHE_FOLDERNAME);
    }

    static File getSharedIncludesFolder(File cacheFolder, String sha256) {
        return new File(new File(cacheFolder, INCLUDES_FOLDERNAME), sha256.toLowerCase(Locale.ROOT));
    }

    public static boolean useBuiltinLibc(File clangExecutable, LibcMode libcMode) {
        return useBuiltinLibc(clangExecutable, libcMode, null);
    }

    public static boolean isFrameworkSearchRoot(String includeRoot) {
        var root = Path.of(includeRoot).toAbsolutePath().normalize();
        var key = root.toString();
        return FRAMEWORK_SEARCH_ROOTS.computeIfAbsent(key, ignored -> containsFrameworkBundle(root));
    }

    private static boolean containsFrameworkBundle(Path root) {
        if (!Files.isDirectory(root)) {
            return false;
        }

        try (var entries = Files.list(root)) {
            return entries.anyMatch(entry -> entry.getFileName().toString().endsWith(".framework")
                    && Files.isDirectory(entry.resolve("Headers")));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not inspect bundled include root '" + root + "'", e);
        }
    }

    private static boolean useBuiltinLibc(File clangExecutable, LibcMode libcMode, File systemResourceDir) {
        return switch (libcMode) {
            case AUTO -> !hasLibC(clangExecutable, systemResourceDir);
            case BUILTIN_AND_LIBC -> true;
            case SYSTEM -> false;
        };
    }

    private static boolean hasLibC(File clangExecutable, File systemResourceDir) {
        var executableKey = SpecsIo.getCanonicalPath(clangExecutable);
        var resourceDirKey = systemResourceDir == null ? "default-resource-dir"
                : SpecsIo.getCanonicalPath(systemResourceDir);
        var probeKey = executableKey + "|" + ExecutableDigest.sha256(clangExecutable)
                + "|" + resourceDirKey + "|" + ProtoAstReader.schemaHash() + "|protobuf-c-cpp-nonempty-v1";
        return HAS_LIBC.computeIfAbsent(probeKey, ignored -> detectLibC(clangExecutable, systemResourceDir));
    }

    private static boolean detectLibC(File clangExecutable, File systemResourceDir) {
        File clangTest = SpecsIo.getTempFolder("clang_ast_test_" + UUID.randomUUID());

        try {
            var testFiles = List.of(
                    ClangAstResource.TEST_INCLUDES_C.write(clangTest),
                    ClangAstResource.TEST_INCLUDES_CPP.write(clangTest));

            boolean needsLib = false;
            for (var testFile : testFiles) {
                var dumpFile = new File(clangTest, testFile.getName() + ".pb");
                var output = runClangAstDumper(clangExecutable, testFile, dumpFile, systemResourceDir);

                if (output.getReturnValue() != 0 || !dumpFile.isFile()) {
                    ClavaLog.debug("Could not produce a Protobuf dump while checking system libc/libcxx");
                    needsLib = true;
                    break;
                }

                if (!isValidProtobufDump(dumpFile)) {
                    ClavaLog.debug("Could not validate the Protobuf dump while checking system libc/libcxx");
                    needsLib = true;
                    break;
                }
            }

            if (needsLib) {
                ClavaLog.debug("Could not find system libc/libcxx");
            } else {
                ClavaLog.debug("Detected system's libc and libcxx");
            }

            return !needsLib;
        } finally {
            SpecsIo.deleteFolder(clangTest);
        }
    }

    static boolean isValidProtobufDump(File dumpFile) {
        try {
            var result = ProtoAstReader.read(dumpFile.toPath(), new ClavaContext(), null,
                    dumpFile.getAbsolutePath());
            return result.metrics().nodes() > 0;
        } catch (IOException | RuntimeException e) {
            ClavaLog.debug(() -> "Could not read libc probe dump '" + dumpFile + "': " + e.getMessage());
            return false;
        }
    }

    private static ProcessOutputAsString runClangAstDumper(File clangExecutable, File testFile, File dumpFile,
            File systemResourceDir) {
        var arguments = new ArrayList<String>();
        arguments.add(clangExecutable.getAbsolutePath());
        arguments.add(testFile.getAbsolutePath());
        arguments.add("-o");
        arguments.add(dumpFile.getAbsolutePath());
        arguments.add("--");
        if (systemResourceDir != null) {
            arguments.add("-resource-dir=" + systemResourceDir.getAbsolutePath());
        }
        return SpecsSystem.runProcess(arguments, true, false);
    }

    private PreparedIncludes prepareIncludes(ClangDumperManifest manifest, LibcMode libcMode) {
        if (libcMode == LibcMode.SYSTEM) {
            return new PreparedIncludes(List.of(), null);
        }

        var extractedFolder = prepareIncludesFolder(manifest);
        var includeFolders = getIncludeFolders(extractedFolder);
        SpecsLogs.debug(() -> "Includes folders: " + includeFolders);

        return new PreparedIncludes(includeFolders.stream().map(File::getAbsolutePath).toList(), extractedFolder);
    }

    static int getLocalLLVMMajor(ClangDumperManifestAsset localAsset) {
        return getLLVMMajor(localAsset);
    }

    private static int getLLVMMajor(ClangDumperManifestAsset asset) {
        var llvmMajor = asset.llvm_major();
        if (llvmMajor < 1) {
            throw new RuntimeException("Clang-dumper manifest asset must specify llvm_major "
                    + "to select matching system Clang resources");
        }

        return llvmMajor;
    }

    private File findSystemClangResourceDir(Integer llvmMajor) {
        var resourceDir = tryFindSystemClangResourceDir(llvmMajor);
        if (resourceDir != null) {
            return resourceDir;
        }

        var commandNames = getSystemClangCommandNames(llvmMajor);
        var expectedVersion = llvmMajor == null ? "the local clang-dumper build's version"
                : "LLVM " + llvmMajor;
        var installHint = llvmMajor == null ? "clang++" : "clang++-" + llvmMajor;
        throw new RuntimeException("Could not find a system Clang resource directory for SYSTEM mode with built-in CUDA"
                + " on host '" + SupportedPlatform.getCurrentPlatform() + "' (expected " + expectedVersion
                + "). Tried: " + commandNames
                + ". SYSTEM mode does not bundle Clang's CUDA wrapper headers, so a matching system Clang"
                + " installation is required: install '" + installHint + "' (e.g. 'apt install " + installHint
                + "' or 'brew install llvm'), or set the libc mode to 'builtin' to use the bundled includes"
                + " instead of the system libc");
    }

    private File tryFindSystemClangResourceDir(Integer llvmMajor) {
        if (llvmMajor == null || llvmMajor < 1) {
            return null;
        }

        var commandNames = getSystemClangCommandNames(llvmMajor);
        for (var commandName : commandNames) {
            final ProcessOutputAsString output;
            try {
                output = SpecsSystem.runProcess(List.of(commandName, "-print-resource-dir"), true, false);
            } catch (RuntimeException e) {
                continue;
            }

            if (output.getReturnValue() != 0 || output.getStdOut() == null) {
                continue;
            }

            var resourceDir = new File(output.getStdOut().trim());
            if (isSystemClangResourceDir(resourceDir, llvmMajor)) {
                SpecsLogs.debug(() -> "Using system Clang resource directory '"
                        + resourceDir.getAbsolutePath() + "'");
                return resourceDir;
            }
        }
        return null;
    }

    private static List<String> getSystemClangCommandNames(Integer llvmMajor) {
        var suffix = SupportedPlatform.getCurrentPlatform().isWindows() ? ".exe" : "";
        if (llvmMajor == null) {
            return List.of("clang++" + suffix);
        }

        return List.of("clang++-" + llvmMajor + suffix, "clang++" + suffix);
    }

    private static boolean isSystemClangResourceDir(File resourceDir, Integer llvmMajor) {
        if (!resourceDir.isDirectory() || !new File(resourceDir, "include").isDirectory()) {
            return false;
        }

        return llvmMajor == null || resourceDir.getName().equals(Integer.toString(llvmMajor));
    }

    private File prepareIncludesFolder(ClangDumperManifest manifest) {
        var includesAsset = getCurrentAsset(manifest, "includes");
        return resolveIncludes(getClangCacheRoot(), includesAsset,
                ClangAstWebResource.getAssetResource(includesAsset));
    }

    static File resolveIncludes(File cacheFolder, ClangDumperManifestAsset includesAsset,
                                FileResourceProvider archiveResource) {
        var extractedFolder = getSharedIncludesFolder(cacheFolder, includesAsset.sha256());
        var existingFolder = useExistingIncludes(cacheFolder, extractedFolder, includesAsset.sha256());
        if (existingFolder != null) {
            return existingFolder;
        }

        var includesRoot = extractedFolder.getParentFile().toPath();
        CacheFiles.deleteUnlockedStagingLocks(cacheFolder.toPath(), includesRoot);
        var stagingFolder = CacheFiles.createStagingDirectory(cacheFolder.toPath(), includesRoot,
                "." + includesAsset.sha256() + ".tmp-");
        try {
            var downloadFolder = CacheFiles.createTemporaryDirectory(stagingFolder.path(), ".download-");
            try {
                var archive = archiveResource.write(downloadFolder.toFile());
                if (archive == null || !archive.isFile()) {
                    throw new RuntimeException("Could not download clang-dumper includes archive '"
                            + includesAsset.filename() + "'");
                }

                if (!CacheFiles.hasExpectedSha256(archive, includesAsset.sha256())) {
                    throw new RuntimeException("Downloaded clang-dumper asset '" + includesAsset.filename()
                            + "' does not match expected SHA-256 '" + includesAsset.sha256() + "'");
                }

                if (!SpecsIo.extractZip(archive, stagingFolder.path().toFile())) {
                    throw new RuntimeException("Could not extract clang-dumper includes archive '"
                            + includesAsset.filename() + "'");
                }
            } finally {
                CacheFiles.delete(downloadFolder);
            }

            getIncludeFolders(stagingFolder.path().toFile());
            existingFolder = useExistingIncludes(cacheFolder, extractedFolder, includesAsset.sha256());
            if (existingFolder != null) {
                return existingFolder;
            }

            var publishedFolder = CacheFiles.publish(stagingFolder.path(), extractedFolder.toPath()).toFile();
            existingFolder = useExistingIncludes(cacheFolder, publishedFolder, includesAsset.sha256());
            if (existingFolder == null) {
                throw new RuntimeException("Published clang-dumper includes disappeared: '"
                        + publishedFolder.getAbsolutePath() + "'");
            }

            return existingFolder;
        } finally {
            try {
                CacheFiles.delete(stagingFolder.path());
            } finally {
                stagingFolder.close();
            }
        }
    }

    private static File useExistingIncludes(File cacheFolder, File includesFolder, String sha256) {
        if (!includesFolder.exists()) {
            return null;
        }

        return CacheFiles.withMaintenanceLock(cacheFolder.toPath(), () -> {
            if (!includesFolder.exists()) {
                return null;
            }

            CacheFiles.touch(includesFolder.toPath());
            if (!isIncludesCacheValid(includesFolder)) {
                throw invalidIncludesCache(includesFolder, sha256);
            }

            return includesFolder;
        });
    }

    private static RuntimeException invalidIncludesCache(File includesFolder, String sha256) {
        return new RuntimeException("Invalid clang-dumper includes cache directory '"
                + includesFolder.getAbsolutePath() + "' for SHA-256 '" + sha256
                + "'; delete this directory manually to regenerate");
    }

    static List<File> getIncludeFolders(File extractedFolder) {
        if (!extractedFolder.isDirectory()) {
            throw new RuntimeException("Could not find extracted clang-dumper includes folder '" + extractedFolder + "'");
        }

        var entrypointsFile = new File(extractedFolder, "entrypoints.txt");
        if (!entrypointsFile.isFile()) {
            throw new RuntimeException("Could not find include archive entrypoints file '" + entrypointsFile + "'");
        }

        Path root = extractedFolder.toPath().toAbsolutePath().normalize();
        var includeFolders = new ArrayList<File>();
        var entrypoints = SpecsIo.read(entrypointsFile).lines()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        for (String line : entrypoints) {
            Path includeFolder = root.resolve(line).normalize();
            if (!includeFolder.startsWith(root) || !Files.isDirectory(includeFolder)) {
                throw new RuntimeException("Include archive entrypoint is not a usable directory: '" + line + "'");
            }

            includeFolders.add(includeFolder.toFile());
        }

        return includeFolders;
    }

    private ClangDumperManifestAsset getCurrentAsset(ClangDumperManifest manifest, String kind) {
        var platform = getManifestPlatform();
        var arch = getManifestArch(platform);
        return manifest.getAsset(platform, arch, kind);
    }

    static boolean isIncludesCacheValid(File includesFolder) {
        try {
            getIncludeFolders(includesFolder);
            return true;
        } catch (RuntimeException e) {
            SpecsLogs.info("Cached clang-dumper includes are invalid: " + includesFolder);
            return false;
        }
    }

    private void updateLastUsedAndCleanupStaleVersions(File resourceFolder, File includesFolder) {
        var now = Instant.now();
        touchUse(resourceFolder, includesFolder);
        deleteStaleVersions(now, resourceFolder, includesFolder);
    }

    void deleteStaleVersions(Instant now, File currentVersionFolder) {
        deleteStaleVersions(now, currentVersionFolder, null);
    }

    private void deleteStaleVersions(Instant now, File currentVersionFolder, File currentIncludesFolder) {
        var cutoff = now.minus(STALE_CACHE_MAX_AGE);
        var cacheRoot = getClangCacheRoot().toPath();
        try {
            CacheFiles.deleteStaleDirectories(cacheRoot, getReleasesFolder().toPath(), cutoff,
                    currentVersionFolder.toPath());
            CacheFiles.deleteStaleDirectories(cacheRoot, getIncludesRoot().toPath(), cutoff,
                    currentIncludesFolder == null ? null : currentIncludesFolder.toPath());
            CacheFiles.deleteUnlockedStagingLocks(cacheRoot, currentVersionFolder.toPath());
            CacheFiles.deleteUnlockedStagingLocks(cacheRoot, getIncludesRoot().toPath());
        } catch (RuntimeException e) {
            SpecsLogs.warn("Could not clean stale clang-dumper cache resources", e);
        }
    }

    private static String getManifestPlatform() {
        var platform = SupportedPlatform.getCurrentPlatform();

        if (platform.isLinux()) {
            return "linux";
        }

        if (platform.isMacOs()) {
            return "macos";
        }

        if (platform.isWindows()) {
            return "windows";
        }

        throw new RuntimeException("Unsupported platform: " + platform);
    }

    private static String getManifestArch(String platform) {
        var osArch = System.getProperty("os.arch").toLowerCase();

        if (osArch.equals("amd64") || osArch.equals("x86_64")) {
            return platform.equals("windows") ? "x86_64" : "x64";
        }

        if (osArch.equals("aarch64") || osArch.equals("arm64")) {
            return "arm64";
        }

        throw new RuntimeException("Unsupported architecture for clang-dumper: " + osArch);
    }

    private record PreparedIncludes(List<String> folders, File extractedFolder) {
    }

    record CachedClangFiles(ClangFiles files, File includesFolder) {
    }

    /** A small LRU for path metadata; values must not retain parsed ASTs. */
    static final class BoundedMetadataCache<K, V> {

        private static final int LOCK_STRIPE_COUNT = 64;

        private final int maxEntries;
        private final LinkedHashMap<K, V> entries = new LinkedHashMap<>(16, 0.75f, true);
        private final Object[] computationLocks = new Object[LOCK_STRIPE_COUNT];

        BoundedMetadataCache(int maxEntries) {
            if (maxEntries < 1) {
                throw new IllegalArgumentException("Metadata cache limit must be positive");
            }

            this.maxEntries = maxEntries;
            Arrays.setAll(computationLocks, ignored -> new Object());
        }

        synchronized V get(K key) {
            return entries.get(key);
        }

        synchronized V putIfAbsent(K key, V value) {
            var existing = entries.get(key);
            if (existing != null || entries.containsKey(key)) {
                return existing;
            }

            entries.put(key, Objects.requireNonNull(value, "value"));
            evictEldest();
            return null;
        }

        synchronized boolean remove(K key, V value) {
            if (!Objects.equals(entries.get(key), value)) {
                return false;
            }

            entries.remove(key);
            return true;
        }

        V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            Objects.requireNonNull(mappingFunction, "mappingFunction");
            var lock = computationLocks[Math.floorMod(key.hashCode(), LOCK_STRIPE_COUNT)];
            synchronized (lock) {
                var existing = get(key);
                if (existing != null || containsKey(key)) {
                    return existing;
                }

                var computed = mappingFunction.apply(key);
                if (computed == null) {
                    return null;
                }

                var prior = putIfAbsent(key, computed);
                return prior == null ? computed : prior;
            }
        }

        synchronized int size() {
            return entries.size();
        }

        synchronized boolean containsKey(K key) {
            return entries.containsKey(key);
        }

        synchronized void clear() {
            entries.clear();
        }

        synchronized int maxEntries() {
            return maxEntries;
        }

        private void evictEldest() {
            if (entries.size() > maxEntries) {
                var eldest = entries.keySet().iterator();
                eldest.next();
                eldest.remove();
            }
        }
    }
}
