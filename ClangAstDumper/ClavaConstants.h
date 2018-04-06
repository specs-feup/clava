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
        FUNCTION_DECL,
        CXX_METHOD_DECL,
        VAR_DECL,
        PARM_VAR_DECL,
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
        CHARACTER_LITERAL,
        INTEGER_LITERAL,
        FLOATING_LITERAL
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
        BUILTIN_TYPE
    };


    const std::string getName(const DeclNode D);
    const std::string getName(const StmtNode S);
    const std::string getName(const TypeNode T);


}
#endif //CLANGASTDUMPER_INFODUMPERCONSTANTS_H

