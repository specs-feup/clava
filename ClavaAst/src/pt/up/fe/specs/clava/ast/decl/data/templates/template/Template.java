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

package pt.up.fe.specs.clava.ast.decl.data.templates.template;

import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.NullDecl;
import pt.up.fe.specs.clava.ast.decl.TemplateDecl;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentTemplate;
import pt.up.fe.specs.clava.ast.type.enums.TemplateNameKind;
import pt.up.fe.specs.util.SpecsCheck;

public class Template extends TemplateArgumentTemplate {

    /// DATAKEYS BEGIN

    public final static DataKey<Optional<TemplateDecl>> TEMPLATE_DECL = KeyFactory.optional("templateDecl");

    /// DATAKEYS END

    public Template() {
        super(TemplateNameKind.Template);
    }

    @Override
    public String getCode(ClavaNode node) {
        TemplateDecl templateDecl = get(TEMPLATE_DECL).get();
        Decl decl = templateDecl.getTemplateDecl();

        if (decl instanceof NullDecl) {
            ClavaLog.debug(
                    () -> "pt.up.fe.specs.clava.ast.decl.data.templates.template.Template: Code generation not verified yet");
            return "";
        }

        SpecsCheck.checkNotNull(decl instanceof NamedDecl, () -> "Check if this should always be a NamedDecl");
        return ((NamedDecl) decl).getDeclName();
    }
}
