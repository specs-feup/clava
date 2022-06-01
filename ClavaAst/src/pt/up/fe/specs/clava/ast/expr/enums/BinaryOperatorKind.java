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

package pt.up.fe.specs.clava.ast.expr.enums;

import java.util.EnumSet;
import java.util.Set;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum BinaryOperatorKind implements StringProvider {
    PtrMemD,
    PtrMemI,
    Mul,
    Div,
    Rem,
    Add,
    Sub,
    Shl,
    Shr,
    Cmp,
    LT,
    GT,
    LE,
    GE,
    EQ,
    NE,
    And,
    Xor,
    Or,
    LAnd,
    LOr,
    Assign,
    MulAssign,
    DivAssign,
    RemAssign,
    AddAssign,
    SubAssign,
    ShlAssign,
    ShrAssign,
    AndAssign,
    XorAssign,
    OrAssign,
    Comma;

    private static final Set<BinaryOperatorKind> BITWISE = EnumSet.of(And, Or, Xor, Shl, Shr, AndAssign, OrAssign,
            XorAssign, ShlAssign, ShrAssign);

    private static final Set<BinaryOperatorKind> COMPOUND_ASSIGN = EnumSet.of(MulAssign, DivAssign, RemAssign,
            AddAssign, SubAssign, ShlAssign, ShrAssign, AndAssign, XorAssign);

    private static final Lazy<EnumHelperWithValue<BinaryOperatorKind>> HELPER = EnumHelperWithValue
            .newLazyHelperWithValue(BinaryOperatorKind.class);

    public static EnumHelperWithValue<BinaryOperatorKind> getHelper() {
        return HELPER.get();
    }

    public boolean isBitwise() {
        return BITWISE.contains(this);
    }

    public boolean isCompoundAssign() {
        return COMPOUND_ASSIGN.contains(this);
    }

    public boolean isAssign() {
        return isCompoundAssign() || this.equals(Assign);
    }

    // private static final Set<BinaryOperatorKind> IS_ASSIGN = EnumSet.of(ASSIGN, MUL_ASSIGN, DIV_ASSIGN, REM_ASSIGN,
    // ADD_ASSIGN, SUB_ASSIGN, SHL_ASSIGN, SHR_ASSIGN, AND_ASSIGN, XOR_ASSIGN, OR_ASSIGN);

    // private final String opString;
    // private final boolean isAssign;

    // private BinaryOperatorKind(String opString, boolean isAssign) {
    // this.opString = opString;
    // this.isAssign = isAssign;
    // }

    // private BinaryOperatorKind(String opString) {
    // this(opString, false);
    // }

    // private BinaryOperatorKind() {
    // this("<UNDEFINED>");
    // }

    public String getOpString() {
        switch (this) {
        case Mul:
            return "*";
        case Div:
            return "/";
        case Rem:
            return "%";
        case Add:
            return "+";
        case Sub:
            return "-";
        case Shl:
            return "<<";
        case Shr:
            return ">>";
        case Cmp:
            return "<=>";
        case LT:
            return "<";
        case GT:
            return ">";
        case LE:
            return "<=";
        case GE:
            return ">=";
        case EQ:
            return "==";
        case NE:
            return "!=";
        case And:
            return "&";
        case Xor:
            return "^";
        case Or:
            return "|";
        case LAnd:
            return "&&";
        case LOr:
            return "||";
        case Assign:
            return "=";
        case MulAssign:
            return "*=";
        case DivAssign:
            return "/=";
        case RemAssign:
            return "%=";
        case AddAssign:
            return "+=";
        case SubAssign:
            return "-=";
        case ShlAssign:
            return "<<=";
        case ShrAssign:
            return ">>=";
        case AndAssign:
            return "&=";
        case XorAssign:
            return "^=";
        case OrAssign:
            return "|=";
        case Comma:
            return ",";
        case PtrMemD:
            return ".*";
        case PtrMemI:
            return "->*";
        default:
            // Using debug level to not show messages every time the op map is built
            ClavaLog.debug("Code not defined for binary operator '" + this + "'");
            return "<UNDEFINED_BINARY_OP_STRING:" + this + ">";
        }
        // return opString;
    }

    @Override
    public String getString() {
        return getOpString();
    }

}