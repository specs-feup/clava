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

package pt.up.fe.specs.clava.ast.decl.data.templates;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ast.decl.data.templates.template.DependentTemplate;
import pt.up.fe.specs.clava.ast.decl.data.templates.template.QualifiedTemplate;
import pt.up.fe.specs.clava.ast.decl.data.templates.template.SubstTemplateTemplateParm;
import pt.up.fe.specs.clava.ast.decl.data.templates.template.Template;
import pt.up.fe.specs.clava.ast.decl.data.templates.template.UsingTemplate;
import pt.up.fe.specs.clava.ast.type.enums.TemplateNameKind;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public abstract class TemplateArgumentTemplate extends TemplateArgument {

    /// DATAKEYS BEGIN

    public final static DataKey<TemplateNameKind> TEMPLATE_NAME_KIND = KeyFactory.enumeration("templateNameKind",
            TemplateNameKind.class);

    /// DATAKEYS END

    public TemplateArgumentTemplate(TemplateNameKind templateNameKind) {
        super(TemplateArgumentKind.Template);

        set(TEMPLATE_NAME_KIND, templateNameKind);
    }

    public static TemplateArgumentTemplate newInstance(TemplateNameKind nameKind) {
        switch (nameKind) {
        case Template:
            return new Template();
        case QualifiedTemplate:
            return new QualifiedTemplate();
        case SubstTemplateTemplateParm:
            return new SubstTemplateTemplateParm();
        case UsingTemplate:
            return new UsingTemplate();
        case DependentTemplate:
            return new DependentTemplate();
        default:
            throw new NotImplementedException(nameKind);
        }
    }
}
