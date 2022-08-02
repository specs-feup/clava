/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clava.ast.decl.data;

import java.util.Optional;

import org.suikasoft.jOptions.DataStore.ADataClass;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.type.AdjustedType;
import pt.up.fe.specs.clava.ast.type.TagType;
import pt.up.fe.specs.clava.ast.type.TemplateSpecializationType;
import pt.up.fe.specs.clava.ast.type.TemplateTypeParmType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.AccessSpecifier;
import pt.up.fe.specs.clava.utils.Typable;

public class CXXBaseSpecifier extends ADataClass<CXXBaseSpecifier> implements Typable {

    /// DATAKEYS BEGIN

    /**
     * True if this is a virtual base class.
     */
    public final static DataKey<Boolean> IS_VIRTUAL = KeyFactory.bool("isVirtual");

    /**
     * True if this base specifier is a pack expansion.
     */
    public final static DataKey<Boolean> IS_PACK_EXPANSION = KeyFactory.bool("isPackExpansion");

    /**
     * The access specifier as written in the source code.
     */
    public final static DataKey<AccessSpecifier> ACCESS_SPECIFIER_AS_WRITTEN = KeyFactory
            .enumeration("accessSpecifierAsWritten", AccessSpecifier.class);

    /**
     * The actual base specifier, after semantic analysis.
     */
    public final static DataKey<AccessSpecifier> ACCESS_SPECIFIER_SEMANTIC = KeyFactory
            .enumeration("accessSpecifierSemantic", AccessSpecifier.class);

    /**
     * The type of the base class.
     */
    public final static DataKey<Type> TYPE = KeyFactory.object("type", Type.class);

    /// DATAKEYS END

    @Override
    public Type getType() {
        return get(TYPE);
    }

    @Override
    public void setType(Type type) {
        set(TYPE, type);
    }

    @Override
    public Optional<AdjustedType> getAdjustedType() {
        if (!hasValue(ADJUSTED_TYPE)) {
            return Optional.empty();
        }
        return get(ADJUSTED_TYPE);
    }

    @Override
    public void setAdjustedType(AdjustedType type) {
        set(ADJUSTED_TYPE, Optional.of(type));
    }

    /**
     * 
     * @return
     */
    public String getCode(ClavaNode sourceNode) {
        StringBuilder code = new StringBuilder();

        // Add access specifier
        AccessSpecifier specifier = get(ACCESS_SPECIFIER_AS_WRITTEN);
        if (specifier != AccessSpecifier.NONE) {
            code.append(get(ACCESS_SPECIFIER_AS_WRITTEN).getString());
        }

        // Add virtual, if present
        if (get(IS_VIRTUAL)) {
            if (code.length() != 0) {
                code.append(" ");
            }

            code.append("virtual");
        }

        // Add type
        if (code.length() != 0) {
            code.append(" ");
        }

        code.append(get(TYPE).getCode(sourceNode));

        if (get(IS_PACK_EXPANSION)) {
            code.append("...");
        }

        return code.toString();
    }

    /**
     * 
     * @return the declaration of the class of this base
     */
    public Decl getBaseDecl(ClavaNode sourceNode) {
        var classType = get(TYPE).desugarAll();

        // TagType has decl directly available
        if (classType instanceof TagType) {
            return classType.get(TagType.DECL);
        }

        // TemplateSpecializationType has the class name
        if (classType instanceof TemplateSpecializationType) {

            var templateClass = classType.get(TemplateSpecializationType.TEMPLATE_DECL)
                    .orElseThrow(() -> new RuntimeException("Could not find a declaration for the class with name '"
                            + classType.get(TemplateSpecializationType.TEMPLATE_NAME) + "'"));

            // SpecsCheck.checkArgument(templateClass instanceof ClassTemplateDecl,
            // () -> "Expected class to be '" + ClassTemplateDecl.class + "', found '" + templateClass.getClass()
            // + "'");

            return templateClass.getTemplateDecl();
            // var className = classType.get(TemplateSpecializationType.TEMPLATE_NAME);
            // System.out.println("TEMPLATE DECL: " + classType.get(TemplateSpecializationType.TEMPLATE_DECL));
            // var decls = sourceNode.getApp().getDescendantsStream()
            // .filter(node -> node instanceof CXXRecordDecl)
            // .map(record -> (CXXRecordDecl) record)
            // .filter(record -> record.getDeclName().equals(className))
            // .collect(Collectors.toList());
            //
            // SpecsCheck.checkArgument(!decls.isEmpty(),
            // () -> "Could not find a declaration for the class with name '" + className + "'");
            //
            // // Prioritize definition
            // return decls.stream()
            // .filter(record -> record.isCompleteDefinition())
            // .findFirst().orElse(decls.get(0));
        }

        // TemplateTypeParmType has decl directly available
        if (classType instanceof TemplateTypeParmType) {
            return classType.get(TemplateTypeParmType.DECL).get();
        }

        throw new RuntimeException("Not yet implemented for class " + classType.getClass() + ": " + classType);
    }
}
