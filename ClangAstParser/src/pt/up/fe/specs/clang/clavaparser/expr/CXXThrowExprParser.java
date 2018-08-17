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

import java.util.List;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.CXXThrowExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXThrowExprParser extends AClangNodeParser<CXXThrowExpr> {

    public CXXThrowExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected CXXThrowExpr parse(ClangNode node, StringParser parser) {
        // Examples:
        //
        // 'void'

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        List<ClavaNode> children = parseChildren(node);

        Expr throwExpr = null;
        if (!children.isEmpty()) {
            throwExpr = SpecsCollections.popSingle(children, Expr.class);
        }

        Preconditions.checkArgument(children.isEmpty());
        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.cxxThrowExpr(exprData, node.getInfo(), throwExpr);
    }

}
