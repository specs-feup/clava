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

package pt.up.fe.specs.clang.codeparser.clangparser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.JOptionsUtils;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.streamparser.LineStreamParser;

import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.ClangAstParser;
import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.ast.genericnode.ClangRootNode;
import pt.up.fe.specs.clang.ast.genericnode.ClangRootNode.ClangRootData;
import pt.up.fe.specs.clang.astlineparser.AstParser;
import pt.up.fe.specs.clang.clavaparser.ClavaParser;
import pt.up.fe.specs.clang.datastore.LocalOptionsKeys;
import pt.up.fe.specs.clang.includes.ClangIncludes;
import pt.up.fe.specs.clang.parsers.ClangParserKeys;
import pt.up.fe.specs.clang.parsers.ClangStreamParserV2;
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clang.streamparserv2.ClangStreamParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.omp.OMPDirective;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.parsing.arguments.ArgumentsParser;
import pt.up.fe.specs.util.system.ProcessOutput;

/**
 * Implementation of ClangParser that builds a TranslationUnit based on the AST dump of Clang.
 * 
 * <p>
 * This parsed will be deprecated once refactoring of ClavaNodes to DataStore format is done.
 * 
 * @author JoaoBispo
 *
 */
public class AstDumpParser implements ClangParser {

    private int currentId;

    public AstDumpParser() {
        this.currentId = 0;
    }

    private int nextId() {
        // Increment and return
        currentId++;

        return currentId;
    }

    @Override
    public TranslationUnit parse(File file, DataStore config) {

        ClangRootNode clangRootNode = parseSource(file, config);

        // Parse dump information
        try (ClavaParser clavaParser = new ClavaParser(clangRootNode)) {
            return clavaParser.parseTranslationUnit(file);
        } catch (Exception e) {
            throw new RuntimeException("Could not parse file '" + file + "'", e);
        }

        // if (get(SHOW_CLANG_DUMP)) {
        // SpecsLogs.msgInfo("Clang Dump:\n" + SpecsIo.read(new File(ClangAstParser.getClangDumpFilename())));
        // }
        //
        // if (get(SHOW_CLANG_AST)) {
        // SpecsLogs.msgInfo("Clang AST:\n" + ast);
        // }
    }

    private ClangRootNode parseSource(File sourceFile, DataStore config) {

        // Create instance of ClangAstParser
        ClangAstParser clangAstParser = new ClangAstParser();

        DataStore localData = JOptionsUtils.loadDataStore(ClangAstParser.getLocalOptionsFile(), getClass(),
                LocalOptionsKeys.getProvider().getStoreDefinition());

        // Get version for the executable
        String version = config.get(ClangAstKeys.CLANGAST_VERSION);

        // Copy resources
        File clangExecutable = clangAstParser.prepareResources(version);

        List<String> arguments = new ArrayList<>();
        arguments.add(clangExecutable.getAbsolutePath());

        arguments.add(sourceFile.getAbsolutePath());

        int id = nextId();
        arguments.add("-id=" + id);

        arguments.add("--");

        // Add standard
        config.getTry(ClavaOptions.STANDARD).ifPresent(standard -> arguments.add(standard.getFlag()));
        // arguments.add(config.get(ClavaOptions.STANDARD).getFlag());

        List<String> systemIncludes = new ArrayList<>();

        // Add includes bundled with program
        // (only on Windows, it is expected that a Linux system has its own headers for libc/libc++)
        // if (Platforms.isWindows()) {
        systemIncludes.addAll(clangAstParser.prepareIncludes(clangExecutable));
        // }

        // Add custom includes
        systemIncludes.addAll(localData.get(LocalOptionsKeys.SYSTEM_INCLUDES).getStringList());

        // Add local system includes
        // for (String systemInclude : localData.get(LocalOptionsKeys.SYSTEM_INCLUDES)) {
        for (String systemInclude : systemIncludes) {
            arguments.add("-isystem");
            arguments.add(systemInclude);
        }

        // If there still are arguments left using, pass them after '--'
        arguments.addAll(ArgumentsParser.newCommandLine().parse(config.get(ClavaOptions.FLAGS)));

        SpecsLogs.msgInfo("Calling Clang AST Dumper: " + arguments.stream().collect(Collectors.joining(" ")));

        ClavaContext context = new ClavaContext();

        // Add context to config
        config.add(ClavaNode.CONTEXT, context);

        // ProcessOutputAsString output = SpecsSystem.runProcess(arguments, true, false);
        LineStreamParser lineStreamParser = ClangStreamParserV2.newInstance(context);
        if (SpecsSystem.isDebug()) {
            lineStreamParser.getData().set(ClangParserKeys.DEBUG, true);
        }

        ProcessOutput<List<ClangNode>, DataStore> output = SpecsSystem.runProcess(arguments,
                clangAstParser::processOutput,
                inputStream -> clangAstParser.processStdErr(config, inputStream, lineStreamParser));

        // Error output has information about types, separate this information from the warnings
        DataStore stderr = output.getStdErr();

        ClangStreamParser clangStreamParser = new ClangStreamParser(lineStreamParser.getData(), SpecsSystem.isDebug());
        App newApp = clangStreamParser.parse();

        if (SpecsSystem.isDebug()) {
            // App newApp = new ClangStreamParser(stderr).parse();
            System.out.println("NEW APP CODE:\n" + newApp.getCode());
        }

        String warnings = stderr.get(StreamKeys.WARNINGS);

        // Throw exception if there as any error
        if (output.getReturnValue() != 0) {
            throw new RuntimeException("There where errors during dumping:\n" + warnings);
        }

        // Check if there are error messages
        if (!warnings.isEmpty()) {
            SpecsLogs.msgInfo("There where warnings during dumping:\n" + warnings);
        }

        // Check if there where no interleaved executions
        // ClangAstParser.checkInterleavedExecutions();

        // Get clang output
        // String clangOutput = output.getStdOut();

        // Always write to a file, to be able to check dump
        // IoUtils.write(new File("clangDump.txt"), clangOutput);
        SpecsIo.write(new File("types.txt"), stderr.get(StreamKeys.TYPES));
        // System.out.println("OUTPUT:\n" + output.getOutput());

        // Parse Clang output
        // List<ClangNode> clangDump = new AstParser().parse(output.getStdOut());
        List<ClangNode> clangDump = output.getStdOut();

        // Add Source Ranges
        ClangAstParser.addSourceRanges(clangDump, stderr);

        // Parse Clang types
        List<ClangNode> clangTypes = ClangAstParser
                .removeDuplicates(new AstParser().parse(stderr.get(StreamKeys.TYPES)));

        ClangAstParser.addSourceRanges(clangTypes, stderr);

        // Parse node-types map
        // This file includes several repetitions and even addresses that will not appear
        // in the dump. However, all nodes in the dump (clangDump and types) should be
        // represented in the map.
        // Map<String, String> nodeToTypes = parseNodeToTypes(IoUtils.read(new File("nodetypes.txt")));
        Map<String, String> nodeToTypes = ClangAstParser.parseAddrToAddr(SpecsIo.read(new File("nodetypes.txt")));

        // Remove top-level nodes that do not have a corresponding type
        // clangDump = cleanup(clangDump, nodeToTypes);

        // Make sure all nodes in clangDump have a corresponding type
        // check(clangDump, clangTypes, nodeToTypes);

        // Parse includes
        ClangIncludes includes = ClangIncludes.newInstance(SpecsIo.existingFile("includes.txt"));

        // Get nodes that can have template arguments
        // Set<String> hasTemplateArguments = parseTemplateArguments(SpecsIo.read("template_args.txt"));

        // Get nodes that are temporary
        Set<String> isTemporary = ClangAstParser.parseIsTemporary(SpecsIo.read("is_temporary.txt"));

        // Get OpenMP directives
        // Map<String, OMPDirective> ompDirectives = parseOmpDirectives(IoUtils.read("omp.txt"));
        Map<String, OMPDirective> ompDirectives = new HashMap<>();

        // Get enum integer types
        Map<String, String> enumToIntegerType = ClangAstParser
                .parseEnumIntegerTypes(SpecsIo.read("enum_integer_type.txt"));

        // Check if no new nodes should be used
        // Map<String, ClavaNode> newNodes = disableNewParsingMethod ? new HashMap<>()
        // : lineStreamParser.getData().get(ClangParserKeys.CLAVA_NODES);
        Map<String, ClavaNode> newNodes = lineStreamParser.getData().get(ClangParserKeys.CLAVA_NODES);

        ClangRootData clangRootData = new ClangRootData(config, includes, clangTypes, nodeToTypes,
                isTemporary, ompDirectives, enumToIntegerType, stderr,
                newNodes);

        return new ClangRootNode(clangRootData, clangDump);
    }

}
