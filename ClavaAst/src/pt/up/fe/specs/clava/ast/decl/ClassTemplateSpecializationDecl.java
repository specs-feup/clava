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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.List;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;
import pt.up.fe.specs.clava.ast.decl.enums.TemplateSpecializationKind;

/**
 * Represents a class template specialization, which refers to a class template with a given set of template arguments.
 * 
 * Class template specializations represent both explicit specialization of class templates and implicit instantiations
 * of class templates.
 * 
 * @author JoaoBispo
 *
 */
public class ClassTemplateSpecializationDecl extends CXXRecordDecl {

    /**
     * The template that this specialization specializes.
     */
    public static final DataKey<ClassTemplateDecl> SPECIALIZED_TEMPLATE = KeyFactory.object("specializedTemplate",
            ClassTemplateDecl.class);

    /**
     * The kind of specialization that this declaration represents.
     */
    public static final DataKey<TemplateSpecializationKind> SPECIALIZATION_KIND = KeyFactory.enumeration(
            "specializationKind", TemplateSpecializationKind.class);

    /**
     * The template arguments of the class template specialization.
     */
    public static final DataKey<List<TemplateArgument>> TEMPLATE_ARGUMENTS = KeyFactory.list("templateArguments",
            TemplateArgument.class);

    public ClassTemplateSpecializationDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        var recordCode = super.getCode(get(RECORD_BASES), get(TEMPLATE_ARGUMENTS));

        return "template<>\n" + recordCode;
    }
}
