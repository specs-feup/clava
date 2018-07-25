/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clang;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import pt.up.fe.specs.clang.ast.JsonUtils;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.properties.SpecsProperty;
import pt.up.fe.specs.util.utilities.BuilderWithIndentation;
import pt.up.fe.specs.util.utilities.StringLines;

public class ClangAstLauncher {

    private static final HashSet<String> C_EXTENSIONS = new HashSet<>(Arrays.asList(
            "c", "cpp", "h", "hpp", "hxx"));

    // private static final String CLANGAST_BINARY_VERSION = "v2.9";
    // private static final String CLANGAST_BINARY_VERSION = "v3.0.17";

    public static void main(String[] args) {
        SpecsProperty.ShowStackTrace.applyProperty("false");
        SpecsSystem.programStandardInit();

        // At least one argument, the file name
        if (args.length < 1) {
            SpecsLogs.msgInfo("Needs at least one argument, the name of the C/C++ file to parse");
            return;
        }

        // List<String> files = new ArrayList<>();
        List<File> files = new ArrayList<>();

        // Add arguments while they are identified as C/C+ files
        int currentIndex = 0;
        while (currentIndex < args.length) {
            String currentArg = args[currentIndex];
            if (isValidFile(currentArg)) {
                // arguments.add(currentArg);
                files.add(new File(currentArg));
                currentIndex++;
            } else {
                break;
            }
        }

        // If there still are arguments left using, pass them after '--'
        List<String> options = Arrays.asList(args).subList(currentIndex, args.length);
        // DataStore config = ClavaOptions.toDataStore(options);

        // Set version of ClangAst
        // config.set(ClangAstKeys.CLANGAST_VERSION, CLANGAST_BINARY_VERSION);

        // boolean useCustomResources = config.get(ClavaOptions.CUSTOM_RESOURCES);
        // ClangRootNode clangAst = new ClangAstParser(false, useCustomResources).parse(files, config);

        App clavaAst = CodeParser.newInstance().parse(files, options);

        // Write AST
        SpecsIo.write(new File("ast_parsed.txt"), clavaAst.toString());

        // Write JSON for the AST
        SpecsIo.write(new File("ast_parsed.json"), JsonUtils.toJson(clavaAst));

        // Write nodes->type map
        // Map<String, String> nodesToTypes = parseNodesToTypes(IoUtils.read("nodetypes.txt"));
        SpecsIo.write(new File("nodes_to_types.json"), toJsonNodeToTypes(SpecsIo.read("nodetypes.txt")));

        // Delete temp files, unless there is a 'debug' file
        if (!new File("debug").isFile()) {
            // Delete ClangAst files
            ClangAstParser.getTempFiles().stream()
                    .forEach(filename -> new File(filename).delete());
        }

        /*
        
        ClangRootNode clangAst = new ClangAstParser().parse(files, config);
        
        // Parse dump information
        try (ClavaParser clavaParser = new ClavaParser(clangAst)) {
        
            App clavaAst = clavaParser.parse();
        
            // Write Clang output
            // IoUtils.write(new File("clang_output.txt"), clangAst.getClangOutput());
        
            // Write AST
            SpecsIo.write(new File("ast_parsed.txt"), clavaAst.toString());
        
            // Write JSON for the AST
            SpecsIo.write(new File("ast_parsed.json"), JsonUtils.toJson(clavaAst));
        
            // Write JSON for the types
            SpecsIo.write(new File("types.json"), toJson(clavaParser.getTypes()));
        
            // Write nodes->type map
            // Map<String, String> nodesToTypes = parseNodesToTypes(IoUtils.read("nodetypes.txt"));
            SpecsIo.write(new File("nodes_to_types.json"), toJsonNodeToTypes(SpecsIo.read("nodetypes.txt")));
        
            // Delete temp files, unless there is a 'debug' file
            if (!new File("debug").isFile()) {
                // Delete ClangAst files
                ClangAstParser.getTempFiles().stream()
                        .forEach(filename -> new File(filename).delete());
            }
        
        } catch (Exception e) {
            // If there is an exception and there is no output error, just show the output and exit
            String message = e.getMessage();
            SpecsLogs.msgInfo(message);
            if (message.startsWith("USAGE")) {
                System.exit(0);
            }
        
            SpecsLogs.msgInfo("\n\nParsing aborted");
            System.exit(1);
        }
        */
    }

    // private static Map<String, String> parseNodesToTypes(String string) {
    // return StringLines.getLines(string).stream()
    // .map(nodeType -> nodeType.split("->"))
    // .collect(Collectors.toMap(ids -> ids[0], ids -> ids[1]));
    // }

    private static String toJsonNodeToTypes(String nodeToTypes) {
        BuilderWithIndentation json = new BuilderWithIndentation();

        json.addLine("{");

        // For each type, create a json entry
        json.increaseIndentation();

        String nodesToTypesJson = StringLines.getLines(nodeToTypes).stream()
                .map(nodeType -> nodeType.split("->"))
                .map(ids -> "\"" + SpecsStrings.escapeJson(ids[0]) + "\": \"" + SpecsStrings.escapeJson(ids[1]) + "\"")
                .collect(Collectors.joining(",\n"));

        json.addLines(nodesToTypesJson);

        json.decreaseIndentation();

        json.addLine("}");

        return json.toString();
    }

    private static String toJson(Map<String, Type> types) {
        BuilderWithIndentation json = new BuilderWithIndentation();

        json.addLine("{");

        // For each type, create a json entry
        json.increaseIndentation();

        String typesJson = types.entrySet().stream()
                .map(ClangAstLauncher::toJson)
                .collect(Collectors.joining(",\n"));

        json.addLines(typesJson);

        json.decreaseIndentation();

        json.addLine("}");

        return json.toString();
    }

    private static String toJson(Entry<String, Type> entry) {
        return "\"" + SpecsStrings.escapeJson(entry.getKey()) + "\": " + JsonUtils.toJson(entry.getValue());
    }

    private static boolean isValidFile(String filename) {
        // Check extension
        String extension = SpecsIo.getExtension(filename);

        if (extension.isEmpty()) {
            return false;
        }

        return C_EXTENSIONS.contains(extension.toLowerCase());
    }
}
