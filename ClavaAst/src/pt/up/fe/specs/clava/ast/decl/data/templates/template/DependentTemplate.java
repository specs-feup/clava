/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.decl.data.templates.template;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.data.nestedname.NestedNameSpecifier;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentTemplate;
import pt.up.fe.specs.clava.ast.type.enums.TemplateNameKind;

public class DependentTemplate extends TemplateArgumentTemplate {

    /// DATAKEYS BEGIN

    public final static DataKey<NestedNameSpecifier> QUALIFIER = KeyFactory.object("qualifier",
            NestedNameSpecifier.class);

    public final static DataKey<String> NAME = KeyFactory.string("name");

    /// DATAKEYS END

    public DependentTemplate() {
        super(TemplateNameKind.DependentTemplate);
    }

    @Override
    public String getCode(ClavaNode node) {
        return get(QUALIFIER).getQualifier() + get(NAME);
    }
}
