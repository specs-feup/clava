/**
 * Copyright 2024 SPeCS.
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

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.ImplicitValueInitExpr;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AImplicitValue;

public class CxxImplicitValue extends AImplicitValue {

    private final ImplicitValueInitExpr implicitValue;

    public CxxImplicitValue(ImplicitValueInitExpr implicitValue) {
        super(new CxxExpression(implicitValue));
        this.implicitValue = implicitValue;
    }

    @Override
    public ClavaNode getNode() {
        return implicitValue;
    }
}
