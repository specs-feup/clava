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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.EnumType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.TagKind;

/**
 * Represents a declaration of a struct, union, class or enum.
 * 
 * @author JoaoBispo
 *
 */
public class EnumDecl extends TagDecl {

    private final static Set<String> DEFAULT_TYPES = new HashSet<>(Arrays.asList("int", "unsigned int"));

    private final boolean isClass;
    private final Type integerType;

    public EnumDecl(boolean isClass, String tagName, Type integerType, EnumType type, ClavaNodeInfo info,
            Collection<? extends EnumConstantDecl> children) {

        super(TagKind.ENUM, tagName, type, info, children);

        this.isClass = isClass;
        this.integerType = integerType;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new EnumDecl(isClass, getDeclName(), getIntegerType(), getEnumType(), getInfo(),
                Collections.emptyList());
    }

    public EnumType getEnumType() {
        return (EnumType) getType();
    }

    public Type getIntegerType() {
        return integerType;
    }

    @Override
    public TagKind getTagKind() {
        return TagKind.ENUM;
    }

    @Override
    public String getCode() {
        StringBuilder builder = new StringBuilder();

        builder.append(ln() + "enum ");

        if (isClass) {
            builder.append("class ");
        }

        builder.append(getDeclName());

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

    private static void addType(Type type, StringBuilder builder) {
        String typeCode = type.getCode();

        // Only append if not a default type
        if (DEFAULT_TYPES.contains(typeCode)) {
            return;
        }

        builder.append(" : ").append(typeCode);
    }

    public List<EnumConstantDecl> getEnumConstants() {
        return getChildrenOf(EnumConstantDecl.class);
    }

}
