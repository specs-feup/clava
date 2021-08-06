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
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.VariableArrayType;

/**
 * Declaration of a typedef-name via the 'typedef' type specifier.
 * 
 * @author JoaoBispo
 *
 */
public class TypedefDecl extends TypedefNameDecl {

    public TypedefDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public Type getType() {
        // System.out.println("CALLING TYPEDEF_DECL.getType()");
        // System.out.println("Type for decl: " + get(TYPE_FOR_DECL));
        // HACK: Sometimes, the returned type is the same as type being declared, we have not discovered why
        // In this case, desugar type
        // Type type = super.getType();
        Type type = getUnderlyingType();

        if (type.getCode(this).equals(getDeclName())) {

            Type desugared = type.desugar();

            // System.out.println("RETURNING DESUGARED: " + desugared);

            return desugared;
        }

        // System.out.println("RETURNING SUPER: " + type);
        return type;
    }

    @Override
    public String getCode() {
        Type type = getType();

        // There can be complicated situations such as having function pointer with VLAs
        // that need the name of parameters, which are not available for function types in Clang
        // For those cases, directly use the source code of the typedef
        // However, this can be problematic if typedef uses macros and they are not re-inserted in the code.
        List<FunctionProtoType> functionTypes = type.getNodeFieldsRecursive().stream()
                .filter(FunctionProtoType.class::isInstance)
                .map(FunctionProtoType.class::cast)
                .collect(Collectors.toList());
        if (functionTypes.stream().flatMap(ftype -> ftype.getNodeFieldsRecursive().stream())
                .anyMatch(VariableArrayType.class::isInstance)) {
            return getLocation().getSource()
                    .orElseThrow(() -> new RuntimeException("Could not find source for location " + getLocation()));
        }

        return "typedef " + type.getCode(this, getDeclName());

    }

    @Override
    public String getTypelessCode() {
        return getDeclName();
    }

}
