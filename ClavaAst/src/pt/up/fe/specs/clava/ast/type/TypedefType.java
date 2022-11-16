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

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.TypedefNameDecl;
import pt.up.fe.specs.clava.ast.decl.ValueDecl;

/**
 * Represents the type of a typedef.
 * 
 * @author jbispo
 *
 */
public class TypedefType extends Type {

    /// DATAKEYS BEGIN

    public final static DataKey<TypedefNameDecl> DECL = KeyFactory.object("decl", TypedefNameDecl.class);

    /// DATAKEYS END

    public TypedefType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public Type desugar() {
        return get(DECL).getUnderlyingType();
    }

    public Type getTypeClass() {
        Decl decl = get(DECL);

        if (!(decl instanceof ValueDecl)) {
            return null;
        }

        return decl.hasValue(ValueDecl.TYPE) ? decl.get(ValueDecl.TYPE) : null;
    }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {
        String type = get(DECL).getTypelessCode();

        // Inside getCode(), resolve qualified name. "Absolute" namespace can be used
        // This cannot be done... there are cases such as ElaboratedType that calls
        // getNamedType().getCode(), and in some cases the type does not have qualifier
        // (e.g. TemplateSpecializationType) but needs one
        // String type = get(DECL).getCurrentQualifiedName();
        return name == null ? type : type + " " + name;
    }

    // @Override
    // public boolean hasQualifier() {
    // return true;
    // }

}
