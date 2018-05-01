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

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.data2.IntegerLiteralData;

/**
 * Represents an implicit type conversion which has no direct representation in the original source code.
 * 
 * @author JoaoBispo
 *
 */
public class IntegerLiteral extends Literal {

    /// DATAKEYS BEGIN

    public final static DataKey<BigInteger> VALUE = KeyFactory.bigInteger("value");

    /// DATAKEYS END

    // private final String literal;

    public IntegerLiteral(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /**
     * For legacy support.
     * 
     * @param exprData
     * @param info
     */
    protected IntegerLiteral(ExprData exprData, ClavaNodeInfo info) {
        // According to CPP reference, all literals (except for string literal) are rvalues
        // http://en.cppreference.com/w/cpp/language/value_category
        super(exprData, info, Collections.emptyList());
    }

    @Override
    public IntegerLiteralData getData() {
        return (IntegerLiteralData) super.getData();
    }

    public BigInteger getValue() {
        return get(VALUE);
    }

}
