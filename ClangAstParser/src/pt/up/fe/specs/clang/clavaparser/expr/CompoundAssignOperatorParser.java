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
import pt.up.fe.specs.clava.ast.expr.BinaryOperator.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.CompoundAssignOperator;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CompoundAssignOperatorParser extends AClangNodeParser<CompoundAssignOperator> {

    public CompoundAssignOperatorParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected CompoundAssignOperator parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // 'float' lvalue '+=' ComputeLHSTy='float' ComputeResultTy='float'
        // 'int' lvalue '+=' ComputeLHSTy='int' ComputeResultTy='int'

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        // Parse operator kind
        String compountOperator = parser.apply(string -> ClangGenericParsers.parseNested(string, '\'', '\''));
        // Check that there is an '=' at the end of the string
        if (!compountOperator.endsWith("=")) {
            throw new RuntimeException("Expected string '" + compountOperator + "' to end with '='");
        }
        String operatorString = compountOperator.substring(0, compountOperator.length() - 1);
        BinaryOperatorKind op = BinaryOperatorKind.getHelper().valueOf(operatorString);

        List<ClavaNode> children = parseChildren(node);
        checkNumChildren(children, 2);

        // Parse LHS and result types after confirming there are two children

        // Get type of LHS
        parser.apply(string -> ClangGenericParsers.ensureStringStarts(string, "ComputeLHSTy="));
        Type lhsType = parser.apply(ClangGenericParsers::parseClangType, node.getChild(0), getTypesMap());

        // Get type of Result
        parser.apply(string -> ClangGenericParsers.ensureStringStarts(string, "ComputeResultTy="));
        Type resultType = parser.apply(ClangGenericParsers::parseClangType, node.getChild(1), getTypesMap());

        Expr lhs = toExpr(children.get(0));
        Expr rhs = toExpr(children.get(1));

        return ClavaNodeFactory.compoundAssignOperator(lhsType, resultType, op, exprData, node.getInfo(), lhs, rhs);
    }

}
