
//
// Created by JoaoBispo on 18/03/2018.
//

#ifndef CLANGASTDUMPER_NODEDUMPER_H
#define CLANGASTDUMPER_NODEDUMPER_H

#include "clang/AST/Decl.h"
#include "clang/AST/DeclCXX.h"

#include <string>

//#include "ClangAstDumper.h"

using namespace clang;

/**
 * Dumps information about each node
 */
class InfoDumper {

private:
    //ClangAstDumper dumper;
    int id;

public:
    // Constructor
    explicit InfoDumper(int id);

    // Utility methods
    std::string getId(const void* addr);

    void DumpHeader(const Decl* D);
    void DumpHeader(const Stmt* S);
    void DumpHeader(const Type* T);



        // void DumpHeader(const std::string tag, const void *pointer);
    //void dumpHeader(const std::string tag, Stmt *D);
    //void dumpHeader(const std::string tag, Type *D);

    // DECLS

    void DumpDeclInfo(const Decl *D);
    void DumpNamedDeclInfo(const NamedDecl *D); // Not being called yet in ClangAstDumper
    void DumpFunctionDeclInfo(const FunctionDecl *D);
    void DumpCXXMethodDeclInfo(const CXXMethodDecl *D);
    void DumpVarDeclInfo(const VarDecl *D);
    void DumpParmVarDeclInfo(const ParmVarDecl *D);


private:
    //void dumpHeaderAny(const std::string tag, void *D);
    void DumpHeader(const std::string tag, const void *pointer);


    void dump(bool boolean);
    void dump(int integer);
    void dump(std::string string);
};

#endif //CLANGASTDUMPER_NODEDUMPER_H
