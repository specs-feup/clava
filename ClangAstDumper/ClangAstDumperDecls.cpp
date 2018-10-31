//
// Created by JoaoBispo on 20/01/2017.
//

#include "ClangAstDumper.h"
#include "ClangAstDumperConstants.h"
#include "ClangNodes.h"
//#include "InfoDumperConstants.h"

#include "clang/AST/AST.h"

#include <iostream>
#include <sstream>

using namespace clang;


//#define OLD_OUTPUT

void ClangAstDumper::visitChildrenAndData(const Decl *D) {

    // Visit children
    visitChildren(D);

    // Dump data
    dataDumper.dump(D);

    // Dump id
    dumpIdToClassMap(D, clava::getClassName(D));
}




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

// Method shared by FunctionDecl hierarchy
//void VisitFunctionDeclBody(ClangAstDumper* dumper, const FunctionDecl *D);

//void VisitFunctionDeclBody(ClangAstDumper* dumper, const FunctionDecl *D) {



// Method shared by CXXMethodDecl hierarchy
//void VisitCXXMethodDeclBody(ClangAstDumper* dumper, const CXXMethodDecl *D);

//void VisitCXXMethodDeclBody(ClangAstDumper* dumper, const CXXMethodDecl *D) {


// Method shared by VarDecl hierarchy
//void VisitVarDeclBody(ClangAstDumper* dumper, const VarDecl *D);

//void VisitVarDeclBody(ClangAstDumper* dumper, const VarDecl *D) {


/*
 * DECLS
 */

bool ClangAstDumper::dumpDecl(const Decl* declAddr) {
    if(declAddr == nullptr) {
        return true;
    }

    if(seenDecls.count(declAddr) != 0) {
        return true;
    }

    log(declAddr);

    // A StmtDumper is created for each context,
    // no need to use id to disambiguate
    seenDecls.insert(declAddr);
/*
    // If system header dumping is disabled, add to null nodes
    FullSourceLoc fullLocation = Context->getFullLoc(declAddr->getLocStart());
    if (fullLocation.isValid() && fullLocation.isInSystemHeader()) {
        // Add as a null node
        // ...
        return true;
    }
*/

    std::ostringstream extendedId;
    extendedId << declAddr << "_" << id;


    // Dump location
#ifdef OLD_OUTPUT
    dumpSourceRange(extendedId.str(), declAddr->getLocStart(), declAddr->getLocEnd());
#endif
/*
    // If NamedDecl, check if it has name
    if (const NamedDecl *ND = dyn_cast<NamedDecl>(declAddr)) {
//        llvm::errs() << "Testing NAMED DECL: " << getId(ND) << "\n";

        // Check if has decl name, and decl name is not empty
        if (ND->getDeclName() && ND->getNameAsString().length() > 0) {
            llvm::errs() << DUMP_NAMED_DECL_WITHOUT_NAME << "\n";
            llvm::errs() << clava::getId(ND, id) << "\n";
            llvm::errs() << ND->getNameAsString() << "\n";
        }
    }
*/
/*
    // Dump system header decls, that are not dumped otherwise
    // This way they can be used as children for new nodes
    FullSourceLoc fullLocation = Context->getFullLoc(declAddr->getLocStart());
    if (fullLocation.isValid() && fullLocation.isInSystemHeader()) {
        clava::dump("<System Header Node Dump Begin>");
        clava::dump(clava::getId(declAddr, id));
        (*declAddr).dump(llvm::errs());
        clava::dump("<System Header Node Dump End>");
    }
*/
    // Top-level Node
    //llvm::errs() << "<All Decls>" << "\n";
    //llvm::errs() << declAddr << "_" << id << "\n";

    //dumpIdToClassMap(declAddr, clava::getClassName(declAddr));

    return false;
}

void ClangAstDumper::VisitDecl(const Decl *D) {
    if(dumpDecl(D)) {
        return;
    }

    bool isSystemHeader = clava::isSystemHeader(D, Context);
    if(isSystemHeader) {
        currentSystemHeaderLevel++;
    }

    /*
    if(systemHeaderThreashold > 0 && currentSystemHeaderLevel > systemHeaderThreashold) {
        // Add node as skipped node
        llvm::errs() << SKIPPED_NODES_MAP << "\n";
        llvm::errs() << clava::getId(D, id) << "\n";
        llvm::errs() << clava::getId((Decl*) nullptr, id) << "\n";

        currentSystemHeaderLevel--;
        return;
    }
     */

    visitChildrenAndData(D);

    if(isSystemHeader) {
        currentSystemHeaderLevel--;
    }
}

/*
void ClangAstDumper::VisitCXXRecordDecl(const CXXRecordDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    visitChildrenAndData(D);

#ifdef OLD_OUTPUT
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
#endif

}


void ClangAstDumper::VisitCXXConstructorDecl(const CXXConstructorDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    visitChildrenAndData(D);

#ifdef OLD_OUTPUT
    // Check if there are CXXCtorInitializers
    if(D->init_begin() != D->init_end()) {
        llvm::errs() << CXX_CTOR_INITIALIZER_BEGIN << "\n";

        // Dump address of decl
        llvm::errs() << clava::getId(D, id) << "\n";

        // Dump initializers info
        for (CXXConstructorDecl::init_const_iterator I = D->init_begin(), E = D->init_end(); I != E; ++I) {
            dumpCXXCtorInitializer(*I);
        }

        llvm::errs() << CXX_CTOR_INITIALIZER_END << "\n";
    }
#endif

}



void ClangAstDumper::VisitObjCImplementationDecl(const ObjCImplementationDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    visitChildrenAndData(D);
#ifdef OLD_OUTPUT
    // Dump data
    dataDumper.dump(clava::DeclNode::NAMED_DECL, D);

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
#endif

}

void ClangAstDumper::VisitTemplateDecl(const TemplateDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    visitChildrenAndData(D);

#ifdef OLD_OUTPUT
    dumpNumberTemplateParameters(D, D->getTemplateParameters());
#endif
}

void ClangAstDumper::VisitTemplateTypeParmDecl(const TemplateTypeParmDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    visitChildrenAndData(D);
}

void ClangAstDumper::VisitNamespaceAliasDecl(const NamespaceAliasDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    visitChildrenAndData(D);
#ifdef OLD_OUTPUT
    // Dump data
    //dataDumper.dump(clava::DeclNode::NAMED_DECL, D);

    // Dump nested namespace prefix
    llvm::errs() << DUMP_NAMESPACE_ALIAS_PREFIX << "\n";
    llvm::errs() << clava::getId(D, id) << "\n";
    llvm::errs() << loc2str(D->getQualifierLoc().getBeginLoc(), D->getQualifierLoc().getEndLoc()) << "\n";
#endif
}

void ClangAstDumper::VisitFieldDecl(const FieldDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    visitChildrenAndData(D);
}

void ClangAstDumper::VisitParmVarDecl(const ParmVarDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    visitChildrenAndData(D);

#ifdef OLD_OUTPUT
    if(D->hasInheritedDefaultArg()) {
        llvm::errs() << DUMP_PARM_VAR_DECL_HAS_INHERITED_DEFAULT_ARG << "\n";
        llvm::errs() << clava::getId(D, id) << "\n";
    }
#endif
}

void ClangAstDumper::VisitTypedefDecl(const TypedefDecl *D) {
    if(dumpDecl(D)) {
        return;
    }

    visitChildrenAndData(D);
}
*/
