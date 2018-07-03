//
// Created by JoaoBispo on 05/04/2018.
//
#include "ClangAstDumper.h"
#include "ClavaConstants.h"
#include "ClangNodes.h"

#include <string>

const std::map<const std::string, clava::TypeNode > ClangAstDumper::TYPE_CHILDREN_MAP = {
        {"FunctionProtoType", clava::TypeNode::FUNCTION_PROTO_TYPE},
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
        case clava::TypeNode::FUNCTION_PROTO_TYPE:
            VisitFunctionProtoTypeChildren(static_cast<const FunctionProtoType *>(T), visitedChildren); break;

        default: throw std::invalid_argument("ChildrenVisitorTypes::visitChildren(TypeNode): Case not implemented, '"+clava::getName(typeNode)+"'");

    }

    dumpVisitedChildren(T, visitedChildren);
}


void ClangAstDumper::visitChildren(const QualType &T) {
    std::vector<std::string> visitedChildren;

    // Visit underlying (unqualified) type
    //TypeVisitor::Visit(T.getTypePtr());
    VisitTypeTop(T.getTypePtr());
    visitedChildren.push_back(clava::getId(T.getTypePtr(), id));

    dumpVisitedChildren(T.getAsOpaquePtr(), visitedChildren);
}


void ClangAstDumper::VisitTypeChildren(const Type *T, std::vector<std::string> &visitedChildren) {

    // If has sugar, visit desugared type
    const Type *singleStepDesugar = T->getUnqualifiedDesugaredType();
    if(singleStepDesugar != T) {
        VisitTypeTop(singleStepDesugar);
        visitedChildren.push_back(clava::getId(singleStepDesugar, id));
    }
}

//     TypeVisitor::Visit(T.getTypePtr());

void ClangAstDumper::VisitFunctionTypeChildren(const FunctionType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Return type
    VisitTypeTop(T->getReturnType());
    visitedChildren.push_back(clava::getId(T->getReturnType(), id));
}

void ClangAstDumper::VisitFunctionProtoTypeChildren(const FunctionProtoType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitFunctionTypeChildren(T, visitedChildren);

    // Parameters types
    for (QualType paramType : T->getParamTypes()) {
        VisitTypeTop(paramType);
        visitedChildren.push_back(clava::getId(paramType, id));
    }

    // Just visit noexcept expression, if present
    VisitStmtTop(T->getExtProtoInfo().ExceptionSpec.NoexceptExpr);

    //llvm::errs() << "TEST:" << clava::getClassName(T->getExtProtoInfo().ExceptionSpec.NoexceptExpr);
}

void ClangAstDumper::VisitTagTypeChildren(const TagType *T, std::vector<std::string> &visitedChildren) {
    // Just visit decl
    VisitDeclTop(T->getDecl());
}

void ClangAstDumper::VisitArrayTypeChildren(const ArrayType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Element type
    VisitTypeTop(T->getElementType());
    visitedChildren.push_back(clava::getId(T->getElementType(), id));
}

