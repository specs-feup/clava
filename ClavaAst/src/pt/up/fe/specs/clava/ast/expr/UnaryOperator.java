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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorPosition;
import pt.up.fe.specs.util.lazy.Lazy;

/**
 * Represents a unary expression.
 * 
 * @author JoaoBispo
 *
 */
public class UnaryOperator extends Operator {

    /// DATAKEYS BEGIN

    public final static DataKey<UnaryOperatorKind> OP = KeyFactory.enumeration("op", UnaryOperatorKind.class);

    public final static DataKey<UnaryOperatorPosition> POSITION = KeyFactory.enumeration("position",
            UnaryOperatorPosition.class);

    /// DATAKEYS END

    // Hardcoding kind names, in order to avoid breaking code that depends on these names when changing Clang
    // version
    private static final Map<UnaryOperatorKind, String> KIND_NAMES;
    static {
        KIND_NAMES = new HashMap<>();
        KIND_NAMES.put(UnaryOperatorKind.PostInc, "post_inc");
        KIND_NAMES.put(UnaryOperatorKind.PostDec, "post_dec");
        KIND_NAMES.put(UnaryOperatorKind.PreInc, "pre_inc");
        KIND_NAMES.put(UnaryOperatorKind.PreDec, "pre_dec");
        KIND_NAMES.put(UnaryOperatorKind.AddrOf, "addr_of");
        KIND_NAMES.put(UnaryOperatorKind.Deref, "deref");
        KIND_NAMES.put(UnaryOperatorKind.Plus, "plus");
        KIND_NAMES.put(UnaryOperatorKind.Minus, "minus");
        KIND_NAMES.put(UnaryOperatorKind.Not, "not");
        KIND_NAMES.put(UnaryOperatorKind.LNot, "l_not");
        KIND_NAMES.put(UnaryOperatorKind.Real, "real");
        KIND_NAMES.put(UnaryOperatorKind.Imag, "imag");
        KIND_NAMES.put(UnaryOperatorKind.Extension, "extension");
        KIND_NAMES.put(UnaryOperatorKind.Coawait, "cowait");
    }

    private static final Lazy<Map<String, UnaryOperatorKind>> OP_MAP = Lazy.newInstance(UnaryOperator::buildOpMap);

    private static Map<String, UnaryOperatorKind> buildOpMap() {
        Map<String, UnaryOperatorKind> opMap = new HashMap<>();
        for (var entry : KIND_NAMES.entrySet()) {
            opMap.put(entry.getValue(), entry.getKey());
        }

        return opMap;
    }

    public static Map<String, UnaryOperatorKind> getOpMap() {
        return OP_MAP.get();
    }

    public static Optional<UnaryOperatorKind> getOpTry(String opName) {
        return Optional.ofNullable(OP_MAP.get().get(opName));
    }

    public static UnaryOperatorKind getOpByNameOrSymbol(String op) {
        // First, try by name
        UnaryOperatorKind opKind = UnaryOperator.getOpTry(op).orElse(null);

        if (opKind != null) {
            return opKind;
        }

        // If null, try by symbol
        opKind = UnaryOperatorKind.getEnumHelper().fromValueTry(op).orElse(null);

        if (opKind != null) {
            return opKind;
        }

        // If still null, throw exception

        var operators = new ArrayList<>(UnaryOperator.getOpMap().keySet());
        Collections.sort(operators);

        var opBySymbol = UnaryOperatorKind.getEnumHelper().getValuesTranslationMap().keySet().stream()
                .filter(opSym -> !opSym.equals("<UNDEFINED_UNARY_OP>"))
                .collect(Collectors.toList());
        Collections.sort(opBySymbol);

        throw new RuntimeException("unaryOp: operator '" + op + "' is not valid. Available operators by name ("
                + operators + ") and by symbol ("
                + opBySymbol + ")");

    }

    public UnaryOperator(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {

        StringBuilder builder = new StringBuilder();

        // Get code of child
        builder.append(getSubExpr().getCode());
        UnaryOperatorKind opcode = get(OP);
        String code = opcode.getCode();

        switch (get(POSITION)) {
        case PREFIX:
            if (opcode.requiresSpace()) {
                code += " ";
            }

            builder.insert(0, code);
            break;
        case POSTFIX:

            if (opcode.requiresSpace()) {
                code = " " + code;
            }

            builder.append(code);
            break;
        }

        return builder.toString();
    }

    public UnaryOperatorKind getOp() {
        return get(OP);
    }

    public Expr getSubExpr() {
        return getChild(Expr.class, 0);
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
        return getOp().getCode();
    }

}
