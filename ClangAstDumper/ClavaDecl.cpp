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
        case clava::DeclNode::FUNCTION_DECL: return "FunctionDecl";
        case clava::DeclNode::CXX_METHOD_DECL: return "CXXMethodDecl";
        case clava::DeclNode::CXX_CONSTRUCTOR_DECL: return "CXXConstructorDecl";
        case clava::DeclNode::VAR_DECL: return "VarDecl";
        case clava::DeclNode::PARM_VAR_DECL: return "ParmVarDecl";
        case clava::DeclNode::TAG_DECL: return "TagDecl";
        case clava::DeclNode::ENUM_DECL: return "EnumDecl";
        case clava::DeclNode::RECORD_DECL: return "RecordDecl";
        case clava::DeclNode::CXX_RECORD_DECL: return "CXXRecordDecl";
        case clava::DeclNode::TEMPLATE_DECL: return "TemplateDecl";
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


        default: {
            std::string enumValue = std::to_string(static_cast<std::underlying_type<DeclNode>::type>(declNode));
            throw std::invalid_argument("ClavaConstants::getDataName(DeclNode):: Case not implemented, '" + enumValue + "'");
        }
    }
}