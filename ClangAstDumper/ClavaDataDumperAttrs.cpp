//
// Created by JoaoBispo on 12/04/2018.
//

#include "ClavaDataDumper.h"
#include "ClangNodes.h"
#include "ClavaConstants.h"
#include "ClangEnums.h"

#include <map>

const std::map<const std::string, clava::AttrNode > clava::ATTR_DATA_MAP = {
        //{"CXXConstructorDecl", clava::DeclNode::CXX_METHOD_DECL}
};


void clava::ClavaDataDumper::dump(const Attr* A) {

    // Get classname
    const std::string classname = clava::getClassName(A);

    // Get corresponding AttrNode
    AttrNode attrNode = ATTR_DATA_MAP.count(classname) == 1 ? ATTR_DATA_MAP.find(classname)->second : AttrNode::ATTR;

    dump(attrNode, A);

}


void clava::ClavaDataDumper::dump(clava::AttrNode attrNode, const Attr* A) {
    DumpHeader(getDataName(attrNode), A);

    switch(attrNode) {
        case clava::AttrNode::ATTR:
            DumpAttrData(A); break;
        default:
            throw std::invalid_argument("ClangDataDumper::dump(DeclNode):: Case not implemented, '" + getName(attrNode) + "'");
    }
}


void clava::ClavaDataDumper::DumpAttrData(const Attr *A) {
    clava::dumpSourceInfo(Context, A->getRange().getBegin(), A->getRange().getEnd());

    // Print information about Attr
    clava::dump(clava::ATTRIBUTES[A->getKind()]);
    clava::dump(A->isImplicit());
    clava::dump(A->isInherited());
    clava::dump(A->isLateParsed());
    clava::dump(A->isPackExpansion());

}
