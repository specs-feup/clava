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

package pt.up.fe.specs.clava.ast.decl.data2;

public class VarDeclDataV2 extends NamedDeclDataV2 {

    private final String qualifiedName;
    private final boolean isConstexpr;
    private final boolean isStaticDataMember;
    private final boolean isOutOfLine;
    private final boolean hasGlobalStorage;

    public VarDeclDataV2(String qualifiedName, boolean isConstexpr, boolean isStaticDataMember, boolean isOutOfLine,
            boolean hasGlobalStorage, NamedDeclDataV2 namedDeclData) {
        super(namedDeclData);

        this.qualifiedName = qualifiedName;
        this.isConstexpr = isConstexpr;
        this.isStaticDataMember = isStaticDataMember;
        this.isOutOfLine = isOutOfLine;
        this.hasGlobalStorage = hasGlobalStorage;
    }

    public VarDeclDataV2(VarDeclDataV2 varDeclData) {
        this(varDeclData.qualifiedName, varDeclData.isConstexpr, varDeclData.isStaticDataMember,
                varDeclData.isOutOfLine, varDeclData.hasGlobalStorage, varDeclData);
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public boolean isConstexpr() {
        return isConstexpr;
    }

    public boolean isStaticDataMember() {
        return isStaticDataMember;
    }

    public boolean isOutOfLine() {
        return isOutOfLine;
    }

    public boolean isHasGlobalStorage() {
        return hasGlobalStorage;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("qualified name: " + qualifiedName);
        string.append(",is constexpr: " + isConstexpr);
        string.append(",is static data member: " + isStaticDataMember);
        string.append(",is out-of-line: " + isOutOfLine);
        string.append(",has global storage: " + hasGlobalStorage);

        return toString(super.toString(), string.toString());
    }
}
