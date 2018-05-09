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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.FloatingLiteral;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;

public class FloatingLiteralLegacy extends FloatingLiteral {

    public static enum FloatKind {
        DOUBLE(BuiltinKind.DOUBLE),
        FLOAT(BuiltinKind.LONG),
        LONG_DOUBLE(BuiltinKind.LONG_DOUBLE);

        private final BuiltinKind builtinKind;

        private FloatKind(BuiltinKind builtinKind) {
            this.builtinKind = builtinKind;
        }

        public BuiltinKind getBuiltinKind() {
            return builtinKind;
        }
    }

    private final FloatKind floatKind;
    private final String number;

    public FloatingLiteralLegacy(FloatKind floatKind, String number, ExprData exprData,
            ClavaNodeInfo info) {

        super(exprData, info);

        this.floatKind = floatKind;
        this.number = number;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new FloatingLiteralLegacy(floatKind, number, getExprData(), getInfo());
    }

    /**
     * @return
     */
    /*
    public FloatKind getFloatKind() {
        return floatKind;
    }
    */
    @Override
    public String getLiteral() {
        return number;
    }

    @Override
    public String getCode() {
        return number;
    }

    @Override
    public String toContentString() {
        return super.toContentString() + ", literal:" + number + ", kind:" + floatKind;
    }
}
