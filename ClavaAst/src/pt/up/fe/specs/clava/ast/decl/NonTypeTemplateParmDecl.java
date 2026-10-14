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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.TemplateParameter;
import pt.up.fe.specs.clava.ast.type.Type;

public class NonTypeTemplateParmDecl extends NonTypeTemplateParmDeclBase implements TemplateParameter {

    // Data declarations are generated from the wire schema in NonTypeTemplateParmDeclBase.

    public NonTypeTemplateParmDecl(DataStore data, Collection<? extends ClavaNode> children, NonTypeTemplateParmDeclBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public NonTypeTemplateParmDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {

        StringBuilder code = new StringBuilder();

        String declName = getDeclName();
        if (declName.isEmpty()) {
            code.append(getTypeCode());
        } else {
            code.append(getTypeCode(declName));
        }

        getWireDefaultArgument().ifPresent(expr -> code.append(" = ").append(expr.getCode()));

        return code.toString();

    }
}
