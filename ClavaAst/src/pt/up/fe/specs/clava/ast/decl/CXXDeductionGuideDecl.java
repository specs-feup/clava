/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.data.ExplicitSpecifier;
import pt.up.fe.specs.clava.ast.decl.enums.DeductionCandidateKind;
import pt.up.fe.specs.clava.ast.expr.Expr;

/** A C++ class-template deduction guide with its Clang semantic links. */
public class CXXDeductionGuideDecl extends FunctionDecl {

    public static final DataKey<TemplateDecl> DEDUCED_TEMPLATE = KeyFactory.object(
            "deducedTemplate", TemplateDecl.class);

    public static final DataKey<Optional<CXXConstructorDecl>> CORRESPONDING_CONSTRUCTOR =
            KeyFactory.optional("correspondingConstructor");

    public static final DataKey<Boolean> IS_EXPLICIT = KeyFactory.bool("isExplicit");

    public static final DataKey<ExplicitSpecifier> EXPLICIT_SPECIFIER = KeyFactory.object(
            "explicitSpecifier", ExplicitSpecifier.class);

    public static final DataKey<DeductionCandidateKind> DEDUCTION_CANDIDATE_KIND =
            KeyFactory.enumeration("deductionCandidateKind", DeductionCandidateKind.class);

    public static final DataKey<Optional<Expr>> TEMPLATE_REQUIRES_CLAUSE =
            KeyFactory.optional("templateRequiresClause");

    public CXXDeductionGuideDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public TemplateDecl getDeducedTemplate() {
        return get(DEDUCED_TEMPLATE);
    }

    @Override
    protected String getNameForCode() {
        // Clang stores a special CXXDeductionGuideName, not the source identifier.
        return getDeducedTemplate().getDeclName();
    }

    @Override
    public String getCode() {
        if (get(IS_IMPLICIT)) {
            return "";
        }

        StringBuilder code = new StringBuilder();
        code.append(getTemplateHeadersCode());
        get(TEMPLATE_REQUIRES_CLAUSE)
                .ifPresent(expression -> code.append("requires ").append(expression.getCode()).append("\n"));
        code.append(getAttributesCode());

        ExplicitSpecifier explicitSpecifier = get(EXPLICIT_SPECIFIER);
        if (explicitSpecifier.get(ExplicitSpecifier.IS_SPECIFIED)) {
            Optional<Expr> explicitExpression = explicitSpecifier.get(ExplicitSpecifier.EXPR);
            if (explicitExpression.isPresent()) {
                code.append("explicit(").append(explicitExpression.get().getCode()).append(") ");
            } else if (get(IS_EXPLICIT)) {
                code.append("explicit ");
            }
        }

        code.append(getCurrentQualifiedPrefix().map(prefix -> prefix + "::").orElse(""));
        code.append(getNameForCode()).append("(").append(getParametersCode()).append(") -> ");
        code.append(getReturnType().getCode(this));
        code.append(getCodeBody());
        return code.toString();
    }
}
