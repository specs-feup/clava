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



void ClangAstDumper::DumpFunctionDeclInfo(const FunctionDecl *D) {
    // Hierarchy
    DumpNamedDeclInfo(D);

    // Visit parameters
    for (FunctionDecl::param_const_iterator I = D->param_begin(), E = D->param_end(); I != E; ++I) {
        VisitDeclTop(*I);
    }

    // Visit body
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
    // Hierarchy
    DumpFunctionDeclInfo(D);

    // Dump the corresponding CXXRecordDecl
    llvm::errs() << DUMP_CXX_METHOD_DECL_PARENT << "\n";
    llvm::errs() << getId(D) << "\n";
    llvm::errs() << getId(D->getParent()) << "\n";

    // Visit type
    //llvm::errs() << "Visiting type " << dumper->getId(D->getType().getTypePtr()) << " for node " << dumper->getId(D) << "\n";
    //dumper->VisitTypeTop(D->getType().getTypePtr());
}

/*
void ClangAstDumper::DumpVarDeclInfo(const VarDecl *D) {
    // Hierarchy
    DumpNamedDeclInfo(D);

    if (D->hasInit()) {
        VisitStmtTop(D->getInit());
    }

    // Print information about VarDecl
    llvm::errs() << VARDECL_INFO << "\n";
    llvm::errs() << getId(D) << "\n";
    llvm::errs() << D->getQualifiedNameAsString() << "\n";
    llvm::errs() << D->isConstexpr() << "\n";
    llvm::errs() << D->isStaticDataMember() << "\n";
    llvm::errs() << D->isOutOfLine() << "\n";
    llvm::errs() << D->hasGlobalStorage() << "\n";


}
*/



