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

import java.util.Collection;
import java.util.Collections;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.data2.FloatingLiteralData;
import pt.up.fe.specs.util.SpecsStrings;

public class FloatingLiteral extends Literal {

    /// DATAKEYS BEGIN

    public final static DataKey<Double> VALUE = KeyFactory.double64("value");

    /// DATAKEYS END

    private static final double ERROR_THRESHOLD = 1e-6;

    public FloatingLiteral(FloatingLiteralData data, Collection<? extends ClavaNode> children) {
        super(data, children);

    }

    /**
     * Legacy support.
     * 
     * @param floatKind
     * @param number
     * @param exprData
     * @param info
     */
    public FloatingLiteral(ExprData exprData, ClavaNodeInfo info) {
        super(exprData, info, Collections.emptyList());
    }

    @Override
    public FloatingLiteralData getData() {
        return (FloatingLiteralData) super.getData();
    }

    @Override
    public String getCode() {
        // Current parser (Clang 3.8) does not correctly return the literal of FloatingLiterals
        // when they use C++14 single quotes. As a temporary fix, tries to measure the difference
        // between the value of the literal and the approximate value given by Clang, and if
        // above a certain threshold, returns the approximate value

        String literal = getLiteral();
        Double parsedDouble = SpecsStrings.parseDouble(literal, false);

        // Could not parse, return literal
        if (parsedDouble == null) {
            return literal;
            // System.out.println("COULD NOT PARSE: " + literal);
        }

        double diff = Math.abs((parsedDouble - getData().getValue()));

        // There is no difference, return literal
        if (diff == 0.0) {
            return literal;
        }

        // Difference is below threshold, return literal
        if (diff < ERROR_THRESHOLD) {
            return literal;
        }

        // Return approximate value
        return Double.toString(getData().getValue());

        // System.out.println("LITERAL VALUE:" + getLiteral());
        // System.out.println("APPROXIMATE VALUE:" + getData().getValue());
        // System.out.println("DIFF:" + diff);
        // System.out.println("ULP LITERAL:" + Math.ulp(parsedDouble));
        // System.out.println("ULP APPROXIMATE:" + Math.ulp(getData().getValue()));

    }

    /**
     * @return
     */
    /*
    public FloatKind getFloatKind() {
        return floatKind;
    }
    */

}
