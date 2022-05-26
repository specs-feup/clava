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

package pt.up.fe.specs.clava.extra;

import java.util.Collections;
import java.util.Map;

import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.symja.SymjaPlusUtils;
import pt.up.fe.specs.symja.ast.SymjaAst;

public class ClavaMathExtra {

    public static Expr simplify(Expr expr, Map<String, String> constants) {

        // Expr -> Symja string
        var symjaString = expr.getCode(); // TODO

        // Symplify
        var simplifiedSymja = SymjaPlusUtils.simplify(symjaString, constants);

        // Symja string to Symja AST
        var symjaRoot = SymjaAst.parse(simplifiedSymja);

        System.out.println("Symja AST: " + symjaRoot);

        // Symja AST to Clava AST
        var factory = expr.getFactory();

        // TODO
        var simplifiedClava = factory.integerLiteral(10);

        return simplifiedClava;
    }

    public static Expr simplify(Expr expr) {
        return simplify(expr, Collections.emptyMap());
    }
}
