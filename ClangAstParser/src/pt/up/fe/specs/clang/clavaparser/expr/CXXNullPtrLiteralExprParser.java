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

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.CXXNullPtrLiteralExpr;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXNullPtrLiteralExprParser extends AClangNodeParser<CXXNullPtrLiteralExpr> {

    public CXXNullPtrLiteralExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected CXXNullPtrLiteralExpr parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // 'nullptr_t'

        Type type = parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());

        checkNoChildren(node);

        return ClavaNodeFactory.cxxNullPtrLiteralExpr(type, node.getInfo());
    }

}
