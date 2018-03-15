//
// Created by JoaoBispo on 20/01/2017.
//

#include "ClangAstDumper.h"

#include "clang/AST/AST.h"

#include <iostream>
#include <sstream>

using namespace clang;


/*
 * DECLS PARTS
 */
void ClangAstDumper::dumpNumberTemplateParameters(const Decl *D, const TemplateParameterList *TPL) {
    int numberOfTemplateParameters = 0;
    if(TPL) {
        for (auto I = TPL->begin(), E = TPL->end(); I != E; ++I) {
            numberOfTemplateParameters++;
        }
    }

    llvm::errs() << DUMP_NUMBER_TEMPLATE_PARAMETERS << "\n";
    // Dump id
    llvm::errs() << D << "_" << id << "\n";
    // Dump number
    llvm::errs() << numberOfTemplateParameters << "\n";
}

/*
 * Shared DECL methods
 */

// Method shared by CXXMethodDecl hierarchy
void VisitCXXMethodDeclBody(ClangAstDumper* dumper, const CXXMethodDecl *D);

void VisitCXXMethodDeclBody(ClangAstDumper* dumper, const CXXMethodDecl *D) {
    if(D->hasBody()) {
        dumper->VisitStmtTop(D->getBody());
    }

    // Dump the corresponding CXXRecordDecl
    llvm::errs() << DUMP_CXX_METHOD_DECL_PARENT << "\n";
    llvm::errs() << dumper->getId(D) << "\n";
    llvm::errs() << dumper->getId(D->getParent()) << "\n";

    // Visit type
    //llvm::errs() << "Visiting type " << dumper->getId(D->getType().getTypePtr()) << " for node " << dumper->getId(D) << "\n";
    //dumper->VisitTypeTop(D->getType().getTypePtr());
}

// Method shared by VarDecl hierarchy
void VisitVarDeclBody(ClangAstDumper* dumper, const VarDecl *D);

void VisitVarDeclBody(ClangAstDumper* dumper, const VarDecl *D) {
    // Print information about VarDecl
    llvm::errs() << VARDECL_INFO << "\n";
    llvm::errs() << dumper->getId(D) << "\n";
    llvm::errs() << D->getQualifiedNameAsString() << "\n";
    llvm::errs() << D->isConstexpr() << "\n";
    llvm::errs() << D->isStaticDataMember() << "\n";
    llvm::errs() << D->isOutOfLine() << "\n";
    llvm::errs() << D->hasGlobalStorage() << "\n";
}

/*
 * DECLS
 */

bool ClangAstDumper::dumpDecl(const Decl* declAddr) {
    if(seenDecls.count(declAddr) != 0) {
        return true;
    }

    // A StmtDumper is created for each context,
    // no need to use id to disambiguate
    seenDecls.insert(declAddr);

    std::ostringstream extendedId;
    extendedId << declAddr << "_" << id;

    // Dump location
    dumpSourceRange(extendedId.str(), declAddr->getLocStart(), declAddr->getLocEnd());

    // If NamedDecl, check if it has name
    if (const NamedDecl *ND = dyn_cast<NamedDecl>(declAddr)) {
//        llvm::errs() << "Testing NAMED DECL: " << getId(ND) << "\n";

        // Check if has decl name, and decl name is not empty
        if (ND->getDeclName() && ND->getNameAsString().length() > 0) {
            llvm::errs() << DUMP_NAMED_DECL_WITHOUT_NAME << "\n";
            llvm::errs() << getId(ND) << "\n";
            llvm::errs() << ND->getNameAsString() << "\n";
        }
    }

    return false;
}

void ClangAstDumper::VisitDecl(const Decl *D) {
    dumpDecl(D);
}

void ClangAstDumper::VisitVarDecl(const VarDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    log("VarDecl", D);
    if (D->hasInit()) {
        VisitStmtTop(D->getInit());
    }

    VisitVarDeclBody(this, D);

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


void ClangAstDumper::VisitCXXRecordDecl(const CXXRecordDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    log("CXXRecordDecl", D);

//    llvm::errs() << "CXXRECPRD DECL: " << getId(D) <<  "\n";
    // Visit definition
    if(D->hasDefinition()) {
        VisitDeclTop(D->getDefinition());

        // If has bases, dump types of bases
        if(D->getNumBases() != 0) {
            llvm::errs() << "<CXXRecordDecl Bases Start>\n";
            // First address is id of CXXRecordDecl
            llvm::errs() << D << "_" << id << "\n";
            for (const auto &I : D->bases()) {
                llvm::errs() << I.getType().getTypePtr() << "_" << id << "\n";
            }
            llvm::errs() << "<CXXRecordDecl Bases End>\n";

        }

        // TODO: CAN ALWAYS CALL THIS, OUTSIDE OF IF?

        // Visit constructors
        for (auto ctor : D->ctors()) {
            VisitDeclTop(ctor);
        }


        // Visit methods
        // This makes the program explode

        for (auto method : D->methods()) {
            VisitDeclTop(method);
        }

        // Visit fields
        for (auto field : D->fields()) {
            VisitDeclTop(field);
        }

    }



    //for (auto &I : D->redecls()) {
    //for (auto &I : ((clang::Redeclarable<clang::TagDecl>*) D)->redecls()) {
    /*
    for (auto &I : ((clang::Decl*) D)->redecls()) {
        //llvm::errs() << "REDECLARABLE___:" << getId(I) << "\n";
        VisitDeclTop(I);
    }
     */

    /*
    // Visit constructors
    for (auto ctor : D->ctors()) {
        VisitDeclTop(ctor);
    }
     */


//    llvm::errs() << "LAMBDA CALL OP:" << D->getLambdaCallOperator() << "\n";
//    llvm::errs() << "LAMBDA CONTEXT DECL:" << D->getLambdaContextDecl() << "\n";



    /*
    // Visit bases
    for (const auto &I : D->bases()) {
        llvm::errs() << "BASE:" << &I << "\n";
    }

    // Visit virtual bases
    for (const auto &I : D->vbases()) {
        llvm::errs() << "VBASE:" << &I << "\n";
    }

    // Visit friends
    for (const auto &I : D->friends()) {
        llvm::errs() << "FRIEND:" << &I << "\n";
    }
    */
    // Visit captures
    for (const auto &I : D->captures()) {
        if(I.capturesVariable()) {
            VisitDeclTop(I.getCapturedVar());
        }

        //llvm::errs() << "CAPTURE:" << &I << "\n";
    }


    // Visit canonical decl
    if(D->getCanonicalDecl()) {
        VisitDeclTop(D->getCanonicalDecl());
    }

    // Visit previous decl
    if(D->getPreviousDecl()) {
        VisitDeclTop(D->getPreviousDecl());
        //llvm::errs() << "PREVIUIOS DECL: " << D->getPreviousDecl() << "\n";
    }

    // Visit most recent decl
    if(D->getMostRecentDecl()) {
        VisitDeclTop(D->getMostRecentDecl());
        //llvm::errs() << "MOST RECENT DECL: " << D->getMostRecentDecl() << "\n";
    }

    //  Visit member class from which it was instantiated
    if(D->getInstantiatedFromMemberClass()) {
        VisitDeclTop(D->getInstantiatedFromMemberClass());
        //llvm::errs() << "INST MEMBER: " << D->getInstantiatedFromMemberClass() << "\n";
    }

    //  Visit the record declaration from which this record could be instantiated.
    if(D->getTemplateInstantiationPattern()) {
        VisitDeclTop(D->getTemplateInstantiationPattern());
        //llvm::errs() << "MEMBER CLASS: " << D->getTemplateInstantiationPattern() << "\n";
    }

}




void ClangAstDumper::VisitCXXMethodDecl(const CXXMethodDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    log("CXXMethodDecl", D);

    VisitCXXMethodDeclBody(this, D);

}

void ClangAstDumper::VisitCXXConstructorDecl(const CXXConstructorDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    log("CXXConstructorDecl", D);

    VisitCXXMethodDeclBody(this, D);


    // Check if there are CXXCtorInitializers
    if(D->init_begin() != D->init_end()) {
        llvm::errs() << CXX_CTOR_INITIALIZER_BEGIN << "\n";

        // Dump address of decl
        llvm::errs() << getId(D) << "\n";

        // Dump initializers info
        for (CXXConstructorDecl::init_const_iterator I = D->init_begin(), E = D->init_end(); I != E; ++I) {
            dumpCXXCtorInitializer(*I);
        }

        llvm::errs() << CXX_CTOR_INITIALIZER_END << "\n";
    }




    // Store id if it has no name
    // Replaced with code in dumpDecl()
    /*
    if(D->getDeclName().getAsString().length() == 0) {
        llvm::errs() << DUMP_NAMED_DECL_WITHOUT_NAME << "\n";
        llvm::errs() << getId(D) << "\n";
    }
     */

}


void ClangAstDumper::VisitObjCImplementationDecl(const ObjCImplementationDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    log("ObjCImplementationDecl", D);

    // Check if there are CXXCtorInitializers
    if(D->init_begin() != D->init_end()) {
        llvm::errs() << CXX_CTOR_INITIALIZER_BEGIN << "\n";

        // Dump address of decl
        llvm::errs() << D << "_" << id << "\n";

        // Dump initializers info
        for (ObjCImplementationDecl::init_const_iterator I = D->init_begin(), E = D->init_end(); I != E; ++I) {
            dumpCXXCtorInitializer(*I);
        }

        llvm::errs() << CXX_CTOR_INITIALIZER_END << "\n";
    }


}

void ClangAstDumper::VisitTemplateDecl(const TemplateDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    log("TemplateDecl", D);

    dumpNumberTemplateParameters(D, D->getTemplateParameters());
}

void ClangAstDumper::VisitTemplateTypeParmDecl(const TemplateTypeParmDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    log("TemplateTypeParmDecl", D);
}

void ClangAstDumper::VisitNamespaceAliasDecl(const NamespaceAliasDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    log("NamespaceAliasDecl", D);

    // Dump nested namespace prefix
    llvm::errs() << DUMP_NAMESPACE_ALIAS_PREFIX << "\n";
    llvm::errs() << getId(D) << "\n";
    llvm::errs() << loc2str(D->getQualifierLoc().getBeginLoc(), D->getQualifierLoc().getEndLoc()) << "\n";

}

void ClangAstDumper::VisitFieldDecl(const FieldDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    log("FieldDecl", D);

//    llvm::errs() << "DUMPING FIELD DECL: " << getId(D) << "\n";

    // Dump nested namespace prefix
    llvm::errs() << DUMP_FIELD_DECL_INFO << "\n";
    llvm::errs() << getId(D) << "\n";
    llvm::errs() << toBoolString(D->isBitField()) << "\n";
    llvm::errs() << toBoolString(D->getInClassInitializer() != nullptr) << "\n";

}

void ClangAstDumper::VisitParmVarDecl(const ParmVarDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    log("ParmVarDecl", D);

    if(D->hasInheritedDefaultArg()) {
        llvm::errs() << DUMP_PARM_VAR_DECL_HAS_INHERITED_DEFAULT_ARG << "\n";
        llvm::errs() << getId(D) << "\n";
    }

    VisitVarDeclBody(this, D);

}

void ClangAstDumper::VisitTypedefDecl(const TypedefDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    log("TypedefDecl", D);

    // Dump typedef source
    llvm::errs() << TYPEDEF_DECL_SOURCE << "\n";
    llvm::errs() << getId(D) << "\n";
    llvm::errs() << loc2str(D->getLocStart(), D->getLocEnd()) << "\n";
}

