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

import static pt.up.fe.specs.clava.ast.expr.BinaryOperator.BinaryOperatorKind.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.stringparser.StringParser;

public class BinaryOperatorParser extends AClangNodeParser<BinaryOperator> {

    public BinaryOperatorParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected BinaryOperator parse(ClangNode node, StringParser parser) {
        // Examples:
        //
        // 'double' lvalue '='
        // 'double' '-'
        // '_Bool' '<'
        // 'double (*)(const map<AtomIndex, class Atom>, const vector<class Atom> &, const int)' lvalue '='

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());
        String opString = parser.apply(string -> ClangGenericParsers.parseNested(string, '\'', '\''));
        BinaryOperatorKind op = parseOp(opString);

        List<ClavaNode> children = parseChildren(node);
        checkNumChildren(children, 2);

        Expr lhs = toExpr(children.get(0));
        Expr rhs = toExpr(children.get(1));

        return ClavaNodeFactory.binaryOperator(op, exprData, info(node), lhs, rhs);
    }

    private static final Map<String, BinaryOperatorKind> BINARY_OPERATOR_PARSER;
    static {
        BINARY_OPERATOR_PARSER = new HashMap<>();

        BINARY_OPERATOR_PARSER.put("*", MUL);
        BINARY_OPERATOR_PARSER.put("/", DIV);
        BINARY_OPERATOR_PARSER.put("%", REM);
        BINARY_OPERATOR_PARSER.put("+", ADD);
        BINARY_OPERATOR_PARSER.put("-", SUB);
        BINARY_OPERATOR_PARSER.put("<<", SHL);
        BINARY_OPERATOR_PARSER.put(">>", SHR);
        BINARY_OPERATOR_PARSER.put("<", LT);
        BINARY_OPERATOR_PARSER.put(">", GT);
        BINARY_OPERATOR_PARSER.put("<=", LE);
        BINARY_OPERATOR_PARSER.put(">=", GE);
        BINARY_OPERATOR_PARSER.put("==", EQ);
        BINARY_OPERATOR_PARSER.put("!=", NE);
        BINARY_OPERATOR_PARSER.put("&", AND);
        BINARY_OPERATOR_PARSER.put("|", OR);
        BINARY_OPERATOR_PARSER.put("^", XOR);
        BINARY_OPERATOR_PARSER.put("&&", L_AND);
        BINARY_OPERATOR_PARSER.put("||", L_OR);
        BINARY_OPERATOR_PARSER.put("=", ASSIGN);
        BINARY_OPERATOR_PARSER.put(",", COMMA);
        // PTR_MEM_D,
        // PTR_MEM_I,

        // MUL_ASSIGN,
        // DIV_ASSIGN,
        // REM_ASSIGN,
        // ADD_ASSIGN,
        // SUB_ASSIGN,
        // SHL_ASSIGN,
        // SHR_ASSIGN,
        // AND_ASSIGN,
        // XOR_ASSIGN,
        // OR_ASSIGN,

    }

    private static BinaryOperatorKind parseOp(String opString) {
        BinaryOperatorKind op = BINARY_OPERATOR_PARSER.get(opString);
        if (op != null) {
            return op;
        }

        throw new RuntimeException("Case not defined: " + opString);
    }

}
