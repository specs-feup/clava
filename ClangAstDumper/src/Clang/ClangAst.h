//
// Created by JoaoBispo on 01/05/2016.
//
// Based on public domain code by Eli Bendersky (eliben@gmail.com)
// http://eli.thegreenplace.net/
//

#ifndef CLANGASTDUMPER_CLANGAST_H
#define CLANGASTDUMPER_CLANGAST_H

#include <clang/AST/AST.h>
#include <clang/AST/RecursiveASTVisitor.h>
#include <clang/Frontend/FrontendActions.h>

#include <fcntl.h>
#include <pthread.h>
#include <sys/mman.h>
#include <unistd.h>

#include "../ClangAstDumper/ClangAstDumper.h"

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
 * Based on explanation from this website:
 * https://xaizek.github.io/2015-04-23/detecting-wrong-first-include/
 */
class DumpIncludesAction : public PreprocessOnlyAction {
  protected:
    virtual void ExecuteAction();
};

class IncludeDumper : public PPCallbacks {
  public:
    IncludeDumper(CompilerInstance &compilerInstance);

    std::unique_ptr<PPCallbacks> createPreprocessorCallbacks();

    virtual void InclusionDirective(
        SourceLocation HashLoc, const Token &IncludeTok, StringRef FileName,
        bool IsAngled, CharSourceRange FilenameRange, const FileEntry *File,
        StringRef SearchPath, StringRef RelativePath, const Module *Imported,
        SrcMgr::CharacteristicKind FileType);
    virtual void MacroExpands(const Token &MacroNameTok,
                              const MacroDefinition &MD, SourceRange Range,
                              const MacroArgs *Args);
    virtual void PragmaDirective(SourceLocation Loc,
                                 PragmaIntroducerKind Introducer);
    virtual void FileChanged(SourceLocation Loc, FileChangeReason Reason,
                             SrcMgr::CharacteristicKind FileType,
                             FileID PrevFID);

  private:
    const CompilerInstance &compilerInstance;
    const clang::SourceManager &sm;
};

struct SharedData {
    pthread_mutex_t mutex;
    int counter;
};

static const char *const SHM_NAME = "/clang_shared_counter";
static const size_t SHM_SIZE = sizeof(struct SharedData);

class SharedCounter {
  public:
    SharedCounter() {
      // Open shared memory object
      shm_fd = shm_open(SHM_NAME, O_CREAT | O_RDWR, 0666);
      if (shm_fd == -1) {
        perror("shm_open");
        exit(1);
      }

      // Set the size of the shared memory object
      if (ftruncate(shm_fd, SHM_SIZE) == -1) {
        perror("ftruncate");
        exit(1);
      }

      // Map shared memory object
      shm_ptr =
          mmap(0, SHM_SIZE, PROT_READ | PROT_WRITE, MAP_SHARED, shm_fd, 0);
      if (shm_ptr == MAP_FAILED) {
        perror("mmap");
        exit(1);
      }

      // Initialize shared memory
      data = (SharedData *)shm_ptr;

      // Initialize the mutex only if it's the first process creating it
      if (pthread_mutex_init(&data->mutex, nullptr) != 0 && errno != EBUSY) {
        perror("pthread_mutex_init");
        exit(1);
      }
    }

    ~SharedCounter() {
      // Unmap shared memory
      munmap(shm_ptr, SHM_SIZE);
      // Close shared memory object
      close(shm_fd);
    }

    int fetch_and_increment() {
      pthread_mutex_lock(&data->mutex);
      int ret = data->counter++;
      pthread_mutex_unlock(&data->mutex);
      return ret;
    }

  private:
    int shm_fd;
    void *shm_ptr;
    SharedData *data;
};

// For each source file provided to the tool, a new FrontendAction is created.
class DumpAstAction : public PluginASTAction {
  public:
    virtual std::unique_ptr<ASTConsumer>
    CreateASTConsumer(CompilerInstance &CI, StringRef file) override;

    bool ParseArgs(const CompilerInstance &CI,
                   const std::vector<std::string> &args) override {
        // No arguments to parse
        return true;
    }

    PluginASTAction::ActionType getActionType() override {
        return AddBeforeMainAction;
    }

    void dumpCompilerInstanceData(CompilerInstance &CI, StringRef file);

  private:
    SharedCounter Counter;
};

// By implementing RecursiveASTVisitor, we can specify which AST nodes
// we're interested in by overriding relevant methods.
class DumpAstVisitor : public RecursiveASTVisitor<DumpAstVisitor> {

  public:
    explicit DumpAstVisitor(ASTContext *Context, int id)
        : Context(Context), id(id) {}
    bool TraverseDecl(Decl *D);

  private:
    ASTContext *Context;
    int id;
};

class PrintNodesTypesRelationsVisitor
    : public RecursiveASTVisitor<PrintNodesTypesRelationsVisitor> {

  private:
    ASTContext *Context;
    int id;
    ClangAstDumper dumper;
    std::set<void *> seenNodes;

  public:
    explicit PrintNodesTypesRelationsVisitor(ASTContext *Context, int id,
                                             ClangAstDumper dumper);
    bool VisitCXXConstructExpr(CXXConstructExpr *D);
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
    void dumpNodeToType(std::ofstream &stream, void *nodeAddr,
                        const Type *typeAddr, bool checkDuplicates = true);

    void dumpNodeToType(std::ofstream &stream, void *nodeAddr,
                        const QualType &type, bool checkDuplicates = true);
};

// Implementation of the ASTConsumer interface for reading an AST produced by
// the Clang parser.
class MyASTConsumer : public ASTConsumer {

  private:
    int id;
    DumpAstVisitor topLevelDeclVisitor;
    PrintNodesTypesRelationsVisitor printRelationsVisitor;

  public:
    MyASTConsumer(ASTContext *C, int id, ClangAstDumper dumper);
    ~MyASTConsumer();

    bool HandleTopLevelDecl(DeclGroupRef DR) override;

    /// HandleTranslationUnit - This method is called when the ASTs for entire
    /// translation unit have been parsed.
    void HandleTranslationUnit(ASTContext &Ctx) override;
};
#endif // CLANGASTDUMPER_CLANGAST_H
