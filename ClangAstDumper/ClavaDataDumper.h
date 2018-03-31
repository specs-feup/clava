
//
// Created by JoaoBispo on 18/03/2018.
//

#ifndef CLANGASTDUMPER_CLAVADATADUMPER_H
#define CLANGASTDUMPER_CLAVADATADUMPER_H

#include "clang/AST/AST.h"
#include "clang/AST/Decl.h"
#include "clang/AST/Stmt.h"
#include "clang/AST/Type.h"

/*
#include "clang/AST/TypeVisitor.h"
#include "clang/AST/StmtVisitor.h"
#include "clang/AST/DeclVisitor.h"
*/
///#include "clang/AST/Decl.h"
//#include "clang/AST/DeclCXX.h"

#include <string>

//#include "ClangAstDumper.h"
#include "ClavaConstants.h"


using namespace clang;

namespace clava {


    /**
    * Dumps information about each node
    */
    class ClavaDataDumper  {

    private:
        //ClangAstDumper dumper;
        ASTContext *Context;
        int id;

    public:
        // Constructor
        explicit ClavaDataDumper(ASTContext *Context, int id);

        // Utility methods
        std::string getId(const void* addr);

        void dump(clava::DeclNode declNode, const Decl* D);
        void dump(clava::StmtNode stmtNode, const Stmt* S);
        void dump(clava::TypeNode typeNode, const Type* T);

        //void DumpHeader(const Decl* D);
        //void DumpHeader(const Stmt* S);
        //void DumpHeader(const Type* T);



        // void DumpHeader(const std::string tag, const void *pointer);
        //void dumpHeader(const std::string tag, Stmt *D);
        //void dumpHeader(const std::string tag, Type *D);




    private:
        //void dumpHeaderAny(const std::string tag, void *D);
        void DumpHeader(const std::string tag, const void *pointer);

        // Generic value dumpers
        void dump(bool boolean);
        void dump(int integer);
        void dump(std::string string);

        // DECLS

        void DumpDeclData(const Decl *D);
        void DumpNamedDeclData(const NamedDecl *D); // Not being called yet in ClangAstDumper
        void DumpFunctionDeclData(const FunctionDecl *D);
        void DumpCXXMethodDeclData(const CXXMethodDecl *D);
        void DumpVarDeclData(const VarDecl *D);
        void DumpParmVarDeclData(const ParmVarDecl *D);


        // STMTS
        void DumpStmtData(const Stmt *S);


        // EXPRS
        void DumpExprData(const Expr *E);


        // TYPES
        void DumpTypeData(const Type *T);
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


    /**
    * Names of StmtData dumpers. In Clang Expr is a subclass of Stmt, but in Clava they are different classes.
    */
    static std::map<StmtNode,std::string>  STMT_DATA_NAMES = {
            // Stmts
            {StmtNode::STMT, "<StmtData>"},

            // Exprs
            {StmtNode::EXPR, "<ExprData>"}
    };

    /**
    * Names of ExprData dumpers. In Clang Expr is a subclass of Stmt, but in Clava they are different classes.
    */
/*
    static std::map<ExprNode,std::string>  EXPR_DATA_NAMES = {

    };
*/
    /**
    * Names of StmtData dumpers
    */
    static std::map<TypeNode,std::string>  TYPE_DATA_NAMES = {
            {TypeNode::TYPE, "<TypeData>"}
    };
}

#endif //CLANGASTDUMPER_CLAVADATADUMPER_H
