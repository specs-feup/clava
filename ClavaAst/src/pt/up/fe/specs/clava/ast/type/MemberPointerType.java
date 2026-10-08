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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.type;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class MemberPointerType extends Type {

    /// DATAKEYS BEGIN

    public static final DataKey<Type> CLASS_TYPE = KeyFactory.object("classType", Type.class);
    public static final DataKey<Type> POINTEE_TYPE = KeyFactory.object("pointeeType", Type.class);

    /// DATAKEYS END

    public MemberPointerType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Type getClassType() {
        return get(CLASS_TYPE);
    }

    public Type getPointeeType() {
        return get(POINTEE_TYPE);
    }

    /** Replaces the class type used by source-level member-pointer printing. */
    public void setClassType(Type classType) {
        set(CLASS_TYPE, classType);
    }

    /** Replaces the pointee type used by source-level member-pointer printing. */
    public void setPointeeType(Type pointeeType) {
        set(POINTEE_TYPE, pointeeType);
    }

    @Override
    public String getCode(ClavaNode sourceNode, String declaratorName) {
        String classCode = getClassCode(sourceNode);
        Type semanticPointee = getFunctionPointee(getPointeeType());
        boolean groupedDataPointer = classCode.startsWith("::")
                && !(semanticPointee instanceof BuiltinType)
                && !(semanticPointee instanceof FunctionType)
                && !(semanticPointee instanceof ArrayType);
        String name = declaratorName == null ? "" : declaratorName;
        if (groupedDataPointer) {
            while (hasWholeParentheses(name)) {
                name = name.substring(1, name.length() - 1);
            }
        }
        String memberPointerDeclarator = classCode + "::*" + name;
        if (groupedDataPointer) {
            memberPointerDeclarator = "(" + memberPointerDeclarator + ")";
        }
        Type pointeeType = groupedDataPointer ? withoutDataParentheses(getPointeeType()) : getPointeeType();
        Type unqualifiedPointeeType = getUnqualifiedType(pointeeType);

        String code = requiresParenthesizedDeclarator(unqualifiedPointeeType)
                ? pointeeType.getCode(sourceNode, "(" + memberPointerDeclarator + ")")
                : pointeeType.getCode(sourceNode, memberPointerDeclarator);

        if (getFunctionPointee(pointeeType) instanceof FunctionProtoType functionType) {
            List<String> suffix = new ArrayList<>();
            if (functionType.get(FunctionType.IS_CONST)) {
                suffix.add("const");
            }
            if (functionType.get(FunctionType.IS_VOLATILE)) {
                suffix.add("volatile");
            }
            String referenceQualifier = functionType.get(FunctionProtoType.REFERENCE_QUALIFIER).getCode();
            if (!referenceQualifier.isEmpty()) {
                suffix.add(referenceQualifier);
            }
            String exception = functionType.get(FunctionProtoType.EXCEPTION_SPECIFICATION).getCode(functionType);
            if (!exception.isEmpty()) {
                suffix.add(exception);
            }
            String qualifiers = String.join(" ", suffix);
            if (!qualifiers.isEmpty()) {
                code += " " + qualifiers;
            }
        }

        return code;
    }

    // The grouped declarator creates ParenType sugar when reparsed. Render one
    // canonical group while retaining the pointee's qualifiers.
    private static Type withoutDataParentheses(Type type) {
        if (type instanceof ParenType parenthesized) {
            return withoutDataParentheses(parenthesized.getInnerType());
        }
        if (type instanceof QualType qualified) {
            Type inner = withoutDataParentheses(qualified.getUnqualifiedType());
            if (inner != qualified.getUnqualifiedType()) {
                QualType copy = (QualType) qualified.copy();
                copy.set(QualType.UNQUALIFIED_TYPE, inner);
                return copy;
            }
        }
        return type;
    }

    private static boolean hasWholeParentheses(String name) {
        if (!name.startsWith("(") || !name.endsWith(")")) {
            return false;
        }
        int depth = 0;
        char quote = 0;
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (quote != 0) {
                if (character == '\\') {
                    index++;
                } else if (character == quote) {
                    quote = 0;
                }
                continue;
            }
            if (character == '\'' || character == '"') {
                quote = character;
            } else if (character == '(') {
                depth++;
            } else if (character == ')' && --depth == 0) {
                return index == name.length() - 1;
            }
        }
        return false;
    }

    private String getClassCode(ClavaNode sourceNode) {
        Type classType = getUnqualifiedType(getClassType());
        if (classType instanceof RecordType recordType) {
            var declaration = recordType.getDecl();
            String name = declaration.getFullyQualifiedName();
            if (declaration.getAncestorTry(pt.up.fe.specs.clava.ast.decl.FunctionDecl.class).isEmpty()) {
                name = "::" + name;
            }
            if (declaration instanceof pt.up.fe.specs.clava.ast.decl.ClassTemplateSpecializationDecl specialization) {
                String arguments = specialization.get(
                        pt.up.fe.specs.clava.ast.decl.ClassTemplateSpecializationDecl.TEMPLATE_ARGUMENTS).stream()
                        .map(argument -> argument.getCode(sourceNode)).collect(java.util.stream.Collectors.joining(", "));
                name += "<" + arguments + ">";
            }
            return name;
        }
        return getClassType().getCode(sourceNode);
    }

    private static Type getFunctionPointee(Type type) {
        type = getUnqualifiedType(type);
        while (type instanceof ParenType parenthesized) {
            type = getUnqualifiedType(parenthesized.getInnerType());
        }
        return type;
    }

    private static boolean requiresParenthesizedDeclarator(Type type) {
        return type instanceof FunctionType || type instanceof ArrayType;
    }

    private static Type getUnqualifiedType(Type type) {
        while (type instanceof QualType qualType) {
            type = qualType.getUnqualifiedType();
        }

        return type;
    }

    @Override
    protected List<DataKey<Type>> getUnderlyingTypeKeys() {
        return Arrays.asList(CLASS_TYPE, POINTEE_TYPE);
    }
}
