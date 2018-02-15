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

package pt.up.fe.specs.clang.clavaparser.expr;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import pt.up.fe.specs.clang.CppParsing;
import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.FloatingLiteral;
import pt.up.fe.specs.clava.ast.expr.FloatingLiteral.FloatKind;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.BuiltinTypeKeyword;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class FloatingLiteralParser extends AClangNodeParser<FloatingLiteral> {

    private static final Pattern FLOATING_LITERAL_CXX = Pattern.compile(
            "(0x(((\\d|[a-f])*\\.(\\d|[a-f])+)|((\\d|[a-f])+\\.?))(p(\\+|\\-)?(\\d|[a-f])+)?(f|l)?)|(((\\d*\\.\\d+)|(\\d+\\.?))(e(\\+|\\-)?\\d+)?(f|l)?)",
            Pattern.CASE_INSENSITIVE);

    private static final Map<BuiltinTypeKeyword, FloatKind> FLOAT_KINDS;
    static {
        FLOAT_KINDS = new HashMap<>();

        FLOAT_KINDS.put(BuiltinTypeKeyword.DOUBLE, FloatKind.DOUBLE);
        FLOAT_KINDS.put(BuiltinTypeKeyword.FLOAT, FloatKind.FLOAT);
        FLOAT_KINDS.put(BuiltinTypeKeyword.LONG_DOUBLE, FloatKind.LONG_DOUBLE);
    }

    public FloatingLiteralParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected FloatingLiteral parse(ClangNode node, StringParser parser) {
        // Examples:
        //
        // 'double' 1.000000e-04
        // 'float' 1.000000e-16

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        FloatKind floatKind = getFloatKind(exprData.getType());
        String dumpLiteral = parser.apply(StringParsers::parseWord);

        String number = getSourceLiteral(node).orElse(dumpLiteral);
        // System.out.println("DUMP FLOAT:" + number);

        // Optional<String> source = location.getSource();

        checkNoChildren(node);

        return ClavaNodeFactory.floatingLiteral(floatKind, number, exprData, info(node));
    }

    private Optional<String> getSourceLiteral(ClangNode node) {
        return CppParsing.getLiteralFromSource(node, getConverter().getFileService(), this::parseFloatingLiteral);
    }

    private String parseFloatingLiteral(String line) {
        // Remove all quotes (') (since C++14)
        String cleanLine = line.replace("'", "");

        // We have two methods for extraction, with a regex and manually with a function
        // The regex is unreadable, but appears to be significantly faster
        String number = SpecsStrings.getRegexGroup(cleanLine, FLOATING_LITERAL_CXX, 0);
        /*
        long javaExtBegin = System.nanoTime();
        String javaExtractor = extractFloat(line);
        long javaExtEnd = System.nanoTime();
        long javaRegexBegin = System.nanoTime();
        String regexExtractor = ParseUtils.getRegexGroup(line, FLOATING_LITERAL_CXX, 0);
        long javaRegexEnd = System.nanoTime();
        
        System.out.println("JAVA NANOS : " + (javaExtEnd - javaExtBegin));
        System.out.println("REGEX NANOS: " + (javaRegexEnd - javaRegexBegin));
        
        if (!javaExtractor.equals(regexExtractor)) {
            System.out.println("JAVA EXTRACTOR:" + javaExtractor);
            System.out.println("REGEX EXTRACTOR:" + regexExtractor);
        }
        
        number = javaExtractor;
        */

        return number;

    }

    private static FloatKind getFloatKind(Type type) {
        BuiltinTypeKeyword keyword = BuiltinTypeKeyword.getHelper().valueOf(type.getCode());

        if (keyword != null) {
            FloatKind kind = FLOAT_KINDS.get(keyword);

            if (kind != null) {
                return kind;
            }
        }

        throw new RuntimeException("Could not determine FLOAT_KIND for type '" + type + "'");
    }

}
