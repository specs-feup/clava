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

public class VarDeclDumperInfo {

    private final String qualifiedName;
    private final boolean isConstexpr;
    private final boolean isStaticDataMember;
    private final boolean isOutOfLine;
    private final boolean hasGlobalStorage;

    public VarDeclDumperInfo(String qualifiedName, boolean isConstexpr, boolean isStaticDataMember,
            boolean isOutOfLine, boolean hasGlobalStorage) {

        this.qualifiedName = qualifiedName;
        this.isConstexpr = isConstexpr;
        this.isStaticDataMember = isStaticDataMember;
        this.isOutOfLine = isOutOfLine;
        this.hasGlobalStorage = hasGlobalStorage;
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

    public boolean hasGlobalStorage() {
        return hasGlobalStorage;
    }
}
