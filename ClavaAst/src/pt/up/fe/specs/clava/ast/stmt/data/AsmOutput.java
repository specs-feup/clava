/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.clava.ast.stmt.data;

import org.suikasoft.jOptions.DataStore.ADataClass;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ast.expr.Expr;

public class AsmOutput extends ADataClass<AsmOutput> {

    public static final DataKey<Expr> EXPR = KeyFactory.object("expr", Expr.class);
    public static final DataKey<String> CONSTRAINT = KeyFactory.string("constraint");
    public static final DataKey<Boolean> IS_PLUS_CONSTRAINT = KeyFactory.bool("isPlusConstraint");

    public String getCode() {
        var code = new StringBuilder();

        code.append("\"");
        // var prefix = get(IS_PLUS_CONSTRAINT) ? "+" : "=";
        code.append(get(CONSTRAINT));
        code.append("\" (");
        code.append(get(EXPR).getCode());
        code.append(")");

        return code.toString();
    }

}
