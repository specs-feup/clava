//
// Created by JoaoBispo on 20/01/2017.
//

#include "ClangAstDumper.h"

#include "clang/AST/AST.h"

using namespace clang;


/*
 * EXTRA Nodes
 */

void ClangAstDumper::dumpCXXCtorInitializer(const CXXCtorInitializer *Init) {

    if (Init->isAnyMemberInitializer()) {
        llvm::errs() << "AnyMemberInitializer\n";
    } else if (Init->isBaseInitializer()) {
        // QUALTYPE EXP
        llvm::errs() << "BaseInitializer:" << QualType(Init->getBaseClass(), 0).getAsOpaquePtr() << "_" << id << "\n";
    } else if (Init->isDelegatingInitializer()) {
        llvm::errs() << "DelegatingInitializer:" << Init->getTypeSourceInfo()->getType().getTypePtr() << "_" << id << "\n";
    } else {
        llvm_unreachable("ClangAstDumper: Unknown initializer type");
    }

}
