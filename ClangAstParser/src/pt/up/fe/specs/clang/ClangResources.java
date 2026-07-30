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
import pt.up.fe.specs.clang.ClangAstWebResource.Release;
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
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClangResources {

    private static final Map<String, ClangFiles> CLANG_FILES_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Object> CLANG_FILES_LOCKS = new ConcurrentHashMap<>();
    private final static String CLANG_FOLDERNAME = "clang_ast_exe";
    private final static String INCLUDES_FOLDERNAME = "includes";
    private final static String INCLUDES_HASHES_FILENAME = "includes.sha256";
    private final static String INCLUDES_VALIDATED_FILENAME = "includes-validated.txt";
    private final static String LAST_USED_FILENAME = "last-used.txt";
    private final static String CACHE_LOCK_FILENAME = ".cache.lock";
    private final static Duration STALE_CACHE_MAX_AGE = Duration.ofDays(60);
    private final static Duration INCLUDES_DEEP_VALIDATION_INTERVAL = Duration.ofDays(1);

    private static final AtomicInteger HAS_LIBC = new AtomicInteger(-1);

    private final CodeParser options;

    public ClangResources(CodeParser options) {
        this.options = options;
    }

    public ClangFiles getClangFiles(LibcMode libcMode) {

        var source = ClangAstWebResource.getDumperSource();
        var useBuiltinCuda = options.get(CodeParser.CUDA_PATH).equalsIgnoreCase(CodeParser.getBuiltinOption());
        var sourceKey = source instanceof Release
                ? source + "_" + getClangResourceFolder().getAbsolutePath()
                : source.toString();
        var key = libcMode.name() + "_" + useBuiltinCuda + "_" + sourceKey;

        var cachedFiles = CLANG_FILES_CACHE.get(key);
        if (cachedFiles != null) {
            if (source instanceof Release) {
                writeLastUsed(Instant.now());
            }
            SpecsLogs.debug(() -> "Using cached version of Clang files: " + cachedFiles);
            return cachedFiles;
        }

        // The JVM lock must protect the same cache file as the inter-process lock. The cache key also contains the
        // libc and CUDA configuration, so using it here would let two configurations acquire different JVM locks for
        // the same release folder and trigger OverlappingFileLockException.
        var jvmLockKey = source instanceof Release
                ? new File(getClangResourceFolder(), CACHE_LOCK_FILENAME).getAbsolutePath()
                : source.toString();
        var jvmLock = CLANG_FILES_LOCKS.computeIfAbsent(jvmLockKey, ignored -> new Object());
        synchronized (jvmLock) {
            var files = CLANG_FILES_CACHE.get(key);
            if (files != null) {
                if (source instanceof Release) {
                    writeLastUsed(Instant.now());
                }
                return files;
            }

            if (source instanceof LocalBuild localBuild) {
                var newFiles = new ClangFiles(getLocalExecutable(localBuild.folder()), List.of());
                CLANG_FILES_CACHE.put(key, newFiles);
                return newFiles;
            }

            var lockFile = new File(getClangResourceFolder(), CACHE_LOCK_FILENAME);
            try (var lockChannel = FileChannel.open(lockFile.toPath(), StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE);
                    var ignored = lockChannel.lock()) {

                var manifest = ClangAstWebResource.getManifest(getClangResourceFolder());
                File clangExecutable = prepareResources(manifest);
                List<String> builtinIncludes = prepareIncludes(manifest, clangExecutable, libcMode);

                validateTopLevelCacheFiles(manifest);
                updateLastUsedAndCleanupStaleVersions();

                var newFiles = new ClangFiles(clangExecutable, builtinIncludes);
                SpecsLogs.debug(() -> "Using downloaded version of Clang files: " + newFiles);

                CLANG_FILES_CACHE.put(key, newFiles);
                return newFiles;
            } catch (IOException e) {
                throw new UncheckedIOException("Could not lock clang-dumper cache '" + lockFile + "'", e);
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

    private File prepareResources(ClangDumperManifest manifest) {
        File resourceFolder = getClangResourceFolder();
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

    private List<String> prepareIncludes(ClangDumperManifest manifest, File clangExecutable, LibcMode libcMode) {
        var useBuiltinLibc = useBuiltinLibc(clangExecutable, libcMode);
        var useBuiltinCuda = options.get(CodeParser.CUDA_PATH).equalsIgnoreCase(CodeParser.getBuiltinOption());

        if (!useBuiltinLibc && !useBuiltinCuda) {
            return List.of();
        }

        return prepareIncludes(manifest);
    }

    private List<String> prepareIncludes(ClangDumperManifest manifest) {
        var extractedFolder = prepareIncludesFolder(manifest);
        var includeFolders = getIncludeFolders(extractedFolder);
        SpecsLogs.debug(() -> "Includes folders: " + includeFolders);

        return includeFolders.stream().map(File::getAbsolutePath).toList();
    }

    private File prepareIncludesFolder(ClangDumperManifest manifest) {
        File resourceFolder = getClangResourceFolder();
        var includesAsset = getCurrentAsset(manifest, "includes");
        var extractedFolder = new File(resourceFolder, INCLUDES_FOLDERNAME);
        var includesHashesFile = new File(resourceFolder, INCLUDES_HASHES_FILENAME);
        var includesValidatedFile = new File(resourceFolder, INCLUDES_VALIDATED_FILENAME);

        if (isIncludesCacheValid(extractedFolder, includesHashesFile, includesValidatedFile, includesAsset)) {
            return extractedFolder;
        }

        ResourceWriteData zipFile = downloadAsset(includesAsset, resourceFolder);

        try {
            SpecsIo.mkdir(extractedFolder);
            SpecsIo.deleteFolderContents(extractedFolder);
            SpecsIo.extractZip(zipFile.getFile(), extractedFolder);
            writeIncludesHashes(extractedFolder, includesHashesFile, includesAsset);
            writeTimestamp(includesValidatedFile, Instant.now());
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

    private boolean isIncludesCacheValid(File includesFolder, File includesHashesFile, File includesValidatedFile,
                                         ClangDumperManifestAsset includesAsset) {

        if (!includesFolder.isDirectory() || !includesHashesFile.isFile()) {
            return false;
        }

        if (!hasExpectedIncludesAssetHash(includesHashesFile, includesAsset)) {
            return false;
        }

        var entrypointsFile = new File(includesFolder, "entrypoints.txt");
        if (!entrypointsFile.isFile()) {
            SpecsLogs.info("Cached clang-dumper includes are missing entrypoints, extracting them again.");
            return false;
        }

        if (!shouldRunIncludesDeepValidation(includesValidatedFile)) {
            return true;
        }

        var validationStart = Instant.now();
        SpecsLogs.info("Validating clang-dumper includes cache: " + includesFolder);
        if (isIncludesCacheDeepValid(includesFolder, includesHashesFile, includesAsset)) {
            writeTimestamp(includesValidatedFile, Instant.now());
            SpecsLogs.info("Validated clang-dumper includes cache in "
                    + Duration.between(validationStart, Instant.now()).toMillis() + " ms");
            return true;
        }

        SpecsLogs.info("Invalidating clang-dumper includes cache metadata: " + includesHashesFile);
        if (includesHashesFile.isFile()) {
            SpecsIo.delete(includesHashesFile);
        }
        if (includesValidatedFile.isFile()) {
            SpecsIo.delete(includesValidatedFile);
        }
        return false;
    }

    private static boolean shouldRunIncludesDeepValidation(File includesValidatedFile) {
        if (!includesValidatedFile.isFile()) {
            return true;
        }

        try {
            var lastValidated = Instant.parse(SpecsIo.read(includesValidatedFile).trim());
            return lastValidated.isBefore(Instant.now().minus(INCLUDES_DEEP_VALIDATION_INTERVAL));
        } catch (RuntimeException e) {
            return true;
        }
    }

    private boolean isIncludesCacheDeepValid(File includesFolder, File includesHashesFile,
                                             ClangDumperManifestAsset includesAsset) {

        var expectedHashes = readIncludesHashes(includesHashesFile, includesAsset);
        if (expectedHashes == null) {
            return false;
        }

        var expectedFiles = expectedHashes.keySet();
        var actualFiles = listRegularFiles(includesFolder);

        if (!actualFiles.equals(expectedFiles)) {
            SpecsLogs.info("Cached clang-dumper includes contain missing or extra files, extracting them again.");
            return false;
        }

        for (var entry : expectedHashes.entrySet()) {
            var file = new File(includesFolder, entry.getKey());
            if (!entry.getValue().equalsIgnoreCase(calculateSha256(file))) {
                SpecsLogs.info("Cached clang-dumper include file '" + file
                        + "' does not match the expected checksum, extracting includes again.");
                return false;
            }
        }

        return true;
    }

    private static boolean hasExpectedIncludesAssetHash(File includesHashesFile,
                                                        ClangDumperManifestAsset includesAsset) {

        try (var reader = Files.newBufferedReader(includesHashesFile.toPath())) {
            return ("# clang-dumper-includes " + includesAsset.sha256()).equals(reader.readLine());
        } catch (IOException e) {
            return false;
        }
    }

    private static Map<String, String> readIncludesHashes(File includesHashesFile,
                                                          ClangDumperManifestAsset includesAsset) {

        var lines = SpecsIo.read(includesHashesFile).lines().toList();
        if (lines.size() < 2 || !lines.get(0).equals("# clang-dumper-includes " + includesAsset.sha256())) {
            return null;
        }

        Map<String, String> hashes = new HashMap<>();
        for (int i = 1; i < lines.size(); i++) {
            var line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }

            if (line.length() <= 65 || line.charAt(64) != ' ') {
                return null;
            }

            var hash = line.substring(0, 64);
            var relativePath = line.substring(65);
            hashes.put(relativePath, hash);
        }

        return hashes;
    }

    private static void writeIncludesHashes(File includesFolder, File includesHashesFile,
                                            ClangDumperManifestAsset includesAsset) {

        var actualFiles = listRegularFiles(includesFolder).stream()
                .sorted()
                .toList();

        StringBuilder hashes = new StringBuilder();
        hashes.append("# clang-dumper-includes ").append(includesAsset.sha256()).append(System.lineSeparator());

        for (var relativePath : actualFiles) {
            var file = new File(includesFolder, relativePath);
            hashes.append(calculateSha256(file))
                    .append(' ')
                    .append(relativePath)
                    .append(System.lineSeparator());
        }

        SpecsIo.write(includesHashesFile, hashes.toString());
    }

    private static Set<String> listRegularFiles(File folder) {
        try (Stream<Path> paths = Files.walk(folder.toPath())) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(folder.toPath()::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace(File.separatorChar, '/'))
                    .collect(Collectors.toCollection(HashSet::new));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list files in folder '" + folder + "'", e);
        }
    }

    private void validateTopLevelCacheFiles(ClangDumperManifest manifest) {
        File resourceFolder = getClangResourceFolder();
        Set<String> expectedNames = new HashSet<>();
        expectedNames.add(ClangAstWebResource.MANIFEST_FILENAME);
        var executableKind = ClangAstDumper.usePlugin() ? "plugin" : "tool";
        expectedNames.add(getCurrentAsset(manifest, executableKind).filename());
        expectedNames.add(INCLUDES_FOLDERNAME);
        expectedNames.add(INCLUDES_HASHES_FILENAME);
        expectedNames.add(INCLUDES_VALIDATED_FILENAME);
        expectedNames.add(LAST_USED_FILENAME);
        expectedNames.add(CACHE_LOCK_FILENAME);

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

    private void updateLastUsedAndCleanupStaleVersions() {
        var now = Instant.now();
        File resourceFolder = getClangResourceFolder();
        writeLastUsed(now);

        var staleCleanup = new Thread(() -> deleteStaleVersions(now, resourceFolder),
                "clang-dumper-stale-cache-cleanup");
        staleCleanup.setDaemon(true);
        staleCleanup.start();
    }

    private void writeLastUsed(Instant timestamp) {
        writeTimestamp(new File(getClangResourceFolder(), LAST_USED_FILENAME), timestamp);
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
            if (versionFolder.equals(currentVersionFolder)) {
                continue;
            }

            var lastUsedFile = new File(versionFolder, LAST_USED_FILENAME);
            if (!lastUsedFile.isFile()) {
                continue;
            }

            var lockFile = new File(versionFolder, CACHE_LOCK_FILENAME);
            try (var lockChannel = FileChannel.open(lockFile.toPath(), StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE)) {

                var lock = tryLock(lockChannel);
                if (lock == null) {
                    SpecsLogs.debug(() -> "Skipping locked clang-dumper cache folder: " + versionFolder);
                    continue;
                }

                try (lock) {
                    if (!lastUsedFile.isFile()) {
                        continue;
                    }

                    var lastUsed = Instant.parse(SpecsIo.read(lastUsedFile).trim());
                    if (lastUsed.isBefore(now.minus(STALE_CACHE_MAX_AGE))) {
                        SpecsLogs.info("Deleting stale clang-dumper cache folder: " + versionFolder);
                        SpecsIo.deleteFolder(versionFolder);
                    }
                }
            } catch (IOException | RuntimeException e) {
                SpecsLogs.warn("Could not inspect clang-dumper cache folder '" + versionFolder + "'", e);
            }
        }
    }

    private static FileLock tryLock(FileChannel lockChannel) throws IOException {
        try {
            return lockChannel.tryLock();
        } catch (OverlappingFileLockException e) {
            return null;
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
