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
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.expr.LambdaExpr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.data.LambdaExprData;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
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

        checkNumChildren(children, 2); // Not sure if there can be more than 2

        CXXRecordDecl lambdaClass = (CXXRecordDecl) toDecl(children.get(0));
        CompoundStmt body = toCompoundStmt(children.get(1));
        // System.out.println("CHILDREN:" + children);
        return ClavaNodeFactory.lambdaExpr(lambdaExprData, exprData, node.getInfo(), lambdaClass, body);
    }

}
