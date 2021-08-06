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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.extra.TemplateParameter;

/**
 * Declaration of a redeclarable template.
 * 
 * @author JoaoBispo
 *
 */
public class RedeclarableTemplateDecl extends TemplateDecl implements TemplateParameter {

    public RedeclarableTemplateDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        return getCode("");
    }

    public String getCode(String declPrefix) {

        StringBuilder code = new StringBuilder();

        code.append("template <");

        String parameterList = getTemplateParameters().stream()
                .map(param -> param.getCode())
                .collect(Collectors.joining(", "));

        code.append(parameterList).append(">\n");

        if (!declPrefix.isEmpty()) {
            code.append(declPrefix).append(" ");
        }

        code.append(getTemplateDecl().getCode());
        return code.toString();
    }

}
