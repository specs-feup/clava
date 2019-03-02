/**
 * Copyright 2019 SPeCS.
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

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class ArrayInitLoopExpr extends Expr {

    /// DATAKEYS BEGIN

    // public final static DataKey<BigInteger> ARRAY_SIZE = KeyFactory
    // .object("arraySize", BigInteger.class);

    /// DATAKEYS END

    public ArrayInitLoopExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public DeclRefExpr getDeclRef() {
        return getChild(DeclRefExpr.class, 0);
    }

    @Override
    public String getCode() {
        return getDeclRef().getCode();
        // for (ClavaNode node : getChildren()) {
        // System.out.println("CODE: " + node.getCode());
        // }
        // // TODO Auto-generated method stub
        // return super.getCode();
    }
}
