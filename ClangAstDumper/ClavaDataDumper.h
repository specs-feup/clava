
//
// Created by JoaoBispo on 18/03/2018.
//

#ifndef CLANGASTDUMPER_NODEDUMPER_H
#define CLANGASTDUMPER_NODEDUMPER_H

#include "clang/AST/Decl.h"
#include "clang/AST/DeclCXX.h"

#include <string>

//#include "ClangAstDumper.h"
#include "ClavaConstants.h"


using namespace clang;

namespace clava {


    /**
    * Dumps information about each node
    */
    class ClavaDataDumper {

    private:
        //ClangAstDumper dumper;
        int id;

    public:
        // Constructor
        explicit ClavaDataDumper(int id);

        // Utility methods
        std::string getId(const void* addr);

        void dump(clava::DeclNode declNode, const Decl* D);

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

        void DumpDeclInfo(const Decl *D);
        void DumpNamedDeclInfo(const NamedDecl *D); // Not being called yet in ClangAstDumper
        void DumpFunctionDeclInfo(const FunctionDecl *D);
        void DumpCXXMethodDeclInfo(const CXXMethodDecl *D);
        void DumpVarDeclInfo(const VarDecl *D);
        void DumpParmVarDeclInfo(const ParmVarDecl *D);

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

#endif //CLANGASTDUMPER_NODEDUMPER_H
