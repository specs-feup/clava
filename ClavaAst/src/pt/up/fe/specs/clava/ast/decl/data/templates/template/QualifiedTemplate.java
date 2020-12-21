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

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.TemplateDecl;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentTemplate;
import pt.up.fe.specs.clava.ast.type.enums.TemplateNameKind;

public class QualifiedTemplate extends TemplateArgumentTemplate {

    /// DATAKEYS BEGIN

    public final static DataKey<String> QUALIFIER = KeyFactory.string("qualifier");

    public final static DataKey<Boolean> HAS_TEMPLATE_KEYWORD = KeyFactory.bool("hasTemplateKeyword");

    public final static DataKey<TemplateDecl> TEMPLATE_DECL = KeyFactory.object("templateDecl", TemplateDecl.class);

    /// DATAKEYS END

    public QualifiedTemplate() {
        super(TemplateNameKind.QualifiedTemplate);
    }

    @Override
    public String getCode(ClavaNode node) {
        TemplateDecl templateDecl = get(TEMPLATE_DECL);
        Decl decl = templateDecl.getTemplateDecl();
        return get(QUALIFIER) + ((NamedDecl) decl).getDeclName();
    }
}
