//
// Created by JoaoBispo on 02/04/2018.
//

#include "ClavaConstants.h"

#include <string>
#include <stdexcept>



const std::string clava::getName(const StmtNode stmtNode) {
    switch(stmtNode) {
        // STMTS
        case clava::StmtNode::STMT: return "Stmt";
        case clava::StmtNode::COMPOUND_STMT: return "CompoundStmt";
        case clava::StmtNode::DECL_STMT: return "DeclStmt";
        case clava::StmtNode::IF_STMT: return "IfStmt";
        case clava::StmtNode::FOR_STMT: return "ForStmt";
        case clava::StmtNode::WHILE_STMT: return "WhileStmt";
        case clava::StmtNode::DO_STMT: return "DoStmt";
        case clava::StmtNode::CXX_FOR_RANGE_STMT: return "CXXForRangeStmt";
        case clava::StmtNode::CXX_CATCH_STMT: return "CXXCatchStmt";
        case clava::StmtNode::CXX_TRY_STMT: return "CXXTryStmt";
        case clava::StmtNode::CASE_STMT: return "CaseStmt";
        case clava::StmtNode::DEFAULT_STMT: return "CaseStmt";
        case clava::StmtNode::LABEL_STMT: return "LabelStmt";
        case clava::StmtNode::GOTO_STMT: return "GotoStmt";
        case clava::StmtNode::ATTRIBUTED_STMT: return "AttributedStmt";
        case clava::StmtNode::CAPTURED_STMT: return "CapturedStmt";


        // EXPRS
        case clava::StmtNode::EXPR: return "Expr";
        case clava::StmtNode::CAST_EXPR: return "CastExpr";
        case clava::StmtNode::CXX_FUNCTIONAL_CAST_EXPR: return "CXXFunctionalCastExpr";
        case clava::StmtNode::EXPLICIT_CAST_EXPR: return "ExplicitCastExpr";
        case clava::StmtNode::CXX_NAMED_CAST_EXPR: return "CXXNamedCastExpr";
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
        case clava::StmtNode::CXX_CONSTRUCT_EXPR: return "CXXConstructExpr";
        case clava::StmtNode::CXX_TEMPORARY_OBJECT_EXPR: return "CXXTemporaryObjectExpr";
        case clava::StmtNode::MEMBER_EXPR: return "MemberExpr";
        case clava::StmtNode::MATERIALIZE_TEMPORARY_EXPR: return "MaterializeTemporaryExpr";
        case clava::StmtNode::OFFSET_OF_EXPR: return "OffsetOfExpr";
        case clava::StmtNode::BINARY_OPERATOR: return "BinaryOperator";
        case clava::StmtNode::UNARY_OPERATOR: return "UnaryOperator";
        case clava::StmtNode::COMPOUND_ASSIGN_OPERATOR: return "CompoundAssignOperator";
        case clava::StmtNode::CALL_EXPR: return "CallExpr";
        case clava::StmtNode::CXX_MEMBER_CALL_EXPR: return "CXXMemberCallExpr";
        case clava::StmtNode::CXX_TYPEID_EXPR: return "CXXTypeidExpr";
        case clava::StmtNode::CXX_DEPENDENT_SCOPE_MEMBER_EXPR: return "CXXDependentScopeMemberExpr";
        case clava::StmtNode::OPAQUE_VALUE_EXPR: return "OpaqueValueExpr";
        case clava::StmtNode::UNARY_EXPR_OR_TYPE_TRAIT_EXPR: return "UnaryExprOrTypeTraitExpr";
        case clava::StmtNode::CXX_NEW_EXPR: return "CXXNewExpr";
        case clava::StmtNode::CXX_DELETE_EXPR: return "CXXDeleteExpr";
        case clava::StmtNode::SUBST_NON_TYPE_TEMPLATE_PARM_EXPR: return "SubstNonTypeTemplateParmExpr";
        case clava::StmtNode::LAMBDA_EXPR: return "LambdaExpr";
        case clava::StmtNode::PREDEFINED_EXPR: return "PredefinedExpr";
        case clava::StmtNode::SIZE_OF_PACK_EXPR: return "SizeOfPackExpr";
        case clava::StmtNode::ARRAY_INIT_LOOP_EXPR: return "ArrayInitLoopExpr";
        case clava::StmtNode::DESIGNATED_INIT_EXPR: return "DesignatedInitExpr";

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
        case clava::TypeNode::TYPE_WITH_KEYWORD: return "TypeWithKeyword";
        case clava::TypeNode::ELABORATED_TYPE: return "ElaboratedType";
        case clava::TypeNode::REFERENCE_TYPE: return "ReferenceType";
        case clava::TypeNode::LVALUE_REFERENCE_TYPE: return "LValueReferenceType";
        case clava::TypeNode::RVALUE_REFERENCE_TYPE: return "RValueReferenceType";
        case clava::TypeNode::INJECTED_CLASS_NAME_TYPE: return "InjectedClassNameType";
        case clava::TypeNode::TEMPLATE_TYPE_PARM_TYPE: return "TemplateTypeParmType";
        case clava::TypeNode::TYPEDEF_TYPE: return "TypedefType";
        case clava::TypeNode::SUBST_TEMPLATE_TYPE_PARM_TYPE: return "SubstTemplateTypeParmType";
        case clava::TypeNode::TEMPLATE_SPECIALIZATION_TYPE: return "TemplateSpecializationType";
        case clava::TypeNode::ADJUSTED_TYPE: return "AdjustedType";
        case clava::TypeNode::DECAYED_TYPE: return "DecayedType";
        case clava::TypeNode::DECLTYPE_TYPE: return "DecltypeType";
        case clava::TypeNode::AUTO_TYPE: return "AutoType";
        case clava::TypeNode::PACK_EXPANSION_TYPE: return "PackExpansionType";
        case clava::TypeNode::TYPE_OF_EXPR_TYPE: return "TypeOfExprType";
        case clava::TypeNode::ATTRIBUTED_TYPE: return "AttributedType";
        case clava::TypeNode::UNARY_TRANSFORM_TYPE: return "UnaryTransformType ";
        case clava::TypeNode::COMPLEX_TYPE: return "ComplexType";

        default: {
            std::string enumValue = std::to_string(static_cast<std::underlying_type<TypeNode>::type>(typeNode));
            throw std::invalid_argument("ClavaConstants::getName(TypeNode):: Case not implemented, '" + enumValue + "'");
        }
    }
}

const std::string clava::getName(const AttrNode attrNode) {
    switch(attrNode) {
        case clava::AttrNode::ATTR: return "Attribute";
        case clava::AttrNode::ALIGNED: return "AlignedAttr";
        case clava::AttrNode::OPENCL_UNROLL_HINT: return "OpenCLUnrollHintAttr";
        case clava::AttrNode::FORMAT: return "FormatAttr";
        case clava::AttrNode::NON_NULL: return "NonNullAttr";

        default: {
            std::string enumValue = std::to_string(static_cast<std::underlying_type<AttrNode>::type>(attrNode));
            throw std::invalid_argument("ClavaConstants::getDataName(AttrNode):: Case not implemented, '" + enumValue + "'");
        }
    }
}