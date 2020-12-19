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

package pt.up.fe.specs.clava.ast.decl.data.templates;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ast.decl.data.templates.template.QualifiedTemplate;
import pt.up.fe.specs.clava.ast.decl.data.templates.template.SubstTemplateTemplateParm;
import pt.up.fe.specs.clava.ast.decl.data.templates.template.Template;
import pt.up.fe.specs.clava.ast.type.enums.TemplateNameKind;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public abstract class TemplateArgumentTemplate extends TemplateArgument {

    /// DATAKEYS BEGIN

    public final static DataKey<TemplateNameKind> TEMPLATE_NAME_KIND = KeyFactory.enumeration("templateNameKind",
            TemplateNameKind.class);

    // public final static DataKey<Optional<TemplateDecl>> TEMPLATE_DECL = KeyFactory.optional("templateDecl");

    /// DATAKEYS END

    public TemplateArgumentTemplate(TemplateNameKind templateNameKind) {
        // this();
        super(TemplateArgumentKind.Template);

        set(TEMPLATE_NAME_KIND, templateNameKind);
    }

    // public TemplateArgumentTemplate() {
    // super(TemplateArgumentKind.Template);
    // }

    public static TemplateArgumentTemplate newInstance(TemplateNameKind nameKind) {
        switch (nameKind) {
        case Template:
            return new Template();
        case QualifiedTemplate:
            return new QualifiedTemplate();
        case SubstTemplateTemplateParm:
            return new SubstTemplateTemplateParm();
        default:
            throw new NotImplementedException(nameKind);
        }
    }

    // @Override
    // public String getCode(ClavaNode node) {
    // switch (get(TEMPLATE_NAME_KIND)) {
    //
    // case Template:
    // TemplateDecl templateDecl = get(TEMPLATE_DECL).get();
    // Decl decl = templateDecl.getTemplateDecl();
    // SpecsCheck.checkNotNull(decl instanceof NamedDecl, () -> "Check if this should always be a NamedDecl");
    // return ((NamedDecl) decl).getDeclName();
    // default:
    // throw new RuntimeException("Case not implemented: " + get(TEMPLATE_NAME_KIND));
    // }
    // // System.out.println("NAME KIND: " + get(TEMPLATE_NAME_KIND));
    // // System.out.println("TEMPALTE DECL: " + get(TEMPLATE_DECL).get().getTemplateDecl());
    // // System.out.println("TEMPALTE DECL CODE: " + get(TEMPLATE_DECL).get().getTemplateDecl().getCode());
    // // throw new RuntimeException("STOP");
    // // return get(INTEGRAL).toString();
    // }
}
