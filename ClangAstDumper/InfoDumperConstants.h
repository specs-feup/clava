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
        FUNCTION_DECL,
        CXX_METHOD_DECL,
        VAR_DECL,
        PARM_VAR_DECL
    };



    /**
     * Names of DeclData dumpers
     */
    static std::map<DeclNode,std::string>  DECL_DATA_NAMES = {
        {DeclNode::DECL, "<DeclData>"},
        {DeclNode::NAMED_DECL, "<NamedDeclData>"},
        {DeclNode::FUNCTION_DECL, "<FunctionDeclData>"},
        {DeclNode::CXX_METHOD_DECL, "<CXXMethodDeclData>"},
        {DeclNode::VAR_DECL, "<VarDeclData>"},
        {DeclNode::PARM_VAR_DECL, "<ParmVarDeclData>"}
    };

}
#endif //CLANGASTDUMPER_INFODUMPERCONSTANTS_H

