/**
 * Copyright 2017 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.enums.UnaryTransformTypeKind;

/**
 * A unary type transform, which is a type constructed from another.
 * 
 * @author JoaoBispo
 *
 */
public class UnaryTransformType extends UnaryTransformTypeBase {

    // Data declarations are generated from the wire schema in UnaryTransformTypeBase.

    public UnaryTransformType(DataStore data, Collection<? extends ClavaNode> children, UnaryTransformTypeBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public UnaryTransformType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Type getBaseType() {
        return getWireBaseType();
    }

    public Optional<Type> getUnderlyingType() {
        return getWireUnderlyingType();
    }

}
