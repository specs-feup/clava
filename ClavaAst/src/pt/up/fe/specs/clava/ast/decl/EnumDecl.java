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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.EnumType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.TagKind;
import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

/**
 * Represents a declaration of a struct, union, class or enum.
 * 
 * @author JoaoBispo
 *
 */
public class EnumDecl extends TagDecl {

    /// DATAKEYS BEGIN

    public final static DataKey<EnumScopeType> ENUM_SCOPE_KIND = KeyFactory.enumeration("enumScopeKind",
            EnumScopeType.class);

    // public final static DataKey<Type> INTEGER_TYPE = KeyFactory.object("integerType", Type.class);
    public final static DataKey<Optional<Type>> INTEGER_TYPE = KeyFactory.optional("integerType");

    /// DATAKEYS END

    private final static Set<String> DEFAULT_TYPES = new HashSet<>(Arrays.asList("int", "unsigned int"));

    // private final EnumScopeType enumScopeType;
    // private final boolean isModulePrivate;
    // private final Type integerType;
    //
    /*
    public EnumDecl(DataStore data, Collection<? extends EnumConstantDecl> children) {
        super(data, children);
    
        // TODO: THIS IS TEMPORARY!!!!
        this.enumScopeType = EnumScopeType.CLASS;
        this.isModulePrivate = false;
        this.integerType = getFactory().builtinType("int");
    }
    */

    public EnumDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // public EnumDecl(EnumScopeType enumScopeType, String tagName, boolean isModulePrivate, Type integerType,
    // EnumType type, DeclData declData,
    // ClavaNodeInfo info, Collection<? extends EnumConstantDecl> children) {
    //
    // super(TagKind.ENUM, tagName, type, declData, info, children);
    //
    // this.enumScopeType = enumScopeType;
    // this.isModulePrivate = isModulePrivate;
    // this.integerType = integerType;
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new EnumDecl(enumScopeType, getDeclName(), isModulePrivate, getIntegerType(), getEnumType(),
    // getDeclData(), getInfo(), Collections.emptyList());
    // }

    public EnumType getEnumType() {
        return (EnumType) getType();
    }

    public Type getIntegerType() {
        return get(INTEGER_TYPE).orElse(getContext().getFactory().builtinType("int"));
        // return integerType;
    }

    @Override
    public TagKind getTagKind() {
        return TagKind.ENUM;
    }

    @Override
    public String getCode() {
        // if (isModulePrivate) {
        // SpecsLogs.msgWarn("Code generation not implemented when enum is module private: " + getLocation());
        // }
        StringBuilder builder = new StringBuilder();

        builder.append(ln() + "enum ");

        switch (get(ENUM_SCOPE_KIND)) {
        case CLASS:
            builder.append("class ");
            break;
        case STRUCT:
            builder.append("struct ");
            break;
        case NO_SCOPE:
            // Do nothing
        }
        // if (isClass) {
        // builder.append("class ");
        // }
        // System.out.println("QUAL NAME:" + get(QUALIFIED_NAME));
        // System.out.println("DECL NAME:" + getDeclName());
        // System.out.println("TYPE:" + getType().toTree());
        // System.out.println("TYPE CODE:" + getType().getCode(this));
        // builder.append(getDeclName());
        String enumTypeCode = getType().getCode(this);
        // System.out.println("ENUM TYPE CODE: " + enumTypeCode);
        // Check if anonymous
        // if (enumTypeCode.startsWith("enum (anonymous")) {
        // enumTypeCode = "";
        // System.out.println("ENUM TYPE: " + getType());
        // }

        // enumType.get(EnumType.)

        builder.append(enumTypeCode);

        // Add integer type
        addType(getIntegerType(), builder);
        // getEnumType().getUnderlyingType().ifPresent(type -> addType(type, builder));

        builder.append(" {" + ln());

        // Add each enum declaration
        for (ClavaNode child : getChildren()) {
            builder.append(getTab()).append(child.getCode());
            if (child instanceof EnumConstantDecl) {
                builder.append(",");
            }
            builder.append(ln());
        }

        builder.append("};" + ln());

        return builder.toString();
    }

    private void addType(Type type, StringBuilder builder) {
        String typeCode = type.getCode(this);

        // Only append if not a default type
        if (DEFAULT_TYPES.contains(typeCode)) {
            return;
        }

        builder.append(" : ").append(typeCode);
    }

    public List<EnumConstantDecl> getEnumConstants() {
        return getChildrenOf(EnumConstantDecl.class);
    }

    public enum EnumScopeType implements StringProvider {
        CLASS,
        STRUCT,
        NO_SCOPE;

        private static final Lazy<EnumHelperWithValue<EnumScopeType>> ENUM_HELPER = EnumHelperWithValue
                .newLazyHelperWithValue(EnumScopeType.class,
                        NO_SCOPE);

        public static EnumHelperWithValue<EnumScopeType> getEnumHelper() {
            return ENUM_HELPER.get();
        }

        @Override
        public String getString() {
            return name().toLowerCase();
        }

    }

}
