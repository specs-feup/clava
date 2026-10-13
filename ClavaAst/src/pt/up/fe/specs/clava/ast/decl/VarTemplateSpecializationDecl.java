/**
 * Copyright 2021 SPeCS.
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

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.SourceLocation;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;
import pt.up.fe.specs.clava.ast.decl.enums.TemplateSpecializationKind;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;

/**
 * Represents a variable template specialization, which refers to a variable template with a given set of template
 * arguments.
 * <p>
 * Variable template specializations represent both explicit specializations of variable templates and implicit
 * instantiations of variable templates.
 * 
 * @author JBispo
 *
 */
public class VarTemplateSpecializationDecl extends VarDecl {

    public static final DataKey<VarTemplateDecl> SPECIALIZED_TEMPLATE = KeyFactory.object("specializedTemplate",
            VarTemplateDecl.class);

    public static final DataKey<Optional<VarTemplatePartialSpecializationDecl>> SPECIALIZED_PARTIAL_SPECIALIZATION =
            KeyFactory.optional("specializedPartialSpecialization");

    public static final DataKey<TemplateSpecializationKind> SPECIALIZATION_KIND = KeyFactory.enumeration(
            "specializationKind", TemplateSpecializationKind.class);

    public static final DataKey<List<TemplateArgument>> TEMPLATE_ARGUMENTS = KeyFactory.list("templateArguments",
            TemplateArgument.class);

    public static final DataKey<List<TemplateArgument>> TEMPLATE_INSTANTIATION_ARGUMENTS = KeyFactory.list(
            "templateInstantiationArguments", TemplateArgument.class);

    public static final DataKey<Optional<SourceLocation>> POINT_OF_INSTANTIATION = KeyFactory.optional(
            "pointOfInstantiation");

    public VarTemplateSpecializationDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        return getCode(true);
    }

    protected String getCode(boolean includeSpecializationPrefix) {
        StringBuilder code = new StringBuilder();

        var storageClass = get(STORAGE_CLASS);
        if (storageClass != StorageClass.None) {
            code.append(storageClass.getString()).append(" ");
        }

        code.append(getAttributesCode());
        String declName = getCurrentQualifiedName() + TemplateArgument.getCode(getTemplateArgumentsForCode(), this);
        declName = declName.isEmpty() ? null : declName;
        code.append(getType().getCode(this, declName));
        code.append(getInitializationCode());

        String declaration = code.toString();
        if (!includeSpecializationPrefix || !hasValue(SPECIALIZATION_KIND)) {
            return declaration;
        }

        return switch (get(SPECIALIZATION_KIND)) {
            case ExplicitSpecialization -> "template<>\n" + declaration;
            case ExplicitInstantiationDeclaration -> "extern template " + declaration;
            case ExplicitInstantiationDefinition -> "template " + declaration;
            case Undeclared, ImplicitInstantiation -> declaration;
        };
    }

    protected List<TemplateArgument> getTemplateArgumentsForCode() {
        return get(TEMPLATE_ARGUMENTS);
    }

}
