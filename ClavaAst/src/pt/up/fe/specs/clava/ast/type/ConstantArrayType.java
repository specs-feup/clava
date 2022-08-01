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

package pt.up.fe.specs.clava.ast.type;

import java.math.BigInteger;
import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.util.SpecsMath;

/**
 * Represents a C array with a specified constant size.
 *
 * <p>
 * For multiple dimensions there will be nested {@link ConstantArrayType} nodes, accessible through getElementType().
 * Top-most nodes represent outermost dimensions of the array. E.g., int [3][4] will have dimension [3] as topmost node,
 * them [4], then int.
 *
 * <p>
 * In this regard, the AST is opposite to what Clang does (topmost nodes represent innermost dimensions).
 *
 * @author JoaoBispo
 *
 */
public class ConstantArrayType extends ArrayType {

    /// DATAKEYS BEGIN

    public final static DataKey<BigInteger> ARRAY_SIZE = KeyFactory
            .object("arraySize", BigInteger.class);

    /// DATAKEYS END

    public ConstantArrayType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public int getConstant() {
        return get(ARRAY_SIZE).intValue();
    }

    public int getArraySize() {
        return (int) SpecsMath.multiply(getArrayDims());
    }

    // public List<Integer> getArrayDims() {
    //
    // List<Integer> dims = new ArrayList<>();
    // getArrayDims(dims);
    // return dims;
    // }
    //
    // private void getArrayDims(List<Integer> dims) {
    // dims.add(getConstant());
    //
    // var elementType = getElementType();
    //
    // if (!(elementType instanceof ConstantArrayType)) {
    // return;
    // }
    //
    // ((ConstantArrayType) elementType).getArrayDims(dims);
    // }

    @Override
    protected String getArrayCode() {
        return get(ARRAY_SIZE).toString();
    }

}
