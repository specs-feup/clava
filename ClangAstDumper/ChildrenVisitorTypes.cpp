//
// Created by JoaoBispo on 05/04/2018.
//
#include "ClangAstDumper.h"
#include "ClavaConstants.h"
#include "ClangNodes.h"

#include <string>

const std::map<const std::string, clava::TypeNode > ClangAstDumper::TYPE_CHILDREN_MAP = {

};

void ClangAstDumper::visitChildren(const Type* T) {
    // Get classname
    const std::string classname = clava::getClassName(T);

    // Get corresponding ExprNode
    clava::TypeNode typeNode = TYPE_CHILDREN_MAP.count(classname) == 1 ? TYPE_CHILDREN_MAP.find(classname)->second :
                               clava::TypeNode::TYPE;

    visitChildren(typeNode, T);
}

void ClangAstDumper::visitChildren(clava::TypeNode typeNode, const Type* T) {

    std::vector<std::string> visitedChildren;

    switch(typeNode) {
        case clava::TypeNode::TYPE:
            VisitTypeChildren(T, visitedChildren); break;

        default: throw std::invalid_argument("ChildrenVisitorTypes::visitChildren(TypeNode): Case not implemented, '"+clava::getName(typeNode)+"'");

    }

    dumpVisitedChildren(T, visitedChildren);
}


void ClangAstDumper::visitChildren(const QualType &T) {
    std::vector<std::string> visitedChildren;

    // Visit underlying (unqualified) type
    //TypeVisitor::Visit(T.getTypePtr());
    VisitTypeTop(T.getTypePtr());
    visitedChildren.push_back(getId(T.getTypePtr()));

    dumpVisitedChildren(T.getAsOpaquePtr(), visitedChildren);
}


void ClangAstDumper::VisitTypeChildren(const Type *T, std::vector<std::string> &children) {

    // If has sugar, visit desugared type
    const Type *singleStepDesugar = T->getUnqualifiedDesugaredType();
    if(singleStepDesugar != T) {
        VisitTypeTop(singleStepDesugar);
        children.push_back(getId(singleStepDesugar));
    }
}

//     TypeVisitor::Visit(T.getTypePtr());