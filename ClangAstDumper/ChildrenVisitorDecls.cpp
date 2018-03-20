//
// Created by JoaoBispo on 20/03/2018.
//

#include "ClangAstDumper.h"

void ClangAstDumper::VisitFunctionDeclChildren(const FunctionDecl *D) {
    // Visit parameters
    for (FunctionDecl::param_const_iterator I = D->param_begin(), E = D->param_end(); I != E; ++I) {
        VisitDeclTop(*I);
    }

    // Visit decls in prototype scope
    for (ArrayRef<NamedDecl *>::iterator I = D->getDeclsInPrototypeScope().begin(),
                 E = D->getDeclsInPrototypeScope().end(); I != E; ++I) {
        VisitDeclTop(*I);
    }

    // Visit body
    //if(D->hasBody()) {
    if (D->doesThisDeclarationHaveABody()) {
        VisitStmtTop(D->getBody());
    }




}

/*
void ClangAstDumper::VisitCXXConstructorDeclChildren(const CXXConstructorDecl *D) {
    // Hierarchy
    VisitFunctionDeclChildren(D);


}
 */


void ClangAstDumper::VisitVarDeclChildren(const VarDecl *D) {
    if (D->hasInit()) {
        VisitStmtTop(D->getInit());
    }
}

void ClangAstDumper::VisitParmVarDeclChildren(const ParmVarDecl *D) {
    // Hierarchy
    VisitVarDeclChildren(D);
}