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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.extra.TemplateParameter;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.TemplateTypeParmKind;

/**
 * Declaration of a template type parameter.
 * 
 * @author JoaoBispo
 *
 */
public class TemplateTypeParmDecl extends TemplateTypeParmDeclBase implements TemplateParameter {

    // Data declarations are generated from the wire schema in TemplateTypeParmDeclBase.

    public TemplateTypeParmDecl(DataStore data, Collection<? extends ClavaNode> children, TemplateTypeParmDeclBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public TemplateTypeParmDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append(getWireKind().getString());

        if (getWireIsParameterPack()) {
            code.append("...");
            Preconditions.checkArgument(!getWireDefaultArgument().isPresent());
        }

        if (hasDeclName()) {
            code.append(" ").append(getDeclName());
        }

        getWireDefaultArgument().ifPresent(arg -> code.append(" = ").append(arg.getCode(this)));

        return code.toString();
    }

}
