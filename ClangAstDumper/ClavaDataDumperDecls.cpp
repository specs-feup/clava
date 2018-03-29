//
// Created by JoaoBispo on 18/03/2018.
//

#include "ClavaDataDumper.h"

void clava::ClavaDataDumper::dump(clava::DeclNode declNode, const Decl* D) {
    DumpHeader(clava::DECL_DATA_NAMES[declNode], D);

    switch(declNode) {
        case clava::DeclNode::DECL: DumpDeclInfo(D); break;
        case clava::DeclNode::NAMED_DECL: DumpNamedDeclInfo(static_cast<const NamedDecl*>(D)); break;
        case clava::DeclNode::FUNCTION_DECL: DumpFunctionDeclInfo(static_cast<const FunctionDecl*>(D)); break;
        case clava::DeclNode::CXX_METHOD_DECL: DumpCXXMethodDeclInfo(static_cast<const CXXMethodDecl*>(D)); break;
        case clava::DeclNode::VAR_DECL: DumpVarDeclInfo(static_cast<const VarDecl*>(D)); break;
        case clava::DeclNode::PARM_VAR_DECL: DumpParmVarDeclInfo(static_cast<const ParmVarDecl*>(D)); break;
        default: throw std::invalid_argument("ClangDataDumper::dump: Case not implemented, '"+clava::DECL_DATA_NAMES[declNode]+"'");
    }
}



void clava::ClavaDataDumper::DumpDeclInfo(const Decl *D) {

    // Print information about Decl
    dump(D->isImplicit());
    dump(D->isUsed());
    dump(D->isReferenced());
    dump(D->isInvalidDecl());
    //llvm::errs() << D->isImplicit() << "\n";
    //llvm::errs() << D->isUsed() << "\n";
    //llvm::errs() << D->isReferenced() << "\n";
    //llvm::errs() << D->isInvalidDecl() << "\n";

    // TODO: Attributes?
//    for (Decl::attr_iterator I = D->attr_begin(), E = D->attr_end(); I != E; ++I)


}

void clava::ClavaDataDumper::DumpNamedDeclInfo(const NamedDecl *D) {
    // Hierarchy
    DumpDeclInfo(D);

    // Print information about NamedDecl
    dump(D->getQualifiedNameAsString());
    dump(D->getDeclName().getNameKind());
    dump(D->isHidden());
    //llvm::errs() << D->getQualifiedNameAsString() << "\n";
    //llvm::errs() << D->getDeclName().getNameKind() << "\n";
    //llvm::errs() << D->isHidden() << "\n";

}



void clava::ClavaDataDumper::DumpFunctionDeclInfo(const FunctionDecl *D) {
    // Hierarchy
    DumpNamedDeclInfo(D);

    // Print information about FunctionDecl
    dump(D->isConstexpr());
    dump(D->getTemplatedKind());
//    llvm::errs() << D->isConstexpr() << "\n";
//    llvm::errs() << D->getTemplatedKind() << "\n";


/*
  StorageClass SC = D->getStorageClass();
  if (SC != SC_None)
    OS << ' ' << VarDecl::getStorageClassSpecifierString(SC);
  if (D->isInlineSpecified())
    OS << " inline";
  if (D->isVirtualAsWritten())
    OS << " virtual";
  if (D->isModulePrivate())
    OS << " __module_private__";
*/
}

void clava::ClavaDataDumper::DumpCXXMethodDeclInfo(const CXXMethodDecl *D) {
    // Hierarchy
    DumpFunctionDeclInfo(D);

    dump(getId(D->getParent()));
    // Dump the corresponding CXXRecordDecl
//    llvm::errs() << DUMP_CXX_METHOD_DECL_PARENT << "\n";
//    llvm::errs() << getId(D) << "\n";
//    llvm::errs() << getId(D->getParent()) << "\n";

    // Visit type
    //llvm::errs() << "Visiting type " << dumper->getId(D->getType().getTypePtr()) << " for node " << dumper->getId(D) << "\n";
    //dumper->VisitTypeTop(D->getType().getTypePtr());
}


void clava::ClavaDataDumper::DumpVarDeclInfo(const VarDecl *D) {
    // Hierarchy
    DumpNamedDeclInfo(D);

    // Print information about VarDecl
    dump(D->getStorageClass());
    dump(D->getTLSKind());
    dump(D->isModulePrivate());
    dump(D->isNRVOVariable());
    dump(D->getInitStyle());

    dump(D->isConstexpr());
    dump(D->isStaticDataMember() );
    dump(D->isOutOfLine());
    dump(D->hasGlobalStorage());
//    llvm::errs() << D->isConstexpr() << "\n";
//    llvm::errs() << D->isStaticDataMember() << "\n";
//    llvm::errs() << D->isOutOfLine() << "\n";
//    llvm::errs() << D->hasGlobalStorage() << "\n";


    /**



  if (D->hasInit()) {
    switch (D->getInitStyle()) {
    case VarDecl::CInit: OS << " cinit"; break;
    case VarDecl::CallInit: OS << " callinit"; break;
    case VarDecl::ListInit: OS << " listinit"; break;
    }
    dumpStmt(D->getInit());
  }
     */

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

void clava::ClavaDataDumper::DumpParmVarDeclInfo(const ParmVarDecl *D) {

    // Hierarchy
    DumpVarDeclInfo(D);

    // Print information about ParmVarDecl
    dump(D->hasInheritedDefaultArg());
    //llvm::errs() << D->hasInheritedDefaultArg() << "\n";
}