//
// Created by JoaoBispo on 05/04/2018.
//
#include "ClangAstDumper.h"
#include "ClavaConstants.h"
#include "ClangNodes.h"

#include <string>

void ClangAstDumper::visitChildren(clava::TypeNode typeNode, const Type* T) {

    std::vector<std::string> visitedChildren;

    switch(typeNode) {
        case clava::TypeNode::TYPE:
            VisitTypeChildren(T, visitedChildren); break;

        default: throw std::invalid_argument("ClangDataDumper::visitChildren(TypeNode): Case not implemented, '"+clava::getName(typeNode)+"'");

    }

    dumpVisitedChildren(T, visitedChildren);
}


void ClangAstDumper::visitChildren(const QualType &T) {
    std::vector<std::string> visitedChildren;

    // Visit underlying (unqualified) type
    TypeVisitor::Visit(T.getTypePtr());
    visitedChildren.push_back(getId(T.getTypePtr()));

    dumpVisitedChildren(T.getAsOpaquePtr(), visitedChildren);
}


void ClangAstDumper::VisitTypeChildren(const Type *T, std::vector<std::string> &children) {

}

//     TypeVisitor::Visit(T.getTypePtr());