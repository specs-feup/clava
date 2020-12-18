/**
 * Copyright 2020 SPeCS.
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

import pt.up.fe.specs.clava.ast.decl.TemplateTemplateParmDecl;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentTemplate;
import pt.up.fe.specs.clava.ast.type.enums.TemplateNameKind;

public class SubstTemplateTemplateParm extends TemplateArgumentTemplate {

    public static final DataKey<TemplateTemplateParmDecl> PARAMETER = KeyFactory.object("parameter",
            TemplateTemplateParmDecl.class);

    public static final DataKey<TemplateArgumentTemplate> REPLACEMENT = KeyFactory.object("replacement",
            TemplateArgumentTemplate.class);

    public SubstTemplateTemplateParm() {
        super(TemplateNameKind.SubstTemplateTemplateParm);
    }
}
