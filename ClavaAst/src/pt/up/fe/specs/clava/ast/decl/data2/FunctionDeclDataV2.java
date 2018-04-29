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

import pt.up.fe.specs.clava.ast.decl.enums.TemplateKind;

/**
 * @deprecated
 * @author JoaoBispo
 *
 */
@Deprecated
public class FunctionDeclDataV2 extends NamedDeclData {

    private final boolean isConstexpr;
    private final TemplateKind templateKind;

    public FunctionDeclDataV2(boolean isConstexpr, TemplateKind templateKind, NamedDeclData namedDeclData) {
        super(namedDeclData);

        this.isConstexpr = isConstexpr;
        this.templateKind = templateKind;
    }

    public FunctionDeclDataV2(FunctionDeclDataV2 data) {
        this(data.isConstexpr, data.templateKind, data);
    }

    public boolean isConstexpr() {
        return isConstexpr;
    }

    public TemplateKind getTemplateKind() {
        return templateKind;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("is constexpr: " + isConstexpr);
        string.append(", template kind: " + templateKind);

        return toString(super.toString(), string.toString());
    }
}
