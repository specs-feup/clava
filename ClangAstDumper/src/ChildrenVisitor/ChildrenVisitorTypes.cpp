//
// Created by JoaoBispo on 05/04/2018.
//
#include "../Clang/ClangNodes.h"
#include "../ClangAstDumper/ClangAstDumper.h"
#include "../Clava/ClavaConstants.h"

#include <string>

const std::map<const std::string, clava::TypeNode>
    ClangAstDumper::TYPE_CHILDREN_MAP = {
        {"FunctionProtoType", clava::TypeNode::FUNCTION_PROTO_TYPE},
        {"FunctionNoProtoType", clava::TypeNode::FUNCTION_TYPE},
        {"ConstantArrayType", clava::TypeNode::ARRAY_TYPE},
        {"DependentSizedArrayType",
         clava::TypeNode::DEPENDENT_SIZED_ARRAY_TYPE},
        {"IncompleteArrayType", clava::TypeNode::ARRAY_TYPE},
        {"VariableArrayType", clava::TypeNode::VARIABLE_ARRAY_TYPE},
        {"PointerType", clava::TypeNode::POINTER_TYPE},
        {"EnumType", clava::TypeNode::TAG_TYPE},
        {"RecordType", clava::TypeNode::TAG_TYPE},
        {"ElaboratedType", clava::TypeNode::ELABORATED_TYPE},
        {"LValueReferenceType", clava::TypeNode::REFERENCE_TYPE},
        {"RValueReferenceType", clava::TypeNode::REFERENCE_TYPE},
        {"InjectedClassNameType", clava::TypeNode::INJECTED_CLASS_NAME_TYPE},
        {"TemplateTypeParmType", clava::TypeNode::TEMPLATE_TYPE_PARM_TYPE},
        {"SubstTemplateTypeParmType",
         clava::TypeNode::SUBST_TEMPLATE_TYPE_PARM_TYPE},
        {"TemplateSpecializationType",
         clava::TypeNode::TEMPLATE_SPECIALIZATION_TYPE},
        {"TypedefType", clava::TypeNode::TYPEDEF_TYPE},
        {"DecayedType", clava::TypeNode::DECAYED_TYPE},
        {"DecltypeType", clava::TypeNode::DECLTYPE_TYPE},
        {"AutoType", clava::TypeNode::AUTO_TYPE},
        {"PackExpansionType", clava::TypeNode::PACK_EXPANSION_TYPE},
        {"TypeOfExprType", clava::TypeNode::TYPE_OF_EXPR_TYPE},
        {"AttributedType", clava::TypeNode::ATTRIBUTED_TYPE},
        {"UnaryTransformType", clava::TypeNode::UNARY_TRANSFORM_TYPE},
        {"ComplexType", clava::TypeNode::COMPLEX_TYPE},
};

void ClangAstDumper::visitChildren(const Type *T) {
    // Get classname
    const std::string classname = clava::getClassName(T);

    // Get corresponding ExprNode
    clava::TypeNode typeNode = TYPE_CHILDREN_MAP.count(classname) == 1
                                   ? TYPE_CHILDREN_MAP.find(classname)->second
                                   : clava::TypeNode::TYPE;

    visitChildren(typeNode, T);
}

void ClangAstDumper::visitChildren(clava::TypeNode typeNode, const Type *T) {

    std::vector<std::string> visitedChildren;

    switch (typeNode) {
    case clava::TypeNode::TYPE:
        VisitTypeChildren(T, visitedChildren);
        break;
    case clava::TypeNode::FUNCTION_TYPE:
        VisitFunctionTypeChildren(static_cast<const FunctionType *>(T),
                                  visitedChildren);
        break;
    case clava::TypeNode::FUNCTION_PROTO_TYPE:
        VisitFunctionProtoTypeChildren(
            static_cast<const FunctionProtoType *>(T), visitedChildren);
        break;
    case clava::TypeNode::ARRAY_TYPE:
        VisitArrayTypeChildren(static_cast<const ArrayType *>(T),
                               visitedChildren);
        break;
    case clava::TypeNode::VARIABLE_ARRAY_TYPE:
        VisitVariableArrayTypeChildren(
            static_cast<const VariableArrayType *>(T), visitedChildren);
        break;
    case clava::TypeNode::DEPENDENT_SIZED_ARRAY_TYPE:
        VisitDependentSizedArrayTypeChildren(
            static_cast<const DependentSizedArrayType *>(T), visitedChildren);
        break;
    case clava::TypeNode::POINTER_TYPE:
        VisitPointerTypeChildren(static_cast<const PointerType *>(T),
                                 visitedChildren);
        break;
    case clava::TypeNode::TAG_TYPE:
        VisitTagTypeChildren(static_cast<const TagType *>(T), visitedChildren);
        break;
    case clava::TypeNode::ELABORATED_TYPE:
        VisitElaboratedTypeChildren(static_cast<const ElaboratedType *>(T),
                                    visitedChildren);
        break;
    case clava::TypeNode::REFERENCE_TYPE:
        VisitReferenceTypeChildren(static_cast<const ReferenceType *>(T),
                                   visitedChildren);
        break;
    case clava::TypeNode::INJECTED_CLASS_NAME_TYPE:
        VisitInjectedClassNameTypeChildren(
            static_cast<const InjectedClassNameType *>(T), visitedChildren);
        break;
    case clava::TypeNode::TEMPLATE_TYPE_PARM_TYPE:
        VisitTemplateTypeParmTypeChildren(
            static_cast<const TemplateTypeParmType *>(T), visitedChildren);
        break;
    case clava::TypeNode::SUBST_TEMPLATE_TYPE_PARM_TYPE:
        VisitSubstTemplateTypeParmTypeChildren(
            static_cast<const SubstTemplateTypeParmType *>(T), visitedChildren);
        break;
    case clava::TypeNode::TEMPLATE_SPECIALIZATION_TYPE:
        VisitTemplateSpecializationTypeChildren(
            static_cast<const TemplateSpecializationType *>(T),
            visitedChildren);
        break;
    case clava::TypeNode::TYPEDEF_TYPE:
        VisitTypedefTypeChildren(static_cast<const TypedefType *>(T),
                                 visitedChildren);
        break;
    case clava::TypeNode::ADJUSTED_TYPE:
        VisitAdjustedTypeChildren(static_cast<const AdjustedType *>(T),
                                  visitedChildren);
        break;
    case clava::TypeNode::DECAYED_TYPE:
        VisitDecayedTypeChildren(static_cast<const DecayedType *>(T),
                                 visitedChildren);
        break;
    case clava::TypeNode::DECLTYPE_TYPE:
        VisitDecltypeTypeChildren(static_cast<const DecltypeType *>(T),
                                  visitedChildren);
        break;
    case clava::TypeNode::AUTO_TYPE:
        VisitAutoTypeChildren(static_cast<const AutoType *>(T),
                              visitedChildren);
        break;
    case clava::TypeNode::PACK_EXPANSION_TYPE:
        VisitPackExpansionTypeChildren(
            static_cast<const PackExpansionType *>(T), visitedChildren);
        break;
    case clava::TypeNode::TYPE_OF_EXPR_TYPE:
        VisitTypeOfExprTypeChildren(static_cast<const TypeOfExprType *>(T),
                                    visitedChildren);
        break;
    case clava::TypeNode::ATTRIBUTED_TYPE:
        VisitAttributedTypeChildren(static_cast<const AttributedType *>(T),
                                    visitedChildren);
        break;
    case clava::TypeNode::UNARY_TRANSFORM_TYPE:
        VisitUnaryTransformTypeChildren(
            static_cast<const UnaryTransformType *>(T), visitedChildren);
        break;
    case clava::TypeNode::COMPLEX_TYPE:
        VisitComplexTypeChildren(static_cast<const ComplexType *>(T),
                                 visitedChildren);
        break;

    default:
        throw std::invalid_argument("ChildrenVisitorTypes::visitChildren("
                                    "TypeNode): Case not implemented, '" +
                                    clava::getName(typeNode) + "'");
    }

    dumpVisitedChildren(T, visitedChildren);
}

void ClangAstDumper::visitChildren(const QualType &T) {
    std::vector<std::string> visitedChildren;

    VisitTypeTop(T.getTypePtr());

    // Visit unqualified type
    VisitTypeTop(
        T.getSingleStepDesugaredType(*const_cast<const ASTContext *>(Context)));

    dumpVisitedChildren(T.getAsOpaquePtr(), visitedChildren);
}

void ClangAstDumper::VisitTypeChildren(
    const Type *T, std::vector<std::string> &visitedChildren) {

    // If has sugar, visit desugared type
    QualType singleStepDesugar =
        T->getLocallyUnqualifiedSingleStepDesugaredType();

    if (singleStepDesugar != QualType(T, 0)) {
        VisitTypeTop(singleStepDesugar);
    }
}

void ClangAstDumper::VisitFunctionTypeChildren(
    const FunctionType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Return type
    VisitTypeTop(T->getReturnType());
}

void ClangAstDumper::VisitFunctionProtoTypeChildren(
    const FunctionProtoType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitFunctionTypeChildren(T, visitedChildren);

    // Parameters types
    for (QualType paramType : T->getParamTypes()) {
        VisitTypeTop(paramType);
    }

    const auto &exptSpec = T->getExtProtoInfo().ExceptionSpec;

    // Visit exception types
    for (auto &exceptType : exptSpec.Exceptions) {
        VisitTypeTop(exceptType);
    }

    // Visit noexcept expression, if present
    VisitStmtTop(exptSpec.NoexceptExpr);

    // Visit source decl, if present
    VisitDeclTop(exptSpec.SourceDecl);

    // Visit source template, if present
    VisitDeclTop(exptSpec.SourceTemplate);
}

void ClangAstDumper::VisitTagTypeChildren(
    const TagType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Just visit decl
    VisitDeclTop(T->getDecl());
}

void ClangAstDumper::VisitArrayTypeChildren(
    const ArrayType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Element type
    VisitTypeTop(T->getElementType());
}

void ClangAstDumper::VisitVariableArrayTypeChildren(
    const VariableArrayType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitArrayTypeChildren(T, visitedChildren);

    // Visit and add size expression
    VisitStmtTop(T->getSizeExpr());
}

void ClangAstDumper::VisitDependentSizedArrayTypeChildren(
    const DependentSizedArrayType *T,
    std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitArrayTypeChildren(T, visitedChildren);

    // Visit and add size expression
    VisitStmtTop(T->getSizeExpr());
}

void ClangAstDumper::VisitPointerTypeChildren(
    const PointerType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Visit pointee
    VisitTypeTop(T->getPointeeType());
}

void ClangAstDumper::VisitElaboratedTypeChildren(
    const ElaboratedType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Visit named type
    VisitTypeTop(T->getNamedType());
}

void ClangAstDumper::VisitReferenceTypeChildren(
    const ReferenceType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    VisitTypeTop(T->getPointeeTypeAsWritten());
}

void ClangAstDumper::VisitInjectedClassNameTypeChildren(
    const InjectedClassNameType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Visit decl
    VisitDeclTop(T->getDecl());
}

void ClangAstDumper::VisitTemplateTypeParmTypeChildren(
    const TemplateTypeParmType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Visit decl
    VisitDeclTop(T->getDecl());
}

void ClangAstDumper::VisitSubstTemplateTypeParmTypeChildren(
    const SubstTemplateTypeParmType *T,
    std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    VisitTypeTop(T->getReplacedParameter());
    VisitTypeTop(T->getReplacementType());
};

void ClangAstDumper::VisitTemplateSpecializationTypeChildren(
    const TemplateSpecializationType *T,
    std::vector<std::string> &visitedChildren) {

    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Visit each argument
    for (unsigned i = 0; i < T->getNumArgs(); i++) {
        VisitTemplateArgChildren(T->getArg(i));
    }

    // Visit type alias
    if (T->isTypeAlias()) {
        VisitTypeTop(T->getAliasedType());
    }

    // Visit template delc, if present
    VisitDeclTop(T->getTemplateName().getAsTemplateDecl());
};

void ClangAstDumper::VisitTypedefTypeChildren(
    const TypedefType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    VisitDeclTop(T->getDecl());
    VisitTypeTop(T->getPointeeType());
};

void ClangAstDumper::VisitAdjustedTypeChildren(
    const AdjustedType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Original type
    VisitTypeTop(T->getOriginalType());

    // Adjusted type
    VisitTypeTop(T->getAdjustedType());
};

void ClangAstDumper::VisitDecayedTypeChildren(
    const DecayedType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitAdjustedTypeChildren(T, visitedChildren);

    VisitTypeTop(T->getDecayedType());

    VisitTypeTop(T->getPointeeType());
};

void ClangAstDumper::VisitDecltypeTypeChildren(
    const DecltypeType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    VisitStmtTop(T->getUnderlyingExpr());
};

void ClangAstDumper::VisitAutoTypeChildren(
    const AutoType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    VisitTypeTop(T->getDeducedType());
};

void ClangAstDumper::VisitPackExpansionTypeChildren(
    const PackExpansionType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    if (!T->isSugared()) {
        VisitTypeTop(T->getPattern());
    }
};

void ClangAstDumper::VisitTypeOfExprTypeChildren(
    const TypeOfExprType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    VisitStmtTop(T->getUnderlyingExpr());
};

void ClangAstDumper::VisitAttributedTypeChildren(
    const AttributedType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    VisitTypeTop(T->getModifiedType());
    VisitTypeTop(T->getEquivalentType());
};

void ClangAstDumper::VisitUnaryTransformTypeChildren(
    const UnaryTransformType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    VisitTypeTop(T->getUnderlyingType());
    VisitTypeTop(T->getBaseType());
};

void ClangAstDumper::VisitComplexTypeChildren(
    const ComplexType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    VisitTypeTop(T->getElementType());
};
