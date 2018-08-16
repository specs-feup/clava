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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.CXXTypeidExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.data.TypeidData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXTypeidExprParser extends AClangNodeParser<CXXTypeidExpr> {

    public CXXTypeidExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected CXXTypeidExpr parse(ClangNode node, StringParser parser) {
        // Example:
        //
        // 'const class std::type_info' lvalue

        TypeidData typeidData = getStdErr().get(StreamKeys.TYPEID_DATA).get(node.getExtendedId());
        Preconditions.checkNotNull(typeidData, "Expected to find a typeid data");

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        List<ClavaNode> children = parseChildren(node);
        // System.out.println("TYPEID DATA:" + typeidData);
        // System.out.println("CHILDREN:" + children);
        if (typeidData.isTypeOperand()) {
            checkNoChildren(node);
            Type operandType = getOriginalTypes().get(typeidData.getOperandId());
            Preconditions.checkNotNull(operandType);
            typeidData.setOperandType(operandType);
            throw new RuntimeException("deprecated");
            // return ClavaNodeFactory.cxxTypeidExpr(typeidData, exprData, node.getInfo());
        }

        checkNumChildren(children, 1);
        Expr operandExpr = toExpr(children.get(0));
        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.cxxTypeidExpr(typeidData, exprData, node.getInfo(), operandExpr);

    }

}
