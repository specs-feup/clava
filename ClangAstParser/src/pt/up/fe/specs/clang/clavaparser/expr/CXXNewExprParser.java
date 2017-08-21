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

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.data.BareDeclData;
import pt.up.fe.specs.clava.ast.expr.CXXNewExpr;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXNewExprParser extends AClangNodeParser<CXXNewExpr> {

    public CXXNewExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected CXXNewExpr parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // 'char *' array Function 0x50ec688 'operator new[]' 'void *(std::size_t)'
        //

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());
        boolean isGlobal = parser.apply(string -> ClangGenericParsers.checkWord(string, "global"));
        boolean isArray = parser.apply(string -> ClangGenericParsers.checkWord(string, "array"));

        BareDeclData newOperator = null;
        if (!parser.isEmpty()) {
            newOperator = parser.apply(ClangDataParsers::parseBareDecl);
        }
        /*
        // CHECK: Always expects word 'Function'?
        parser.apply(string -> ClangGenericParsers.checkWord(string, "Function"));
        Long newFunctionId = parser.apply(ClangGenericParsers::parseHex);
        
        // CHECK: Allocated type-source information, as written in the source?
        String typeSourceInfo = parser.apply(string -> ClangGenericParsers.parseNested(string, '\'', '\''));
        
        // CHECK: Function type?
        // String ftype = parser.apply(string -> ClangParseWorkers.parseNested(string, '\'', '\''));
        Type ftype = parser.apply(ClangGenericParsers::parseClangType, node, getTypesMap());
        
        Type type2 = getTypesMap().get(node.getExtendedId());
        Preconditions.checkArgument(type2 != null);
        // System.out.println("CLASS NEW EXPR:" + type2.getClass());
        */
        // If not a NullNode, the children are, in order:
        // 1) an expression representing the array size
        // 2) an expression representing the initialization
        // 3) a variable number of placement arguments

        List<ClavaNode> children = parseChildren(node);

        // If array, if first node is the array expression
        Expr arrayExpr = isArray ? toExpr(children.remove(0)) : null;

        // Check if last node is nothrow
        DeclRefExpr nothrow = removeNothrow(children);

        // It might have zero or one child
        checkChildrenBetween(children, 0, 1);

        Expr constructorExpr = children.isEmpty() ? null : toExpr(children.get(0));

        // return ClavaNodeFactory.cxxNewExpr(newFunctionId, typeSourceInfo, ftype, exprData, node.getInfo(),
        return ClavaNodeFactory.cxxNewExpr(isGlobal, isArray, newOperator, exprData, node.getInfo(), arrayExpr,
                constructorExpr, nothrow);
    }

    private static DeclRefExpr removeNothrow(List<? extends ClavaNode> children) {
        if (children.isEmpty()) {
            return null;
        }

        ClavaNode node = SpecsCollections.last(children);
        if (!(node instanceof DeclRefExpr)) {
            return null;
        }

        DeclRefExpr expr = (DeclRefExpr) node;

        // Remove node
        children.remove(children.size() - 1);
        return expr.getRefName().equals("nothrow") ? expr : null;
    }

}
