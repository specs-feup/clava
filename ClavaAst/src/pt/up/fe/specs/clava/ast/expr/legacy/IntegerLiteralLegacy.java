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

package pt.up.fe.specs.clava.ast.expr.legacy;

import java.math.BigInteger;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;

/**
 * Represents an implicit type conversion which has no direct representation in the original source code.
 * 
 * @author JoaoBispo
 *
 */
public class IntegerLiteralLegacy extends IntegerLiteral {

    private final String literal;

    public IntegerLiteralLegacy(String literal, ExprData exprData, ClavaNodeInfo info) {
        // According to CPP reference, all literals (except for string literal) are rvalues
        // http://en.cppreference.com/w/cpp/language/value_category
        super(exprData, info);

        this.literal = literal;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new IntegerLiteralLegacy(literal, getExprData(), getInfo());
    }

    @Override
    public String getCode() {

        return getType().getConstantCode(literal);
        // // Check if literal needs "U" or "UL" postfix
        // String unsigned = Types.getUnsignedPostfix(getType());
        // return literal + unsigned;
    }

    @Override
    public String getLiteral() {
        return literal;
    }

    @Override
    public String toContentString() {
        return super.toContentString() + ", literal:" + literal;
    }

    @Override
    public BigInteger getValue() {
        return new BigInteger(literal);
    }

}
