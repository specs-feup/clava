#include "ClangAstDumper.h"
#include "ClangAstDumperConstants.h"


void ClangAstDumper::DumpDeclInfo(const Decl *D) {

    // Print information about Decl

}

void ClangAstDumper::DumpNamedDeclInfo(const NamedDecl *D) {
    // Hierarchy
    DumpDeclInfo(D);

    // Print information about NamedDecl
//    llvm::errs() << NAMED_DECL_INFO << "\n";
//    llvm::errs() << getId(D) << "\n";

    //llvm::errs() << D->isHidden() << "\n";
}

