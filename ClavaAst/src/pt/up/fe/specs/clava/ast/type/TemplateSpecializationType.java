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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
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
                isTypeAlias, getTypeData(), getInfo(), Collections.emptyList());

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
        ((TemplateArgumentType) templateArgument).setType(newTemplateArgType, true);
        templateArgumentsStrings.set(index, newTemplateArgType.getCode());
        // templateArgumentTypes.set(index, newTemplateArgType);
        setUpdatedTemplateArgTypes(true);
        // hasUpdatedArgumentTypes = true;

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

        int aliasIndex = getTemplateArgumentStrings().size();

        return Optional.of(getChild(Type.class, aliasIndex));
    }

    public Optional<Type> getDesugaredType() {
        if (!hasSugar()) {
            return Optional.empty();
        }

        int desugaredIndex = getTemplateArgumentStrings().size() + 1;

        return Optional.of(getChild(Type.class, desugaredIndex));
    }

    public void setDesugaredType(Type desugaredType) {
        int desugaredIndex = getTemplateArgumentStrings().size() + 1;
        setChild(desugaredIndex, desugaredType);
    }

    @Override
    protected Type desugarImpl() {
        return getChild(Type.class, 1);
    }

    @Override
    protected void setDesugarImpl(Type desugaredType) {
        setDesugaredType(desugaredType);
    }
    /*
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
    */

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

        // System.out.println("SETTING ARGS OF TYPE " + argsTypes);
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
        int numCurrentArgTypes = typeTemplateArguments.size();
        // System.out.println("TEMPLATE ARGS:" + getTemplateArguments());

        int numArgsTypes = argsTypes.size();

        // Preconditions.checkArgument(numArgsTypes <= numCurrentArgTypes,
        // "Expected number of template argument types (" + argsTypes.size()
        // + ") to be the same or lower as the number of template nodes of kind 'type' ("
        // + numCurrentArgTypes
        // + ")\nTemplate arguments: " + templateArgumentsStrings + "\nNew types:"
        // + argsTypes.stream().map(Type::getCode).collect(Collectors.joining(", ")));

        // templateArgumentTypes = argsTypes;

        // No arguments to set, return
        if (numArgsTypes == 0) {
            return;
        }

        // System.out.println("BEFORE:" + this);
        // System.out.println("ARG:" + typeTemplateArguments.get(0));

        // Remove previous arguments and add new arguments
        for (int i = 0; i < numCurrentArgTypes; i++) {
            removeChild(0);
        }

        for (int i = 0; i < numArgsTypes; i++) {
            Type type = argsTypes.get(i);

            TemplateArgumentType templateArg = ClavaNodeFactory.templateArgumentType(type,
                    ClavaNodeInfo.undefinedInfo());

            addChild(i, templateArg);
        }

        // IntStream.range(0, numArgsTypes)
        // .forEach(index -> typeTemplateArguments.get(index)
        // .setType(argsTypes.get(index), updateTemplateArgumentStrings));

        // Remove extra arguments
        // int extraArguments = numCurrentArgTypes - numArgsTypes;

        // ... from template argument children
        // for (int i = 0; i < extraArguments; i++) {
        // int indexToRemove = numArgsTypes;
        // removeChild(indexToRemove);
        // }

        // ... from template argument strings
        // IntStream.range(0, extraArguments).forEach(index -> SpecsCollections.removeLast(templateArgumentsStrings));

        // IntStream.range(0, extraArguments).forEach(index -> SpecsCollections.removeLast(typeTemplateArguments));

        // getTemplateArguments().stream()
        // .filter(templateArg instanceof TemplateArgumentType)
        // .
        // System.out.println("AFTER:" + this);
        // System.out.println("ARG:" + typeTemplateArguments.get(0));
        // Update template argument strings, if necessary
        if (updateTemplateArgumentStrings) {

            // Update strings
            templateArgumentsStrings = argsTypes.stream()
                    .map(Type::getCode)
                    .collect(Collectors.toList());

            // Signal update, because of ElaboratedType
            // this.hasUpdatedArgumentTypes = true;
            setUpdatedTemplateArgTypes(true);

            // Get indexes to update
            // List<TemplateArgument> allTemplateArguments = getTemplateArguments();
            // int[] indexesToUpdate = IntStream.range(0, allTemplateArguments.size())
            // .filter(index -> allTemplateArguments.get(index) instanceof TemplateArgumentType)
            // .toArray();

            // IntStream.range(0, numArgsTypes)
            // .forEach(index -> templateArgumentsStrings.set(indexesToUpdate[index],
            // argsTypes.get(index).getCode()));

        }

        // if (!argsTypes.isEmpty() && argsTypes.get(0).getCode().equals("float")) {
        // System.out.println("ARG TYPES:" + argsTypes);
        // System.out.println("TEMPLATE ARGS AFTER:" + getTemplateArguments());
        // System.out.println("CODE:" + this.getCode());
        // System.out.println("ROOT:" + getRoot());
        // }

    }

    // @Override
    // public String getCode() {
    // return templateName + "<" + getTemplateArgument().getCode() + ">";
    // }

    private void setUpdatedTemplateArgTypes(boolean hasUpdatedArgumentTypes) {
        // Set field
        this.hasUpdatedArgumentTypes = hasUpdatedArgumentTypes;

        // Propagate upward
        // getAncestorTry(TemplateSpecializationType.class)
        // .ifPresent(type -> type.setUpdatedTemplateArgTypes(hasUpdatedArgumentTypes));
        //
        // getAncestorTry(TemplateSpecializationType.class).ifPresent(type -> System.out.println("HEKOOASDOAODAOSD"));
    }

    @Override
    public String toContentString() {

        return super.toContentString() + " " + hashCode();
    }

    // @Override
    // public String toNodeString() {
    // System.out.println("TEMPLATE SPECIALIZATION CHILDREN:" + getChildren());
    // return super.toNodeString();
    // }

}
