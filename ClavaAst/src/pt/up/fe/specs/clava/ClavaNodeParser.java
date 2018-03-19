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

package pt.up.fe.specs.clava;

import java.util.Arrays;
import java.util.List;

import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.pragma.GenericPragma;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.util.utilities.StringLines;

public class ClavaNodeParser {

    /**
     * Parses certain occurrences of code. If could not parse, returns LiteralStmt.
     * 
     * <p>
     * Currently supports: <br>
     * - Single line comments <br>
     * - Pragmas <br>
     * 
     * @param code
     * @return
     */
    public static Stmt parseStmt(String code) {
        List<String> codeLines = StringLines.getLines(code);

        // Check if single line
        if (codeLines.size() != 1) {
            return ClavaNodeFactory.literalStmt(code);
        }

        String currentCode = code.trim();
        String lowerCurrentCode = code.toLowerCase();
        ClavaNodeInfo undefinedInfo = ClavaNodeInfo.undefinedInfo();

        // Inline Comment
        if (lowerCurrentCode.startsWith("//")) {
            return ClavaNodes
                    .toStmt(ClavaNodeFactory.inlineComment(currentCode.substring("//".length()), true, undefinedInfo));
        }

        // Multiline comment
        if (lowerCurrentCode.startsWith("/*") && lowerCurrentCode.endsWith("*/")) {
            String comment = currentCode.substring("/*".length(), currentCode.length() - "*/".length());
            return ClavaNodes.toStmt(ClavaNodeFactory.multiLineComment(Arrays.asList(comment), undefinedInfo));
        }

        // String pragmaPrefix = extractPragmaPrefix(currentCode);
        if (lowerCurrentCode.startsWith("#pragma ")) {
            // String pragmaContent = currentCode.substring("#pragma ".length());

            // Check if OpenMP pragma
            // String pragmaKind = new StringParser(pragmaContent).apply(StringParsers::parseWord);

            // if (pragmaKind.equals("omp")) {
            // return PragmaParsers.parse(pragmaContent, undefinedInfo);
            // }

            // Try to parse pragma. If pragma not parsable, create generic pragma
            // ClavaNode pragmaNode = PragmaParsers.parse(pragmaFullContent, info)
            // .orElse(ClavaNodeFactory.genericPragmaStmt(Arrays.asList(pragmaFullContent), info));

            GenericPragma pragma = ClavaNodeFactory
                    .genericPragmaStmt(Arrays.asList(currentCode.substring("#pragma ".length())), undefinedInfo);
            return ClavaNodes.toStmt(pragma);
        }

        // Case not supported
        return ClavaNodeFactory.literalStmt(code);
    }

    // private static String extractPragmaPrefix(String currentCode) {
    // // TODO Auto-generated method stub
    // return null;
    // }
}
