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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.extra.TemplateArgument;
import pt.up.fe.specs.clava.ast.extra.TemplateArgumentType;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.util.SpecsCollections;

public class TemplateSpecializationType extends Type {

    private final String templateName;
    private final boolean isTypeAlias;

    private List<String> templateArgumentsStrings;
    // private List<Type> templateArgumentTypes;
    private boolean hasUpdatedArgumentTypes;

    public TemplateSpecializationType(String templateName, List<String> templateArguments,
            TypeData typeData, ClavaNodeInfo info, List<TemplateArgument> templateNodes, Type aliasedType,
            Type desugaredType) {

        this(templateName, templateArguments, aliasedType != null, typeData, info,
                SpecsCollections.concat(templateNodes,
                        SpecsCollections.asListT(ClavaNode.class, aliasedType, desugaredType)));
    }

    private TemplateSpecializationType(String templateName, List<String> templateArgsNames, boolean isTypeAlias,
            TypeData typeData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {

        super(typeData, info, children);

        this.templateName = templateName;
        this.templateArgumentsStrings = templateArgsNames;
        this.isTypeAlias = isTypeAlias;
        // System.out.println("TEMPLATE ARG TYPES " + getInfo().getExtendedId() + ": CONSTRUCTOR (NULL)");
        // this.templateArgumentTypes = null;
        this.hasUpdatedArgumentTypes = false;
    }

    @Override
    protected ClavaNode copyPrivate() {
        TemplateSpecializationType type = new TemplateSpecializationType(templateName, templateArgumentsStrings,
                isTypeAlias,
                getTypeData(),
                getInfo(), Collections.emptyList());

        // Set argument types
        // System.out.println("TEMPLATE ARG TYPES " + getInfo().getExtendedId() + ": COPY (NULL? "
        // + (templateArgumetTypes == null) + ")");
        // type.templateArgumentTypes = templateArgumentTypes;

        return type;
    }

    public String getTemplateName() {
        return templateName;
    }

    @Override
    public List<String> getTemplateArgumentStrings() {
        return templateArgumentsStrings;
    }

    @Override
    public List<Type> getTemplateArgumentTypes() {
        return getTemplateArguments(TemplateArgumentType.class).stream()
                .map(TemplateArgumentType::getType)
                .collect(Collectors.toList());
        // if (templateArgumentTypes == null) {
        // SpecsLogs.msgWarn("Template argument types not set yet for type '" + getInfo().getExtendedId() + "'!");
        // return Collections.emptyList();
        // }
        // // System.out.println("TEMPLATE ARG TYPES " + getInfo().getExtendedId() + ": GET (NOT NULL)");
        // return templateArgumentTypes;
    }

    // @Override
    // public void setTemplateArgTypes(List<Type> newTemplateArgTypes) {
    // setArgsTypes(newTemplateArgTypes);
    // // // Replace arguments types
    // // this.templateArgumentTypes = new ArrayList<>(newTemplateArgTypes);
    // //
    // // // Replace arguments
    // // this.templateArguments = newTemplateArgTypes.stream()
    // // .map(Type::getCode)
    // // .collect(Collectors.toList());
    // //
    // // hasUpdatedArgumentTypes = true;
    // }

    @Override
    public boolean hasUpdatedTemplateArgTypes() {
        return hasUpdatedArgumentTypes;
    }

    @Override
    public void setTemplateArgumentType(int index, Type newTemplateArgType) {
        // Check if template argument types are set type
        // if (templateArgumentTypes == null) {
        // ClavaLog.info(
        // "Tried to setting single type of template argument, but there is no info about the template argument types,
        // only literal arguments");
        // return;
        // }

        // Check if there are enough arguments
        int argsNumber = templateArgumentsStrings.size();
        Preconditions.checkArgument(index < argsNumber, "Tried to set template argument type at index '" + index
                + "', but template only has '" + argsNumber + "' template arguments.");

        // Check if template argument is of the right kind
        TemplateArgument templateArgument = getTemplateArgument(index);
        Preconditions.checkArgument(templateArgument instanceof TemplateArgumentType,
                "Expected template argument at index '" + index
                        + "' to be a 'type', however it is '"
                        + templateArgument.getClass().getName() + "'");

        // Set argument
        ((TemplateArgumentType) templateArgument).setType(newTemplateArgType);
        templateArgumentsStrings.set(index, newTemplateArgType.getCode());
        // templateArgumentTypes.set(index, newTemplateArgType);
        hasUpdatedArgumentTypes = true;
    }

    public TemplateArgument getTemplateArgument(int index) {
        return getChild(TemplateArgument.class, index);
    }

    public List<TemplateArgument> getTemplateArguments() {
        return SpecsCollections.cast(getChildren().subList(0, getTemplateArgumentStrings().size()),
                TemplateArgument.class);
    }

    public <T extends TemplateArgument> List<T> getTemplateArguments(Class<T> templateArgumentClass) {
        return getTemplateArguments().stream()
                .filter(templateArgumentClass::isInstance)
                .map(templateArgumentClass::cast)
                .collect(Collectors.toList());
    }

    public Optional<Type> getAliasedType() {
        if (!isTypeAlias) {
            return Optional.empty();
        }

        return Optional.of(getChild(Type.class, getTemplateArgumentStrings().size()));
    }

    @Override
    public Type desugar() {
        return getChild(Type.class, 1);
        // Type desugared = getChild(Type.class, 1);
        //
        // if (desugared == this) {
        // System.out.println("SAME!!!!!");
        // return null;
        // }
        //
        // return desugared;
        // return this;
    }

    /*
    @Override
    public Type desugar() {
        System.out.println("TEMPLATE NAME:" + templateName);
        System.out.println("CHILDREN:" + getChildren());
        if (isTypeAlias) {
            System.out.println("HAS TYPE ALIAS");
            return getAliasedType().get().desugar();
        }
        System.out.println("DOES NOT HAVE TYPE ALIAS");
        return this;
        // getAliasedType().orElseThrow(() -> new RuntimeException("Expected type to be aliased"));
        // List<Type> types = getChildrenOf(Type.class);
        //
        // if (!isTypeAlias) {
        // Preconditions.checkArgument(!types.isEmpty(), "Expected at least one node");
        // return types.get(0);
        // }
        //
        // Preconditions.checkArgument(types.size() > 1, "Expected at least two nodes");
        // return types.get(1);
    }
    */

    /*
    public Optional<Type> getUnqualifiedDesugaredType() {
        if (!getTypeData().hasSugar()) {
            return Optional.empty();
        }
    
        int index = getTemplateArgs().size();
        index = isTypeAlias ? index + 1 : index;
    
        return Optional.of(getChild(Type.class, index));
    }
    */

    @Override
    public String getCode(String name) {

        StringBuilder code = new StringBuilder();

        String templateName = getTemplateName();

        // If record type, prepend namespace - nope, that is the job of ElaboratedType
        // if (hasSugar()) {
        // Type desugaredType = desugar();
        // if (desugaredType instanceof RecordType) {
        // String namespace = ((RecordType) desugaredType).getNamespace();
        // namespace = namespace.isEmpty() ? namespace : namespace + "::";
        // templateName = namespace + templateName;
        // // RecordType recordType = (RecordType) desugaredType;
        // // System.out.println("RECORD TYPE NAMESPACe:" + recordType.getNamespace());
        // }
        //
        // }

        code.append(templateName);

        String templateArgs = getTemplateArgumentStrings().stream()
                .collect(Collectors.joining(", ", "<", ">"));
        code.append(templateArgs);
        if (name != null) {
            code.append(" ").append(name);
        }

        return code.toString();

        // System.out.println("TemplateName:" + getTemplateName());
        // System.out.println("TYPE:" + getAliasedType().getCode(name));
        // System.out.println("Children:" + getChildren());
        // return getAliasedType().getCode(name);
        // return super.getCode(name);
    }

    /**
     * Helper method with sets the template arguments strings by default.
     * 
     * @param argsTypes
     */
    @Override
    public void setTemplateArgumentTypes(List<Type> argsTypes) {
        setTemplateArgumentTypes(argsTypes, true);
    }

    public void setTemplateArgumentTypes(List<Type> argsTypes, boolean updateTemplateArgumentStrings) {
        // System.out.println("TEMPLATE ARG TYPES " + getInfo().getExtendedId() + ": SET");
        // if (templateArgumentTypes != null) {
        // throw new RuntimeException("Expected argument types to be null");
        // }

        // Calculate how many type arguments are expected
        List<TemplateArgumentType> typeTemplateArguments = getTemplateArguments(TemplateArgumentType.class);
        // System.out.println("TEMPLATE NODES:" + getTemplateArguments());
        // System.out.println("TYPE TEMPLATE NODES:" + typeTemplateArguments);
        // System.out.println("NEW TYPES:" + argsTypes);
        // int numExpectedArgTypes = templateArguments.size();
        int numExpectedArgTypes = typeTemplateArguments.size();
        // System.out.println("TEMPLATE ARGS:" + getTemplateArguments());
        Preconditions.checkArgument(argsTypes.size() == numExpectedArgTypes,
                "Expected number of template argument types (" + argsTypes.size()
                        + ") to be the same as the number of template nodes of kind'type' (" + numExpectedArgTypes
                        + ")\nTemplate arguments: " + templateArgumentsStrings + "\nNew types:"
                        + argsTypes.stream().map(Type::getCode).collect(Collectors.joining(", ")));
        // templateArgumentTypes = argsTypes;

        // No arguments to set, return
        if (numExpectedArgTypes == 0) {
            return;
        }

        // Set arguments
        IntStream.range(0, numExpectedArgTypes)
                .forEach(index -> typeTemplateArguments.get(index).setType(argsTypes.get(index)));
        // getTemplateArguments().stream()
        // .filter(templateArg instanceof TemplateArgumentType)
        // .

        // Update template argument strings, if necessary
        if (updateTemplateArgumentStrings) {
            // Signal update, because of ElaboratedType
            this.hasUpdatedArgumentTypes = true;

            // Get indexes to update
            List<TemplateArgument> allTemplateArguments = getTemplateArguments();
            int[] indexesToUpdate = IntStream.range(0, allTemplateArguments.size())
                    .filter(index -> allTemplateArguments.get(index) instanceof TemplateArgumentType)
                    .toArray();

            IntStream.range(0, numExpectedArgTypes)
                    .forEach(index -> templateArgumentsStrings.set(indexesToUpdate[index],
                            argsTypes.get(index).getCode()));

        }
    }

    // @Override
    // public String getCode() {
    // return templateName + "<" + getTemplateArgument().getCode() + ">";
    // }

}
