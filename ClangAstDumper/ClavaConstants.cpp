//
// Created by JoaoBispo on 02/04/2018.
//

#include "ClavaConstants.h"

#include <string>
#include <stdexcept>

const std::string clava::getName(const DeclNode declNode) {
    switch(declNode) {
        case clava::DeclNode::DECL: return "Decl";
        case clava::DeclNode::NAMED_DECL: return "NamedDecl";
        case clava::DeclNode::VALUE_DECL: return "ValueDecl";
        case clava::DeclNode::DECLARATOR_DECL: return "DeclaratorDecl";
        case clava::DeclNode::TYPE_DECL: return "TypeDecl";
        case clava::DeclNode::FUNCTION_DECL: return "FunctionDecl";
        case clava::DeclNode::CXX_METHOD_DECL: return "CXXMethodDecl";
        case clava::DeclNode::VAR_DECL: return "VarDecl";
        case clava::DeclNode::PARM_VAR_DECL: return "ParmVarDecl";
        case clava::DeclNode::TAG_DECL: return "TagDecl";
        case clava::DeclNode::RECORD_DECL: return "RecordDecl";
        case clava::DeclNode::CXX_RECORD_DECL: return "CXXRecordDecl";

        default: {
            std::string enumValue = std::to_string(static_cast<std::underlying_type<DeclNode>::type>(declNode));
            throw std::invalid_argument("ClavaConstants::getDataName(DeclNode):: Case not implemented, '" + enumValue + "'");
        }
    }
}

const std::string clava::getName(const StmtNode stmtNode) {
    switch(stmtNode) {
        // STMTS
        case clava::StmtNode::STMT: return "Stmt";
        case clava::StmtNode::COMPOUND_STMT: return "CompoundStmt";
        case clava::StmtNode::DECL_STMT: return "DeclStmt";


        // EXPRS
        case clava::StmtNode::EXPR: return "Expr";
        case clava::StmtNode::CAST_EXPR: return "CastExpr";
        //case clava::StmtNode::CXX_FUNCTIONAL_CAST_EXPR: return "CXXFunctionalCastExpr";
        case clava::StmtNode::LITERAL: return "Literal";
        case clava::StmtNode::CHARACTER_LITERAL: return "CharacterLiteral";
        case clava::StmtNode::INTEGER_LITERAL: return "IntegerLiteral";
        case clava::StmtNode::FLOATING_LITERAL: return "FloatingLiteral";
        case clava::StmtNode::CXX_BOOL_LITERAL_EXPR: return "CXXBoolLiteralExpr";
        case clava::StmtNode::COMPOUND_LITERAL_EXPR: return "CompoundLiteralExpr";
        case clava::StmtNode::INIT_LIST_EXPR: return "InitListExpr";
        case clava::StmtNode::STRING_LITERAL: return "StringLiteral";
        case clava::StmtNode::DECL_REF_EXPR: return "DeclRefExpr";
        case clava::StmtNode::DEPENDENT_SCOPE_DECL_REF_EXPR: return "DependentScopeDeclRefExpr";
        case clava::StmtNode::OVERLOAD_EXPR: return "OverloadExpr";
        case clava::StmtNode::UNRESOLVED_LOOKUP_EXPR: return "UnresolvedLookupExpr";
        case clava::StmtNode::UNRESOLVED_MEMBER_EXPR: return "UnresolvedMemberExpr";

        default: {
            std::string enumValue = std::to_string(static_cast<std::underlying_type<StmtNode>::type>(stmtNode));
            throw std::invalid_argument("ClavaConstants::getDataName(StmtNode):: Case not implemented, '" + enumValue + "'");
        }
    }
}

const std::string clava::getName(const TypeNode typeNode) {
    switch(typeNode) {
        case clava::TypeNode::TYPE: return "Type";
        case clava::TypeNode::QUAL_TYPE: return "QualType";
        case clava::TypeNode::BUILTIN_TYPE: return "BuiltinType";
        case clava::TypeNode::FUNCTION_TYPE: return "FunctionType";
        case clava::TypeNode::FUNCTION_PROTO_TYPE: return "FunctionProtoType";
        case clava::TypeNode::FUNCTION_NO_PROTO_TYPE: return "FunctionNoProtoType";
        case clava::TypeNode::TAG_TYPE: return "TagType";
        case clava::TypeNode::RECORD_TYPE: return "RecordType";
        case clava::TypeNode::ENUM_TYPE: return "EnumType";
        case clava::TypeNode::ARRAY_TYPE: return "ArrayType";
        case clava::TypeNode::CONSTANT_ARRAY_TYPE: return "ConstantArrayType";
        case clava::TypeNode::DEPENDENT_SIZED_ARRAY_TYPE: return "DependentSizedArrayType";
        case clava::TypeNode::INCOMPLETE_ARRAY_TYPE: return "IncompleteArrayType";
        case clava::TypeNode::VARIABLE_ARRAY_TYPE: return "VariableArrayType";
        case clava::TypeNode::POINTER_TYPE: return "PointerType";

        default: {
            std::string enumValue = std::to_string(static_cast<std::underlying_type<TypeNode>::type>(typeNode));
            throw std::invalid_argument("ClavaConstants::getDataName(TypeNode):: Case not implemented, '" + enumValue + "'");
        }
    }
}

const std::string clava::getName(const AttrNode attrNode) {
    switch(attrNode) {
        case clava::AttrNode::ATTR: return "Attribute";
        case clava::AttrNode::ALIGNED: return "AlignedAttr";

        default: {
            std::string enumValue = std::to_string(static_cast<std::underlying_type<AttrNode>::type>(attrNode));
            throw std::invalid_argument("ClavaConstants::getDataName(AttrNode):: Case not implemented, '" + enumValue + "'");
        }
    }
}