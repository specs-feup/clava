//
// Created by JoaoBispo on 12/04/2018.
//

#include "ClangAstDumper.h"
#include "ClangNodes.h"

#include "clang/AST/Attr.h"

#include <sstream>


using namespace clang;


void ClangAstDumper::VisitAttr(const Attr *A) {
    if(dumpAttr(A)) {
        return;
    }

    visitChildrenAndData(A);

    // Dump data
    //dataDumper.dump(clava::DeclNode::DECL, D);


//    llvm::errs() << DECL_INFO << "\n";
//    llvm::errs() << getId(D) << "\n";
//    DumpDeclData(D);
}

bool ClangAstDumper::dumpAttr(const Attr* attrAddr) {
    if(seenAttrs.count(attrAddr) != 0) {
        return true;
    }

    log(attrAddr);

    // A Dumper is created for each context,
    // no need to use id to disambiguate
    seenAttrs.insert(attrAddr);

    //std::ostringstream extendedId;
    //extendedId << attrAddr << "_" << id;


    dumpIdToClassMap(attrAddr, clava::getClassName(attrAddr));

    return false;
}


void ClangAstDumper::visitChildrenAndData(const Attr *A) {
    // Visit children
    visitChildren(A);

    // Dump data
    dataDumper.dump(A);
}
