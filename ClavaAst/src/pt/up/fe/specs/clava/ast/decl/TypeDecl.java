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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.TagType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.TypedefType;
import pt.up.fe.specs.clava.utils.Typable;
import pt.up.fe.specs.util.SpecsCheck;

/**
 * Represents a declaration of a type.
 * 
 * @author JoaoBispo
 *
 */
public abstract class TypeDecl extends NamedDecl implements Typable {

    /// DATAKEYS BEGIN

    /**
     * The type associated with this TypeDecl.
     */
    public final static DataKey<Optional<Type>> TYPE_FOR_DECL = KeyFactory.optional("type_for_decl");

    /// DATAKEYS END

    public TypeDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    protected Type processType(Type type) {
        return type == null ? getFactory().nullType() : type.copy();
    }

    @Override
    public Type getType() {
        return get(TYPE_FOR_DECL).orElse(getFactory().nullType());
    }

    @Override
    public void setType(Type type) {
        set(TYPE_FOR_DECL, Optional.of(type));
    }

    @Override
    public ClavaNode copy(boolean keepId, boolean copyChildren) {
        var declCopy = (TypeDecl) super.copy(keepId, copyChildren);
        // System.out.println("TYPE DECL: " + getClass());

        // Each TypeDecl appears to have a corresponding Type node that is linked to this decl
        // If the Decl is copied, the type must be copied too and linked
        // TypeForDecl type is linked to this Decl, also copy the type and replace the Decl
        var type = get(TYPE_FOR_DECL);
        // System.out.println("TYPE FOR DECL: " + type);
        if (type.isPresent()) {
            // System.out.println("TYPE: " + this.getCode());
            // System.out.println("TYPE FOR DECL: " + type.get().getCode());

            // Copy the type
            var typeCopy = type.get().copy(keepId);

            // Set as the type of the decl copy
            declCopy.setType(typeCopy);

            // System.out.println("TYPE AS STRING: " + typeCopy.get(Type.TYPE_AS_STRING));
            // Erase TypeAsString
            typeCopy.set(Type.TYPE_AS_STRING, "<INVALID TYPE_AS_STRING>");
            // System.out.println(
            // "SETTING IN TYPE COPY " + typeCopy.getClass() + " THE DECL COPY " + declCopy.getClass());
            // Link new decl copy
            if (typeCopy instanceof TagType) {
                TagType tagType = (TagType) typeCopy;
                tagType.set(TagType.DECL, declCopy);
                // String newTypeAsString = tagType.getTagKind().getCode() + " " + tagType.getDecl().getDeclName();
                // tagType.set(Type.TYPE_AS_STRING, newTypeAsString);

                // System.out.println("TYPE COPY TYPE AS STRING: " + typeCopy.get(Type.TYPE_AS_STRING));
            } else if (typeCopy instanceof TypedefType) {
                TypedefType typedefType = (TypedefType) typeCopy;

                SpecsCheck.checkArgument(declCopy instanceof TypedefNameDecl,
                        () -> "TypeDecl.copy: expected declCopy to be an instance of TypedefNameDecl, it is "
                                + declCopy.getClass() + " instead");

                typedefType.set(TypedefType.DECL, (TypedefNameDecl) declCopy);
            } else {
                ClavaLog.warning("TypeDecl.copy: not defined when type is " + typeCopy.getClass());
            }

        }

        return declCopy;
    }
}
