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

package pt.up.fe.specs.clava.ast.expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.util.lazy.Lazy;

/**
 * Represents a builtin binary operation expression.
 *
 *
 * @author JoaoBispo
 *
 */
public class BinaryOperator extends Operator {

    /// DATAKEYS BEGIN

    public final static DataKey<BinaryOperatorKind> OP = KeyFactory.enumeration("op", BinaryOperatorKind.class);

    /// DATAKEYS END

    // Hardcoding kind names, in order to avoid breaking code that depends on these names when changing Clang
    // version
    private static final Map<BinaryOperatorKind, String> KIND_NAMES;
    static {
        KIND_NAMES = new HashMap<>();
        KIND_NAMES.put(BinaryOperatorKind.PtrMemD, "ptr_mem_d");
        KIND_NAMES.put(BinaryOperatorKind.PtrMemI, "ptr_mem_i");
        KIND_NAMES.put(BinaryOperatorKind.Mul, "mul");
        KIND_NAMES.put(BinaryOperatorKind.Div, "div");
        KIND_NAMES.put(BinaryOperatorKind.Rem, "rem");
        KIND_NAMES.put(BinaryOperatorKind.Add, "add");
        KIND_NAMES.put(BinaryOperatorKind.Sub, "sub");
        KIND_NAMES.put(BinaryOperatorKind.Shl, "shl");
        KIND_NAMES.put(BinaryOperatorKind.Shr, "shr");
        KIND_NAMES.put(BinaryOperatorKind.Cmp, "cmp");
        KIND_NAMES.put(BinaryOperatorKind.LT, "lt");
        KIND_NAMES.put(BinaryOperatorKind.GT, "gt");
        KIND_NAMES.put(BinaryOperatorKind.LE, "le");
        KIND_NAMES.put(BinaryOperatorKind.GE, "ge");
        KIND_NAMES.put(BinaryOperatorKind.EQ, "eq");
        KIND_NAMES.put(BinaryOperatorKind.NE, "ne");
        KIND_NAMES.put(BinaryOperatorKind.And, "and");
        KIND_NAMES.put(BinaryOperatorKind.Xor, "xor");
        KIND_NAMES.put(BinaryOperatorKind.Or, "or");
        KIND_NAMES.put(BinaryOperatorKind.LAnd, "l_and");
        KIND_NAMES.put(BinaryOperatorKind.LOr, "l_or");
        KIND_NAMES.put(BinaryOperatorKind.Assign, "assign");
        KIND_NAMES.put(BinaryOperatorKind.MulAssign, "mul_assign");
        KIND_NAMES.put(BinaryOperatorKind.DivAssign, "div_assign");
        KIND_NAMES.put(BinaryOperatorKind.RemAssign, "rem_assign");
        KIND_NAMES.put(BinaryOperatorKind.AddAssign, "add_assign");
        KIND_NAMES.put(BinaryOperatorKind.SubAssign, "sub_assign");
        KIND_NAMES.put(BinaryOperatorKind.ShlAssign, "shl_assign");
        KIND_NAMES.put(BinaryOperatorKind.ShrAssign, "shr_assign");
        KIND_NAMES.put(BinaryOperatorKind.AndAssign, "and_assign");
        KIND_NAMES.put(BinaryOperatorKind.XorAssign, "xor_assign");
        KIND_NAMES.put(BinaryOperatorKind.OrAssign, "or_assign");
        KIND_NAMES.put(BinaryOperatorKind.Comma, "comma");

    }

    /**
     * Operators that when generating code, do not add spaces between operands
     */
    private static final Set<BinaryOperatorKind> NO_SPACES = EnumSet.of(BinaryOperatorKind.PtrMemD,
            BinaryOperatorKind.PtrMemI);

    private static final Lazy<Map<String, BinaryOperatorKind>> OP_MAP = Lazy.newInstance(BinaryOperator::buildOpMap);

    private static Map<String, BinaryOperatorKind> buildOpMap() {
        Map<String, BinaryOperatorKind> opMap = new HashMap<>();
        for (var entry : KIND_NAMES.entrySet()) {
            opMap.put(entry.getValue(), entry.getKey());
        }

        return opMap;
    }

    public static Map<String, BinaryOperatorKind> getOpMap() {
        return OP_MAP.get();
    }

    public static Optional<BinaryOperatorKind> getOpTry(String opName) {
        return Optional.ofNullable(OP_MAP.get().get(opName));
    }

    public static BinaryOperatorKind getOpByNameOrSymbol(String op) {
        // First, try by name
        BinaryOperatorKind opKind = BinaryOperator.getOpTry(op).orElse(null);

        if (opKind != null) {
            return opKind;
        }

        // If null, try by symbol
        opKind = BinaryOperatorKind.getHelper().fromValueTry(op).orElse(null);

        if (opKind != null) {
            return opKind;
        }

        // If still null, throw exception

        var operators = new ArrayList<>(BinaryOperator.getOpMap().keySet());
        Collections.sort(operators);

        var opBySymbol = BinaryOperatorKind.getHelper().getValuesTranslationMap().keySet().stream()
                .filter(opSym -> !opSym.equals("<UNDEFINED_BINARY_OP>"))
                .collect(Collectors.toList());
        Collections.sort(opBySymbol);

        throw new RuntimeException("binaryOp: operator '" + op + "' is not valid. Available operators by name ("
                + operators + ") and by symbol ("
                + opBySymbol + ")");

    }

    public BinaryOperator(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Expr getLhs() {
        return getChild(Expr.class, 0);
    }

    public Expr getRhs() {
        return getChild(Expr.class, 1);
    }

    public Expr setLhs(Expr newLhs) {
        return (Expr) setChild(0, newLhs);
    }

    public Expr setRhs(Expr newRhs) {
        return (Expr) setChild(1, newRhs);
    }

    public BinaryOperatorKind getOp() {
        return get(OP);
    }

    @Override
    public String getCode() {
        var addSpaces = !NO_SPACES.contains(get(OP));
        StringBuilder opCode = new StringBuilder();

        opCode.append(getLhs().getCode());

        if (addSpaces) {
            opCode.append(" ");
        }

        opCode.append(get(OP).getOpString());

        if (addSpaces) {
            opCode.append(" ");
        }

        opCode.append(getRhs().getCode());

        return opCode.toString();
    }

    @Override
    public boolean isBitwise() {
        return getOp().isBitwise();
    }

    @Override
    public String getKindName() {
        String kindName = KIND_NAMES.get(getOp());
        if (kindName == null) {
            throw new RuntimeException("Not implemented for " + getOp());
        }

        return kindName;
    }

    @Override
    public String getOperatorCode() {
        return getOp().getOpString();
    }

}
