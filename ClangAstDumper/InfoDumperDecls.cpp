//
// Created by JoaoBispo on 18/03/2018.
//

#include "InfoDumper.h"

void InfoDumper::DumpDeclInfo(const Decl *D) {

    // Print information about Decl

}

void InfoDumper::DumpNamedDeclInfo(const NamedDecl *D) {
    // Hierarchy
    DumpDeclInfo(D);

    //llvm::errs() << "INFO DUMPER\n";

    // Print information about NamedDecl
//    llvm::errs() << NAMED_DECL_INFO << "\n";
//    llvm::errs() << getId(D) << "\n";

    //llvm::errs() << D->isHidden() << "\n";
}



void InfoDumper::DumpFunctionDeclInfo(const FunctionDecl *D) {
    // Hierarchy
    DumpNamedDeclInfo(D);

    // Print information about FunctionDecl
    llvm::errs() << D->getTemplatedKind() << "\n";

/*
    llvm::errs() << D->getQualifiedNameAsString() << "\n";
    llvm::errs() << D->isConstexpr() << "\n";
    llvm::errs() << D->isOutOfLine() << "\n";
*/
}

void InfoDumper::DumpCXXMethodDeclInfo(const CXXMethodDecl *D) {
    // Hierarchy
    DumpFunctionDeclInfo(D);

    // Dump the corresponding CXXRecordDecl
//    llvm::errs() << DUMP_CXX_METHOD_DECL_PARENT << "\n";
//    llvm::errs() << getId(D) << "\n";
//    llvm::errs() << getId(D->getParent()) << "\n";

    // Visit type
    //llvm::errs() << "Visiting type " << dumper->getId(D->getType().getTypePtr()) << " for node " << dumper->getId(D) << "\n";
    //dumper->VisitTypeTop(D->getType().getTypePtr());
}


void InfoDumper::DumpVarDeclInfo(const VarDecl *D) {
    // Hierarchy
    DumpNamedDeclInfo(D);

    // Print information about VarDecl
    llvm::errs() << D->getQualifiedNameAsString() << "\n";
    llvm::errs() << D->isConstexpr() << "\n";
    llvm::errs() << D->isStaticDataMember() << "\n";
    llvm::errs() << D->isOutOfLine() << "\n";
    llvm::errs() << D->hasGlobalStorage() << "\n";

/*
    if(D->isConstexpr()) {
        llvm::errs() << IS_CONST_EXPR << "\n";
        llvm::errs() << getId(D) << "\n";
    }

    // Print qualified name for all VarDecls
    llvm::errs() << VARDECL_QUALIFIED_NAME << "\n";
    llvm::errs() << getId(D) << "\n";
    llvm::errs() << D->getQualifiedNameAsString() << "\n";

    llvm::errs() << "VARDECL: " << D->getNameAsString() << "\n";
    llvm::errs() << "IS OUT OF LINE: " << D->isOutOfLine() << "\n";
    llvm::errs() << "IS STATIC DATA MEMBER: " << D->isStaticDataMember() << "\n";
*/
}

void InfoDumper::DumpParmVarDeclInfo(const ParmVarDecl *D) {

    // Hierarchy
    DumpVarDeclInfo(D);
}