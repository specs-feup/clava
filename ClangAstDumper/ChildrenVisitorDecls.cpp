//
// Created by JoaoBispo on 20/03/2018.
//

#include "ClangAstDumper.h"

std::vector<std::string> ClangAstDumper::VisitFunctionDeclChildren(const FunctionDecl *D) {
    std::vector<std::string> children;

    // Visit parameters
    for (FunctionDecl::param_const_iterator I = D->param_begin(), E = D->param_end(); I != E; ++I) {
        VisitDeclTop(*I);
        children.push_back(getId(I));
    }

    // Visit decls in prototype scope
    for (ArrayRef<NamedDecl *>::iterator I = D->getDeclsInPrototypeScope().begin(),
                 E = D->getDeclsInPrototypeScope().end(); I != E; ++I) {
        VisitDeclTop(*I);
        children.push_back(getId(I));
    }

    // Visit body
    //if(D->hasBody()) {
    if (D->doesThisDeclarationHaveABody()) {
        VisitStmtTop(D->getBody());
        children.push_back(getId(D));
    }

    return children;
}

std::vector<std::string> ClangAstDumper::VisitCXXRecordDeclChildren(const CXXRecordDecl *D) {
    std::vector<std::string> children;

    return children;
}

/*
void ClangAstDumper::VisitCXXConstructorDeclChildren(const CXXConstructorDecl *D) {
    // Hierarchy
    VisitFunctionDeclChildren(D);


}
 */


std::vector<std::string> ClangAstDumper::VisitVarDeclChildren(const VarDecl *D) {
    std::vector<std::string> children;

    if (D->hasInit()) {
        VisitStmtTop(D->getInit());
        children.push_back(getId(D));
    }

    return children;
}

std::vector<std::string> ClangAstDumper::VisitParmVarDeclChildren(const ParmVarDecl *D) {
    std::vector<std::string> children;

    // Hierarchy
    VisitVarDeclChildren(D);
    children.push_back(getId(D));

    return children;
}