/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;

public class VarTemplatePartialSpecializationDecl extends VarTemplateSpecializationDecl {

    public static final DataKey<List<NamedDecl>> TEMPLATE_PARAMETERS = KeyFactory.list("templateParameters",
            NamedDecl.class);

    public static final DataKey<Optional<VarTemplatePartialSpecializationDecl>> INSTANTIATED_FROM_MEMBER =
            KeyFactory.optional("instantiatedFromMember");

    public static final DataKey<Boolean> IS_MEMBER_SPECIALIZATION = KeyFactory.bool("isMemberSpecialization");

    public static final DataKey<List<TemplateArgument>> TEMPLATE_ARGUMENTS_AS_WRITTEN = KeyFactory.list(
            "templateArgumentsAsWritten", TemplateArgument.class);

    public VarTemplatePartialSpecializationDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        String parameters = get(TEMPLATE_PARAMETERS).stream()
                .map(ClavaNode::getCode)
                .collect(Collectors.joining(", "));

        return "template<" + parameters + ">\n" + super.getCode(false);
    }

    @Override
    protected List<TemplateArgument> getTemplateArgumentsForCode() {
        var arguments = get(TEMPLATE_ARGUMENTS_AS_WRITTEN);
        return arguments.isEmpty() ? super.getTemplateArgumentsForCode() : arguments;
    }
}
