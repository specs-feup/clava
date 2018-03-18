#include "ClangAstDumper.h"
#include "ClangAstDumperConstants.h"

void ClangAstDumper::DumpFunctionDeclInfo(const FunctionDecl *D) {

    if(D->hasBody()) {
        VisitStmtTop(D->getBody());
    }

    // Print information about FunctionDecl
    llvm::errs() << FUNCTION_DECL_INFO << "\n";
    llvm::errs() << getId(D) << "\n";
    llvm::errs() << D->getTemplatedKind() << "\n";

/*
    llvm::errs() << D->getQualifiedNameAsString() << "\n";
    llvm::errs() << D->isConstexpr() << "\n";
    llvm::errs() << D->isOutOfLine() << "\n";
*/
}

void ClangAstDumper::DumpCXXMethodDeclInfo(const CXXMethodDecl *D) {

    DumpFunctionDeclInfo(D);

    // Dump the corresponding CXXRecordDecl
    llvm::errs() << DUMP_CXX_METHOD_DECL_PARENT << "\n";
    llvm::errs() << getId(D) << "\n";
    llvm::errs() << getId(D->getParent()) << "\n";

    // Visit type
    //llvm::errs() << "Visiting type " << dumper->getId(D->getType().getTypePtr()) << " for node " << dumper->getId(D) << "\n";
    //dumper->VisitTypeTop(D->getType().getTypePtr());
}