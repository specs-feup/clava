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

import java.util.Optional;

import pt.up.fe.specs.clava.ast.decl.data.NameKind;

public class NamedDeclData extends DeclDataV2 {

    public static NamedDeclData empty() {
        return new NamedDeclData(null, NameKind.IDENTIFIER, false, DeclDataV2.empty());
    }

    private final String qualifiedName;
    private final NameKind nameKind;
    private final boolean isHidden;

    public NamedDeclData(String qualifiedName, NameKind nameKind, boolean isHidden, DeclDataV2 declData) {
        super(declData);

        this.qualifiedName = qualifiedName == null ? "" : qualifiedName;
        this.nameKind = nameKind;
        this.isHidden = isHidden;

        // Preconditions.checkNotNull(qualifiedName, "Just to check if it can be null");
    }

    public NamedDeclData(NamedDeclData data) {
        this(data.qualifiedName, data.nameKind, data.isHidden, data);
    }

    public Optional<String> getQualifiedName() {
        if (qualifiedName.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(qualifiedName);
    }

    public NameKind getNameKind() {
        return nameKind;
    }

    public boolean isHidden() {
        return isHidden;
    }

    @Override
    public String toString() {

        return toString(super.toString(), "is hidded: " + isHidden);
    }

}
