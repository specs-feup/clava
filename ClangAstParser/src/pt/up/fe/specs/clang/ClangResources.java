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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clang;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.dumper.ClangAstDumper;
import pt.up.fe.specs.clang.parsers.TopLevelNodesParser;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.exceptions.CaseNotDefinedException;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.FileResourceManager;
import pt.up.fe.specs.util.providers.FileResourceProvider;
import pt.up.fe.specs.util.providers.FileResourceProvider.ResourceWriteData;
import pt.up.fe.specs.util.system.ProcessOutputAsString;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ClangResources {

    private static final FileResourceManager CLANG_AST_RESOURCES = FileResourceManager
            .fromEnum(ClangAstFileResource.class);

    private static final Map<String, ClangFiles> CLANG_FILES_CACHE = new ConcurrentHashMap<>();

    private final static String CLANG_FOLDERNAME = "clang_ast_exe";

    private final static Lazy<File> CUDALIB_FOLDER = Lazy.newInstance(ClangResources::prepareBuiltinCudaLib);

    private final FileResourceManager clangAstResources;
    private final CodeParser options;


    private static final AtomicInteger HAS_LIBC = new AtomicInteger(-1);

    public ClangResources(CodeParser options) {
        clangAstResources = FileResourceManager.fromEnum(ClangAstFileResource.class);
        this.options = options;
    }

    public ClangFiles getClangFiles(String version, LibcMode libcMode) {

        // Create key
        var key = libcMode.name() + "_" + version;

        // Check if cached
        var files = CLANG_FILES_CACHE.get(key);
        if (files != null) {
            SpecsLogs.debug(() -> "Using cached version of Clang files: " + files);
            return files;
        }

        File clangExecutable = prepareResources(version);
        List<String> builtinIncludes = prepareIncludes(clangExecutable, libcMode);

        var newFiles = new ClangFiles(clangExecutable, builtinIncludes);
        SpecsLogs.debug(() -> "Using downloaded version of Clang files: " + newFiles);

        // Store in cache
        CLANG_FILES_CACHE.put(key, newFiles);

        return newFiles;
    }

    /**
     * @return path to the executable that was copied
     */
    private File prepareResources(String version) {

        File resourceFolder = getClangResourceFolder();

        SupportedPlatform platform = SupportedPlatform.getCurrentPlatform();
        FileResourceProvider executableResource = getVersionedResource(getExecutableResource(platform), version);

        // Copy executable
        ResourceWriteData executable = executableResource.writeVersioned(resourceFolder, ClangResources.class);

        // If Windows, copy additional dependencies
        if (platform == SupportedPlatform.WINDOWS) {
            for (FileResourceProvider resource : getWindowsResources()) {
                resource.writeVersioned(resourceFolder, ClangResources.class);
            }
        } else if (platform == SupportedPlatform.MAC_OS) {
            //var dynLibsFolder = new File("/usr/local/lib/");
            for (FileResourceProvider resource : getMacOSResources()) {
                resource.writeVersioned(resourceFolder, ClangResources.class);
            }
        } else if (platform == SupportedPlatform.LINUX_5) {
            for (FileResourceProvider resource : getLinuxResources()) {
                resource.writeVersioned(resourceFolder, ClangResources.class);
            }
        }

        // If on Windows, preemptively unblock file, due to possible Mark-of-the-Web restrictions
        if (platform.isWindows()) {
            var command = List.of(SpecsSystem.getWindowsPowershell(), "-NoLogo", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass",
                    "-Command",
                    "Unblock-File",
                    "-Path",
                    "\"" + executable.getFile().getAbsolutePath() + "\"",
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

        // If file is new and we are in a flavor of Linux, make file executable
        if (executable.isNewFile() && platform.isLinux()) {
            SpecsSystem.runProcess(Arrays.asList("chmod", "+x", executable.getFile().getAbsolutePath()), false, true);
        }

        // If on linux, make folders and files accessible to all users
        if (platform.isLinux()) {
            SpecsSystem.runProcess(Arrays.asList("chmod", "-R", "777", resourceFolder.getAbsolutePath()), false, true);
        }

        return executable.getFile();
    }

    private FileResourceProvider getVersionedResource(FileResourceProvider resource) {
        return getVersionedResource(resource, "");
    }

    private FileResourceProvider getVersionedResource(FileResourceProvider resource, String version) {

        // If version not defined, use the latest version of the resource
        if (version.isEmpty()) {
            version = resource.getVersion();
        }

        // ClangAst executable versions are separated by an underscore
        resource = resource.createResourceVersion("_" + version);
        return resource;
    }

    public static File getClangResourceFolder() {
        return SpecsIo.getTempFolder(CLANG_FOLDERNAME);
    }

    private Optional<FileResourceProvider> getCustomExecutable() {
        // Check if theres is a custom executable
        var customExe = options.get(CodeParser.CUSTOM_CLANG_AST_DUMPER_EXE);

        if (customExe.getName().isBlank()) {
            return Optional.empty();
        }

        if (!customExe.isFile()) {
            SpecsLogs.info("Specified a custom executable but could not find file '" + customExe
                    + "', using built-in executable");

            return Optional.empty();
        }

        SpecsLogs.info("Using custom executable for ClangAstDumper: '" + customExe.getAbsolutePath() + "'");

        return Optional.of(FileResourceProvider.newInstance(customExe));
    }

    private FileResourceProvider getExecutableResource(SupportedPlatform platform) {

        var customExecutable = getCustomExecutable();

        if (customExecutable.isPresent()) {
            return customExecutable.get();
        }

        switch (platform) {
            case WINDOWS:
                return CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_EXE);
            case LINUX_5:
                if (ClangAstDumper.usePlugin()) {
                    return CLANG_AST_RESOURCES.get(ClangAstFileResource.LINUX_PLUGIN);
                } else {
                    return CLANG_AST_RESOURCES.get(ClangAstFileResource.LINUX_EXE);
                }

            case MAC_OS:
                return CLANG_AST_RESOURCES.get(ClangAstFileResource.MAC_OS_EXE);
            default:
                throw new RuntimeException("Case not defined: '" + platform + "'");
        }
    }

    private List<FileResourceProvider> getWindowsResources() {
        List<FileResourceProvider> windowsResources = new ArrayList<>();

        windowsResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_DLL1));
        windowsResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_DLL2));
        windowsResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_DLL3));
        windowsResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_DLL4));
        windowsResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_DLL5));
        windowsResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_DLL6));
        windowsResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_DLL7));
        windowsResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_DLL8));
        windowsResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_DLL9));
        windowsResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_CLANG_DLL));
        windowsResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_LLVM_DLL));

        return windowsResources;
    }

    private List<FileResourceProvider> getMacOSResources() {
        List<FileResourceProvider> macosResources = new ArrayList<>();

        macosResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.MAC_OS_LLVM_DLL));
        macosResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.MAC_OS_DLL1));

        return macosResources;
    }

    private List<FileResourceProvider> getLinuxResources() {
        List<FileResourceProvider> linuxResources = new ArrayList<>();

        linuxResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.LINUX_LLVM_DLL));

        return linuxResources;
    }

    private List<String> prepareIncludes(File clangExecutable, LibcMode libcMode) {

        // Get base resource folder
        File resourceFolder = getClangResourceFolder();

        // Create list of include zips
        List<FileResourceProvider> includesZips = new ArrayList<>();

        // Get libc/libcxx resources, if required
        if (useBuiltinLibc(clangExecutable, libcMode)) {
            // Common Clang files (except Linux, it is a self-contained package)
            if (!SupportedPlatform.getCurrentPlatform().isLinux()) {
                var builtinResource = CLANG_AST_RESOURCES.get(ClangAstFileResource.LIBC_CXX_LLVM);
                includesZips.add(getVersionedResource(builtinResource, builtinResource.getVersion()));
            }
            // Self-contained Linux includes
            else {
                var linuxBuiltinResource = CLANG_AST_RESOURCES.get(ClangAstFileResource.LIBC_CXX_LINUX);
                includesZips.add(getVersionedResource(linuxBuiltinResource, linuxBuiltinResource.getVersion()));
            }


            // Windows-exclusive files
            if (SupportedPlatform.getCurrentPlatform().isWindows()) {
                var windowsBuiltinResource = CLANG_AST_RESOURCES.get(ClangAstFileResource.LIBC_CXX_WIN32);
                includesZips.add(getVersionedResource(windowsBuiltinResource, windowsBuiltinResource.getVersion()));
            }


        }

        // Always add OpenMP includes
        includesZips.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.OPENMP_INCLUDES));

        // Download includes zips, later we check if any of them is new
        List<ResourceWriteData> zipFiles = includesZips.stream()
                .map(resource -> resource.writeVersioned(resourceFolder, ClangResources.class))
                .collect(Collectors.toList());


        var extractedFolders = new ArrayList<File>();

        // If a new file has been written or if folder exists but is empty, delete corresponding includes folder, and extract zip again
        for (var zipFile : zipFiles) {

            // Obtain folder for zip
            var zipFoldername = "include_" + SpecsIo.removeExtension(zipFile.getFile());
            var extractedFolder = SpecsIo.mkdir(resourceFolder, zipFoldername);

            // Add to extracted folders list
            extractedFolders.add(extractedFolder);

            // Skip extraction if zip is not new and folder is not empty
            if (!zipFile.isNewFile() && !SpecsIo.isEmptyFolder(extractedFolder)) {
                continue;
            }

            // Clean folder
            SpecsIo.deleteFolderContents(extractedFolder);

            // Extract zip contents to folder
            SpecsIo.extractZip(zipFile.getFile(), extractedFolder);
        }

        // Add all folders inside extracted folders as system include
        var includes = new ArrayList<String>();
        for (var extractedFolder : extractedFolders) {
            var includeFolders = SpecsIo.getFolders(extractedFolder).stream()
                    .map(file -> file.getAbsolutePath())
                    .toList();

            includes.addAll(includeFolders);
        }


        // Sort them alphabetically, include order is important
        Collections.sort(includes);
        SpecsLogs.debug(() -> "Includes folders: " + includes);

        // If on linux, make folders and files accessible to all users
        if (SupportedPlatform.getCurrentPlatform().isLinux()) {
            SpecsSystem.runProcess(Arrays.asList("chmod", "-R", "777", resourceFolder.getAbsolutePath()), false, true);
        }

        return includes;
    }

    private boolean useBuiltinLibc(File clangExecutable, LibcMode libcMode) {

        switch (libcMode) {
            case AUTO:
                return !hasLibC(clangExecutable);
            // Builtin and libc/libcxx are now merged in the same zip
            case BUILTIN_AND_LIBC:
            case BASE_BUILTIN_ONLY:
                return true;
            case SYSTEM:
                return false;
            default:
                throw new CaseNotDefinedException(libcMode);
        }
    }

    private boolean hasLibC(File clangExecutable) {
        var value = HAS_LIBC.get();

        // Check if initiallized
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

    /**
     * Detects if the system has libc/licxx installed.
     *
     * @param clangExecutable
     * @return
     */
    private boolean detectLibC(File clangExecutable) {

        // return false;

        // If Windows, return false and always use bundled LIBC++
        // if (SupportedPlatform.getCurrentPlatform().isWindows()) {
        // return false;
        // }

        File clangTest = SpecsIo.mkdir(SpecsIo.getTempFolder(), "clang_ast_test");

        // Write test files
        List<File> testFiles = Arrays.asList(ClangAstResource.TEST_INCLUDES_C, ClangAstResource.TEST_INCLUDES_CPP)
                .stream()
                .map(resource -> resource.write(clangTest))
                .collect(Collectors.toList());

        // If on linux, make folders and files accessible to all users
        if (SupportedPlatform.getCurrentPlatform().isLinux()) {
            SpecsSystem.runProcess(Arrays.asList("chmod", "-R", "777", clangTest.getAbsolutePath()), false, true);
        }

        // boolean needsLib = Arrays.asList(ClangAstResource.TEST_INCLUDES_C, ClangAstResource.TEST_INCLUDES_CPP)

        boolean needsLib = false;
        for (File testFile : testFiles) {

            // Invoke dumper
            var output = runClangAstDumper(clangExecutable, testFile);

            // First check if there where no problems running the dumper
            if (output.getReturnValue() != 0) {
                ClavaLog.info("Problems while running dumper to test in libc/libcxx is needed");
                needsLib = true;
                break;
            }

            // Test files where built in such a way so that if a system include is present, it will generate code with a
            // top level nodes, otherwise it generates an empty file
            var topLevelNodesHeader = TopLevelNodesParser.getTopLevelNodesHeader();

            var foundInclude = output.getOutput().contains(topLevelNodesHeader);

            if (!foundInclude) {
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

    private ProcessOutputAsString runClangAstDumper(File clangExecutable, File testFile) {
        List<String> arguments = Arrays.asList(clangExecutable.getAbsolutePath(), testFile.getAbsolutePath(), "--");
        return SpecsSystem.runProcess(arguments, true, false);
    }

    public static File getBuiltinCudaLib() {
        return CUDALIB_FOLDER.get();
    }

    private static File prepareBuiltinCudaLib() {
        var fileResource = CLANG_AST_RESOURCES.get(ClangAstFileResource.CUDA_LIB);
        var resourceFolder = getClangResourceFolder();
        var cudalibFolder = SpecsIo.mkdir(new File(resourceFolder, "cudalib"));

        // Download includes zips, check if any of them is new
        ResourceWriteData zipFile = fileResource.writeVersioned(resourceFolder, ClangResources.class);

        // If a new file has been written, delete includes folder, and extract all zips again
        // Extracting all because zips might have several folders and we are not determining which should be updated
        if (zipFile.isNewFile()) {
            // Clean folder
            SpecsIo.deleteFolderContents(cudalibFolder);

            // Extract zip
            SpecsIo.extractZip(zipFile.getFile(), cudalibFolder);
        }

        // Returnb cuda lib folder
        return cudalibFolder;
    }
}
