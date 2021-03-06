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

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.util.SpecsStrings;

public class FloatingLiteral extends Literal {

    /// DATAKEYS BEGIN

    public final static DataKey<Double> VALUE = KeyFactory.double64("value");

    /// DATAKEYS END

    private static final double ERROR_THRESHOLD = 1e-6;

    public FloatingLiteral(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);

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

        // Because of the parsing problems, approximate value is "closer" to the truth than literal
        double approximateValue = get(VALUE);

        // If approximate value is zero, return literal
        if (approximateValue == 0.0) {
            return literal;
        }

        // Calculate relative diff
        double diff = Math.abs((parsedDouble - approximateValue)) / approximateValue;

        // There is no difference, return literal
        if (diff == 0.0) {
            return literal;
        }

        // Difference is below threshold, return literal
        if (diff < ERROR_THRESHOLD) {
            return literal;
        }

        // Return approximate value
        return Double.toString(approximateValue);

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
