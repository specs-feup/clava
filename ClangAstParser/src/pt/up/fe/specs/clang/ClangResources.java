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
import pt.up.fe.specs.util.providers.FileResourceProvider.ResourceWriteData;
import pt.up.fe.specs.util.system.ProcessOutputAsString;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

public class ClangResources {

    private static final Map<String, ClangFiles> CLANG_FILES_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Object> CLANG_FILES_LOCKS = new ConcurrentHashMap<>();
    private final static String CLANG_FOLDERNAME = "clang_ast_exe";
    private final static String INCLUDES_FOLDERNAME = "includes";
    private final static String LAST_USED_FILENAME = "last-used.txt";
    private final static String CACHE_LOCK_FOLDERNAME = ".cache.lock";
    private final static String CACHE_LOCK_OWNER_PREFIX = "owner-";
    private final static Duration CACHE_LOCK_RETRY_INTERVAL = Duration.ofMillis(100);
    private final static Duration CACHE_LOCK_STALE_MAX_AGE = Duration.ofMinutes(5);
    private final static Duration STALE_CACHE_MAX_AGE = Duration.ofDays(60);

    private static final AtomicInteger HAS_LIBC = new AtomicInteger(-1);

    private final CodeParser options;

    public ClangResources(CodeParser options) {
        this.options = options;
    }

    public Optional<File> getSystemCudaInstallation() {
        return findCudaInstallation(
                Arrays.asList(System.getenv("CUDA_HOME"), System.getenv("CUDA_PATH"), System.getenv("CUDA_ROOT")),
                System.getenv("PATH"), getConventionalCudaRoots());
    }

    static Optional<File> findCudaInstallation(List<String> configuredRoots, String pathValue,
                                                List<File> conventionalRoots) {
        var candidates = new ArrayList<File>();

        configuredRoots.stream()
                .filter(root -> root != null && !root.isBlank())
                .map(File::new)
                .forEach(candidates::add);

        if (pathValue != null) {
            for (var pathEntry : pathValue.split(Pattern.quote(File.pathSeparator))) {
                if (pathEntry.isBlank()) {
                    continue;
                }

                var binFolder = new File(pathEntry);
                for (var executableName : List.of("nvcc", "nvcc.exe")) {
                    var executable = new File(binFolder, executableName);
                    if (executable.isFile() && binFolder.getParentFile() != null) {
                        candidates.add(binFolder.getParentFile());
                    }
                }
            }
        }

        candidates.addAll(conventionalRoots);

        return candidates.stream()
                .map(File::getAbsoluteFile)
                .filter(ClangResources::isCudaInstallation)
                .findFirst();
    }

    private static boolean isCudaInstallation(File folder) {
        return folder.isDirectory() && new File(folder, "include/cuda_runtime.h").isFile();
    }

    private static List<File> getConventionalCudaRoots() {
        if (!SupportedPlatform.getCurrentPlatform().isWindows()) {
            return List.of(
                    new File("/usr/local/cuda"),
                    new File("/opt/cuda"),
                    new File("/opt/homebrew/opt/cuda"));
        }

        var roots = new ArrayList<File>();
        for (var programFiles : Arrays.asList(System.getenv("ProgramFiles"), System.getenv("ProgramFiles(x86)"))) {
            if (programFiles == null || programFiles.isBlank()) {
                continue;
            }

            var cudaRoot = new File(programFiles, "NVIDIA GPU Computing Toolkit/CUDA");
            var versions = cudaRoot.listFiles(File::isDirectory);
            if (versions != null) {
                Arrays.sort(versions, Comparator.comparing(File::getName).reversed());
                roots.addAll(Arrays.asList(versions));
            }
        }

        return roots;
    }

    public ClangFiles getClangFiles(LibcMode libcMode) {

        var source = ClangAstWebResource.getDumperSource();

        if (source instanceof LocalBuild localBuild) {
            var key = source.toString();
            return CLANG_FILES_CACHE.computeIfAbsent(key,
                    ignored -> new ClangFiles(getLocalExecutable(localBuild.folder()), List.of()));
        }

        var useBuiltinCuda = options.get(CodeParser.CUDA_PATH).equalsIgnoreCase(CodeParser.getBuiltinOption());
        var resourceFolder = getClangResourceFolder();
        var key = libcMode.name() + "_" + useBuiltinCuda + "_" + source + "_"
                + resourceFolder.getAbsolutePath();
        var jvmLock = CLANG_FILES_LOCKS.computeIfAbsent(resourceFolder.getAbsolutePath(), ignored -> new Object());
        synchronized (jvmLock) {
            var lockFolder = getCacheLockFolder(resourceFolder);
            try (var ignored = acquireCacheLock(resourceFolder)) {
                var files = CLANG_FILES_CACHE.get(key);
                if (files != null) {
                    if (files.clangExecutable().isFile()) {
                        writeLastUsed(resourceFolder, Instant.now());
                        SpecsLogs.debug(() -> "Using cached version of Clang files: " + files);
                        return files;
                    }

                    CLANG_FILES_CACHE.remove(key, files);
                }

                var manifest = ClangAstWebResource.getManifest(resourceFolder);
                File clangExecutable = prepareResources(manifest, resourceFolder);
                List<String> builtinIncludes = prepareIncludes(manifest, resourceFolder, clangExecutable, libcMode);

                validateTopLevelCacheFiles(manifest, resourceFolder);
                updateLastUsedAndCleanupStaleVersions(resourceFolder);

                var newFiles = new ClangFiles(clangExecutable, builtinIncludes);
                SpecsLogs.debug(() -> "Using downloaded version of Clang files: " + newFiles);

                CLANG_FILES_CACHE.put(key, newFiles);
                return newFiles;
            } catch (IOException e) {
                throw new UncheckedIOException("Could not lock clang-dumper cache '" + lockFolder + "'", e);
            }
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
        ResourceWriteData executable = downloadAsset(manifest, executableKind, resourceFolder);

        if (platform.isWindows()) {
            unblockWindowsFile(executable.getFile());
        }

        if (executable.isNewFile() && (platform.isLinux() || platform.isMacOs())) {
            SpecsSystem.runProcess(Arrays.asList("chmod", "+x", executable.getFile().getAbsolutePath()), false, true);
        }

        return executable.getFile();
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
        return SpecsIo.mkdir(options.get(CodeParser.DUMPER_FOLDER), ClangAstWebResource.getReleaseTag());
    }

    public static File getDefaultTempFolder() {
        return SpecsIo.getTempFolder(CLANG_FOLDERNAME);
    }

    public static boolean useBuiltinLibc(File clangExecutable, LibcMode libcMode) {
        return switch (libcMode) {
            case AUTO -> !hasLibC(clangExecutable);
            case BUILTIN_AND_LIBC -> true;
            case SYSTEM -> false;
        };
    }

    private static boolean hasLibC(File clangExecutable) {
        var value = HAS_LIBC.get();

        if (value == -1) {
            var hasLibC = detectLibC(clangExecutable);
            value = hasLibC ? 1 : 0;
            HAS_LIBC.set(value);
        }

        if (value == 0) {
            return false;
        }

        if (value == 1) {
            return true;
        }

        throw new RuntimeException("Unexpected value: '" + value + "'");
    }

    private static boolean detectLibC(File clangExecutable) {
        File clangTest = SpecsIo.mkdir(SpecsIo.getTempFolder(), "clang_ast_test");

        List<File> testFiles = Arrays.asList(ClangAstResource.TEST_INCLUDES_C, ClangAstResource.TEST_INCLUDES_CPP)
                .stream()
                .map(resource -> resource.write(clangTest))
                .collect(Collectors.toList());

        boolean needsLib = false;
        for (File testFile : testFiles) {
            var output = runClangAstDumper(clangExecutable, testFile);

            if (output.getReturnValue() != 0) {
                ClavaLog.info("Problems while running dumper to test if libc/libcxx is needed");
                needsLib = true;
                break;
            }

            if (!testFile.getName().endsWith(".cpp")) {
                continue;
            }

            var topLevelNodesHeader = TopLevelNodesParser.getTopLevelNodesHeader();
            if (!output.getOutput().contains(topLevelNodesHeader)) {
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
    }

    private static ProcessOutputAsString runClangAstDumper(File clangExecutable, File testFile) {
        List<String> arguments = Arrays.asList(clangExecutable.getAbsolutePath(), testFile.getAbsolutePath(), "--");
        return SpecsSystem.runProcess(arguments, true, false);
    }

    private List<String> prepareIncludes(ClangDumperManifest manifest, File resourceFolder, File clangExecutable,
                                         LibcMode libcMode) {
        var useBuiltinLibc = useBuiltinLibc(clangExecutable, libcMode);
        var useBuiltinCuda = options.get(CodeParser.CUDA_PATH).equalsIgnoreCase(CodeParser.getBuiltinOption());

        if (!useBuiltinLibc && !useBuiltinCuda) {
            return List.of();
        }

        return prepareIncludes(manifest, resourceFolder);
    }

    private List<String> prepareIncludes(ClangDumperManifest manifest, File resourceFolder) {
        var extractedFolder = prepareIncludesFolder(manifest, resourceFolder);
        var includeFolders = getIncludeFolders(extractedFolder);
        SpecsLogs.debug(() -> "Includes folders: " + includeFolders);

        return includeFolders.stream().map(File::getAbsolutePath).toList();
    }

    private File prepareIncludesFolder(ClangDumperManifest manifest, File resourceFolder) {
        var includesAsset = getCurrentAsset(manifest, "includes");
        var extractedFolder = new File(resourceFolder, INCLUDES_FOLDERNAME);

        if (isIncludesCacheValid(extractedFolder)) {
            return extractedFolder;
        }

        ResourceWriteData zipFile = downloadAsset(includesAsset, resourceFolder);

        try {
            SpecsIo.mkdir(extractedFolder);
            SpecsIo.deleteFolderContents(extractedFolder);
            SpecsIo.extractZip(zipFile.getFile(), extractedFolder);
        } finally {
            SpecsIo.delete(zipFile.getFile());
        }

        return extractedFolder;
    }

    private List<File> getIncludeFolders(File extractedFolder) {
        var entrypointsFile = new File(extractedFolder, "entrypoints.txt");
        if (!entrypointsFile.isFile()) {
            throw new RuntimeException("Could not find include archive entrypoints file '" + entrypointsFile + "'");
        }

        return SpecsIo.read(entrypointsFile).lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> new File(extractedFolder, line))
                .toList();
    }

    private ResourceWriteData downloadAsset(ClangDumperManifest manifest, String kind, File resourceFolder) {
        var asset = getCurrentAsset(manifest, kind);
        return downloadAsset(asset, resourceFolder);
    }

    private ResourceWriteData downloadAsset(ClangDumperManifestAsset asset, File resourceFolder) {
        var resource = ClangAstWebResource.getAssetResource(asset);
        var writeData = resource.writeVersioned(resourceFolder, ClangResources.class);

        if (!writeData.isNewFile()) {
            return writeData;
        }

        if (!hasExpectedSha256(writeData.getFile(), asset)) {
            SpecsLogs.info("Downloaded clang-dumper asset '" + asset.filename()
                    + "' does not match the expected checksum, downloading it again.");
            SpecsIo.delete(writeData.getFile());
            writeData = resource.writeVersioned(resourceFolder, ClangResources.class);
        }

        if (!hasExpectedSha256(writeData.getFile(), asset)) {
            throw new RuntimeException("Downloaded clang-dumper asset '" + asset.filename()
                    + "' does not match expected SHA-256 '" + asset.sha256() + "'");
        }

        return writeData;
    }

    private ClangDumperManifestAsset getCurrentAsset(ClangDumperManifest manifest, String kind) {
        var platform = getManifestPlatform();
        var arch = getManifestArch(platform);
        return manifest.getAsset(platform, arch, kind);
    }

    static boolean isIncludesCacheValid(File includesFolder) {

        if (!includesFolder.isDirectory()) {
            return false;
        }

        var entrypointsFile = new File(includesFolder, "entrypoints.txt");
        if (!entrypointsFile.isFile()) {
            SpecsLogs.info("Cached clang-dumper includes are missing entrypoints, extracting them again.");
            return false;
        }

        return true;
    }

    private void validateTopLevelCacheFiles(ClangDumperManifest manifest, File resourceFolder) {
        Set<String> expectedNames = new HashSet<>();
        expectedNames.add(ClangAstWebResource.MANIFEST_FILENAME);
        var executableKind = ClangAstDumper.usePlugin() ? "plugin" : "tool";
        expectedNames.add(getCurrentAsset(manifest, executableKind).filename());
        expectedNames.add(INCLUDES_FOLDERNAME);
        expectedNames.add(LAST_USED_FILENAME);

        var files = resourceFolder.listFiles();
        if (files == null) {
            return;
        }

        for (var file : files) {
            if (expectedNames.contains(file.getName())) {
                continue;
            }

            SpecsLogs.info("Deleting unexpected file from clang-dumper cache: " + file);
            SpecsIo.delete(file);
        }
    }

    private void updateLastUsedAndCleanupStaleVersions(File resourceFolder) {
        var now = Instant.now();
        writeLastUsed(resourceFolder, now);

        var staleCleanup = new Thread(() -> deleteStaleVersions(now, resourceFolder),
                "clang-dumper-stale-cache-cleanup");
        staleCleanup.setDaemon(true);
        staleCleanup.start();
    }

    private static void writeLastUsed(File resourceFolder, Instant timestamp) {
        writeTimestamp(new File(resourceFolder, LAST_USED_FILENAME), timestamp);
    }

    private static void writeTimestamp(File file, Instant timestamp) {
        SpecsIo.write(file, timestamp.toString());
    }

    void deleteStaleVersions(Instant now, File currentVersionFolder) {
        File cacheBaseFolder = options.get(CodeParser.DUMPER_FOLDER);
        var versions = cacheBaseFolder.listFiles(File::isDirectory);
        if (versions == null) {
            return;
        }

        for (var versionFolder : versions) {
            if (versionFolder.getAbsoluteFile().equals(currentVersionFolder.getAbsoluteFile())) {
                continue;
            }

            var jvmLock = CLANG_FILES_LOCKS.computeIfAbsent(versionFolder.getAbsolutePath(), ignored -> new Object());
            try {
                synchronized (jvmLock) {
                    var lastUsedFile = new File(versionFolder, LAST_USED_FILENAME);
                    if (!lastUsedFile.isFile()) {
                        continue;
                    }

                    try (var lock = tryAcquireCacheLock(versionFolder)) {
                        if (lock == null) {
                            SpecsLogs.debug(() -> "Skipping locked clang-dumper cache folder: " + versionFolder);
                            continue;
                        }

                        if (!lastUsedFile.isFile()) {
                            continue;
                        }

                        var lastUsed = Instant.parse(SpecsIo.read(lastUsedFile).trim());
                        if (lastUsed.isBefore(now.minus(STALE_CACHE_MAX_AGE))) {
                            SpecsLogs.info("Deleting stale clang-dumper cache folder: " + versionFolder);
                            SpecsIo.deleteFolder(versionFolder);
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                SpecsLogs.warn("Could not inspect clang-dumper cache folder '" + versionFolder + "'", e);
            }
        }
    }

    static CacheLock acquireCacheLock(File versionFolder) throws IOException {
        return acquireCacheLock(versionFolder, true);
    }

    private static CacheLock tryAcquireCacheLock(File versionFolder) throws IOException {
        return acquireCacheLock(versionFolder, false);
    }

    private static CacheLock acquireCacheLock(File versionFolder, boolean wait) throws IOException {
        var lockFolder = getCacheLockFolder(versionFolder);
        Files.createDirectories(lockFolder.getParentFile().toPath());

        while (true) {
            try {
                Files.createDirectory(lockFolder.toPath());
            } catch (FileAlreadyExistsException e) {
                if (!isCacheLockStale(lockFolder)) {
                    if (!wait) {
                        return null;
                    }

                    waitForCacheLock();
                    continue;
                }

                recoverStaleCacheLock(lockFolder);
                continue;
            }

            var ownerFile = new File(lockFolder, CACHE_LOCK_OWNER_PREFIX + UUID.randomUUID());
            try {
                Files.writeString(ownerFile.toPath(), getProcessIdentity(), StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE);
            } catch (NoSuchFileException e) {
                // Stale-lock recovery removed the directory while this process was claiming it.
                continue;
            } catch (IOException e) {
                deleteEmptyCacheLock(lockFolder);
                throw e;
            }

            return new CacheLock(lockFolder, ownerFile);
        }
    }

    /**
     * Returns the temporary lock folder for a cache version.
     *
     * <p>The lock folder is outside the version folder because stale cleanup deletes that folder while holding the
     * lock. The folder is removed when the lock is released, so normal operation leaves no lock artifact behind.</p>
     */
    static File getCacheLockFolder(File versionFolder) {
        var absoluteVersionFolder = versionFolder.getAbsoluteFile();
        return new File(absoluteVersionFolder.getParentFile(), absoluteVersionFolder.getName() + CACHE_LOCK_FOLDERNAME);
    }

    private static String getProcessIdentity() {
        var process = ProcessHandle.current();
        var startTime = process.info().startInstant().map(Instant::toString).orElse("");
        return process.pid() + System.lineSeparator() + startTime;
    }

    private static boolean isCacheLockStale(File lockFolder) throws IOException {
        if (!lockFolder.exists()) {
            return true;
        }

        if (!lockFolder.isDirectory()) {
            throw new IOException("Cache lock path is not a directory: '" + lockFolder + "'");
        }

        var ownerFiles = lockFolder.listFiles(File::isFile);
        if (ownerFiles == null) {
            if (!lockFolder.exists()) {
                return true;
            }

            throw new IOException("Could not list cache lock folder: '" + lockFolder + "'");
        }

        if (ownerFiles.length == 0) {
            return isCacheLockOld(lockFolder);
        }

        for (var ownerFile : ownerFiles) {
            if (!isCacheLockOwnerStale(lockFolder, ownerFile)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isCacheLockOwnerStale(File lockFolder, File ownerFile) throws IOException {
        List<String> lines;
        try {
            lines = Files.readAllLines(ownerFile.toPath());
        } catch (NoSuchFileException e) {
            return true;
        }

        if (lines.isEmpty()) {
            return isCacheLockOld(lockFolder);
        }

        try {
            var pid = Long.parseLong(lines.get(0).trim());
            var process = ProcessHandle.of(pid);
            if (process.isEmpty() || !process.get().isAlive()) {
                return true;
            }

            if (lines.size() > 1 && !lines.get(1).isBlank()) {
                var processStart = process.get().info().startInstant();
                if (processStart.isPresent() && !processStart.get().toString().equals(lines.get(1).trim())) {
                    return true;
                }
            }

            return false;
        } catch (NumberFormatException e) {
            return isCacheLockOld(lockFolder);
        }
    }

    private static boolean isCacheLockOld(File lockFolder) throws IOException {
        try {
            return Files.getLastModifiedTime(lockFolder.toPath()).toInstant()
                    .isBefore(Instant.now().minus(CACHE_LOCK_STALE_MAX_AGE));
        } catch (NoSuchFileException e) {
            return true;
        }
    }

    private static void waitForCacheLock() throws IOException {
        try {
            Thread.sleep(CACHE_LOCK_RETRY_INTERVAL.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for clang-dumper cache lock", e);
        }
    }

    private static void recoverStaleCacheLock(File lockFolder) throws IOException {
        if (!lockFolder.isDirectory()) {
            return;
        }

        var ownerFiles = lockFolder.listFiles(File::isFile);
        if (ownerFiles == null) {
            return;
        }

        for (var ownerFile : ownerFiles) {
            if (isCacheLockOwnerStale(lockFolder, ownerFile)) {
                Files.deleteIfExists(ownerFile.toPath());
            }
        }

        deleteEmptyCacheLock(lockFolder);
    }

    private static void deleteEmptyCacheLock(File lockFolder) throws IOException {
        try {
            Files.deleteIfExists(lockFolder.toPath());
        } catch (DirectoryNotEmptyException e) {
            // A replacement owner claimed the lock while stale recovery was in progress.
        }
    }

    /**
     * A temporary claim on one cache version. Each claim has its own owner marker, so releasing an old claim cannot
     * remove a newer claim created after stale-lock recovery.
     */
    static final class CacheLock implements AutoCloseable {

        private final File lockFolder;
        private final File ownerFile;

        private CacheLock(File lockFolder, File ownerFile) {
            this.lockFolder = lockFolder;
            this.ownerFile = ownerFile;
        }

        File ownerFile() {
            return ownerFile;
        }

        @Override
        public void close() {
            try {
                Files.deleteIfExists(ownerFile.toPath());
                deleteEmptyCacheLock(lockFolder);
            } catch (IOException e) {
                SpecsLogs.warn("Could not remove temporary clang-dumper cache lock '" + lockFolder + "'", e);
            }
        }
    }

    private static boolean hasExpectedSha256(File file, ClangDumperManifestAsset asset) {
        return asset.sha256().equalsIgnoreCase(calculateSha256(file));
    }

    private static String calculateSha256(File file) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            try (var inputStream = Files.newInputStream(file.toPath())) {
                inputStream.transferTo(new java.io.OutputStream() {
                    @Override
                    public void write(int b) {
                        digest.update((byte) b);
                    }

                    @Override
                    public void write(byte[] b, int off, int len) {
                        digest.update(b, off, len);
                    }
                });
            }

            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not calculate SHA-256 for file '" + file + "'", e);
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

}
