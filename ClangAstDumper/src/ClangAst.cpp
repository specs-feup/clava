//------------------------------------------------------------------------------
//
// Created by JoaoBispo
//
// Based on public domain code by Eli Bendersky (eliben@gmail.com) - http://eli.thegreenplace.net/
//------------------------------------------------------------------------------
#include "ClangAst.h"
#include "ClangAstDumperConstants.h"
#include "ClangNodes.h"

#include "clang/AST/AST.h"
#include "clang/AST/ASTConsumer.h"
#include "clang/AST/RecursiveASTVisitor.h"
#include "clang/Basic/TargetInfo.h"
#include "clang/Frontend/ASTConsumers.h"
#include "clang/Frontend/FrontendActions.h"
#include "clang/Frontend/CompilerInstance.h"
#include "clang/AST/Comment.h"
#include "clang/AST/Stmt.h"
#include "clang/Lex/Lexer.h"
#include "clang/Lex/Preprocessor.h"


#include <iostream>
#include <fstream>
#include <atomic>
#include <sstream>
#include <string>



using namespace clang;

//#define OLD_OUTPUT

static llvm::cl::OptionCategory ToolingSampleCategory("Tooling Sample");


static constexpr const char* const PREFIX = "COUNTER";


/* DumpAstVisitor implementation */

bool DumpAstVisitor::TraverseDecl(Decl *D) {
    FullSourceLoc fullLocation = Context->getFullLoc(D->getBeginLoc());
    if (fullLocation.isValid() && !fullLocation.isInSystemHeader()) {
#ifdef OLD_OUTPUT
        (*D).dump(llvm::outs());
#endif
        // Top-level Node
        llvm::errs() << TOP_LEVEL_NODES << "\n";
        llvm::errs() << D << "_" << id << "\n";

        // If it is not being visited, if this is called it can give an error
        // Visit Top Node
        // Experiment: can this visitor replace all other visitors?
        //dumper.VisitDeclTop(D);
        //dumper->VisitDeclTop(D);
    }

    return false;

}



//bool PrintNodesTypesRelationsVisitor::TraverseDecl(Decl *D) {
/*
    FullSourceLoc fullLocation = Context->getFullLoc(D->getLocStart());
    if (fullLocation.isValid() && !fullLocation.isInSystemHeader()) {
        // Visit Top Node
        // Experiment: can this visitor replace all other visitors?
        dumper.VisitDeclTop(D);
    }

*/
//    return RecursiveASTVisitor::TraverseDecl(D);
    /*
    FullSourceLoc fullLocation = Context->getFullLoc(D->getLocStart());
    if (fullLocation.isValid() && !fullLocation.isInSystemHeader()) {
        // Visit Top Node
        // Experiment: can this visitor replace all other visitors?
        dumper.VisitDeclTop(D);
        //dumper->VisitDeclTop(D);
    }

    // TODO: If at some moment dumper provides all information, this can be set to false
    return true;
*/
//}




PrintNodesTypesRelationsVisitor::PrintNodesTypesRelationsVisitor(ASTContext *Context, int id, ClangAstDumper dumper) : Context(Context), id(id), dumper(dumper)  {};
//PrintNodesTypesRelationsVisitor::PrintNodesTypesRelationsVisitor(ASTContext *Context, int id, ClangAstDumper *dumper) : Context(Context), id(id), dumper(dumper)  {};

static std::string stmt2str(clang::Stmt *d, clang::SourceManager *sm, clang::LangOptions lopt) {
    clang::SourceLocation b(d->getBeginLoc()), _e(d->getEndLoc());
    clang::SourceLocation e(clang::Lexer::getLocForEndOfToken(_e, 0, *sm, lopt));
    return std::string(sm->getCharacterData(b),
                       sm->getCharacterData(e)-sm->getCharacterData(b));
}

    static std::string loc2str(SourceLocation locStart, SourceLocation locEnd, ASTContext *Context) {
        clang::SourceManager *sm = &Context->getSourceManager();
        clang::LangOptions lopt = Context->getLangOpts();

        clang::SourceLocation b(locStart), _e(locEnd);
        clang::SourceLocation e(clang::Lexer::getLocForEndOfToken(_e, 0, *sm, lopt));
        return std::string(sm->getCharacterData(b), sm->getCharacterData(e)-sm->getCharacterData(b));
    }


/*
    bool PrintNodesTypesRelationsVisitor::VisitOMPExecutableDirective(OMPExecutableDirective * D) {

        FullSourceLoc fullLocation = Context->getFullLoc(D->getBeginLoc());

        if (!fullLocation.isInSystemHeader()) {
            // Get OMP Directive
            DumpResources::omp << D << "_" << id << "->" << stmt2str(D, &Context->getSourceManager(), Context->getLangOpts()) << "\n";
            DumpResources::omp << "NUM_CLAUSES" << "->" << D->getNumClauses() << "\n";
            for(unsigned i=0; i<D->getNumClauses(); i++) {
                OMPClause* clause = D->getClause(i);
                DumpResources::omp << clause->getClauseKind() << "->" << loc2str(clause->getBeginLoc(), clause->getEndLoc(), Context) << "\n";
            }
        }

        return true;
    }
*/

/*
    bool PrintNodesTypesRelationsVisitor::VisitDeclRefExpr(DeclRefExpr * D) {

        FullSourceLoc fullLocation = Context->getFullLoc(D->getLocStart());

        if (!fullLocation.isInSystemHeader()) {
            if (D->getQualifier() != nullptr) {
                llvm::errs() << "DECL_REF_EXPR QUALIFIER BEGIN\n";
                llvm::errs() << D << "_" << id << "\n";
                D->getQualifier()->dump();
                llvm::errs() << "\nDECL_REF_EXPR QUALIFIER END\n";
            }


            if(D->hasExplicitTemplateArgs()) {
                DumpResources::template_args <<  D << "_" << id << "\n";
            }

        }
        return true;
    }
*/


    bool PrintNodesTypesRelationsVisitor::VisitCXXConstructExpr(CXXConstructExpr * D) {

        FullSourceLoc fullLocation = Context->getFullLoc(D->getBeginLoc());
        if (fullLocation.isValid() && !fullLocation.isInSystemHeader()) {

            // Check if temporary object
            if(D->isTemporaryObject(D->getConstructor()->getASTContext(), D->getConstructor()->getParent())) {
                DumpResources::is_temporary <<  D << "_" << id << "\n";
            }

        }


        return true;
    }

    // Dump types for Expr, TypeDecl and ValueDecl, as well as the connection between them
    bool PrintNodesTypesRelationsVisitor::VisitExpr(Expr *D) {
#ifdef OLD_OUTPUT
        FullSourceLoc fullLocation = Context->getFullLoc(D->getBeginLoc());
        if (fullLocation.isValid() && !fullLocation.isInSystemHeader()) {
            //if(D->getType().isNull()) {
            //    llvm::errs() << "VisitExpr null type\n";
            //}
            dumpNodeToType(DumpResources::nodetypes,D, D->getType());

        }
#endif
        return true;
    }

    bool PrintNodesTypesRelationsVisitor::VisitTypeDecl(TypeDecl *D) {

#ifdef OLD_OUTPUT
        FullSourceLoc fullLocation = Context->getFullLoc(D->getBeginLoc());
        if (fullLocation.isValid() && !fullLocation.isInSystemHeader()) {
            //llvm::errs() << "Visiting Type Decl: " << D << "\n";
            dumpNodeToType(DumpResources::nodetypes, D, D->getTypeForDecl());
        }
#endif
        return true;
    }

    /**
     * Typedefs will be visited by 'VisitTypeDecl' but will null, this override extracts the correct information for typedefs
     * @param D
     * @return
     */
    bool PrintNodesTypesRelationsVisitor::VisitTypedefNameDecl(TypedefNameDecl *D) {
#ifdef OLD_OUTPUT
        FullSourceLoc fullLocation = Context->getFullLoc(D->getBeginLoc());
        if (fullLocation.isValid() && !fullLocation.isInSystemHeader()) {
            //if(D->getUnderlyingType().isNull()) {
            //    llvm::errs() << "VisitNameDecl null type\n";
            //}
            dumpNodeToType(DumpResources::nodetypes, D, D->getUnderlyingType());

        }
#endif
        return true;
    }


    bool PrintNodesTypesRelationsVisitor::VisitEnumDecl(EnumDecl *D) {
        FullSourceLoc fullLocation = Context->getFullLoc(D->getBeginLoc());
        if (fullLocation.isValid() && !fullLocation.isInSystemHeader()) {
            //if(D->getIntegerType().isNull()) {
            //    llvm::errs() << "VisitEnum null type\n";
            //}
            dumpNodeToType(DumpResources::enum_integer_type, D,D->getIntegerType(), false);

        }

        return true;
    }



    bool PrintNodesTypesRelationsVisitor::VisitValueDecl(ValueDecl *D) {
#ifdef OLD_OUTPUT
        FullSourceLoc fullLocation = Context->getFullLoc(D->getLocStart());
        if (fullLocation.isValid() && !fullLocation.isInSystemHeader()) {
            //llvm::errs() << "Visiting Value Decl: " << D << "\n";
            //if(D->getType().isNull()) {
            //    llvm::errs() << "VisitValueDecl null type\n";
            //}
            dumpNodeToType(DumpResources::nodetypes, D, D->getType());

            return true;
        }
#endif
        return true;
    }

    // Visit only nodes from the source code, ignore system headers
    bool PrintNodesTypesRelationsVisitor::VisitDecl(Decl *D) {
        FullSourceLoc fullLocation = Context->getFullLoc(D->getBeginLoc());

        if (fullLocation.isValid() && !fullLocation.isInSystemHeader()) {
            dumper.VisitDeclTop(D);
            //dumper->VisitDeclTop(D);
            return true;
        }

        return true;
    }

    bool PrintNodesTypesRelationsVisitor::VisitStmt(Stmt *D) {
        FullSourceLoc fullLocation = Context->getFullLoc(D->getBeginLoc());

        if (fullLocation.isValid() && !fullLocation.isInSystemHeader()) {
            dumper.VisitStmtTop(D);
            //dumper->VisitStmtTop(D);
            return true;
        }

        return true;
    }

    bool PrintNodesTypesRelationsVisitor::VisitLambdaExpr(LambdaExpr *D) {

        #ifdef OLD_OUTPUT
        FullSourceLoc fullLocation = Context->getFullLoc(D->getBeginLoc());
        if (fullLocation.isValid() && !fullLocation.isInSystemHeader()) {
            // Dump self
            dumpNodeToType(DumpResources::nodetypes,D, D->getType());

            // Traverse lambda class
            TraverseCXXRecordDecl(D->getLambdaClass());
            //TraverseDecl(D->getLambdaClass());
            //VisitTypeDecl(D->getLambdaClass());
        }
        #endif

        return true;
    }


void PrintNodesTypesRelationsVisitor::dumpNodeToType(std::ofstream &stream, void* nodeAddr, const QualType& type, bool checkDuplicates) {

    // Opaque pointer, so that we can obtain the qualifiers
    void* typeAddr = !type.isNull() ? type.getAsOpaquePtr() : nullptr;
    //const Type* typeAddr = type.getTypePtrOrNull();

    if(checkDuplicates) {
        if(seenNodes.count(nodeAddr) == 0) {
            stream << nodeAddr << "_" << id << "->" << typeAddr << "_" << id << "\n";
            seenNodes.insert(nodeAddr);
        }
    } else {
        stream << nodeAddr << "_" << id << "->" << typeAddr << "_" << id << "\n";
    }


    dumper.VisitTypeTop(type);
    //dumper->VisitTypeTop(type);
}


 void PrintNodesTypesRelationsVisitor::dumpNodeToType(std::ofstream &stream, void* nodeAddr, const Type* typeAddr, bool checkDuplicates) {

    if(!typeAddr) {
        return;
    }


    if(checkDuplicates) {
        if(seenNodes.count(nodeAddr) == 0) {
            stream << nodeAddr << "_" << id << "->" << typeAddr << "_" << id << "\n";
            seenNodes.insert(nodeAddr);
        }
    } else {
        stream << nodeAddr << "_" << id << "->" << typeAddr << "_" << id << "\n";
    }

    dumper.VisitTypeTop(typeAddr);
    // dumper->VisitTypeTop(typeAddr);
}

/*
ClangAstDumper PrintNodesTypesRelationsVisitor::getDumper() {
        return dumper;
    }
*/
/*
    MyASTConsumer::MyASTConsumer(ASTContext *C, int id) : topLevelDeclVisitor(C, id), printRelationsVisitor(C, id), id(id) {
        std::ofstream consumer;
        consumer.open("consumer_order.txt", std::ofstream::app);
        consumer << "ASTConsumer built " << id << "\n";
        consumer.close();
    }
*/
MyASTConsumer::MyASTConsumer(ASTContext *C, int id, ClangAstDumper dumper) : id(id)
//MyASTConsumer::MyASTConsumer(ASTContext *C, int id, ClangAstDumper *dumper) : id(id)
        ,topLevelDeclVisitor(C, id), printRelationsVisitor(C, id, dumper)  {
/*
    this->id = id;
    this->dumper = ClangAstDumper(C, id);
    topLevelDeclVisitor = DumpAstVisitor(C, id, dumper);
    printRelationsVisitor = PrintNodesTypesRelationsVisitor(C, id, dumper);
*/
    //dumper(C, id), topLevelDeclVisitor(C, id, dumper), printRelationsVisitor(C, id, dumper), id(id)
    std::ofstream consumer;
    consumer.open("consumer_order.txt", std::ofstream::app);
    consumer << "ASTConsumer built " << id << "\n";
    consumer.close();
}

    MyASTConsumer::~MyASTConsumer() {
        std::ofstream consumer;
        consumer.open("consumer_order.txt", std::ofstream::app);
        consumer << "ASTConsumer destroyed " << id << "\n";
        consumer.close();
    }




    // Override the method that gets called for each parsed top-level declaration.
    bool MyASTConsumer::HandleTopLevelDecl(DeclGroupRef DR) {

        for (DeclGroupRef::iterator b = DR.begin(), e = DR.end(); b != e; ++b) {
            // Traverse the declaration using our AST visitor.
            topLevelDeclVisitor.TraverseDecl(*b);
        }


        for (DeclGroupRef::iterator b = DR.begin(), e = DR.end(); b != e; ++b) {
            printRelationsVisitor.TraverseDecl(*b);
        }

        return true;
    }

    void MyASTConsumer::HandleTranslationUnit(ASTContext &Ctx) {
    /*
        for (auto b = Ctx.getTranslationUnitDecl()->decls().begin(), e = Ctx.getTranslationUnitDecl()->decls().end(); b != e; ++b) {
            //counter++;
            //llvm::errs() << "Decl kind: " << (*b)->getKind() << " decls\n";
            topLevelDeclVisitor.TraverseDecl(*b);
        }

        for (auto b = Ctx.getTranslationUnitDecl()->decls().begin(), e = Ctx.getTranslationUnitDecl()->decls().end(); b != e; ++b) {
            printRelationsVisitor.TraverseDecl(*b);
        }

        //llvm::errs() << "Found " << counter << " decls\n";

        int counter = 0;
        llvm::errs() << "After translation unit\n";
        for (auto b = Ctx.getTranslationUnitDecl()->decls().begin(), e = Ctx.getTranslationUnitDecl()->decls().end(); b != e; ++b) {
            counter++;
            llvm::errs() << "Decl kind: " << (*b)->getKind() << " decls\n";
        }
        */
    }




    // Not sure if Clang performs compilation of different translation units in parallel, using atomic just in case.
    std::atomic<int> GLOBAL_COUNTER(1);



    /*** DumpAstAction ***/

    // For each source file provided to the tool, a new FrontendAction is created.
    std::unique_ptr<ASTConsumer> DumpAstAction::CreateASTConsumer(CompilerInstance &CI, StringRef file) {
        int counter = GLOBAL_COUNTER.fetch_add(1);

        // If runId greater than 0, use it instead of global counter
        if(DumpResources::runId  > 0) {
            counter = DumpResources::runId;
        }

#ifdef OLD_OUTPUT
        DumpResources::writeCounter(counter);
#endif
        //llvm::errs() << "CUSTOM ID: " << DumpResources::runId  << "\n";


        dumpCompilerInstanceData(CI, file);

        // Dump id->file data
        llvm::errs() << ID_FILE_MAP << "\n";
        llvm::errs() << counter << "\n";
        llvm::errs() << file << "\n";

        ASTContext *Context = &CI.getASTContext();

        ClangAstDumper dumper(Context, counter, DumpResources::systemHeaderThreshold);

        return std::make_unique<MyASTConsumer>(Context, counter, dumper);
        //return llvm::make_unique<MyASTConsumer>(Context, counter, &dumper);
    }


    void DumpAstAction::dumpCompilerInstanceData(CompilerInstance &CI, StringRef file) {
        clava::dump(COMPILER_INSTANCE_DATA);

        //llvm::errs() << "File\n";
        clava::dump(file.str());

        clava::dump(CI.getInvocation().getLangOpts()->LineComment);
        // Derived from Std.isC89 in Clang 3.8
        clava::dump(CI.getInvocation().getLangOpts()->GNUInline);
        clava::dump(CI.getInvocation().getLangOpts()->C99);
        clava::dump(CI.getInvocation().getLangOpts()->C11);
        clava::dump(CI.getInvocation().getLangOpts()->CPlusPlus);
        clava::dump(CI.getInvocation().getLangOpts()->CPlusPlus11);
        clava::dump(CI.getInvocation().getLangOpts()->CPlusPlus14);
        clava::dump(CI.getInvocation().getLangOpts()->CPlusPlus17);
        clava::dump(CI.getInvocation().getLangOpts()->Digraphs);
        clava::dump(CI.getInvocation().getLangOpts()->GNUMode);
        clava::dump(CI.getInvocation().getLangOpts()->HexFloats);
        clava::dump(CI.getInvocation().getLangOpts()->ImplicitInt);

        clava::dump(CI.getInvocation().getLangOpts()->OpenCL);
        clava::dump(CI.getInvocation().getLangOpts()->OpenCLVersion);
        clava::dump(CI.getInvocation().getLangOpts()->NativeHalfType);

        clava::dump(CI.getInvocation().getLangOpts()->CUDA);

        clava::dump(CI.getInvocation().getLangOpts()->Bool);
        clava::dump(CI.getInvocation().getLangOpts()->Half);
        clava::dump(CI.getInvocation().getLangOpts()->WChar);
        
        clava::dump(CI.getTarget().getCharWidth());
        clava::dump(CI.getTarget().getFloatWidth());
        clava::dump(CI.getTarget().getDoubleWidth());
        clava::dump(CI.getTarget().getLongDoubleWidth());
        clava::dump(CI.getTarget().getBoolWidth());
        clava::dump(CI.getTarget().getShortWidth());
        clava::dump(CI.getTarget().getIntWidth());
        clava::dump(CI.getTarget().getLongWidth());
        clava::dump(CI.getTarget().getLongLongWidth());
    }


    /*** DumpIncludesAction ***/
	// Based on explanation from this website https://xaizek.github.io/2015-04-23/detecting-wrong-first-include/
    void DumpIncludesAction::ExecuteAction() {

        IncludeDumper includeDumper(getCompilerInstance());
        getCompilerInstance().getPreprocessor().addPPCallbacks(
                includeDumper.createPreprocessorCallbacks()
        );

        PreprocessOnlyAction::ExecuteAction();
    }



    /*** IncludeDumper ***/

    IncludeDumper::IncludeDumper(CompilerInstance &compilerInstance) : compilerInstance(compilerInstance), sm(compilerInstance.getSourceManager()) {};


    std::unique_ptr<PPCallbacks> IncludeDumper::createPreprocessorCallbacks() {
        return std::unique_ptr<PPCallbacks>(new CallbacksProxy(*this));
    }

    void IncludeDumper::InclusionDirective(SourceLocation HashLoc, const Token &IncludeTok, StringRef FileName,
                                        bool IsAngled, CharSourceRange FilenameRange, const FileEntry *File,
                                        StringRef SearchPath, StringRef RelativePath, const Module *Imported, SrcMgr::CharacteristicKind FileType) {

        //clang::SourceManager &sm = compilerInstance.getSourceManager();

        if (!sm.isInSystemHeader(HashLoc)) {
#ifdef OLD_OUTPUT
            DumpResources::includes << "source:" << sm.getFilename(HashLoc).str() << "|";
            DumpResources::includes << "include:" << FileName.str() << "|";
            DumpResources::includes << "line:" << sm.getSpellingLineNumber(HashLoc) << "|";
            DumpResources::includes << "angled:" << IsAngled ? "true" : "false";
            DumpResources::includes << "\n";
#endif
            // Includes information in stream
            llvm::errs() << INCLUDES << "\n";
            // Source
            llvm::errs() << sm.getFilename(HashLoc).str() << "\n";
            llvm::errs() << FileName.str() << "\n";
            llvm::errs() << sm.getSpellingLineNumber(HashLoc) << "\n";
            llvm::errs() << IsAngled << "\n";
        }
    }


    void IncludeDumper::PragmaDirective(SourceLocation Loc, PragmaIntroducerKind Introducer) {

        //clang::SourceManager &sm = compilerInstance.getSourceManager();

        // Ignore system headers
        if(sm.isInSystemHeader(Loc)) {
            return;
        }

        // Pragma location
        clava::dump(PRAGMA);
        clava::dump(sm.getFilename(Loc));
        clava::dump(sm.getSpellingLineNumber(Loc));
        clava::dump(sm.getSpellingColumnNumber(Loc));
    }

    void IncludeDumper::FileChanged(SourceLocation Loc, FileChangeReason Reason, SrcMgr::CharacteristicKind FileType, FileID PrevFID) {
//llvm::errs() << "File changed: " << sm.getFilename(Loc) << "\n";
//llvm::errs() << "Reason: " << Reason << "\n";
    }

    void IncludeDumper::MacroExpands(const Token & MacroNameTok, const MacroDefinition & MD, SourceRange Range, const MacroArgs * Args) {
/*
        llvm::errs() << "Dumping Macro info:\n";
        MD.getMacroInfo()->dump();
        llvm::errs() << "\n";
        //llvm::errs() << "\nDumping Macro info end\n";


        llvm::errs() << "Definition begin:\n";
        MD.getMacroInfo()->getDefinitionLoc().dump(compilerInstance.getSourceManager());
        llvm::errs() << "\nDefinition length: " << MD.getMacroInfo()->getDefinitionLength(compilerInstance.getSourceManager()) <<"\n";
        llvm::errs() << "\n";
        //llvm::errs() << "\nDefinition begin end\n";

        llvm::errs() << "Expansion begin:\n";
        Range.getBegin().dump(compilerInstance.getSourceManager());
        llvm::errs() << "\n";

        */



/*
        llvm::errs() << "Is id: " << MacroNameTok.isAnyIdentifier() << "\n";
        if(MacroNameTok.isAnyIdentifier()) {
            llvm::errs() << "RAW id: " << MacroNameTok.getRawIdentifier().str() << "\n";
        }
        llvm::errs() << "Kind: " << MacroNameTok.getKind() << "\n";
        */
 //       llvm::errs() << "ID INFO: " << MacroNameTok. << "\n";
//       llvm::errs() << "Token begin:\n";
  //      MacroNameTok.getLocation().dump(compilerInstance.getSourceManager());
    //    llvm::errs() << "\nToken end\n";


        //llvm::errs() << "\nSource begin end\n";

/*
        llvm::errs() << "Source end:\n";
        Range.getEnd().dump(compilerInstance.getSourceManager());
        llvm::errs() << "\nSource end end\n";
*/
/*
        if(MacroNameTok.isLiteral()) {
            llvm::errs() << "Token literal: " << MacroNameTok.getLiteralData() << "\n";
        }
*/
    }



    /*** CallbacksProxy ***/

    CallbacksProxy::CallbacksProxy(IncludeDumper &original) : original(original) {
    }

    void CallbacksProxy::InclusionDirective(SourceLocation HashLoc, const Token &IncludeTok, StringRef FileName,
                                            bool IsAngled, CharSourceRange FilenameRange, const FileEntry *File,
                                            StringRef SearchPath, StringRef RelativePath, const Module *Imported, SrcMgr::CharacteristicKind FileType) {

        original.InclusionDirective(HashLoc, IncludeTok, FileName, IsAngled, FilenameRange, File, SearchPath, RelativePath, Imported, FileType);
    }

    void CallbacksProxy::MacroExpands(const Token & MacroNameTok, const MacroDefinition & MD, SourceRange Range, const MacroArgs * Args) {
        original.MacroExpands(MacroNameTok, MD, Range, Args);
    }

    void CallbacksProxy::PragmaDirective(SourceLocation Loc, PragmaIntroducerKind Introducer) {
        original.PragmaDirective(Loc, Introducer);
    }

    void CallbacksProxy::FileChanged(SourceLocation Loc, FileChangeReason Reason, SrcMgr::CharacteristicKind FileType, FileID PrevFID) {
        original.FileChanged(Loc, Reason, FileType, PrevFID);
    }



/**
 * DumpResources Implementations
 */

// File instantiations
std::ofstream DumpResources::includes;
std::ofstream DumpResources::nodetypes;
//std::ofstream DumpResources::template_args;
std::ofstream DumpResources::is_temporary;
std::ofstream DumpResources::omp;
std::ofstream DumpResources::enum_integer_type;
std::ofstream DumpResources::consumer_order;
std::ofstream DumpResources::types_with_templates;
int DumpResources::runId;
int DumpResources::systemHeaderThreshold;


void DumpResources::writeCounter(int id) {

    // Output is processed with a line iterator, allows multiple-line processing

    llvm::outs() << PREFIX << "\n";
    llvm::outs() << id << "\n";

    llvm::errs() << PREFIX << "\n";
    llvm::errs() << id << "\n";

}

void DumpResources::init(int runId, int systemLevelThreshold) {

    DumpResources::runId = runId;
    DumpResources::systemHeaderThreshold = systemLevelThreshold;

    // Clear files
#ifdef OLD_OUTPUT
    DumpResources::includes.open("includes.txt", std::ofstream::out | std::fstream::trunc);
    DumpResources::nodetypes.open("nodetypes.txt", std::ofstream::out | std::ofstream::trunc);
#endif


    //DumpResources::template_args.open("template_args.txt", std::ofstream::out | std::ofstream::trunc);
    DumpResources::is_temporary.open("is_temporary.txt", std::ofstream::out | std::ofstream::trunc);
    DumpResources::omp.open("omp.txt", std::ofstream::out | std::ofstream::trunc);
    DumpResources::enum_integer_type.open("enum_integer_type.txt", std::ofstream::out | std::ofstream::trunc);
    DumpResources::consumer_order.open("consumer_order.txt", std::ofstream::out | std::ofstream::trunc);
    DumpResources::types_with_templates.open("types_with_templates.txt", std::ofstream::out | std::ofstream::trunc);
}

void DumpResources::finish() {
#ifdef OLD_OUTPUT
    DumpResources::includes.close();
    DumpResources::nodetypes.close();
#endif


    //DumpResources::template_args.close();
    DumpResources::is_temporary.close();
    DumpResources::omp.close();
    DumpResources::enum_integer_type.close();
    DumpResources::consumer_order.close();
    DumpResources::types_with_templates.close();
}
