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

    @Override
    public String getCode(ClavaNode sourceNode, String declaratorName) {
        String memberPointerDeclarator = getClassType().getCode(sourceNode) + "::*"
                + (declaratorName == null ? "" : declaratorName);
        Type pointeeType = getPointeeType();
        Type unqualifiedPointeeType = getUnqualifiedType(pointeeType);

        String code = requiresParenthesizedDeclarator(unqualifiedPointeeType)
                ? pointeeType.getCode(sourceNode, "(" + memberPointerDeclarator + ")")
                : pointeeType.getCode(sourceNode, memberPointerDeclarator);

        if (unqualifiedPointeeType instanceof FunctionProtoType functionType) {
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
