//------------------------------------------------------------------------------
//
// Created by JoaoBispo
//
// Based on public domain code by Eli Bendersky (eliben@gmail.com) -
// http://eli.thegreenplace.net/
//------------------------------------------------------------------------------
#include "ClangAst.h"
#include "../ClangAstDumper/ClangAstDumperConstants.h"
#include "ClangNodes.h"

#include <clang/AST/AST.h>
#include <clang/AST/ASTConsumer.h>
#include <clang/AST/Comment.h>
#include <clang/AST/RecursiveASTVisitor.h>
#include <clang/AST/Stmt.h>
#include <clang/Basic/TargetInfo.h>
#include <clang/Frontend/ASTConsumers.h>
#include <clang/Frontend/CompilerInstance.h>
#include <clang/Frontend/FrontendActions.h>
#include <clang/Lex/Lexer.h>
#include <clang/Lex/Preprocessor.h>

#include <fstream>

using namespace clang;

static llvm::cl::OptionCategory ToolingSampleCategory("Tooling Sample");

static constexpr const char *const PREFIX = "COUNTER";

/* DumpAstVisitor implementation */

bool DumpAstVisitor::TraverseDecl(Decl *D) {
    FullSourceLoc fullLocation = Context->getFullLoc(D->getBeginLoc());
    if (fullLocation.isValid() && !fullLocation.isInSystemHeader()) {

        // Top-level Node
        llvm::errs() << TOP_LEVEL_NODES << "\n";
        llvm::errs() << D << "_" << id << "\n";
    }

    return false;
}

PrintNodesTypesRelationsVisitor::PrintNodesTypesRelationsVisitor(
    ASTContext *Context, int id, ClangAstDumper dumper)
    : Context(Context), id(id), dumper(dumper){};

static std::string stmt2str(clang::Stmt *d, clang::SourceManager *sm,
                            clang::LangOptions lopt) {
    clang::SourceLocation b(d->getBeginLoc()), _e(d->getEndLoc());
    clang::SourceLocation e(
        clang::Lexer::getLocForEndOfToken(_e, 0, *sm, lopt));
    return std::string(sm->getCharacterData(b),
                       sm->getCharacterData(e) - sm->getCharacterData(b));
}

static std::string loc2str(SourceLocation locStart, SourceLocation locEnd,
                           ASTContext *Context) {
    clang::SourceManager *sm = &Context->getSourceManager();
    clang::LangOptions lopt = Context->getLangOpts();

    clang::SourceLocation b(locStart), _e(locEnd);
    clang::SourceLocation e(
        clang::Lexer::getLocForEndOfToken(_e, 0, *sm, lopt));
    return std::string(sm->getCharacterData(b),
                       sm->getCharacterData(e) - sm->getCharacterData(b));
}

bool PrintNodesTypesRelationsVisitor::VisitCXXConstructExpr(
    CXXConstructExpr *D) {

    FullSourceLoc fullLocation = Context->getFullLoc(D->getBeginLoc());
    if (fullLocation.isValid() && !fullLocation.isInSystemHeader()) {

        // Check if temporary object
        if (D->isTemporaryObject(D->getConstructor()->getASTContext(),
                                 D->getConstructor()->getParent())) {
            DumpResources::is_temporary << D << "_" << id << "\n";
        }
    }

    return true;
}

// Dump types for Expr, TypeDecl and ValueDecl, as well as the connection
// between them
bool PrintNodesTypesRelationsVisitor::VisitExpr(Expr *D) { return true; }

bool PrintNodesTypesRelationsVisitor::VisitTypeDecl(TypeDecl *D) {
    return true;
}

/**
 * Typedefs will be visited by 'VisitTypeDecl' but will null, this override
 * extracts the correct information for typedefs
 * @param D
 * @return
 */
bool PrintNodesTypesRelationsVisitor::VisitTypedefNameDecl(TypedefNameDecl *D) {
    return true;
}

bool PrintNodesTypesRelationsVisitor::VisitEnumDecl(EnumDecl *D) {
    FullSourceLoc fullLocation = Context->getFullLoc(D->getBeginLoc());
    if (fullLocation.isValid() && !fullLocation.isInSystemHeader()) {
        dumpNodeToType(DumpResources::enum_integer_type, D, D->getIntegerType(),
                       false);
    }

    return true;
}

bool PrintNodesTypesRelationsVisitor::VisitValueDecl(ValueDecl *D) {
    return true;
}

// Visit only nodes from the source code, ignore system headers
bool PrintNodesTypesRelationsVisitor::VisitDecl(Decl *D) {
    FullSourceLoc fullLocation = Context->getFullLoc(D->getBeginLoc());

    if (fullLocation.isValid() && !fullLocation.isInSystemHeader()) {
        dumper.VisitDeclTop(D);
        return true;
    }

    return true;
}

bool PrintNodesTypesRelationsVisitor::VisitStmt(Stmt *D) {
    FullSourceLoc fullLocation = Context->getFullLoc(D->getBeginLoc());

    if (fullLocation.isValid() && !fullLocation.isInSystemHeader()) {
        dumper.VisitStmtTop(D);
        return true;
    }

    return true;
}

bool PrintNodesTypesRelationsVisitor::VisitLambdaExpr(LambdaExpr *D) {
    return true;
}

void PrintNodesTypesRelationsVisitor::dumpNodeToType(std::ofstream &stream,
                                                     void *nodeAddr,
                                                     const QualType &type,
                                                     bool checkDuplicates) {

    // Opaque pointer, so that we can obtain the qualifiers
    void *typeAddr = !type.isNull() ? type.getAsOpaquePtr() : nullptr;

    if (checkDuplicates) {
        if (seenNodes.count(nodeAddr) == 0) {
            stream << nodeAddr << "_" << id << "->" << typeAddr << "_" << id
                   << "\n";
            seenNodes.insert(nodeAddr);
        }
    } else {
        stream << nodeAddr << "_" << id << "->" << typeAddr << "_" << id
               << "\n";
    }

    dumper.VisitTypeTop(type);
}

void PrintNodesTypesRelationsVisitor::dumpNodeToType(std::ofstream &stream,
                                                     void *nodeAddr,
                                                     const Type *typeAddr,
                                                     bool checkDuplicates) {

    if (!typeAddr) {
        return;
    }

    if (checkDuplicates) {
        if (seenNodes.count(nodeAddr) == 0) {
            stream << nodeAddr << "_" << id << "->" << typeAddr << "_" << id
                   << "\n";
            seenNodes.insert(nodeAddr);
        }
    } else {
        stream << nodeAddr << "_" << id << "->" << typeAddr << "_" << id
               << "\n";
    }

    dumper.VisitTypeTop(typeAddr);
}

MyASTConsumer::MyASTConsumer(ASTContext *C, int id, ClangAstDumper dumper)
    : id(id), topLevelDeclVisitor(C, id), printRelationsVisitor(C, id, dumper) {

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

void MyASTConsumer::HandleTranslationUnit(ASTContext &Ctx) {}

// For each source file provided to the tool, a new FrontendAction is created.
std::unique_ptr<ASTConsumer>
DumpAstAction::CreateASTConsumer(CompilerInstance &CI, StringRef file) {
    int counter = DumpResources::runId;
    
    // If runId is 0 (default value), use the global counter instead
    if (counter == 0) {
        counter = Counter.fetch_and_increment();
    }

    dumpCompilerInstanceData(CI, file);

    // Dump id->file data
    llvm::errs() << ID_FILE_MAP << "\n";
    llvm::errs() << counter << "\n";
    llvm::errs() << file << "\n";

    ASTContext *Context = &CI.getASTContext();

    ClangAstDumper dumper(Context, counter,
                          DumpResources::systemHeaderThreshold);

    return std::make_unique<MyASTConsumer>(Context, counter, dumper);
}

void DumpAstAction::ExecuteAction() {
    CompilerInstance &CI = getCompilerInstance();
    CI.getPreprocessor().addPPCallbacks(std::make_unique<IncludeDumper>(CI));

    PluginASTAction::ExecuteAction();
}

void DumpAstAction::dumpCompilerInstanceData(CompilerInstance &CI,
                                             StringRef file) {
    clava::dump(COMPILER_INSTANCE_DATA);

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

/*** IncludeDumper ***/

IncludeDumper::IncludeDumper(CompilerInstance &compilerInstance)
    : compilerInstance(compilerInstance),
      sm(compilerInstance.getSourceManager()){};

void IncludeDumper::InclusionDirective(
    SourceLocation HashLoc, const Token &IncludeTok, StringRef FileName,
    bool IsAngled, CharSourceRange FilenameRange, const FileEntry *File,
    StringRef SearchPath, StringRef RelativePath, const Module *Imported,
    SrcMgr::CharacteristicKind FileType) {

    if (!sm.isInSystemHeader(HashLoc)) {
        // Includes information in stream
        llvm::errs() << INCLUDES << "\n";
        // Source
        llvm::errs() << sm.getFilename(HashLoc).str() << "\n";
        llvm::errs() << FileName.str() << "\n";
        llvm::errs() << sm.getSpellingLineNumber(HashLoc) << "\n";
        llvm::errs() << IsAngled << "\n";
    }
}

void IncludeDumper::PragmaDirective(SourceLocation Loc,
                                    PragmaIntroducerKind Introducer) {

    // Ignore system headers
    if (sm.isInSystemHeader(Loc)) {
        return;
    }

    // Pragma location
    clava::dump(PRAGMA);
    clava::dump(sm.getFilename(Loc));
    clava::dump(sm.getSpellingLineNumber(Loc));
    clava::dump(sm.getSpellingColumnNumber(Loc));
}

void IncludeDumper::FileChanged(SourceLocation Loc, FileChangeReason Reason,
                                SrcMgr::CharacteristicKind FileType,
                                FileID PrevFID) {}

void IncludeDumper::MacroExpands(const Token &MacroNameTok,
                                 const MacroDefinition &MD, SourceRange Range,
                                 const MacroArgs *Args) {}


/**
 * DumpResources Implementations
 */

// File instantiations
std::ofstream DumpResources::includes;
std::ofstream DumpResources::nodetypes;
std::ofstream DumpResources::is_temporary;
std::ofstream DumpResources::omp;
std::ofstream DumpResources::enum_integer_type;
std::ofstream DumpResources::consumer_order;
std::ofstream DumpResources::types_with_templates;
int DumpResources::runId;
int DumpResources::systemHeaderThreshold;

void DumpResources::setRunId(int runId) {
    DumpResources::runId = runId;
}

void DumpResources::setSystemHeaderThreshold(int systemHeaderThreshold) {
    DumpResources::systemHeaderThreshold = systemHeaderThreshold;
}

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

    DumpResources::is_temporary.open("is_temporary.txt",
                                     std::ofstream::out | std::ofstream::trunc);
    DumpResources::omp.open("omp.txt",
                            std::ofstream::out | std::ofstream::trunc);
    DumpResources::enum_integer_type.open(
        "enum_integer_type.txt", std::ofstream::out | std::ofstream::trunc);
    DumpResources::consumer_order.open(
        "consumer_order.txt", std::ofstream::out | std::ofstream::trunc);
    DumpResources::types_with_templates.open(
        "types_with_templates.txt", std::ofstream::out | std::ofstream::trunc);
}

void DumpResources::finish() {
    DumpResources::is_temporary.close();
    DumpResources::omp.close();
    DumpResources::enum_integer_type.close();
    DumpResources::consumer_order.close();
    DumpResources::types_with_templates.close();
}
