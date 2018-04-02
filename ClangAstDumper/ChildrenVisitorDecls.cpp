//
// Created by JoaoBispo on 20/03/2018.
//

#include "ClangAstDumper.h"
#include "ClangNodes.h"
#include "ClavaConstants.h"

#include <string>

void ClangAstDumper::visitChildren(clava::DeclNode declNode, const Decl* D) {

    std::vector<std::string> visitedChildren;

    switch(declNode) {
        case clava::DeclNode::FUNCTION_DECL:
            VisitFunctionDeclChildren(static_cast<const FunctionDecl *>(D), visitedChildren); break;
        case clava::DeclNode::CXX_RECORD_DECL:
            VisitCXXRecordDeclChildren(static_cast<const CXXRecordDecl *>(D), visitedChildren); break;
        case clava::DeclNode::VAR_DECL:
            VisitVarDeclChildren(static_cast<const VarDecl *>(D), visitedChildren); break;
//        case clava::DeclNode::PARM_VAR_DECL:
//            visitedChildren = VisitParmVarDeclChildren(static_cast<const ParmVarDecl *>(D)); break;
        default: throw std::invalid_argument("ClangDataDumper::visitChildren: Case not implemented, '"+clava::getName(declNode)+"'");
    }

    dumpVisitedChildren(D, visitedChildren);
}


void ClangAstDumper::VisitFunctionDeclChildren(const FunctionDecl *D, std::vector<std::string> &children) {

    // Visit parameters
    for(auto param : D->parameters()) {
        VisitDeclTop(param);
        children.push_back(getId(param));
    }
    /*
    for (auto I = D->param_begin(), E = D->param_end(); I != E; ++I) {
        llvm::errs() << "PARAM: " <<  getId(I) << "\n";
        llvm::errs() << "PARAM CLASS: " <<  clava::getClassName(I) << "\n";

        VisitDeclTop(*I);
        children.push_back(getId(I));
    }
     */

    // Visit decls in prototype scope
    for (ArrayRef<NamedDecl *>::iterator I = D->getDeclsInPrototypeScope().begin(),
                 E = D->getDeclsInPrototypeScope().end(); I != E; ++I) {
        VisitDeclTop(*I);
        children.push_back(getId(I));
    }

    // Visit body
    //if(D->hasBody()) {
    if (D->doesThisDeclarationHaveABody()) {
        //llvm::errs() << "BODY: " <<  getId(D->getBody()) << "\n";
        VisitStmtTop(D->getBody());
        children.push_back(getId(D->getBody()));
    }

}




void ClangAstDumper::VisitCXXRecordDeclChildren(const CXXRecordDecl *D, std::vector<std::string> &children) {

}


/*
void ClangAstDumper::VisitCXXConstructorDeclChildren(const CXXConstructorDecl *D) {
    // Hierarchy
    VisitFunctionDeclChildren(D);


}
 */


void ClangAstDumper::VisitVarDeclChildren(const VarDecl *D, std::vector<std::string> &children) {

    if (D->hasInit()) {
        VisitStmtTop(D->getInit());
        children.push_back(getId(D->getInit()));
    }

}

/*
std::vector<std::string> ClangAstDumper::VisitParmVarDeclChildren(const ParmVarDecl *D) {

    // Hierarchy
    std::vector<std::string> children = VisitVarDeclChildren(D);
    //children.push_back(getId(D));

    return children;
}
 */