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

package pt.up.fe.specs.clang.dumper;

import com.github.luben.zstd.ZstdInputStream;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.JOptionsUtils;
import org.suikasoft.jOptions.streamparser.LineStreamParser;
import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.ClangResources;
import pt.up.fe.specs.clang.LibcMode;
import pt.up.fe.specs.clang.cilk.CilkParser;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clang.parsers.ClangStreamParserV2;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.utils.SourceType;
import pt.up.fe.specs.lang.SpecsPlatforms;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.parsing.arguments.ArgumentsParser;
import pt.up.fe.specs.util.system.ProcessOutput;
import pt.up.fe.specs.util.utilities.LineStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Calls the ClangAstDumper executable and returns the dumped information. Clava AST can be built based on this output.
 *
 * @author JoaoBispo
 */
public class ClangAstDumper {

    private final static boolean USE_PLUGIN = false;
    private final static String SYSTEM_HEADER_THRESHOLD_OPTION = "-system-header-threshold=";

    public static boolean usePlugin() {
        return USE_PLUGIN;
    }

    private final static String CLANG_DUMP_FILENAME = "clangDump.txt";
    private final static String COMPRESSED_CLANG_DUMP_FILENAME = "clangDump.txt.zst";
    private final static String STDERR_DUMP_FILENAME = "stderr.txt";

    /**
     * TODO: Not implemented yet
     * <p>
     * If true, displays the output of the dumper while it executes. If false, stores the output and only shows it after
     * execution.
     * <p>
     * Usually should be disabled when executing several versions of the parser concurrently.
     */
    private final boolean streamConsoleOutput;

    private File lastWorkingFolder;
    private File clangExecutable;
    private List<String> builtinIncludes;
    private File systemResourceDir;
    private int systemIncludesThreshold;
    private final ClangResources clangResources;
    private boolean validationOnly;
    private String lastValidationError;

    private final CodeParser parserConfig;

    /**
     * TODO: Replace some of the arguments with reads to CodeParser?
     *
     * @param dumpStdOut
     * @param useCustomResources
     * @param streamConsoleOutput
     * @param clangExecutable
     * @param builtinIncludes
     * @param systemResourceDir
     * @param parserConfig
     */
    public ClangAstDumper(boolean streamConsoleOutput,
                          File clangExecutable, List<String> builtinIncludes, File systemResourceDir,
                          CodeParser parserConfig) {

        this.streamConsoleOutput = streamConsoleOutput;

        this.clangExecutable = clangExecutable;
        this.builtinIncludes = builtinIncludes;
        this.systemResourceDir = systemResourceDir;

        this.lastWorkingFolder = null;
        this.systemIncludesThreshold = ParallelCodeParser.SYSTEM_INCLUDES_THRESHOLD.getDefault().get();
        this.parserConfig = parserConfig;
        this.clangResources = new ClangResources(parserConfig);
    }

    public File getLastWorkingFolder() {
        return lastWorkingFolder;
    }

    public ClangAstDumper setSystemIncludesThreshold(int systemIncludesThreshold) {
        this.systemIncludesThreshold = systemIncludesThreshold;
        return this;
    }

    public ClangAstData parse(File sourceFile, String id, Standard standard, DataStore config) {

        // Pre-processing before the parsing
        if (config.get(ClangAstKeys.USES_CILK)) {

            // Prepare source file
            sourceFile = new CilkParser().prepareCilkFile(sourceFile);
        }

        return parsePrivate(sourceFile, id, standard, config);
    }

    /**
     * Invokes Clang with the same arguments as parsing, while discarding dumper output.
     *
     * @return null if the syntax is valid, otherwise an error message
     */
    public String validateSyntax(File sourceFile, String id, Standard standard, DataStore config) {
        if (config.get(ClangAstKeys.USES_CILK)) {
            sourceFile = new CilkParser().prepareCilkFile(sourceFile);
        }

        validationOnly = true;
        try {
            parsePrivate(sourceFile, id, standard, config);
            return lastValidationError;
        } finally {
            validationOnly = false;
        }
    }

    private ClangAstData parsePrivate(File sourceFile, String id, Standard standard, DataStore config) {
        ClavaLog.debug(() -> "Data store config for single file parser: " + config);

        File generatedParseRoot = parserConfig.hasValue(CodeParser.GENERATED_PARSE_ROOT)
                ? parserConfig.get(CodeParser.GENERATED_PARSE_ROOT).getAbsoluteFile()
                : null;

        DataStore localData = JOptionsUtils.loadDataStore(LocalOptionsKeys.getLocalOptionsFilename(), getClass(),
                LocalOptionsKeys.getProvider().getStoreDefinition());

        List<String> arguments = new ArrayList<>();
        if (USE_PLUGIN && SpecsPlatforms.isLinux()) {
            arguments.add("clang-16");
            arguments.add("-c");

            arguments.add("-Xclang");
            arguments.add("-load");
            arguments.add("-Xclang");
            arguments.add(clangExecutable.getAbsolutePath());
            arguments.add("-Xclang");
            arguments.add("-plugin");
            arguments.add("-Xclang");
            arguments.add("DumpAst");
            arguments.add("-Xclang");
            arguments.add("-plugin-arg-DumpAst");
            arguments.add("-Xclang");
            arguments.add("-file-id=" + id);
            arguments.add("-Xclang");
            arguments.add("-plugin-arg-DumpAst");
            arguments.add("-Xclang");
            arguments.add(SYSTEM_HEADER_THRESHOLD_OPTION + systemIncludesThreshold);
        } else {
            arguments.add(clangExecutable.getAbsolutePath());

            arguments.add("-c");
            arguments.add(pathForCompiler(sourceFile, generatedParseRoot));

            arguments.add("-id=" + id);

            arguments.add(SYSTEM_HEADER_THRESHOLD_OPTION + systemIncludesThreshold);

            arguments.add("--");
        }

        var extension = SpecsIo.getExtension(sourceFile);
        boolean isOpenCL = extension.equals("cl");
        boolean isCuda = extension.equals("cu");

        // Compilation of header files always need a standard, but OpenCL compilation fails if there is a standard
        // specified that is not an OpenCL standard.
        if (isOpenCL && !standard.isOpenCL()) {

            // Using OpenCL 2.0 as default
            arguments.add("-std=cl2.0");
        }
        // Set standard to CUDA
        else if (isCuda) {
            // The LLVM 18 driver bundled with clang-dumper rejects '-std=cuda'. The .cu extension already
            // selects CUDA mode, so use a C++ standard for host-side parsing.
            arguments.add(standard.isCxx() ? standard.getFlag() : Standard.CXX17.getFlag());
        } else {
            arguments.add(standard.getFlag());
        }

        // If OpenCL file, add necessary flags
        if (isOpenCL) {
            // OpenCL parsing
            // arguments.add("-x");
            // arguments.add("cl");

            // OpenCL header file
            arguments.add("-include");
            arguments.add("opencl-c.h");

            // Needed for the current OpenCL header file
            arguments.add("-fblocks");

            // arguments.add("-D__OPENCL_C_VERSION__=100");

            // Enable extensions
            arguments.add("-Dcl_khr_int64_base_atomics");
            arguments.add("-Dcl_khr_int64_extended_atomics");
            arguments.add("-Dcl_khr_fp16");
            arguments.add("-Dcl_khr_fp64");

        }
        // If CUDA, add corresponding flags
        else if (isCuda) {
            if (!SpecsPlatforms.isLinux()) {
                ClavaLog.info("We only officially support CUDA parsing in Linux, run at your own risk");
                arguments.add("-fms-compatibility");
                if (SpecsPlatforms.isWindows()) {
                    arguments.add("-D_MSC_VER");
                    arguments.add("-D_LIBCPP_MSVCRT");
                }
            }

            arguments.add("--cuda-gpu-arch=" + parserConfig.get(CodeParser.CUDA_GPU_ARCH));

            var cudaPath = parserConfig.get(CodeParser.CUDA_PATH);
            addCudaPathArgument(arguments, cudaPath);

            // Since we only need parsing, enable host-only
            // Can help with errors such as "__float128 is not supported on this target"
            arguments.add("--cuda-host-only");

        }

        // If header file, add the language flag (-x) that corresponds to the standard
        else if (SourceType.isHeader(sourceFile)) {
            arguments.add("-x");
            arguments.add(standard.isCxx() ? "c++" : "c");
        }

        if (systemResourceDir != null) {
            arguments.add("-resource-dir=" + systemResourceDir.getAbsolutePath());
        }

        // The parser has already resolved the libc policy before creating this per-file configuration.
        if (config.get(ClangAstKeys.LIBC_CXX_MODE) == LibcMode.BUILTIN_AND_LIBC) {
            arguments.add("-nostdinc");
            arguments.add("-nostdinc++");
        }

        List<String> systemIncludes = new ArrayList<>();

        // Add bundled includes according to libc/cxx settings
        systemIncludes.addAll(builtinIncludes);

        // Add custom includes
        systemIncludes.addAll(localData.get(LocalOptionsKeys.SYSTEM_INCLUDES).getStringList());

        // Add local system includes
        for (String systemInclude : systemIncludes) {
            arguments.add("-isystem");
            arguments.add(systemInclude);
        }

        arguments.addAll(ArgumentsParser.newCommandLine().parse(config.get(ClavaOptions.FLAGS)));

        arguments.addAll(config.get(ClavaOptions.FLAGS_LIST));

        if (generatedParseRoot != null) {
            relativizeGeneratedPathArguments(arguments, generatedParseRoot);
        }

        if (validationOnly) {
            lastValidationError = validateSyntax(arguments, sourceFile, id);
            return null;
        }

        ClangAstData parsedData = null;
        ProcessOutput<String, String> output = null;

        try (LineStreamParser<ClangAstData> lineStreamParser = ClangStreamParserV2
                .newInstance(config.get(ClavaNode.CONTEXT))) {

            if (SpecsSystem.isDebug()) {
                lineStreamParser.getData().set(ClangAstData.DEBUG, true);
            }
            if (generatedParseRoot != null) {
                lineStreamParser.getData().set(ClangAstData.PARSE_ROOT, generatedParseRoot);
            }

            // Each invocation needs unique output paths, but clang-dumper no longer
            // creates side files or needs a dedicated process working directory.
            lastWorkingFolder = Files.createTempDirectory("clava_ast_").toFile();

            boolean useAstDumpCache = SpecsPlatforms.isLinux() && !USE_PLUGIN
                    && parserConfig.get(CodeParser.AST_DUMP_CACHE)
                    && !parserConfig.get(CodeParser.SHOW_CLANG_DUMP)
                    && !isOpenCL
                    && !SourceType.isHeader(sourceFile)
                    && ClangCcacheAdapter.isAvailable();
            File dumpFile = new File(lastWorkingFolder,
                    useAstDumpCache ? COMPRESSED_CLANG_DUMP_FILENAME : CLANG_DUMP_FILENAME);
            File dependencyFile = new File(lastWorkingFolder, "clangDump.d");
            int separatorIndex = arguments.indexOf("--");
            if (separatorIndex >= 0) {
                arguments.add(separatorIndex, "-o");
                arguments.add(separatorIndex + 1, dumpFile.getAbsolutePath());
                if (useAstDumpCache) {
                    arguments.add(separatorIndex + 2, "-ast-dump-compression=zstd");
                }
            } else {
                arguments.add("-o");
                arguments.add(dumpFile.getAbsolutePath());
            }

            List<String> command = arguments;
            ClangCcacheAdapter.Invocation ccache = null;
            if (useAstDumpCache) {
                ccache = ClangCcacheAdapter.prepare(parserConfig.get(CodeParser.DUMPER_FOLDER), generatedParseRoot);
                command = ClangCcacheAdapter.command(arguments, dependencyFile);
            }

            ClavaLog.debug("Calling Clang AST Dumper: " + command);

            var processBuilder = new ProcessBuilder(command);
            if (generatedParseRoot != null) {
                processBuilder.directory(generatedParseRoot);
            }
            if (ccache != null) {
                ccache.configureEnvironment(processBuilder.environment());
            }

            output = SpecsSystem.runProcess(processBuilder, SpecsIo::read, SpecsIo::read);

            if (output.isError()) {
                ClavaLog.debug("Dumper returned an error value: '" + output.getReturnValue() + "'");
            }

            // If exception happened while processing output, throw exception
            output.getOutputException().ifPresent(exception -> {
                throw new RuntimeException("Exception while processing the output streams", exception);
            });

            // If console output streaming is disabled, show output only at the end
            if (!output.getStdOut().isBlank()) {
                ClavaLog.info(output.getStdOut());
            }

            if (!output.getStdErr().isBlank()) {
                ClavaLog.info("Clang AST dumper diagnostics:\n" + output.getStdErr());
            }

            if (!dumpFile.isFile()) {
                throw new RuntimeException("Clang AST dumper did not produce '" + dumpFile
                        + "'\nDiagnostics:\n" + output.getStdErr());
            }

            String linesNotParsed;
            try (InputStream fileInput = Files.newInputStream(dumpFile.toPath());
                    InputStream dumpInput = useAstDumpCache ? new ZstdInputStream(fileInput) : fileInput) {
                File unparsedDumpFile = SpecsSystem.isDebug()
                        ? new File(lastWorkingFolder, STDERR_DUMP_FILENAME) : null;
                linesNotParsed = lineStreamParser.parse(dumpInput, unparsedDumpFile);
            }

            parsedData = lineStreamParser.getData();
            parsedData.set(ClangAstData.LINES_NOT_PARSED, linesNotParsed);
            parsedData.set(ClangAstData.HAS_ERRORS, output.isError());

            if (lineStreamParser.hasExceptions()) {
                SpecsLogs.warn("Exceptions happened while parsing the file '" + sourceFile.getAbsolutePath() + "'");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while running Clang AST dumper", e);
        }

        ClangAstParser clangStreamParser = new ClangAstParser(parsedData, SpecsSystem.isDebug(), config);

        TranslationUnit tUnit = clangStreamParser.parseTu(sourceFile);

        parsedData.set(ClangAstData.TRANSLATION_UNIT, tUnit);

        return parsedData;
    }

    private String validateSyntax(List<String> arguments, File sourceFile, String id) {
        try {
            lastWorkingFolder = Files.createTempDirectory("clava_ast_").toFile();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create syntax validation working folder", e);
        }

        var output = SpecsSystem.runProcess(arguments, lastWorkingFolder,
                this::discardOutput,
                inputStream -> processOutput(inputStream));

        output.getOutputException().ifPresent(exception -> {
            throw new RuntimeException("Exception while validating syntax", exception);
        });

        if (output.isError()) {
            return "Syntax validation failed for '" + sourceFile.getAbsolutePath() + "':\n" + output.getStdErr();
        }

        return null;
    }

    private String discardOutput(InputStream inputStream) {
        try (LineStream lines = LineStream.newInstance(inputStream, null)) {
            while (lines.hasNextLine()) {
                lines.nextLine();
            }
        }

        return "";
    }

    private String processOutput(InputStream inputStream) {
        StringBuilder output = new StringBuilder();
        try (LineStream lines = LineStream.newInstance(inputStream, null)) {

            while (lines.hasNextLine()) {
                String nextLine = lines.nextLine();

                // Ignore line about 'invalid argument', it will happen when input source is a header file
                if (streamConsoleOutput) {
                    ClavaLog.info(nextLine);
                }

                output.append(nextLine).append("\n");
            }
        }

        return output.toString();
    }

    private static String pathForCompiler(File sourceFile, File generatedParseRoot) {
        if (generatedParseRoot == null) {
            return sourceFile.getAbsolutePath();
        }

        String relativePath = relativizeIfInside(sourceFile, generatedParseRoot);
        return relativePath == null ? sourceFile.getAbsolutePath() : relativePath;
    }

    private static void relativizeGeneratedPathArguments(List<String> arguments, File generatedParseRoot) {
        for (int i = 0; i < arguments.size(); i++) {
            String argument = arguments.get(i);
            if (argument.equals("-I") || argument.equals("-isystem")) {
                if (i + 1 < arguments.size()) {
                    arguments.set(i + 1, relativizePath(arguments.get(i + 1), generatedParseRoot));
                    i++;
                }
                continue;
            }

            if (argument.startsWith("-I") && argument.length() > 2) {
                arguments.set(i, "-I" + relativizePath(argument.substring(2), generatedParseRoot));
            } else if (argument.startsWith("-isystem=") && argument.length() > "-isystem=".length()) {
                arguments.set(i, "-isystem=" + relativizePath(argument.substring("-isystem=".length()),
                        generatedParseRoot));
            } else if (argument.startsWith("--cuda-path=") && argument.length() > "--cuda-path=".length()) {
                arguments.set(i, "--cuda-path=" + relativizePath(argument.substring("--cuda-path=".length()),
                        generatedParseRoot));
            }
        }
    }

    private static String relativizePath(String path, File generatedParseRoot) {
        String relativePath = relativizeIfInside(new File(path), generatedParseRoot);
        return relativePath == null ? path : relativePath;
    }

    private static String relativizeIfInside(File file, File generatedParseRoot) {
        Path root = generatedParseRoot.toPath().toAbsolutePath().normalize();
        Path absolutePath = file.toPath().toAbsolutePath().normalize();
        if (!absolutePath.startsWith(root)) {
            return null;
        }

        String relativePath = root.relativize(absolutePath).toString();
        return relativePath.isEmpty() ? "." : relativePath;
    }

    private void addCudaPathArgument(List<String> arguments, String cudaPath) {
        var useBuiltinCudaLib = cudaPath.toUpperCase().equals(CodeParser.getBuiltinOption());

        if (useBuiltinCudaLib) {
            File cudaFolder = clangResources.getBuiltinCudaLib();

            ClavaLog.debug("Setting --cuda-path to built-in CUDA folder '"
                    + cudaFolder.getAbsolutePath() + "'");
            arguments.add("--cuda-path=" + cudaFolder.getAbsolutePath());
        } else if (!cudaPath.isBlank()) {
            File cudaFolder = SpecsIo.existingFolder(cudaPath);

            ClavaLog.debug("Setting --cuda-path to folder '" + cudaFolder.getAbsolutePath() + "'");
            arguments.add("--cuda-path=" + cudaFolder.getAbsolutePath());
        }
    }

    /**
     * TODO: Current implementation only shows the last file, show all files
     */
    public String getClangDump() {
        if (lastWorkingFolder == null) {
            SpecsLogs.msgInfo("No working folders found, returning empty clang dump");
            return "";
        }

        File clangDumpFile = new File(lastWorkingFolder, CLANG_DUMP_FILENAME);
        if (!clangDumpFile.isFile()) {
            SpecsLogs.msgInfo("Clang dump file not found: '" + clangDumpFile + "'");
            return "";
        }

        return "ClangDump for '" + lastWorkingFolder.getName() + "':\n" + SpecsIo.read(clangDumpFile);
    }

}
