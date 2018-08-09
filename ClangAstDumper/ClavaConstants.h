//
// Created by JoaoBispo on 19/03/2018.
//

#ifndef CLANGASTDUMPER_INFODUMPERCONSTANTS_H
#define CLANGASTDUMPER_INFODUMPERCONSTANTS_H

#include <string>
#include <map>

/*
static const std::string DECL_DATA = "<Decl Data>";
static const std::string NAMED_DECL_DATA = "<NamedDecl Data>";
static const std::string FUNCTION_DECL_DATA = "<FunctionDecl Data>";
static const std::string CXX_METHOD_DECL_DATA = "<CXXMethodDecl Data>";
static const std::string VAR_DECL_DATA = "<VarDecl Data>";
static const std::string PARM_VAR_DECL_DATA = "<ParmVarDecl Data>";
*/
namespace clava {

    /**
     * Represents decl node classes
     */
    enum class DeclNode {
        DECL,
        NAMED_DECL,
        VALUE_DECL,
        DECLARATOR_DECL,
        TYPE_DECL,
        FUNCTION_DECL,
        CXX_METHOD_DECL,
        VAR_DECL,
        PARM_VAR_DECL,
        TAG_DECL,
        RECORD_DECL,
        CXX_RECORD_DECL
    };

    /**
     * Represents stmt and expr node classes
     */
    enum class StmtNode {
        // Stmt nodes
        STMT,
        COMPOUND_STMT,
        DECL_STMT,

        // Expr nodes
        EXPR,
        CAST_EXPR,
        CXX_FUNCTIONAL_CAST_EXPR,
        LITERAL,
        CHARACTER_LITERAL,
        INTEGER_LITERAL,
        FLOATING_LITERAL,
        CXX_BOOL_LITERAL_EXPR,
        COMPOUND_LITERAL_EXPR,
        INIT_LIST_EXPR,
        STRING_LITERAL,
        DECL_REF_EXPR,
        DEPENDENT_SCOPE_DECL_REF_EXPR,
        OVERLOAD_EXPR,
        UNRESOLVED_LOOKUP_EXPR,
        UNRESOLVED_MEMBER_EXPR,
    };

    /**
     * Represents expr node classes
     */
/*
    enum class ExprNode {
        EXPR
    };
*/
    /**
     * Represents type node classes
    */
    enum class TypeNode {
        TYPE,
        QUAL_TYPE,
        BUILTIN_TYPE,
        FUNCTION_TYPE,
        FUNCTION_PROTO_TYPE,
        FUNCTION_NO_PROTO_TYPE,
        TAG_TYPE,
        RECORD_TYPE,
        ENUM_TYPE,
        ARRAY_TYPE,
        CONSTANT_ARRAY_TYPE,
        DEPENDENT_SIZED_ARRAY_TYPE,
        INCOMPLETE_ARRAY_TYPE,
        VARIABLE_ARRAY_TYPE,
        POINTER_TYPE,
        TYPE_WITH_KEYWORD,
        ELABORATED_TYPE,
        REFERENCE_TYPE,
        LVALUE_REFERENCE_TYPE,
        RVALUE_REFERENCE_TYPE,
        INJECTED_CLASS_NAME_TYPE,
        TEMPLATE_TYPE_PARM_TYPE,
        TYPEDEF_TYPE,
        SUBST_TEMPLATE_TYPE_PARM_TYPE,
        TEMPLATE_SPECIALIZATION_TYPE,

    };


    /**
     * Represents attribute node classes
    */
    enum class AttrNode {
        ATTR,
        ALIGNED
    };


    const std::string getName(const DeclNode D);
    const std::string getName(const StmtNode S);
    const std::string getName(const TypeNode T);
    const std::string getName(const AttrNode A);


}
#endif //CLANGASTDUMPER_INFODUMPERCONSTANTS_H

