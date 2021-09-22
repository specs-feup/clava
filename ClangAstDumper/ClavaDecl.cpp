//
// Created by JoaoBispo on 16/08/2018.
//

#include "ClavaDecl.h"

#include <stdexcept>

const std::string clava::getName(const DeclNode declNode) {
    switch(declNode) {
        case clava::DeclNode::DECL: return "Decl";
        case clava::DeclNode::NAMED_DECL: return "NamedDecl";
        case clava::DeclNode::VALUE_DECL: return "ValueDecl";
        case clava::DeclNode::DECLARATOR_DECL: return "DeclaratorDecl";
        case clava::DeclNode::FIELD_DECL: return "FieldDecl";
        case clava::DeclNode::TYPE_DECL: return "TypeDecl";
        case clava::DeclNode::UNRESOLVED_USING_TYPENAME_DECL: return "UnresolvedUsingTypenameDecl";
        case clava::DeclNode::FUNCTION_DECL: return "FunctionDecl";
        case clava::DeclNode::CXX_METHOD_DECL: return "CXXMethodDecl";
        case clava::DeclNode::CXX_CONSTRUCTOR_DECL: return "CXXConstructorDecl";
        case clava::DeclNode::CXX_CONVERSION_DECL: return "CXXConversionDecl";
        case clava::DeclNode::VAR_DECL: return "VarDecl";
        case clava::DeclNode::PARM_VAR_DECL: return "ParmVarDecl";
        case clava::DeclNode::TAG_DECL: return "TagDecl";
        case clava::DeclNode::ENUM_DECL: return "EnumDecl";
        case clava::DeclNode::RECORD_DECL: return "RecordDecl";
        case clava::DeclNode::CXX_RECORD_DECL: return "CXXRecordDecl";
        case clava::DeclNode::CLASS_TEMPLATE_SPECIALIZATION_DECL: return "ClassTemplateSpecializationDecl";
        case clava::DeclNode::CLASS_TEMPLATE_PARTIAL_SPECIALIZATION_DECL: return "ClassTemplatePartialSpecializationDecl";
        case clava::DeclNode::TEMPLATE_DECL: return "TemplateDecl";
        //case clava::DeclNode::REDECLARABLE_TEMPLATE_DECL: return "RedeclarableTemplateDecl";
        //case clava::DeclNode::CLASS_TEMPLATE_DECL: return "ClassTemplateDecl";
        case clava::DeclNode::TEMPLATE_TYPE_PARM_DECL: return "TemplateTypeParmDecl";
        case clava::DeclNode::ENUM_CONSTANT_DECL: return "EnumConstantDecl";
        case clava::DeclNode::USING_SHADOW_DECL: return "UsingShadowDecl";
        case clava::DeclNode::TYPEDEF_NAME_DECL: return "TypedefNameDecl";
        case clava::DeclNode::TYPE_ALIAS_DECL: return "TypeAliasDecl";
        case clava::DeclNode::TYPEDEF_DECL: return "TypedefDecl";
        case clava::DeclNode::ACCESS_SPEC_DECL: return "AccessSpecDecl";
        case clava::DeclNode::USING_DECL: return "UsingDecl";
        case clava::DeclNode::USING_DIRECTIVE_DECL: return "UsingDirectiveDecl";
        case clava::DeclNode::NAMESPACE_DECL: return "NamespaceDecl";
        case clava::DeclNode::FRIEND_DECL: return "FriendDecl";
        case clava::DeclNode::NAMESPACE_ALIAS_DECL: return "NamespaceAliasDecl";
        case clava::DeclNode::LINKAGE_SPEC_DECL: return "LinkageSpecDecl";
        case clava::DeclNode::LABEL_DECL: return "LabelDecl";
        case clava::DeclNode::STATIC_ASSERT_DECL: return "StaticAssertDecl";
        case clava::DeclNode::TEMPLATE_TEMPLATE_PARM_DECL: return "TemplateTemplateParmDecl";
        case clava::DeclNode::NON_TYPE_TEMPLATE_PARM_DECL: return "NonTypeTemplateParmDecl";
        case clava::DeclNode::VAR_TEMPLATE_SPECIALIZATION_DECL: return "VarTemplateSpecializationDecl";
        case clava::DeclNode::MS_PROPERTY_DECL: return "MSPropertyDecl";



        default: {
            std::string enumValue = std::to_string(static_cast<std::underlying_type<DeclNode>::type>(declNode));
            throw std::invalid_argument("ClavaConstants::getDataName(DeclNode):: Case not implemented, '" + enumValue + "'");
        }
    }
}