/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clava.ast.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.TemplateDecl;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentType;

/**
 * Represents a type template specialization; the template must be a class template, a type alias template, or a
 * template template parameter.
 * 
 * <p>
 * A template which cannot be resolved to one of these, e.g. because it is written with a dependent scope specifier, is
 * instead represented as a DependentTemplateSpecializationType.
 * 
 * <p>
 * A non-dependent template specialization type is always "sugar", typically for a RecordType. For example, a class
 * template specialization type of vector<int> will refer to a tag type for the instantiation std::vector<int,
 * std::allocator<int>>
 * 
 * <p>
 * Template specializations are dependent if either the template or any of the template arguments are dependent, in
 * which case the type may also be canonical.
 * 
 * <p>
 * Instances of this type are allocated with a trailing array of TemplateArguments, followed by a QualType representing
 * the non-canonical aliased type when the template is a type alias template.
 * 
 * @author JBispo
 *
 */
public class TemplateSpecializationType extends TemplateSpecializationTypeBase {

    // Data declarations are generated from the wire schema in TemplateSpecializationTypeBase.

    public TemplateSpecializationType(DataStore data, Collection<? extends ClavaNode> children, TemplateSpecializationTypeBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    private boolean hasUpdatedArgumentTypes;

    public TemplateSpecializationType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);

        hasUpdatedArgumentTypes = false;
    }

    public String getTemplateName() {
        return getWireTemplateName();
    }

    @Override
    public List<String> getTemplateArgumentStrings(ClavaNode sourceNode) {
        return getWireTemplateArguments().stream()
                .map(arg -> arg.getCode(sourceNode))
                .collect(Collectors.toList());
    }

    @Override
    public List<Type> getTemplateArgumentTypes() {
        return getWireTemplateArguments().stream()
                .filter(TemplateArgumentType.class::isInstance)
                .map(argType -> argType.get(TemplateArgumentType.TYPE))
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasUpdatedTemplateArgTypes() {
        return hasUpdatedArgumentTypes;
    }

    public void setTemplateArgument(int index, TemplateArgument templateArgument) {
        List<TemplateArgument> templateArgs = getWireTemplateArguments();

        int argsNumber = templateArgs.size();
        Preconditions.checkArgument(index < argsNumber, "Tried to set template argument type at index '" + index
                + "', but template only has '" + argsNumber + "' template arguments.");

        templateArgs.set(index, templateArgument);

        hasUpdatedArgumentTypes = true;
    }

    @Override
    public void setTemplateArgumentType(int index, Type newTemplateArgType) {

        // Set argument
        setTemplateArgument(index, new TemplateArgumentType(newTemplateArgType));
    }

    public TemplateArgument getTemplateArgument(int index) {
        return getWireTemplateArguments().get(index);
    }

    @Override
    public List<TemplateArgument> getTemplateArguments() {
        return getWireTemplateArguments();
    }

    public <T extends TemplateArgument> List<T> getTemplateArguments(Class<T> templateArgumentClass) {
        return getTemplateArguments().stream()
                .filter(templateArgumentClass::isInstance)
                .map(templateArgumentClass::cast)
                .collect(Collectors.toList());
    }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {

        StringBuilder code = new StringBuilder();

        String templateName = getTemplateName();

        code.append(templateName);

        String templateArgs = getTemplateArguments().stream()
                .map(arg -> arg.getCode(sourceNode))
                .collect(Collectors.joining(", ", "<", ">"));

        code.append(templateArgs);
        if (name != null) {
            code.append(" ").append(name);
        }

        return code.toString();
    }

    /**
     * Helper method with sets the template arguments strings by default.
     * 
     * @param argsTypes
     */
    @Override
    public void setTemplateArgumentTypes(List<Type> argsTypes) {
        setInPlace(TEMPLATE_ARGUMENTS, argsTypes.stream()
                .map(TemplateArgumentType::new)
                .collect(Collectors.toList()));

        hasUpdatedArgumentTypes = true;
    }

}
