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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.ast.decl.data;

import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.AccessSpecifier;

public class RecordBase {

    private final boolean isVirtual;
    private final AccessSpecifier accessSpecifier;
    private final Type type;
    private final boolean isPackExpansion;

    public RecordBase(boolean isVirtual, AccessSpecifier accessSpecifier, Type type, boolean isPackExpansion) {
        this.isVirtual = isVirtual;
        this.accessSpecifier = accessSpecifier;
        this.type = type;
        this.isPackExpansion = isPackExpansion;
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public AccessSpecifier getAccessSpecifier() {
        return accessSpecifier;
    }

    public Type getType() {
        return type;
    }

    public boolean isPackExpansion() {
        return isPackExpansion;
    }

    public String getCode() {
        StringBuilder code = new StringBuilder();

        // Add access specifier
        code.append(accessSpecifier.getString());

        // Add virtual, if present
        if (isVirtual) {
            code.append(" virtual");
        }

        // Add type
        code.append(" ").append(type.getCode());

        if (isPackExpansion) {
            code.append("...");
        }

        return code.toString();
    }

}
