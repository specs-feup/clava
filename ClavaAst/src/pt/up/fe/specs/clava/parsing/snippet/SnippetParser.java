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

package pt.up.fe.specs.clava.parsing.snippet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.parsing.pragma.PragmaParsers;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;
import pt.up.fe.specs.util.utilities.StringLines;

/**
 * Parses small amounts of code.
 * 
 * @author JoaoBispo
 *
 */
public class SnippetParser {

    private final ClavaContext context;

    public SnippetParser(ClavaContext context) {
        this.context = context;
    }

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
    public Stmt parseStmt(String code) {
        List<String> codeLines = StringLines.getLines(code);

        // Check if single line
        if (codeLines.size() != 1) {
            context.getFactory().literalStmt(code);
            // return ClavaNodeFactory.literalStmt(code);
        }

        String currentCode = code.trim();
        String lowerCurrentCode = code.toLowerCase();
        // ClavaNodeInfo undefinedInfo = ClavaNodeInfo.undefinedInfo();

        // Inline Comment
        if (lowerCurrentCode.startsWith("//")) {
            return ClavaNodes
                    .toStmt(context.getFactory().inlineComment(currentCode.substring("//".length()), true));
            // .toStmt(ClavaNodeFactory.inlineComment(currentCode.substring("//".length()), true, undefinedInfo));
        }

        // Multiline comment
        if (lowerCurrentCode.startsWith("/*") && lowerCurrentCode.endsWith("*/")) {
            String comment = currentCode.substring("/*".length(), currentCode.length() - "*/".length());
            // return ClavaNodes.toStmt(ClavaNodeFactory.multiLineComment(Arrays.asList(comment), undefinedInfo));
            return ClavaNodes.toStmt(context.getFactory().multiLineComment(Arrays.asList(comment)));
        }

        // String pragmaPrefix = extractPragmaPrefix(currentCode);
        if (lowerCurrentCode.startsWith("#")) {
            String pragmaContent = currentCode.substring("#".length());
            pragmaContent = pragmaContent.stripLeading();
            if (pragmaContent.startsWith("pragma ")) {
                pragmaContent = pragmaContent.substring("pragma ".length());

                // Split pragmaContent into lines, in case it is multiline
                List<String> pragmaLines = pragmaContent.lines()
                        .map(SnippetParser::removeTrailingBackslash)
                        .collect(Collectors.toList());
                Pragma parsedPragma = PragmaParsers.parse(pragmaLines, context)
                        .orElse(context.getFactory().genericPragma(pragmaLines));
                return ClavaNodes.toStmt(parsedPragma);
                /*
                // Get pragma kind
                StringParser parser = new StringParser(pragmaContent);
                String pragmaKind = parser.apply(StringParsers::parseWord);
                
                // Check if OpenMP pragma
                if (pragmaKind.equals("omp")) {
                return ClavaNodes.toStmt(new OmpParser().parse(parser, context));
                }
                
                GenericPragma pragma = context.getFactory().genericPragma(Arrays.asList(pragmaContent));
                return ClavaNodes.toStmt(pragma);
                */
            }
        }

        if (lowerCurrentCode.startsWith("_pragma")) {
            String pragmaContent = currentCode.substring("_pragma".length());

            StringParser parser = new StringParser(pragmaContent.strip());

            parser.apply(StringParsers::parseString, "(");
            String pragmaFullContent = parser.apply(StringParsers::parseDoubleQuotedString);
            parser.apply(StringParsers::parseString, ")");

            return parseStmt("#pragma " + pragmaFullContent);
        }

        // Case not supported
        return context.getFactory().literalStmt(code);
    }

    // private static String extractPragmaPrefix(String currentCode) {
    // // TODO Auto-generated method stub
    // return null;
    // }

    private static String removeTrailingBackslash(String line) {
        if (!line.stripTrailing().endsWith("\\")) {
            return line;
        }

        int backslashIndex = line.lastIndexOf('\\');
        return line.substring(0, backslashIndex);
    }
}
