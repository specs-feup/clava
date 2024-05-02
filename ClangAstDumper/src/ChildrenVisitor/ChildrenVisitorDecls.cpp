//
// Created by JoaoBispo on 20/03/2018.
//

#include "../Clang/ClangNodes.h"
#include "../ClangAstDumper/ClangAstDumper.h"
#include "../ClangEnums/ClangEnums.h"
#include "../Clava/ClavaConstants.h"

#include <string>

const std::map<const std::string, clava::DeclNode>
    ClangAstDumper::DECL_CHILDREN_MAP = {
        {"CXXConstructorDecl", clava::DeclNode::CXX_CONSTRUCTOR_DECL},
        {"CXXConversionDecl", clava::DeclNode::CXX_CONVERSION_DECL},
        {"CXXDestructorDecl", clava::DeclNode::CXX_METHOD_DECL},
        {"CXXMethodDecl", clava::DeclNode::CXX_METHOD_DECL},
        {"EnumDecl", clava::DeclNode::ENUM_DECL},
        {"RecordDecl", clava::DeclNode::RECORD_DECL},
        {"CXXRecordDecl", clava::DeclNode::CXX_RECORD_DECL},
        {"ClassTemplateSpecializationDecl",
         clava::DeclNode::CLASS_TEMPLATE_SPECIALIZATION_DECL},
        {"ClassTemplatePartialSpecializationDecl",
         clava::DeclNode::CLASS_TEMPLATE_SPECIALIZATION_DECL},
        {"FunctionDecl", clava::DeclNode::FUNCTION_DECL},
        {"VarDecl", clava::DeclNode::VAR_DECL},
        {"ParmVarDecl", clava::DeclNode::VAR_DECL},
        {"TypeDecl", clava::DeclNode::TYPE_DECL},
        {"FieldDecl", clava::DeclNode::FIELD_DECL},
        {"ClassTemplateDecl", clava::DeclNode::TEMPLATE_DECL},
        {"FunctionTemplateDecl", clava::DeclNode::TEMPLATE_DECL},
        {"TypeAliasTemplateDecl", clava::DeclNode::TEMPLATE_DECL},
        {"VarTemplateDecl", clava::DeclNode::TEMPLATE_DECL},
        {"TemplateTemplateParmDecl",
         clava::DeclNode::TEMPLATE_TEMPLATE_PARM_DECL},
        {"TemplateTypeParmDecl", clava::DeclNode::TEMPLATE_TYPE_PARM_DECL},
        {"EnumConstantDecl", clava::DeclNode::ENUM_CONSTANT_DECL},
        {"TypeAliasDecl", clava::DeclNode::TYPEDEF_NAME_DECL},
        {"TypedefDecl", clava::DeclNode::TYPEDEF_NAME_DECL},
        {"UsingDirectiveDecl", clava::DeclNode::USING_DIRECTIVE_DECL},
        {"NamespaceDecl", clava::DeclNode::NAMESPACE_DECL},
        {"FriendDecl", clava::DeclNode::FRIEND_DECL},
        {"NamespaceAliasDecl", clava::DeclNode::NAMESPACE_ALIAS_DECL},
        {"LinkageSpecDecl", clava::DeclNode::LINKAGE_SPEC_DECL},
        {"StaticAssertDecl", clava::DeclNode::STATIC_ASSERT_DECL},
        {"NonTypeTemplateParmDecl",
         clava::DeclNode::NON_TYPE_TEMPLATE_PARM_DECL},
        // TODO: Check if needs more data to dump
        {"VarTemplateSpecializationDecl", clava::DeclNode::VAR_DECL},
        {"UsingDecl", clava::DeclNode::USING_DECL},

};

void ClangAstDumper::visitChildren(const Decl *D) {
    // Get classname
    const std::string classname = clava::getClassName(D);

    // Get corresponding DeclNode
    clava::DeclNode declNode = DECL_CHILDREN_MAP.count(classname) == 1
                                   ? DECL_CHILDREN_MAP.find(classname)->second
                                   : clava::DeclNode::DECL;

    visitChildren(declNode, D);
}

void ClangAstDumper::visitChildren(clava::DeclNode declNode, const Decl *D) {

    std::vector<std::string> visitedChildren;

    switch (declNode) {
    case clava::DeclNode::DECL:
        VisitDeclChildren(D, visitedChildren);
        break;
    case clava::DeclNode::NAMED_DECL:
        VisitNamedDeclChildren(static_cast<const NamedDecl *>(D),
                               visitedChildren);
        break;
    case clava::DeclNode::TYPE_DECL:
        VisitTypeDeclChildren(static_cast<const TypeDecl *>(D),
                              visitedChildren);
        break;
    case clava::DeclNode::TAG_DECL:
        VisitTagDeclChildren(static_cast<const TagDecl *>(D), visitedChildren);
        break;
    case clava::DeclNode::ENUM_DECL:
        VisitEnumDeclChildren(static_cast<const EnumDecl *>(D),
                              visitedChildren);
        break;
    case clava::DeclNode::RECORD_DECL:
        VisitRecordDeclChildren(static_cast<const RecordDecl *>(D),
                                visitedChildren);
        break;
    case clava::DeclNode::VALUE_DECL:
        VisitValueDeclChildren(static_cast<const ValueDecl *>(D),
                               visitedChildren);
        break;
    case clava::DeclNode::FIELD_DECL:
        VisitFieldDeclChildren(static_cast<const FieldDecl *>(D),
                               visitedChildren);
        break;
    case clava::DeclNode::FUNCTION_DECL:
        VisitFunctionDeclChildren(static_cast<const FunctionDecl *>(D),
                                  visitedChildren);
        break;
    case clava::DeclNode::CXX_METHOD_DECL:
        VisitCXXMethodDeclChildren(static_cast<const CXXMethodDecl *>(D),
                                   visitedChildren);
        break;
    case clava::DeclNode::CXX_CONSTRUCTOR_DECL:
        VisitCXXConstructorDeclChildren(
            static_cast<const CXXConstructorDecl *>(D), visitedChildren);
        break;
    case clava::DeclNode::CXX_CONVERSION_DECL:
        VisitCXXConversionDeclChildren(
            static_cast<const CXXConversionDecl *>(D), visitedChildren);
        break;
    case clava::DeclNode::CXX_RECORD_DECL:
        VisitCXXRecordDeclChildren(static_cast<const CXXRecordDecl *>(D),
                                   visitedChildren);
        break;
    case clava::DeclNode::CLASS_TEMPLATE_SPECIALIZATION_DECL:
        VisitClassTemplateSpecializationDeclChildren(
            static_cast<const ClassTemplateSpecializationDecl *>(D),
            visitedChildren);
        break;
    case clava::DeclNode::VAR_DECL:
        VisitVarDeclChildren(static_cast<const VarDecl *>(D), visitedChildren);
        break;
    case clava::DeclNode::TEMPLATE_DECL:
        VisitTemplateDeclChildren(static_cast<const TemplateDecl *>(D),
                                  visitedChildren);
        break;
    case clava::DeclNode::TEMPLATE_TEMPLATE_PARM_DECL:
        VisitTemplateTemplateParmDeclChildren(
            static_cast<const TemplateTemplateParmDecl *>(D), visitedChildren);
        break;
    case clava::DeclNode::TEMPLATE_TYPE_PARM_DECL:
        VisitTemplateTypeParmDeclChildren(
            static_cast<const TemplateTypeParmDecl *>(D), visitedChildren);
        break;
    case clava::DeclNode::ENUM_CONSTANT_DECL:
        VisitEnumConstantDeclChildren(static_cast<const EnumConstantDecl *>(D),
                                      visitedChildren);
        break;
    case clava::DeclNode::TYPEDEF_NAME_DECL:
        VisitTypedefNameDeclChildren(static_cast<const TypedefNameDecl *>(D),
                                     visitedChildren);
        break;
    case clava::DeclNode::USING_DIRECTIVE_DECL:
        VisitUsingDirectiveDeclChildren(
            static_cast<const UsingDirectiveDecl *>(D), visitedChildren);
        break;
    case clava::DeclNode::NAMESPACE_DECL:
        VisitNamespaceDeclChildren(static_cast<const NamespaceDecl *>(D),
                                   visitedChildren);
        break;
    case clava::DeclNode::FRIEND_DECL:
        VisitFriendDeclChildren(static_cast<const FriendDecl *>(D),
                                visitedChildren);
        break;
    case clava::DeclNode::NAMESPACE_ALIAS_DECL:
        VisitNamespaceAliasDeclChildren(
            static_cast<const NamespaceAliasDecl *>(D), visitedChildren);
        break;
    case clava::DeclNode::LINKAGE_SPEC_DECL:
        VisitLinkageSpecDeclChildren(static_cast<const LinkageSpecDecl *>(D),
                                     visitedChildren);
        break;
    case clava::DeclNode::STATIC_ASSERT_DECL:
        VisitStaticAssertDeclChildren(static_cast<const StaticAssertDecl *>(D),
                                      visitedChildren);
        break;
    case clava::DeclNode::NON_TYPE_TEMPLATE_PARM_DECL:
        VisitNonTypeTemplateParmDeclChildren(
            static_cast<const NonTypeTemplateParmDecl *>(D), visitedChildren);
        break;
    case clava::DeclNode::USING_DECL:
        VisitUsingDeclChildren(static_cast<const UsingDecl *>(D),
                               visitedChildren);
        break;
    default:
        throw std::invalid_argument(
            "ChildrenVisitorDecls::visitChildren: Case not implemented, '" +
            clava::getName(declNode) + "'");
    }

    dumpVisitedChildren(D, visitedChildren);
}

void ClangAstDumper::VisitDeclChildren(const Decl *D,
                                       std::vector<std::string> &children) {
    // Visit attributes
    for (Decl::attr_iterator I = D->attr_begin(), E = D->attr_end(); I != E;
         ++I) {
        const Attr *attr = *I;
        VisitAttrTop(attr);
        dumpTopLevelAttr(attr);
    }
}

void ClangAstDumper::VisitNamedDeclChildren(
    const NamedDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitDeclChildren(D, children);
}

void ClangAstDumper::VisitTypeDeclChildren(const TypeDecl *D,
                                           std::vector<std::string> &children) {
    // Hierarchy
    VisitNamedDeclChildren(D, children);

    // Visit type
    VisitTypeTop(D->getTypeForDecl());
    dumpType(D->getTypeForDecl());
}

void ClangAstDumper::VisitTagDeclChildren(const TagDecl *D,
                                          std::vector<std::string> &children) {
    // Hierarchy
    VisitTypeDeclChildren(D, children);

    addChildren(D->decls(), children);
}

void ClangAstDumper::VisitEnumDeclChildren(const EnumDecl *D,
                                           std::vector<std::string> &children) {
    // Hierarchy
    VisitTagDeclChildren(D, children);

    // Visit type
    VisitTypeTop(D->getIntegerType());
}

void ClangAstDumper::VisitValueDeclChildren(
    const ValueDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitNamedDeclChildren(D, children);

    // Visit type
    VisitTypeTop(D->getType());
    dumpTopLevelType(D->getType());
}
void ClangAstDumper::VisitFieldDeclChildren(
    const FieldDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitValueDeclChildren(D, children);

    // Add bitwidth
    addChild(D->getBitWidth(), children);

    // Add init
    addChild(D->getInClassInitializer(), children);
}

void ClangAstDumper::VisitFunctionDeclChildren(
    const FunctionDecl *D, std::vector<std::string> &children) {

    // Hierarchy
    VisitValueDeclChildren(D, children);

    // Visit canonical and previous decls
    VisitDeclTop(D->getPreviousDecl());
    VisitDeclTop(D->getCanonicalDecl());

    if (FunctionTemplateDecl const *primaryTemplate = D->getPrimaryTemplate();
        primaryTemplate != nullptr) {
        VisitDeclTop(primaryTemplate->getTemplatedDecl());
    }

    // Visit template arguments
    if (auto templateSpecializationArgs = D->getTemplateSpecializationArgs();
        templateSpecializationArgs != nullptr) {
        for (auto const &templateArg : templateSpecializationArgs->asArray()) {
            VisitTemplateArgument(templateArg);
        }
    }

    // Visit parameters
    for (auto param : D->parameters()) {
        addChild(param, children);
    }

    // Visit body
    if (D->doesThisDeclarationHaveABody()) {
        addChild(D->getBody(), children);
    }
}

void ClangAstDumper::VisitCXXMethodDeclChildren(
    const CXXMethodDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitFunctionDeclChildren(D, children);

    // Visit record decl
    VisitDeclTop(D->getParent());

    // Visit overridden methods
    for (auto overriddenMethod : D->overridden_methods()) {
        VisitDeclTop(overriddenMethod);
    }

    // Types related to "this"
    if (D->isInstance()) {
        VisitTypeTop(D->getThisType());
        VisitTypeTop(D->getThisObjectType());
    }
}

void ClangAstDumper::VisitCXXConstructorDeclChildren(
    const CXXConstructorDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitCXXMethodDeclChildren(D, children);

    // Visit CXXCtorInitializers
    for (auto init = D->init_begin(), init_last = D->init_end();
         init != init_last; ++init) {
        // Init expr
        VisitStmtTop((*init)->getInit());

        if ((*init)->isAnyMemberInitializer()) {
            VisitDeclTop((*init)->getAnyMember());
            continue;
        }

        if ((*init)->isBaseInitializer()) {
            VisitTypeTop((*init)->getBaseClass());
            continue;
        }

        if ((*init)->isDelegatingInitializer()) {
            VisitTypeTop((*init)->getTypeSourceInfo()->getType());
            continue;
        }

        throw std::invalid_argument(
            "ClangDataDumper::VisitCXXConstructorDeclChildren():: "
            "CXXCtorInitializer case not implemented");
    }

    // Visit ExplicitSpecifier Expression
    VisitExpr(D->getExplicitSpecifier().getExpr());
}

void ClangAstDumper::VisitCXXConversionDeclChildren(
    const CXXConversionDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitCXXMethodDeclChildren(D, children);

    // Visit fields
    VisitTypeTop(D->getConversionType());
}

void ClangAstDumper::VisitRecordDeclChildren(
    const RecordDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitTagDeclChildren(D, children);
}

void ClangAstDumper::VisitCXXRecordDeclChildren(
    const CXXRecordDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitRecordDeclChildren(D, children);

    if (D->hasDefinition()) {
        // Visit types in bases
        for (const auto &I : D->bases()) {
            VisitTypeTop(I.getType());
        }
        VisitDeclTop(D->getDefinition());
    }
}
void ClangAstDumper::VisitClassTemplateSpecializationDeclChildren(
    const ClassTemplateSpecializationDecl *D,
    std::vector<std::string> &children) {
    // Hierarchy
    VisitCXXRecordDeclChildren(D, children);

    VisitDeclTop(D->getSpecializedTemplate());

    // Visit template arguments
    auto &templateArgs = D->getTemplateArgs();
    for (auto &templateArg : templateArgs.asArray()) {
        VisitTemplateArgument(templateArg);
    }
}

void ClangAstDumper::VisitVarDeclChildren(const VarDecl *D,
                                          std::vector<std::string> &children) {
    // Hierarchy
    VisitValueDeclChildren(D, children);

    if (D->hasInit()) {
        addChild(D->getInit(), children);
    }
}

void ClangAstDumper::VisitTemplateDeclChildren(
    const TemplateDecl *D, std::vector<std::string> &children) {

    // Hierarchy
    VisitNamedDeclChildren(D, children);

    if (auto templateParams = D->getTemplateParameters()) {
        for (auto I = templateParams->begin(), E = templateParams->end();
             I != E; ++I) {
            addChild(*I, children);
            VisitDeclTop(*I);
        }
    }

    addChild(D->getTemplatedDecl(), children);
    VisitDeclTop(D->getTemplatedDecl());
}

void ClangAstDumper::VisitTemplateTemplateParmDeclChildren(
    const TemplateTemplateParmDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitTemplateDeclChildren(D, children);

    if (D->hasDefaultArgument()) {
        VisitTemplateArgument(D->getDefaultArgument().getArgument());
    }
}

void ClangAstDumper::VisitTemplateTypeParmDeclChildren(
    const TemplateTypeParmDecl *D, std::vector<std::string> &children) {

    // Hierarchy
    VisitTypeDeclChildren(D, children);

    if (D->hasDefaultArgument()) {
        VisitTypeTop(D->getDefaultArgument());
    }
}

void ClangAstDumper::VisitEnumConstantDeclChildren(
    const EnumConstantDecl *D, std::vector<std::string> &children) {

    // Hierarchy
    VisitValueDeclChildren(D, children);

    addChild(D->getInitExpr(), children);
}

void ClangAstDumper::VisitTypedefNameDeclChildren(
    const TypedefNameDecl *D, std::vector<std::string> &children) {

    // Hierarchy
    VisitTypeDeclChildren(D, children);

    VisitTypeTop(D->getUnderlyingType());
}

void ClangAstDumper::VisitUsingDirectiveDeclChildren(
    const UsingDirectiveDecl *D, std::vector<std::string> &children) {

    // Hierarchy
    VisitNamedDeclChildren(D, children);

    VisitDeclTop(D->getNominatedNamespace());
    VisitDeclTop(D->getNominatedNamespaceAsWritten());
}

void ClangAstDumper::VisitNamespaceDeclChildren(
    const NamespaceDecl *D, std::vector<std::string> &children) {

    // Hierarchy
    VisitNamedDeclChildren(D, children);

    addChildren(D->decls(), children);
}

void ClangAstDumper::VisitFriendDeclChildren(
    const FriendDecl *D, std::vector<std::string> &children) {

    // Hierarchy
    VisitDeclChildren(D, children);

    if (D->getFriendDecl() != nullptr) {
        addChild(D->getFriendDecl(), children);
    } else if (D->getFriendType() != nullptr) {
        addChild(D->getFriendType()->getType(), children);
    } else {
        // Add a null node
        addChild((const Decl *)nullptr, children);
    }
}

void ClangAstDumper::VisitNamespaceAliasDeclChildren(
    const NamespaceAliasDecl *D, std::vector<std::string> &children) {

    // Hierarchy
    VisitNamedDeclChildren(D, children);

    VisitDeclTop(D->getAliasedNamespace());
}

void ClangAstDumper::VisitLinkageSpecDeclChildren(
    const LinkageSpecDecl *D, std::vector<std::string> &children) {

    addChildren(D->decls(), children);
}

void ClangAstDumper::VisitStaticAssertDeclChildren(
    const StaticAssertDecl *D, std::vector<std::string> &children) {

    addChild(D->getAssertExpr(), children);
    addChild(D->getMessage(), children);
}

void ClangAstDumper::VisitNonTypeTemplateParmDeclChildren(
    const NonTypeTemplateParmDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitValueDeclChildren(D, children);

    if (D->hasDefaultArgument()) {
        VisitStmtTop(D->getDefaultArgument());
    }

    if (D->isExpandedParameterPack()) {
        for (unsigned int i = 0; i < D->getNumExpansionTypes(); i++) {
            VisitTypeTop(D->getExpansionType(i));
        }
    }
}

void ClangAstDumper::VisitUsingDeclChildren(
    const UsingDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitNamedDeclChildren(D, children);

    ClangAstDumper::VisitNestedNameSpecifierChildren(D->getQualifier());
}