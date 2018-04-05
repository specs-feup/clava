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
        case clava::DeclNode::FUNCTION_DECL: return "FunctionDecl";
        case clava::DeclNode::CXX_METHOD_DECL: return "CXXMethodDecl";
        case clava::DeclNode::VAR_DECL: return "VarDecl";
        case clava::DeclNode::PARM_VAR_DECL: return "ParmVarDecl";
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
        case clava::StmtNode::CHARACTER_LITERAL: return "CharacterLiteral";

        default: {
            std::string enumValue = std::to_string(static_cast<std::underlying_type<DeclNode>::type>(stmtNode));
            throw std::invalid_argument("ClavaConstants::getDataName(StmtNode):: Case not implemented, '" + enumValue + "'");
        }
    }
}

const std::string clava::getName(const TypeNode typeNode) {
    switch(typeNode) {
        case clava::TypeNode::TYPE: return "Type";
        case clava::TypeNode::QUAL_TYPE: return "QualType";
        case clava::TypeNode::BUILTIN_TYPE: return "BuiltinType";

        default: {
            std::string enumValue = std::to_string(static_cast<std::underlying_type<DeclNode>::type>(typeNode));
            throw std::invalid_argument("ClavaConstants::getDataName(TypeNode):: Case not implemented, '" + enumValue + "'");
        }
    }
}