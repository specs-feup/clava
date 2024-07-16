//
// Created by JoaoBispo on 12/04/2018.
//

#include "ClangAstDumper.h"
#include "ClangNodes.h"
#include "ClavaConstants.h"

#include <string>

const std::map<const std::string, clava::AttrNode> ClangAstDumper::ATTR_CHILDREN_MAP = {
        {"AlignedAttr", clava::AttrNode::ALIGNED}

};

void ClangAstDumper::visitChildren(const Attr* A) {
    // Get classname
    const std::string classname = clava::getClassName(A);

    // Get corresponding Attribute
    clava::AttrNode attrNode = ATTR_CHILDREN_MAP.count(classname) == 1 ? ATTR_CHILDREN_MAP.find(classname)->second :
                               clava::AttrNode::ATTR;

    visitChildren(attrNode, A);
}

void ClangAstDumper::visitChildren(clava::AttrNode attrNode, const Attr* A) {

    std::vector<std::string> visitedChildren;

    switch(attrNode) {
        //case clava::AttrNode::ATTR:
            // By default, do nothing
          //  break;
        case clava::AttrNode::ALIGNED:
            VisitAlignedAttrChildren(static_cast<const AlignedAttr *>(A), visitedChildren); break;
//        default: throw std::invalid_argument("ChildrenVisitorAttrs::visitChildren: Case not implemented, '"+clava::getName(attrNode)+"'");
        default:
            // By default, do nothing
            break;
    }

    dumpVisitedChildren(A, visitedChildren);
}

void ClangAstDumper::VisitAlignedAttrChildren(const AlignedAttr * A, std::vector<std::string> &children) {
    // No hierarchy

    if(A->isAlignmentExpr()) {
        addChild(A->getAlignmentExpr(), children);
        //VisitStmtTop(A->getAlignmentExpr());
        //children.push_back(clava::getId(A->getAlignmentExpr(), id));
    } else {
        VisitTypeTop(A->getAlignmentType()->getType());
        dumpTopLevelType(A->getAlignmentType()->getType());
    }

}

