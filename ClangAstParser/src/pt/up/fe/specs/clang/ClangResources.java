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
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ClangResources {

    private static final Map<String, ClangFiles> CLANG_FILES_CACHE = new ConcurrentHashMap<>();

    private final static String CLANG_FOLDERNAME = "clang_ast_exe";

    private static final AtomicInteger HAS_LIBC = new AtomicInteger(-1);

    private final CodeParser options;

    public ClangResources(CodeParser options) {
        this.options = options;
    }

    public ClangFiles getClangFiles(String version, LibcMode libcMode) {

        var effectiveVersion = version.isEmpty() ? ClangAstWebResource.getReleaseTag() : version;
        var key = libcMode.name() + "_" + effectiveVersion + "_" + getClangResourceFolder().getAbsolutePath();

        var files = CLANG_FILES_CACHE.get(key);
        if (files != null) {
            SpecsLogs.debug(() -> "Using cached version of Clang files: " + files);
            return files;
        }

        var manifest = ClangAstWebResource.getManifest(getClangResourceFolder());
        File clangExecutable = prepareResources(manifest);
        List<String> builtinIncludes = prepareIncludes(manifest, clangExecutable, libcMode);

        var newFiles = new ClangFiles(clangExecutable, builtinIncludes);
        SpecsLogs.debug(() -> "Using downloaded version of Clang files: " + newFiles);

        CLANG_FILES_CACHE.put(key, newFiles);

        return newFiles;
    }

    /**
     * @return path to the executable that was copied
     */
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
        return options.get(CodeParser.DUMPER_FOLDER);
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
        if (!useBuiltinLibc(clangExecutable, libcMode)) {
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
        ResourceWriteData zipFile = downloadAsset(manifest, "includes", resourceFolder);

        var zipFoldername = "include_" + SpecsIo.removeExtension(zipFile.getFile());
        var extractedFolder = SpecsIo.mkdir(resourceFolder, zipFoldername);

        if (zipFile.isNewFile() || SpecsIo.isEmptyFolder(extractedFolder)) {
            SpecsIo.deleteFolderContents(extractedFolder);
            SpecsIo.extractZip(zipFile.getFile(), extractedFolder);
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
        var platform = getManifestPlatform();
        var arch = getManifestArch(platform);
        var asset = manifest.getAsset(platform, arch, kind);
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

    public File getBuiltinCudaLib() {
        var manifest = ClangAstWebResource.getManifest(getClangResourceFolder());
        return prepareIncludesFolder(manifest);
    }
}
