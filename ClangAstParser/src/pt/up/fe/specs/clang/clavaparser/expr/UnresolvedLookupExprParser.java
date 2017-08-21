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

package pt.up.fe.specs.clang.clavaparser.expr;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.UnresolvedLookupExpr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.stringparser.StringParser;

public class UnresolvedLookupExprParser extends AClangNodeParser<UnresolvedLookupExpr> {

    public UnresolvedLookupExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected UnresolvedLookupExpr parse(ClangNode node, StringParser parser) {
        // Examples:
        //
        // '<overloaded function type>' lvalue (ADL) = 'operator+' 0x400ce70 0x3b71110 0x6153d00 0x5fbc770 0x5fbda30
        // 0x6155a00 0x6156660 0x61577c0 0x61144a0 0x61150d0 0x6116400 0x6116e10 0x6117870 0x6118250 0x3bd2f10 0x3bd73b0

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());
        parser.apply(ClangGenericParsers::ensurePrefix, "(");
        // 'double' negative here
        boolean requiresAdl = !parser.apply(ClangGenericParsers::checkWord, "no");
        parser.apply(ClangGenericParsers::ensureWord, "ADL)");
        parser.apply(ClangGenericParsers::ensureWord, "=");

        String name = parser.apply(ClangGenericParsers::parseNested, '\'', '\'');

        boolean noDecls = parser.apply(ClangGenericParsers::checkWord, "empty");
        List<String> decls = noDecls ? Collections.emptyList()
                : parseDecls(parser.apply(ClangGenericParsers::getString), node.getIdSuffix());

        checkNoChildren(node);

        return ClavaNodeFactory.unresolvedLookupExpr(requiresAdl, name, decls, exprData, node.getInfo());
    }

    private List<String> parseDecls(String decls, String suffix) {
        // Split on spaces
        return Arrays.stream(decls.split(" "))
                // Create extended id
                .map(decl -> decl + suffix)
                .collect(Collectors.toList());

    }

}
