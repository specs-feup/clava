/**
 * Copyright 2017 SPeCS.
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.util.io.FileService;
import pt.up.fe.specs.util.stringparser.ParserWorker;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

/**
 * @deprecated
 * @author JBispo
 *
 */
@Deprecated
public class CppParsing {

    public static String extractFloat(String floatString) {
        StringParser parser = new StringParser(floatString);
        StringBuilder builder = new StringBuilder();

        // Check if hexadecimal
        Optional<String> hexaPrefix = parser.apply(StringParsers::checkStringStarts, "0x", false);
        hexaPrefix.ifPresent(builder::append);

        ParserWorker<Optional<Character>> checker = hexaPrefix.isPresent() ? StringParsers::checkHexDigit
                : StringParsers::checkDigit;

        // Collect all digits
        collectDigits(parser, builder, checker);

        // Collect dot
        parser.apply(StringParsers::checkStringStarts, ".").ifPresent(builder::append);

        // Collect remaining digits
        collectDigits(parser, builder, checker);

        String exponent = hexaPrefix.isPresent() ? "p" : "e";
        Optional<String> realExponent = parser.apply(StringParsers::checkStringStarts, exponent, false);
        if (realExponent.isPresent()) {
            realExponent.ifPresent(builder::append);
            parser.apply(StringParsers::checkCharacter, new HashSet<>(Arrays.asList('+', '-')))
                    .ifPresent(builder::append);

            collectDigits(parser, builder, checker);
        }

        parser.apply(StringParsers::checkCharacter, new HashSet<>(Arrays.asList('f', 'l', 'F', 'L')))
                .ifPresent(builder::append);

        return builder.toString();
    }

    public static void collectDigits(StringParser parser, StringBuilder builder,
            ParserWorker<Optional<Character>> checker) {
        Optional<Character> currentChar = Optional.empty();
        while ((currentChar = parser.apply(checker)).isPresent()) {
            builder.append(currentChar.get());
        }
    }

    public static Optional<String> getLiteralFromSource(ClangNode node, FileService fileService,
            Function<String, String> parser) {

        SourceRange location = node.getLocation();

        // If location is not valid, return
        if (!location.isValid()) {
            return Optional.empty();
        }

        File file = new File(location.getFilepath());
        if (!file.isFile()) {
            // SpecsLogs.msgWarn("Location is considered valid, but is not a file:" + file);
            // Location is considered valid, but is not a file
            return Optional.empty();
        }
        // Preconditions.checkArgument(file.isFile(),
        // "Location is considered valid, but is not a file:" + file);

        // String startLine = StringLines.getLines(file).get(location.getStartLine() - 1);
        String startLine = fileService.getLine(file, location.getStartLine());

        Preconditions.checkArgument(!startLine.isEmpty());

        int startCol = location.getStartCol();

        Preconditions.checkArgument(startCol <= startLine.length(),
                "ID '" + node.getExtendedId() + "' does not have a valid location: " + location);

        String line = startLine.substring(startCol - 1);

        return Optional.of(parser.apply(line));
    }

    public static boolean isExprNodeName(String nodeName) {
        /*
        if (nodeName.endsWith("Type")) {
            return false;
        }
        
        return true;
        */

        if (nodeName.endsWith("Expr")) {
            return true;
        }

        if (nodeName.endsWith("Operator")) {
            return true;
        }

        if (nodeName.endsWith("Literal")) {
            return true;
        }

        if (nodeName.equals("ExprWithCleanups")) {
            return true;
        }

        return false;

    }

    public static boolean isTypeNodeName(String nodeName) {
        if (nodeName.equals("TypeWithKeyword")) {
            return true;
        }

        return nodeName.endsWith("Type");
    }

    public static boolean isDeclNodeName(String nodeName) {
        if (nodeName.equals("CXXRecord")) {
            return true;
        }

        return nodeName.endsWith("Decl");
    }
}
