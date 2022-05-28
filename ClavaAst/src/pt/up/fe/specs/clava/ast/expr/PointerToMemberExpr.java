/**
 * Copyright 2022 SPeCS.
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

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.type.Type;

/**
 * Helper class to be used internally, to facilitate handling pointer-to-member binary operations.
 * 
 * @author Joao Bispo
 *
 */
public class PointerToMemberExpr extends MemberExpr {

    /**
     * The original binary operator that originated this pseudo-object.
     */
    public static final DataKey<BinaryOperator> ORIGINAL_OPERATOR = KeyFactory.object("originalOperator",
            BinaryOperator.class);

    public static PointerToMemberExpr newInstance(BinaryOperator pointerToMember) {
        var op = pointerToMember.get(BinaryOperator.OP);

        if (op != BinaryOperatorKind.PtrMemD && op != BinaryOperatorKind.PtrMemI) {
            throw new RuntimeException("Expected a pointer-to-member operator, got " + op);
        }

        String memberName = pointerToMember.getRhs().getCode();
        Type memberType = pointerToMember.getRhs().getType();
        Expr baseExpr = pointerToMember.getLhs();
        boolean isArrow = op == BinaryOperatorKind.PtrMemI ? true : false;

        // Type memberType
        // Expr baseExpr
        var pointerToMemberExpr = pointerToMember.getFactory().pointerToMemberExpr(memberName, memberType, baseExpr,
                isArrow);

        pointerToMemberExpr.set(ORIGINAL_OPERATOR, pointerToMember);

        return pointerToMemberExpr;
    }

    public PointerToMemberExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    protected String getSeparatorCode() {
        return get(ORIGINAL_OPERATOR).getOp().getOpString();
    }

    @Override
    public String getCode() {
        // Put parenthesis around, since the operator has less priority than the call ()
        return "(" + super.getCode() + ")";
    }

}
