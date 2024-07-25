#include <clang/Frontend/FrontendPluginRegistry.h>

#include "Clang/ClangAst.h"

class Plugin : public DumpAstAction, public PluginASTAction {
public:
  Plugin() { DumpResources::init(0, 0); }
  ~Plugin() override { DumpResources::finish(); }

  virtual std::unique_ptr<ASTConsumer> CreateASTConsumer(CompilerInstance &CI,
                                                         StringRef file);

  bool ParseArgs(const CompilerInstance &CI,
                 const std::vector<std::string> &args) override {
    for (const auto &Arg : args) {
      if (Arg.find("-file-id=") == 0) {
        DumpResources::setRunId(std::stoi(Arg.substr(strlen("-file-id="))));
      } else if (Arg.find("-system-threshold=") == 0) {
        DumpResources::setSystemHeaderThreshold(
            std::stoi(Arg.substr(strlen("-system-threshold="))));
      }
    }

    return true; // Return true even if the argument is not found to
                 // continue execution
  }

  PluginASTAction::ActionType getActionType() override { return ReplaceAction; }
};

// Register the plugin with Clang
const static FrontendPluginRegistry::Add<Plugin>
    DumpAst("DumpAst", "Dumps the AST information to feed ClangStreamParserV2");
