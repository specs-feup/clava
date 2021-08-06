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
import java.util.stream.Collectors;

import pt.up.fe.specs.clang.ast.JsonUtils;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.properties.SpecsProperty;
import pt.up.fe.specs.util.utilities.BuilderWithIndentation;
import pt.up.fe.specs.util.utilities.StringLines;

/**
 * TODO: Should this class be deprecated? It is a simple launcher, ClavaViewer has overlapping functionality.
 * 
 * @author JBispo
 *
 */
public class ClangAstLauncher {

    private static final HashSet<String> C_EXTENSIONS = new HashSet<>(Arrays.asList(
            "c", "cpp", "h", "hpp", "hxx"));

    public static void main(String[] args) {
        SpecsProperty.ShowStackTrace.applyProperty("false");
        SpecsSystem.programStandardInit();

        // At least one argument, the file name
        if (args.length < 1) {
            SpecsLogs.msgInfo("Needs at least one argument, the name of the C/C++ file to parse");
            return;
        }

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

        App clavaAst = CodeParser.newInstance().parse(files, options);

        // Write AST
        SpecsIo.write(new File("ast_parsed.txt"), clavaAst.toString());

        // Write JSON for the AST
        SpecsIo.write(new File("ast_parsed.json"), JsonUtils.toJson(clavaAst));

        // Write nodes->type map
        SpecsIo.write(new File("nodes_to_types.json"), toJsonNodeToTypes(SpecsIo.read("nodetypes.txt")));
    }

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

    private static boolean isValidFile(String filename) {
        // Check extension
        String extension = SpecsIo.getExtension(filename);

        if (extension.isEmpty()) {
            return false;
        }

        return C_EXTENSIONS.contains(extension.toLowerCase());
    }
}
