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

import java.util.List;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.CXXDeleteExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class CXXDeleteExprParser extends AClangNodeParser<CXXDeleteExpr> {

    public CXXDeleteExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected CXXDeleteExpr parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // 'void' array Function 0x5496918 'operator delete[]' 'void (void *) noexcept'

        // From Clang documentation:
        // Is this a forced global delete, i.e. "::delete"?
        // bool GlobalDelete : 1;

        Type type = parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());
        boolean isArray = parser.apply(string -> ClangGenericParsers.checkWord(string, "array"));

        parser.apply(string -> ClangGenericParsers.checkWord(string, "Function"));
        Long functionAddress = parser.apply(ClangGenericParsers::parseHex);

        String operator = parser.apply(string -> StringParsers.parseNested(string, '\'', '\''));
        Type functionType = parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());

        Type type2 = getTypesMap().get(node.getExtendedId());
        Preconditions.checkArgument(type2 != null);
        // System.out.println("CLASS DELETE EXPR:" + type2.getClass());

        List<ClavaNode> children = parseChildren(node);

        checkNumChildren(children, 1);
        Expr argument = toExpr(children.get(0));

        return ClavaNodeFactory.cxxDeleteExpr(isArray, functionAddress, operator, functionType, type, node.getInfo(),
                argument);
    }

}
