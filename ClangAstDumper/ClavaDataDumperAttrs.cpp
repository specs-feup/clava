//
// Created by JoaoBispo on 12/04/2018.
//

#include "ClavaDataDumper.h"
#include "ClangNodes.h"
#include "ClavaConstants.h"
#include "ClangEnums.h"

#include <map>

const std::map<const std::string, clava::AttrNode > clava::ATTR_DATA_MAP = {
        {"AlignedAttr", clava::AttrNode::ALIGNED},
        {"OpenCLUnrollHintAttr", clava::AttrNode::OPENCL_UNROLL_HINT},
        {"FormatAttr", clava::AttrNode::FORMAT},
        {"NonNullAttr", clava::AttrNode::NON_NULL},
        {"VisibilityAttr", clava::AttrNode::VISIBILITY},
};


void clava::ClavaDataDumper::dump(const Attr* A) {

    // Get classname
    const std::string classname = clava::getClassName(A);

    // Get corresponding AttrNode
    AttrNode attrNode = ATTR_DATA_MAP.count(classname) == 1 ? ATTR_DATA_MAP.find(classname)->second : AttrNode::ATTR;

    dump(attrNode, A);

}


void clava::ClavaDataDumper::dump(clava::AttrNode attrNode, const Attr* A) {
    // Dump header
    llvm::errs() << getDataName(attrNode) << "\n";
    llvm::errs() << clava::getId(A, id) << "\n";
    llvm::errs() << clava::getClassName(A) << "\n";

    //DumpHeader(getDataName(attrNode), A);

    switch(attrNode) {
        case clava::AttrNode::ATTR:
            DumpAttrData(A); break;
        case clava::AttrNode::ALIGNED:
            DumpAlignedAttrData(static_cast<const AlignedAttr*>(A)); break;
        case clava::AttrNode::OPENCL_UNROLL_HINT:
            DumpOpenCLUnrollHintAttrData(static_cast<const OpenCLUnrollHintAttr*>(A)); break;
        case clava::AttrNode::FORMAT:
            DumpFormatAttrData(static_cast<const FormatAttr*>(A)); break;
        case clava::AttrNode::NON_NULL:
            DumpNonNullAttrData(static_cast<const NonNullAttr*>(A)); break;
        case clava::AttrNode::VISIBILITY:
            DumpVisibilityAttrData(static_cast<const VisibilityAttr*>(A)); break;
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

void clava::ClavaDataDumper::DumpAlignedAttrData(const AlignedAttr *A) {
    // Common
    DumpAttrData(A);

    clava::dump(A->getSpelling());
    clava::dump(A->isAlignmentExpr());
    if(A->isAlignmentExpr()) {
        clava::dump(clava::getId(A->getAlignmentExpr(), id));
    } else {
        clava::dump(A->getAlignmentType()->getType(), id);
    }
}

void clava::ClavaDataDumper::DumpOpenCLUnrollHintAttrData(const OpenCLUnrollHintAttr *A) {
    // Common
    DumpAttrData(A);

    clava::dump(A->getUnrollHint());
}

void clava::ClavaDataDumper::DumpFormatAttrData(const FormatAttr *A) {
    // Common
    DumpAttrData(A);

    clava::dump(A->getType()->getName());
    clava::dump(A->getFormatIdx());
    clava::dump(A->getFirstArg());

//    llvm::errs() << "NAME START: " << A->getType()->getNameStart() << "\n";
//    llvm::errs() << "IS EXTENSION: " << A->getType()->isExtensionToken() << "\n";
//    llvm::errs() <<  "Spellig: " << A->getSpelling() << "\n";
}


void clava::ClavaDataDumper::DumpNonNullAttrData(const NonNullAttr *A) {
    // Common
    DumpAttrData(A);

    // Dump args
    clava::dump(A->args_size());
    for (auto I = A->args_begin(), E = A->args_end(); I != E;
         ++I) {
        clava::dump((*I).getSourceIndex());
    }
}

void clava::ClavaDataDumper::DumpVisibilityAttrData(const VisibilityAttr *A) {
    // Common
    DumpAttrData(A);

    clava::dump(clava::VISIBILITY_ATTR_TYPE[A->getVisibility()]);
}