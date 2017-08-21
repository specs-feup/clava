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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.extra.TemplateArgument;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsCollections;

public class TemplateSpecializationType extends Type {

    private final String templateName;
    private final List<String> templateArguments;
    private final boolean isTypeAlias;

    private List<Type> templateArgumetTypes;

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
        this.templateArguments = templateArgsNames;
        this.isTypeAlias = isTypeAlias;
        // System.out.println("TEMPLATE ARG TYPES " + getInfo().getExtendedId() + ": CONSTRUCTOR (NULL)");
        this.templateArgumetTypes = null;
    }

    @Override
    protected ClavaNode copyPrivate() {
        TemplateSpecializationType type = new TemplateSpecializationType(templateName, templateArguments, isTypeAlias,
                getTypeData(),
                getInfo(), Collections.emptyList());

        // Set argument types
        // System.out.println("TEMPLATE ARG TYPES " + getInfo().getExtendedId() + ": COPY (NULL? "
        // + (templateArgumetTypes == null) + ")");
        type.templateArgumetTypes = templateArgumetTypes;

        return type;
    }

    public String getTemplateName() {
        return templateName;
    }

    @Override
    public List<String> getTemplateArgs() {
        return templateArguments;
    }

    public List<Type> getTemplateArgumetTypes() {
        if (templateArgumetTypes == null) {
            SpecsLogs.msgWarn("Template argument types not set yet for type '" + getInfo().getExtendedId() + "'!");
            return Collections.emptyList();
        }
        // System.out.println("TEMPLATE ARG TYPES " + getInfo().getExtendedId() + ": GET (NOT NULL)");
        return templateArgumetTypes;
    }

    public List<TemplateArgument> getTemplateNodes() {
        return SpecsCollections.cast(getChildren().subList(0, getTemplateArgs().size()), TemplateArgument.class);
    }

    public Optional<Type> getAliasedType() {
        if (!isTypeAlias) {
            return Optional.empty();
        }

        return Optional.of(getChild(Type.class, getTemplateArgs().size()));
    }

    @Override
    public Type desugar() {
        return this;
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

        code.append(getTemplateName());

        String templateArgs = getTemplateArgs().stream()
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

    public void setArgsTypes(List<Type> argsTypes) {
        // System.out.println("TEMPLATE ARG TYPES " + getInfo().getExtendedId() + ": SET");
        if (templateArgumetTypes != null) {
            throw new RuntimeException("Expected argument types to be null");
        }

        templateArgumetTypes = argsTypes;
    }

    // @Override
    // public String getCode() {
    // return templateName + "<" + getTemplateArgument().getCode() + ">";
    // }

}
