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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator.UnaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator.UnaryOperatorPosition;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.stringparser.StringParser;

public class UnaryOperatorParser extends AClangNodeParser<UnaryOperator> {

    private static final Map<String, UnaryOperatorKind> BASE_OPCODES;
    static {
        BASE_OPCODES = new HashMap<>();

        UnaryOperatorParser.BASE_OPCODES.put("&", UnaryOperatorKind.ADDR_OF);
        UnaryOperatorParser.BASE_OPCODES.put("*", UnaryOperatorKind.DEREF);
        UnaryOperatorParser.BASE_OPCODES.put("~", UnaryOperatorKind.NOT);
        UnaryOperatorParser.BASE_OPCODES.put("!", UnaryOperatorKind.L_NOT);
        UnaryOperatorParser.BASE_OPCODES.put("+", UnaryOperatorKind.PLUS);
        UnaryOperatorParser.BASE_OPCODES.put("-", UnaryOperatorKind.MINUS);

    }

    private static final Map<String, UnaryOperatorKind> PREFIX_OPCODES;
    static {
        PREFIX_OPCODES = new HashMap<>();

        UnaryOperatorParser.PREFIX_OPCODES.put("++", UnaryOperatorKind.PRE_INC);
        UnaryOperatorParser.PREFIX_OPCODES.put("--", UnaryOperatorKind.PRE_DEC);
    }

    private static final Map<String, UnaryOperatorKind> POSTFIX_OPCODES;
    static {
        POSTFIX_OPCODES = new HashMap<>();

        UnaryOperatorParser.POSTFIX_OPCODES.put("++", UnaryOperatorKind.POST_INC);
        UnaryOperatorParser.POSTFIX_OPCODES.put("--", UnaryOperatorKind.POST_DEC);
    }

    public UnaryOperatorParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public UnaryOperator parse(ClangNode node, StringParser parser) {

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());
        UnaryOperatorPosition position = parser.apply(ClangGenericParsers::parseEnum,
                UnaryOperatorPosition.getEnumHelper());

        // boolean isPrefix = parser.apply(string -> ClangGenericParsers.checkStringStarts(string, "prefix "));
        // boolean isPostfix = parser.apply(string -> ClangGenericParsers.checkStringStarts(string, "postfix "));

        // Check prefix and postfix are the complement of one another
        // Preconditions.checkArgument(isPrefix != isPostfix, "Expected 'isPrefix' to be the opposite of 'isPostfix'");
        List<String> opcodeList = parser.apply(string -> ClangGenericParsers.parsePrimesSeparatedByString(string, ":"));
        Preconditions.checkArgument(opcodeList.size() == 1, "Expecting a single string with the operator");

        // Parse opcode
        String opcodeString = opcodeList.get(0);

        UnaryOperatorKind opcode = parseOpcode(opcodeString, position);

        List<ClavaNode> children = parseChildren(node);

        // Parse sub-expression
        checkNumChildren(children, 1);

        Expr subExpr = toExpr(children.get(0));

        return ClavaNodeFactory.unaryOperator(opcode, position, exprData, info(node), subExpr);
    }

    private static UnaryOperatorKind parseOpcode(String opcodeString, UnaryOperatorPosition position) {

        // Check if prefix/postfix special opcodes
        Map<String, UnaryOperatorKind> positionOperators = getPositionOperators(position);

        UnaryOperatorKind opcode = positionOperators.get(opcodeString);
        if (opcode != null) {
            return opcode;
        }

        // Treat it as a normal operator
        return UnaryOperatorKind.getEnumHelper().valueOf(opcodeString);

        // // Check base opcodes
        // if(position == UnaryOperatorPosition.)
        // UnaryOperatorKind baseOpcode = UnaryOperatorParser.BASE_OPCODES.get(opcodeString);
        // if (baseOpcode != null) {
        // return baseOpcode;
        // }
        //
        //
        //
        // throw new RuntimeException("Case not defined:" + opcodeString);
    }

    private static Map<String, UnaryOperatorKind> getPositionOperators(UnaryOperatorPosition position) {
        switch (position) {
        case PREFIX:
            return UnaryOperatorParser.PREFIX_OPCODES;
        case POSTFIX:
            return UnaryOperatorParser.POSTFIX_OPCODES;
        default:
            throw new RuntimeException("Case not defined:" + position);
        }
    }

}
