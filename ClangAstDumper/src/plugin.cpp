#include "clang/AST/AST.h"
#include "clang/AST/ASTConsumer.h"
#include "clang/AST/RecursiveASTVisitor.h"
#include "clang/Basic/SourceManager.h"
#include "clang/Frontend/CompilerInstance.h"
#include "clang/Frontend/FrontendPluginRegistry.h"

#include "Clang/ClangAst.h"

using namespace clang;

namespace {

// Define a visitor class that will traverse the AST and detect the function
// call
class MyASTVisitor : public RecursiveASTVisitor<MyASTVisitor> {
  public:
    explicit MyASTVisitor(ASTContext *context) : context_(context) {}

    bool VisitCallExpr(CallExpr *call) {
        if (FunctionDecl const *funcDecl = call->getDirectCallee(); funcDecl && funcDecl->getNameAsString() == "myFunction") {
            SourceManager const &SM = context_->getSourceManager();
            SourceLocation loc = call->getBeginLoc();
            llvm::outs() << "Found call to myFunction at "
                         << SM.getFilename(loc) << ":"
                         << SM.getSpellingLineNumber(loc) << "\n";
        }
        return true;
    }

  private:
    ASTContext *context_;
};

// Define an AST consumer that will create the AST visitor
class MyASTConsumer : public ASTConsumer {
  public:
    explicit MyASTConsumer(ASTContext *context) : visitor_(context) {}

    void HandleTranslationUnit(ASTContext &context) override {
        visitor_.TraverseDecl(context.getTranslationUnitDecl());
    }

  private:
    MyASTVisitor visitor_;
};

// Define a plugin that will create the AST consumer
class MyPlugin : public PluginASTAction {
  public:
    std::unique_ptr<ASTConsumer> CreateASTConsumer(CompilerInstance &CI,
                                                   StringRef file) override {
        return std::make_unique<MyASTConsumer>(&CI.getASTContext());
    }

    bool ParseArgs(const CompilerInstance &CI,
                   const std::vector<std::string> &args) override {
        // No arguments to parse
        return true;
    }

    PluginASTAction::ActionType getActionType() override {
        return AddBeforeMainAction;
    }
};

} // end anonymous namespace

// Register the plugin with Clang
static FrontendPluginRegistry::Add<DumpAstAction>
    X("myplugin", "Detect use of myFunction in input code");
