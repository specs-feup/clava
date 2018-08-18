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

package pt.up.fe.specs.clang.clavaparser.expr;

import java.util.List;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.LambdaExpr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.data.LambdaExprData;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.stringparser.StringParser;

public class LambdaExprParser extends AClangNodeParser<LambdaExpr> {

    public LambdaExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected LambdaExpr parse(ClangNode node, StringParser parser) {
        // Examples:
        //
        // 'class (lambda at
        // C:/Users/JoaoBispo/Desktop/shared/repositories-programming/clava/ClangAstParser/temp-clang-ast/sorted_id.h:87:27)'

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());
        LambdaExprData lambdaExprData = getStdErr().get(StreamKeys.LAMBDA_EXPR_DATA).get(node.getExtendedId());

        List<ClavaNode> children = parseChildren(node);

        // First child should be the lambda class
        CXXRecordDecl lambdaClass = (CXXRecordDecl) toDecl(SpecsCollections.popSingle(children, CXXRecordDecl.class));

        // checkNumChildren(children, 2); // Not sure if there can be more than 2

        // Last child should be the body
        CompoundStmt body = toCompoundStmt(SpecsCollections.removeLast(children));
        // System.out.println("CHILDREN:" + children.size());

        // Remaining children should be capture arguments
        List<Expr> captureArguments = toExpr(children);
        throw new RuntimeException("deprecated");
        // System.out.println("CHILDREN:" + children);
        // return ClavaNodeFactory.lambdaExpr(lambdaExprData, exprData, node.getInfo(), lambdaClass, captureArguments,
        // body);
    }

}
