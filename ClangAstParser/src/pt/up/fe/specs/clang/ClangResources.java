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
import pt.up.fe.specs.clang.parsers.TopLevelNodesParser;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.system.ProcessOutputAsString;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClangResources {

    private static final Map<String, CachedClangFiles> CLANG_FILES_CACHE = new ConcurrentHashMap<>();
    private static final String CLANG_FOLDERNAME = "clang_ast_exe";
    private static final String RELEASES_FOLDERNAME = "releases";
    private static final String INCLUDES_FOLDERNAME = "includes";
    private static final Duration STALE_CACHE_MAX_AGE = Duration.ofDays(60);
    private static final Duration STALE_STAGING_MAX_AGE = Duration.ofHours(1);

    private static final Map<String, Boolean> HAS_LIBC = new ConcurrentHashMap<>();

    private final CodeParser options;

    public ClangResources(CodeParser options) {
        this.options = options;
    }

    public File getBuiltinCudaLib() {
        var cudaResourceFolder = SpecsIo.mkdir(options.get(CodeParser.DUMPER_FOLDER), "cuda");
        var cudaFolder = new File(cudaResourceFolder, "cudalib");
        var zipFile = CacheFiles.installFile(new File(cudaResourceFolder, ClangAstWebResource.CUDA_LIB_FILENAME),
                ClangAstWebResource.CUDA_LIB, null, "built-in CUDA archive");

        if (isCudaInstallation(cudaFolder)) {
            return cudaFolder;
        }

        if (cudaFolder.exists()) {
            CacheFiles.delete(cudaFolder.toPath());
        }

        var stagingFolder = CacheFiles.createStagingDirectory(cudaResourceFolder.toPath(), ".cudalib.tmp-");
        try {
            if (!SpecsIo.extractZip(zipFile, stagingFolder.toFile()) || !isCudaInstallation(stagingFolder.toFile())) {
                throw new RuntimeException("Built-in CUDA archive did not contain a valid CUDA installation");
            }

            return CacheFiles.publish(stagingFolder, cudaFolder.toPath()).toFile();
        } finally {
            CacheFiles.delete(stagingFolder);
        }
    }

    private static boolean isCudaInstallation(File folder) {
        return folder.isDirectory()
                && new File(folder, "include/cuda.h").isFile()
                && new File(folder, "include/cuda_runtime.h").isFile()
                && new File(folder, "nvvm/libdevice/libdevice.10.bc").isFile();
    }

    public ClangFiles getClangFiles(LibcMode libcMode) {

        var source = ClangAstWebResource.getDumperSource();

        if (source instanceof LocalBuild localBuild) {
            return new ClangFiles(getLocalExecutable(localBuild.folder()), List.of());
        }

        var useBuiltinCuda = options.get(CodeParser.CUDA_PATH).equalsIgnoreCase(CodeParser.getBuiltinOption());
        var resourceFolder = getClangResourceFolder();
        var key = libcMode.name() + "_" + useBuiltinCuda + "_" + source + "_"
                + resourceFolder.getAbsolutePath();
        var cached = CLANG_FILES_CACHE.get(key);
        if (isUsable(cached)) {
            touchUse(resourceFolder, cached.includesFolder());
            SpecsLogs.debug(() -> "Using cached version of Clang files: " + cached.files());
            return cached.files();
        }

        if (cached != null) {
            CLANG_FILES_CACHE.remove(key, cached);
        }

        var manifest = ClangAstWebResource.getManifest(resourceFolder);
        File clangExecutable = prepareResources(manifest, resourceFolder);
        var includes = prepareIncludes(manifest, clangExecutable, libcMode);

        if (useBuiltinCuda) {
            getBuiltinCudaLib();
        }

        touchUse(resourceFolder, includes.extractedFolder());
        updateLastUsedAndCleanupStaleVersions(resourceFolder, includes.extractedFolder());

        var newFiles = new CachedClangFiles(new ClangFiles(clangExecutable, includes.folders()),
                includes.extractedFolder());
        var existingFiles = CLANG_FILES_CACHE.putIfAbsent(key, newFiles);
        var selectedFiles = existingFiles == null ? newFiles : existingFiles;
        touchUse(resourceFolder, selectedFiles.includesFolder());
        SpecsLogs.debug(() -> "Using downloaded version of Clang files: " + selectedFiles.files());
        return selectedFiles.files();
    }

    private static boolean isUsable(CachedClangFiles cached) {
        return cached != null
                && cached.files().clangExecutable().isFile()
                && (cached.includesFolder() == null || isIncludesCacheValid(cached.includesFolder()));
    }

    private static void touchUse(File resourceFolder, File includesFolder) {
        CacheFiles.touch(resourceFolder.toPath());
        if (includesFolder != null) {
            CacheFiles.touch(includesFolder.toPath());
        }
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
        SupportedPlatform platform = SupportedPlatform.getCurrentPlatform();

        var executableKind = ClangAstDumper.usePlugin() ? "plugin" : "tool";
        var asset = getCurrentAsset(manifest, executableKind);
        File executable = CacheFiles.installFile(new File(resourceFolder, asset.filename()),
                ClangAstWebResource.getAssetResource(asset), asset.sha256(),
                "clang-dumper asset '" + asset.filename() + "'");

        if (platform.isWindows()) {
            unblockWindowsFile(executable);
        }

        if (platform.isLinux() || platform.isMacOs()) {
            SpecsSystem.runProcess(Arrays.asList("chmod", "+x", executable.getAbsolutePath()), false, true);
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
        var releaseFolder = SpecsIo.mkdir(getReleasesFolder(), ClangAstWebResource.getReleaseTag());
        CacheFiles.touch(releaseFolder.toPath());
        return releaseFolder;
    }

    public static File getDefaultTempFolder() {
        return SpecsIo.getTempFolder(CLANG_FOLDERNAME);
    }

    private File getReleasesFolder() {
        return SpecsIo.mkdir(options.get(CodeParser.DUMPER_FOLDER), RELEASES_FOLDERNAME);
    }

    private File getIncludesRoot() {
        return new File(options.get(CodeParser.DUMPER_FOLDER), INCLUDES_FOLDERNAME);
    }

    private File getIncludesFolder(String sha256) {
        return getSharedIncludesFolder(options.get(CodeParser.DUMPER_FOLDER), sha256);
    }

    static File getSharedIncludesFolder(File cacheFolder, String sha256) {
        return new File(new File(cacheFolder, INCLUDES_FOLDERNAME), sha256.toLowerCase(Locale.ROOT));
    }

    public static boolean useBuiltinLibc(File clangExecutable, LibcMode libcMode) {
        return switch (libcMode) {
            case AUTO -> !hasLibC(clangExecutable);
            case BUILTIN_AND_LIBC -> true;
            case SYSTEM -> false;
        };
    }

    private static boolean hasLibC(File clangExecutable) {
        var executableKey = SpecsIo.getCanonicalPath(clangExecutable);
        return HAS_LIBC.computeIfAbsent(executableKey, ignored -> detectLibC(clangExecutable));
    }

    private static boolean detectLibC(File clangExecutable) {
        File clangTest = SpecsIo.getTempFolder("clang_ast_test_" + UUID.randomUUID());

        try {
            var testFiles = List.of(
                    ClangAstResource.TEST_INCLUDES_C.write(clangTest),
                    ClangAstResource.TEST_INCLUDES_CPP.write(clangTest));

            boolean needsLib = false;
            for (var testFile : testFiles) {
                var output = runClangAstDumper(clangExecutable, testFile);

                if (output.getReturnValue() != 0) {
                    ClavaLog.info("Problems while running dumper to test if libc/libcxx is needed");
                    needsLib = true;
                    break;
                }

                if (testFile.getName().endsWith(".cpp")
                        && !output.getOutput().contains(TopLevelNodesParser.getTopLevelNodesHeader())) {
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

    private static ProcessOutputAsString runClangAstDumper(File clangExecutable, File testFile) {
        List<String> arguments = List.of(clangExecutable.getAbsolutePath(), testFile.getAbsolutePath(), "--");
        return SpecsSystem.runProcess(arguments, true, false);
    }

    private PreparedIncludes prepareIncludes(ClangDumperManifest manifest, File clangExecutable,
                                             LibcMode libcMode) {
        var useBuiltinLibc = useBuiltinLibc(clangExecutable, libcMode);
        var useBuiltinCuda = options.get(CodeParser.CUDA_PATH).equalsIgnoreCase(CodeParser.getBuiltinOption());

        if (!useBuiltinLibc && !useBuiltinCuda) {
            return new PreparedIncludes(List.of(), null);
        }

        var extractedFolder = prepareIncludesFolder(manifest);
        var includeFolders = getIncludeFolders(extractedFolder);
        SpecsLogs.debug(() -> "Includes folders: " + includeFolders);

        return new PreparedIncludes(includeFolders.stream().map(File::getAbsolutePath).toList(), extractedFolder);
    }

    private File prepareIncludesFolder(ClangDumperManifest manifest) {
        var includesAsset = getCurrentAsset(manifest, "includes");
        var extractedFolder = getIncludesFolder(includesAsset.sha256());

        if (isIncludesCacheValid(extractedFolder)) {
            return extractedFolder;
        }

        var includesRoot = getIncludesRoot();
        CacheFiles.deleteStaleStagingDirectories(includesRoot.toPath(),
                Instant.now().minus(STALE_STAGING_MAX_AGE));
        var stagingFolder = CacheFiles.createStagingDirectory(includesRoot.toPath(),
                "." + includesAsset.sha256() + ".tmp-");
        try {
            var downloadFolder = CacheFiles.createStagingDirectory(stagingFolder, ".download-");
            try {
                var archive = ClangAstWebResource.getAssetResource(includesAsset).write(downloadFolder.toFile());
                if (archive == null || !archive.isFile()) {
                    throw new RuntimeException("Could not download clang-dumper includes archive '"
                            + includesAsset.filename() + "'");
                }

                if (!CacheFiles.hasExpectedSha256(archive, includesAsset.sha256())) {
                    throw new RuntimeException("Downloaded clang-dumper asset '" + includesAsset.filename()
                            + "' does not match expected SHA-256 '" + includesAsset.sha256() + "'");
                }

                if (!SpecsIo.extractZip(archive, stagingFolder.toFile())) {
                    throw new RuntimeException("Could not extract clang-dumper includes archive '"
                            + includesAsset.filename() + "'");
                }
            } finally {
                CacheFiles.delete(downloadFolder);
            }

            getIncludeFolders(stagingFolder.toFile());
            if (isIncludesCacheValid(extractedFolder)) {
                CacheFiles.touch(extractedFolder.toPath());
                return extractedFolder;
            }

            if (extractedFolder.exists()) {
                CacheFiles.delete(extractedFolder.toPath());
            }

            var publishedFolder = CacheFiles.publish(stagingFolder, extractedFolder.toPath()).toFile();
            if (!isIncludesCacheValid(publishedFolder)) {
                throw new RuntimeException("Published clang-dumper includes are invalid: '" + publishedFolder + "'");
            }

            CacheFiles.touch(publishedFolder.toPath());
            return publishedFolder;
        } finally {
            CacheFiles.delete(stagingFolder);
        }
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

        var staleCleanup = new Thread(() -> deleteStaleVersions(now, resourceFolder, includesFolder),
                "clang-dumper-stale-cache-cleanup");
        staleCleanup.setDaemon(true);
        staleCleanup.start();
    }

    void deleteStaleVersions(Instant now, File currentVersionFolder) {
        deleteStaleVersions(now, currentVersionFolder, null);
    }

    private void deleteStaleVersions(Instant now, File currentVersionFolder, File currentIncludesFolder) {
        var cutoff = now.minus(STALE_CACHE_MAX_AGE);
        try {
            CacheFiles.deleteStaleDirectories(getReleasesFolder().toPath(), cutoff, currentVersionFolder.toPath());
            CacheFiles.deleteStaleDirectories(getIncludesRoot().toPath(), cutoff,
                    currentIncludesFolder == null ? null : currentIncludesFolder.toPath());
            CacheFiles.deleteStaleStagingDirectories(getReleasesFolder().toPath(),
                    now.minus(STALE_STAGING_MAX_AGE));
            CacheFiles.deleteStaleStagingDirectories(getIncludesRoot().toPath(),
                    now.minus(STALE_STAGING_MAX_AGE));
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

    private record CachedClangFiles(ClangFiles files, File includesFolder) {
    }
}
