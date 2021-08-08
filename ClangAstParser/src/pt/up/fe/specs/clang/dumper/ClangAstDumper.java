/**
 * Copyright 2018 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clang.dumper;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.JOptionsUtils;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.streamparser.LineStreamParser;

import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.ClangResources;
import pt.up.fe.specs.clang.cilk.CilkParser;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clang.parsers.ClangStreamParserV2;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.utils.SourceType;
import pt.up.fe.specs.lang.SpecsPlatforms;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.parsing.arguments.ArgumentsParser;
import pt.up.fe.specs.util.system.ProcessOutput;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * Calls the ClangAstDumper executable and returns the dumped information. Clava AST can be built based on this output.
 * 
 * 
 * @author JoaoBispo
 *
 */
public class ClangAstDumper {

    private final static String CLANG_DUMP_FILENAME = "clangDump.txt";
    private final static String STDERR_DUMP_FILENAME = "stderr.txt";

    private static final List<String> CLANG_AST_DUMPER_TEMP_FILES = Arrays.asList("includes.txt", CLANG_DUMP_FILENAME,
            // "clavaDump.txt", "nodetypes.txt", "types.txt", "is_temporary.txt", "template_args.txt",
            "clavaDump.txt", "nodetypes.txt", "types.txt", "is_temporary.txt",
            "omp.txt", "invalid_source.txt", "enum_integer_type.txt", "consumer_order.txt",
            "types_with_templates.txt");

    public static List<String> getTempFiles() {
        return CLANG_AST_DUMPER_TEMP_FILES;
    }

    /**
     * TODO: Not implemented yet
     * <p>
     * If true, displays the output of the dumper while it executes. If false, stores the output and only shows it after
     * execution.
     * <p>
     * Usually should be disabled when executing several versions of the parser concurrently.
     */
    private final boolean streamConsoleOutput;

    private final List<File> workingFolders;
    private File lastWorkingFolder;
    private File baseFolder;
    private File clangExecutable;
    private List<String> builtinIncludes;
    private int systemIncludesThreshold;

    private final CodeParser parserConfig;

    /**
     * TODO: Replace some of the arguments with reads to CodeParser?
     * 
     * @param dumpStdOut
     * @param useCustomResources
     * @param streamConsoleOutput
     * @param clangExecutable
     * @param builtinIncludes
     * @param parserConfig
     */
    public ClangAstDumper(boolean streamConsoleOutput,
            File clangExecutable, List<String> builtinIncludes, CodeParser parserConfig) {

        this.streamConsoleOutput = streamConsoleOutput;

        this.clangExecutable = clangExecutable;
        this.builtinIncludes = builtinIncludes;

        this.workingFolders = new ArrayList<>();
        this.lastWorkingFolder = null;
        this.baseFolder = null;
        this.systemIncludesThreshold = ParallelCodeParser.SYSTEM_INCLUDES_THRESHOLD.getDefault().get();
        this.parserConfig = parserConfig;
    }

    public File getLastWorkingFolder() {
        return lastWorkingFolder;
    }

    public ClangAstDumper setBaseFolder(File baseFolder) {
        this.baseFolder = baseFolder;
        return this;
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

    private ClangAstData parsePrivate(File sourceFile, String id, Standard standard, DataStore config) {

        ClavaLog.debug(() -> "Data store config for single file parser: " + config);

        DataStore localData = JOptionsUtils.loadDataStore(LocalOptionsKeys.getLocalOptionsFilename(), getClass(),
                LocalOptionsKeys.getProvider().getStoreDefinition());

        List<String> arguments = new ArrayList<>();
        arguments.add(clangExecutable.getAbsolutePath());

        arguments.add(sourceFile.getAbsolutePath());

        arguments.add("-id=" + id);

        arguments.add("-system-header-threshold=" + systemIncludesThreshold);

        arguments.add("--");

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
        else if (isCuda && !standard.isCuda()) {
            arguments.add("-std=cuda");
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
            if (SpecsPlatforms.isWindows()) {
                ClavaLog.info("CUDA parsing is not supported in Windows, run at your own risk");
                arguments.addAll(Arrays.asList("-fms-compatibility", "-D_MSC_VER", "-D_LIBCPP_MSVCRT"));
            }

            arguments.add("--cuda-gpu-arch=" + parserConfig.get(CodeParser.CUDA_GPU_ARCH));

            var cudaPath = parserConfig.get(CodeParser.CUDA_PATH);
            if (!cudaPath.isBlank()) {

                // Check if should use built-in CUDA lib
                File cudaFolder = cudaPath.toUpperCase().equals(CodeParser.getBuiltinOption())
                        ? ClangResources.getBuiltinCudaLib()
                        : SpecsIo.existingFolder(cudaPath);

                ClavaLog.debug("Setting --cuda-path to folder '" + cudaFolder.getAbsolutePath() + "'");
                arguments.add("--cuda-path=" + cudaFolder.getAbsolutePath());
            }

        }
        // If header file, add the language flag (-x) that corresponds to the standard
        else if (SourceType.isHeader(sourceFile)) {
            arguments.add("-x");
            arguments.add(standard.isCxx() ? "c++" : "c");
        }

        List<String> systemIncludes = new ArrayList<>();

        // Add includes bundled with program
        // (only on Windows, it is expected that a Linux system has its own headers for libc/libc++)
        // if (Platforms.isWindows()) {
        // systemIncludes.addAll(clangAstParser.prepareIncludes(clangExecutable, usePlatformLibc));
        systemIncludes.addAll(builtinIncludes);
        // }

        // Add custom includes
        systemIncludes.addAll(localData.get(LocalOptionsKeys.SYSTEM_INCLUDES).getStringList());

        // Add local system includes
        // for (String systemInclude : localData.get(LocalOptionsKeys.SYSTEM_INCLUDES)) {
        for (String systemInclude : systemIncludes) {
            arguments.add("-isystem");
            arguments.add(systemInclude);
        }

        arguments.addAll(ArgumentsParser.newCommandLine().parse(config.get(ClavaOptions.FLAGS)));
        arguments.addAll(config.get(ClavaOptions.FLAGS_LIST));

        ClavaLog.debug(() -> "Calling Clang AST Dumper: " + arguments.stream().collect(Collectors.joining(" ")));

        ClangAstData parsedData = null;
        ProcessOutput<String, ClangAstData> output = null;

        // ProcessOutputAsString output = SpecsSystem.runProcess(arguments, true, false);
        try (LineStreamParser<ClangAstData> lineStreamParser = ClangStreamParserV2
                .newInstance(config.get(ClavaNode.CONTEXT))) {

            if (SpecsSystem.isDebug()) {
                lineStreamParser.getData().set(ClangAstData.DEBUG, true);
            }

            // Create temporary working folder, in order to support running several dumps in parallel
            // File workingFolder = SpecsIo.mkdir(UUID.randomUUID().toString());
            lastWorkingFolder = SpecsIo.mkdir(baseFolder, sourceFile.getName() + "_" + id);

            // Ensure folder is empty
            SpecsIo.deleteFolderContents(lastWorkingFolder);

            workingFolders.add(lastWorkingFolder);

            output = SpecsSystem.runProcess(arguments, lastWorkingFolder,
                    inputStream -> this.processOutput(sourceFile, inputStream),
                    inputStream -> this.processStdErr(inputStream, config.get(ClavaNode.CONTEXT)));

            if (output.isError()) {
                ClavaLog.debug("Dumper returned an error value: '" + output.getReturnValue() + "'");
            }

            // If exception happened while processing output, throw exception
            output.getOutputException().ifPresent(exception -> {
                throw new RuntimeException("Exception while processing the output streams", exception);
            });

            parsedData = output.getStdErr();
            SpecsCheck.checkNotNull(parsedData, () -> "Did not expect error output to be null");
            parsedData.set(ClangAstData.HAS_ERRORS, output.isError());

            // If console output streaming is disabled, show output only at the end
            if (!streamConsoleOutput) {
                ClavaLog.info(output.getStdOut());
            }

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

    private String processOutput(File sourceFile, InputStream inputStream) {
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

    private ClangAstData processStdErr(InputStream inputStream, ClavaContext context) {
        // Create LineStreamParser
        try (LineStreamParser<ClangAstData> lineStreamParser = ClangStreamParserV2.newInstance(context)) {

            // Set debug
            if (SpecsSystem.isDebug()) {
                lineStreamParser.getData().set(ClangAstData.DEBUG, true);
            }

            // Dump file
            File dumpfile = SpecsSystem.isDebug() ? new File(STDERR_DUMP_FILENAME) : null;

            // Parse input stream
            String linesNotParsed = lineStreamParser.parse(inputStream, dumpfile);

            // Add lines not parsed to DataStore
            ClangAstData data = lineStreamParser.getData();
            data.set(ClangAstData.LINES_NOT_PARSED, linesNotParsed);

            // Return data
            return data;
        } catch (Exception e) {
            throw new RuntimeException("Error while parsing output of Clang AST dumper", e);
        }

    }

    /**
     * TODO: Current implementation only shows the last file, show all files
     */
    public String getClangDump() {
        if (workingFolders.isEmpty()) {
            SpecsLogs.msgInfo("No working folders found, returning empty clang dump");
            return "";
        }

        StringBuilder clangDump = new StringBuilder();

        for (File workingFolder : workingFolders) {
            File clangDumpFile = new File(workingFolder, CLANG_DUMP_FILENAME);

            if (!clangDumpFile.isFile()) {
                SpecsLogs.msgInfo("Clang dump file no found: '" + clangDumpFile + "'");
                continue;
            }

            clangDump.append("ClangDump for '" + workingFolder.getName() + "':\n");
            clangDump.append(SpecsIo.read(clangDumpFile));
        }

        return clangDump.toString();
    }

}
