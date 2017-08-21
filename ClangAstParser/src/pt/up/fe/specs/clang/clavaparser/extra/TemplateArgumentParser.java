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

package pt.up.fe.specs.clang.clavaparser.extra;

import java.util.List;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clava.parser.DelayedParsingExpr;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.TemplateArgument;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class TemplateArgumentParser extends AClangNodeParser<TemplateArgument> {

    public TemplateArgumentParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected TemplateArgument parse(ClangNode node, StringParser parser) {
        // Examples:
        //
        // type 'struct std::char_traits<char>':'struct std::char_traits<char>'
        // 'char':'char'

        // Obtain first word, to determine which kind of TemplateArgument is
        String kind = parser.apply(StringParsers::parseWord);

        switch (kind) {
        case "type":
            return parseType(node, parser);
        case "expr":
            return parseExpr(node, parser);
        default:
            throw new RuntimeException("Case not defined: " + kind);
        }

    }

    private TemplateArgument parseType(ClangNode node, StringParser parser) {

        // TemplateArguments to not have an address/id in the dump, cannot associate to a type
        // Type type = parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());
        List<String> type = parser.apply(ClangGenericParsers::parsePrimesSeparatedByString, ":");
        checkNoChildren(node);

        // System.out.println("TEMP ARG ID:" + node.getExtendedId());
        // System.out.println("TEMPLATE ARG:" + node.getExtendedId());

        return ClavaNodeFactory.templateArgumentType(type, node.getInfo());

    }

    private TemplateArgument parseExpr(ClangNode node, StringParser parser) {
        // No contents
        Preconditions.checkArgument(parser.isEmpty(), "Expected parser to be empty");

        // One expression child, delay parsing
        checkNumChildren(node.getChildren(), 1);

        ClangNode exprNode = node.getChild(0);

        Expr expr = new DelayedParsingExpr(exprNode);

        return ClavaNodeFactory.templateArgumentExpr(expr, node.getInfo());
    }

}
