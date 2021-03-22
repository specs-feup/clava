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

package pt.up.fe.specs.clava.language;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum CXXOperator implements StringProvider {

    NEW("new"),
    DELETE("delete"),
    ARRAY_NEW("new[]"),
    ARRAY_DELETE("delete[]"),
    ADDITION("+"),
    SUBTRACTION("-"),
    MULTIPLICATION("*"),
    DIVISION("/"),
    MODULO("%"),
    BITWISE_XOR("^"),
    BITWISE_AND("&"),
    BITWISE_OR("|"),
    BITWISE_NOT("~"),
    LOGICAL_NOT("!"),
    ASSIGNMENT("="),
    LESS_THAN("<"),
    GREATER_THAN(">"),
    ADDITION_ASSIGNMENT("+="),
    SUBTRACTION_ASSIGNMENT("-="),
    MULTIPLICATION_ASSIGNMENT("*="),
    DIVISION_ASSIGNMENT("/="),
    MODULO_ASSIGNMENT("%="),
    BITWISE_XOR_ASSIGNMENT("^="),
    BITWISE_AND_ASSIGNMENN("&="),
    BITWISE_OR_ASSIGNMENT("|="),
    LEFT_SHIFT("<<"),
    RIGHT_SHIFT(">>"),
    LEFT_SHIFT_ASSIGNMENT("<<="),
    RIGHT_SHIFT_ASSIGNMENT(">>="),
    EQUAL("=="),
    NOT_EQUAL("!="),
    LESS_THAN_OR_EQUAL("<="),
    GREATER_THAN_OR_EQUAL(">="),
    SPACESHIP("<=>"),
    LOGICAL_AND("&&"),
    LOGICAL_OR("||"),
    INCREMENT("++", CXXOperator::getCodeUnary),
    DECREMENT("--", CXXOperator::getCodeUnary),
    COMMA(","),
    MEMBER_SELECTED_BY_POINTER("->*"),
    DEREFERENCE("->", CXXOperator::getDereferenceCode),
    FUNCTION_CALL("()", CXXOperator::getFunctionCallCode),
    SUBSCRIPT("[]", (op, args) -> args.get(0).getCode() + "[" + args.get(1).getCode() + "]");

    // + - * / % ^ & | ~ ! = < > += -= *= /= %= ^= &= |= << >> >>= <<= == != <= >= && || ++ -- , ->* -> ( ) [ ]

    private final String operatorCode;
    private final BiFunction<String, List<Expr>, String> toCode;

    private static final Lazy<EnumHelperWithValue<CXXOperator>> HELPER = EnumHelperWithValue
            .newLazyHelperWithValue(CXXOperator.class);

    public static EnumHelperWithValue<CXXOperator> getHelper() {
        return HELPER.get();
    }

    /**
     * Parses strings representing operators, having into account that they may be prefixed with 'operator'.
     * 
     * @param operator
     * @return
     */
    public static Optional<CXXOperator> parseTry(String operatorString) {
        String workString = operatorString;
        if (workString.startsWith("operator")) {
            workString = workString.substring("operator".length());
        }

        CXXOperator operator = getHelper().getValuesTranslationMap().get(workString);

        return Optional.ofNullable(operator);
    }

    private CXXOperator(String operatorCode) {
        this(operatorCode, CXXOperator::getCodeDefault);
    }

    private CXXOperator(String operatorCode, BiFunction<String, List<Expr>, String> toCode) {
        this.operatorCode = operatorCode;
        this.toCode = toCode;
    }

    /**
     * Returns the operator code (e.g., '+' in case of ADDITION).
     */
    @Override
    public String getString() {
        return operatorCode;
    }

    public String getCode(List<Expr> args) {
        return toCode.apply(operatorCode, args);
    }

    /**
     * Default method for generating code for operators.
     * 
     * @param operator
     * @param args
     * @return
     */
    private static String getCodeDefault(String operator, List<Expr> args) {

        if (args.size() == 1) {
            return operator + args.get(0).getCode();
        }

        if (args.size() == 2) {
            return args.get(0).getCode() + " " + operator + " " + args.get(1).getCode();
        }

        throw new RuntimeException("Not defined for arguments of size " + args.size());
    }

    private static String getFunctionCallCode(String operator, List<Expr> args) {
        Preconditions.checkArgument(!args.isEmpty(), "Expected at least one argument, is empty");

        StringBuilder code = new StringBuilder();

        code.append(args.get(0).getCode());

        String argsCode = SpecsCollections.subList(args, 1).stream()
                .map(arg -> arg.getCode())
                .collect(Collectors.joining(", ", "(", ")"));

        code.append(argsCode);

        return code.toString();

    }

    private static String getCodeUnary(String operator, List<Expr> args) {
        // If size is 1, use usual prefix notation
        if (args.size() == 1) {
            return operator + args.get(0).getCode();
        }

        // If size is 2, this is a sign that we should use postfix notation
        if (args.size() == 2) {

            Preconditions.checkArgument(args.get(1) instanceof IntegerLiteral,
                    "Expected argument 1 to be an IntegerLiteral.");
            Preconditions.checkArgument(((IntegerLiteral) args.get(1)).getValue().equals(BigInteger.ZERO),
                    "Expected value to be zero.");

            return args.get(0).getCode() + operator;
        }

        throw new RuntimeException("Not defined for arguments of size " + args.size());
    }

    private static String getDereferenceCode(String operator, List<Expr> args) {
        Preconditions.checkArgument(args.size() == 1);

        // Argument already puts the ->
        return args.get(0).getCode();
    }
}
