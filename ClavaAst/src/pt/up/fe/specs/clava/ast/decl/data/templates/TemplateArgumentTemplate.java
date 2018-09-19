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

import pt.up.fe.specs.clava.ast.type.enums.TemplateNameKind;

public class TemplateArgumentTemplate extends TemplateArgument {

    /// DATAKEYS BEGIN

    public final static DataKey<TemplateNameKind> TEMPLATE_NAME_KIND = KeyFactory.enumeration("templateNameKind",
            TemplateNameKind.class);

    /// DATAKEYS END

    public TemplateArgumentTemplate(TemplateNameKind templateNameKind) {
        this();

        set(TEMPLATE_NAME_KIND, templateNameKind);
    }

    public TemplateArgumentTemplate() {
        super(TemplateArgumentKind.Template);
    }

    // @Override
    // public String getCode(ClavaNode node) {
    // return get(INTEGRAL).toString();
    // }
}
