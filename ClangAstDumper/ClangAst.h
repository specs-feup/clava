//
// Created by JoaoBispo on 01/05/2016.
//
// Based on public domain code by Eli Bendersky (eliben@gmail.com)
// http://eli.thegreenplace.net/
//

#ifndef CLANGASTDUMPER_CLANGAST_H
#define CLANGASTDUMPER_CLANGAST_H

#include "ClangAstDumper.h"


#include "clang/AST/AST.h"
#include "clang/Frontend/FrontendActions.h"
#include "clang/AST/RecursiveASTVisitor.h"

#include <fstream>


using namespace clang;

// Class for managing ClavaDump resources
class DumpResources {

public:
    static void init(int runId, int systemLevelThreshold);
    static void finish();

    static void writeCounter(int set);


    static std::ofstream includes;
    static std::ofstream nodetypes;
    //static std::ofstream template_args;
    static std::ofstream is_temporary;
    static std::ofstream omp;
    static std::ofstream enum_integer_type;
    static std::ofstream consumer_order;
    static std::ofstream types_with_templates;
    static int runId;
    static int systemHeaderThreshold;

private:

};

/**
 * Dumps includes to a file.
 *
 * Based on explanation from this website: https://xaizek.github.io/2015-04-23/detecting-wrong-first-include/
 */
class DumpIncludesAction : public PreprocessOnlyAction {
protected:
    virtual void ExecuteAction();
};

class IncludeDumper : public PPCallbacks {
public:
    IncludeDumper(CompilerInstance &compilerInstance);

    std::unique_ptr<PPCallbacks> createPreprocessorCallbacks();

    virtual void InclusionDirective(SourceLocation HashLoc, const Token &IncludeTok, StringRef FileName, bool IsAngled, CharSourceRange FilenameRange, const FileEntry *File, StringRef SearchPath, StringRef RelativePath, const Module *Imported, SrcMgr::CharacteristicKind FileType);
    virtual void MacroExpands(const Token & MacroNameTok, const MacroDefinition & MD, SourceRange Range, const MacroArgs * Args);
private:

    const CompilerInstance &compilerInstance;

};

class CallbacksProxy : public PPCallbacks {
public:
    CallbacksProxy(IncludeDumper &original);

    virtual void InclusionDirective(SourceLocation HashLoc, const Token &IncludeTok, StringRef FileName, bool IsAngled, CharSourceRange FilenameRange, const FileEntry *File, StringRef SearchPath, StringRef RelativePath, const Module *Imported, SrcMgr::CharacteristicKind FileType);
    virtual void MacroExpands(const Token & MacroNameTok, const MacroDefinition & MD, SourceRange Range, const MacroArgs * Args);

private:

    IncludeDumper original;
};

// For each source file provided to the tool, a new FrontendAction is created.
class DumpAstAction : public ASTFrontendAction {
public:

    virtual std::unique_ptr<ASTConsumer> CreateASTConsumer(CompilerInstance &CI, StringRef file);

    void dumpCompilerInstanceData(CompilerInstance &CI, StringRef file);
};





// By implementing RecursiveASTVisitor, we can specify which AST nodes
// we're interested in by overriding relevant methods.
class DumpAstVisitor : public RecursiveASTVisitor<DumpAstVisitor> {

public:
    //explicit DumpAstVisitor(ASTContext *Context, int id, ClangAstDumper *dumper) : Context(Context) , id(id), dumper(dumper) {}
    explicit DumpAstVisitor(ASTContext *Context, int id) : Context(Context) , id(id) {}
    bool TraverseDecl(Decl *D);

private:
    ASTContext *Context;
    int id;
    //ClangAstDumper dumper;
    //ClangAstDumper *dumper;
};


class PrintNodesTypesRelationsVisitor : public RecursiveASTVisitor<PrintNodesTypesRelationsVisitor> {

private:
    ASTContext *Context;
    int id;
    ClangAstDumper dumper;
    //ClangAstDumper *dumper;
    std::set<void *> seenNodes;

public:
    //explicit PrintNodesTypesRelationsVisitor(ASTContext *Context, int id, ClangAstDumper *dumper);
    explicit PrintNodesTypesRelationsVisitor(ASTContext *Context, int id, ClangAstDumper dumper);
    //bool TraverseDecl(Decl *D);

    bool VisitOMPExecutableDirective(OMPExecutableDirective * D);
    //bool VisitDeclRefExpr(DeclRefExpr * D);
    bool VisitCXXConstructExpr(CXXConstructExpr * D);
    bool VisitExpr(Expr *D);
    bool VisitLambdaExpr(LambdaExpr *D);
    bool VisitTypeDecl(TypeDecl *D);
    bool VisitTypedefNameDecl(TypedefNameDecl *D);
    bool VisitEnumDecl(EnumDecl *D);
    bool VisitValueDecl(ValueDecl *D);
    bool VisitDecl(Decl *D);
    bool VisitStmt(Stmt *D);


    /**
     * Dumps the information regaring a node and the corresponding type.
     *
     * @param nodeAddr
     * @param typeAddr
     * @param id
     * @param seenTypes
     */
    void dumpNodeToType(std::ofstream &stream, void* nodeAddr, const Type* typeAddr, bool checkDuplicates = true);

    void dumpNodeToType(std::ofstream &stream, void* nodeAddr, const QualType& type, bool checkDuplicates = true);

    //ClangAstDumper getDumper();

};


// Implementation of the ASTConsumer interface for reading an AST produced by the Clang parser.
class MyASTConsumer : public ASTConsumer {

private:

    int id;
    //ClangAstDumper dumper;
    DumpAstVisitor topLevelDeclVisitor;
    PrintNodesTypesRelationsVisitor printRelationsVisitor;

    public:
    MyASTConsumer(ASTContext *C, int id, ClangAstDumper dumper);
    //MyASTConsumer(ASTContext *C, int id, ClangAstDumper *dumper);
    ~MyASTConsumer();


    bool HandleTopLevelDecl(DeclGroupRef DR) override;

    /// HandleTranslationUnit - This method is called when the ASTs for entire
    /// translation unit have been parsed.
    void HandleTranslationUnit(ASTContext &Ctx) override;
};
#endif // CLANGASTDUMPER_CLANGAST_H
