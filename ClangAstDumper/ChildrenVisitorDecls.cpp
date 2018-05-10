//
// Created by JoaoBispo on 20/03/2018.
//

#include "ClangAstDumper.h"
#include "ClangNodes.h"
#include "ClavaConstants.h"

#include <string>

const std::map<const std::string, clava::DeclNode > ClangAstDumper::DECL_CHILDREN_MAP = {
        {"CXXConstructorDecl", clava::DeclNode::FUNCTION_DECL},
        {"CXXConversionDecl", clava::DeclNode::FUNCTION_DECL},
        {"CXXDestructorDecl", clava::DeclNode::FUNCTION_DECL},
        {"CXXMethodDecl", clava::DeclNode::FUNCTION_DECL},
        {"CXXRecordDecl", clava::DeclNode::CXX_RECORD_DECL},
        {"FunctionDecl", clava::DeclNode::FUNCTION_DECL},
        {"VarDecl", clava::DeclNode::VAR_DECL},
        {"ParmVarDecl", clava::DeclNode::VAR_DECL}

};


void ClangAstDumper::visitChildren(const Decl* D) {
    // Get classname
    const std::string classname = clava::getClassName(D);

    // Get corresponding DeclNode
    clava::DeclNode declNode = DECL_CHILDREN_MAP.count(classname) == 1 ? DECL_CHILDREN_MAP.find(classname)->second :
                               clava::DeclNode::DECL;

    visitChildren(declNode, D);
}


void ClangAstDumper::visitChildren(clava::DeclNode declNode, const Decl* D) {

    std::vector<std::string> visitedChildren;

    switch(declNode) {
        case clava::DeclNode::DECL:
            VisitDeclChildren(D, visitedChildren); break;
        case clava::DeclNode::NAMED_DECL:
            VisitNamedDeclChildren(static_cast<const NamedDecl *>(D), visitedChildren); break;
        case clava::DeclNode::VALUE_DECL:
            VisitValueDeclChildren(static_cast<const ValueDecl *>(D), visitedChildren); break;
        case clava::DeclNode::FUNCTION_DECL:
            VisitFunctionDeclChildren(static_cast<const FunctionDecl *>(D), visitedChildren); break;
        case clava::DeclNode::CXX_RECORD_DECL:
            VisitCXXRecordDeclChildren(static_cast<const CXXRecordDecl *>(D), visitedChildren); break;
        case clava::DeclNode::VAR_DECL:
            VisitVarDeclChildren(static_cast<const VarDecl *>(D), visitedChildren); break;
//        case clava::DeclNode::PARM_VAR_DECL:
//            visitedChildren = VisitParmVarDeclChildren(static_cast<const ParmVarDecl *>(D)); break;
        default: throw std::invalid_argument("ChildrenVisitorDecls::visitChildren: Case not implemented, '"+clava::getName(declNode)+"'");
    }

    dumpVisitedChildren(D, visitedChildren);
}


void ClangAstDumper::VisitDeclChildren(const Decl *D, std::vector<std::string> &children) {
    // Visit attributes
    for (Decl::attr_iterator I = D->attr_begin(), E = D->attr_end(); I != E;
         ++I) {
        Attr* attr = *I;
        VisitAttrTop(attr);
        dumpTopLevelAttr(attr);
    }
}

void ClangAstDumper::VisitNamedDeclChildren(const NamedDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitDeclChildren(D, children);

    // Just visit underlying decl
    //VisitDeclTop(D->getUnderlyingDecl());
    //llvm::errs() << "VISITING " << clava::getId(D->getUnderlyingDecl(), id) << " -> " << clava::getClassName(D->getUnderlyingDecl()) << "\n";
    //llvm::errs() << "ORIGINAL " << clava::getId(D, id) << "\n";
}

void ClangAstDumper::VisitValueDeclChildren(const ValueDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitNamedDeclChildren(D, children);

    // Visit type
    VisitTypeTop(D->getType());
    dumpTopLevelType(D->getType());


}

void ClangAstDumper::VisitFunctionDeclChildren(const FunctionDecl *D, std::vector<std::string> &children) {

    // Hierarchy
    VisitValueDeclChildren(D, children);

    // Visit parameters
    for(auto param : D->parameters()) {
        VisitDeclTop(param);
        children.push_back(clava::getId(param, id));
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
        children.push_back(clava::getId(*I, id));
    }

    // Visit body
    //if(D->hasBody()) {
    if (D->doesThisDeclarationHaveABody()) {
        //llvm::errs() << "BODY: " <<  getId(D->getBody()) << "\n";
        VisitStmtTop(D->getBody());
        children.push_back(clava::getId(D->getBody(), id));
    }

}




void ClangAstDumper::VisitCXXRecordDeclChildren(const CXXRecordDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitDeclChildren(D, children);
}


/*
void ClangAstDumper::VisitCXXConstructorDeclChildren(const CXXConstructorDecl *D) {
    // Hierarchy
    VisitFunctionDeclChildren(D);


}
 */


void ClangAstDumper::VisitVarDeclChildren(const VarDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitValueDeclChildren(D, children);

    if (D->hasInit()) {
        VisitStmtTop(D->getInit());
        children.push_back(clava::getId(D->getInit(), id));
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