#include <clang/Tooling/CommonOptionsParser.h>
#include <clang/Tooling/Tooling.h>

#include "Clang/ClangAst.h"

int main(int argc, const char *argv[]) {

  // Errs is the main way we dump information, we tested if making it buffered
  // improved performance but could not detect a significant difference
  // llvm::errs().SetBuffered();

  static llvm::cl::OptionCategory MyToolCategory("my-tool options");
  static llvm::cl::opt<int> UserIdOption("id", llvm::cl::cat(MyToolCategory));
  static llvm::cl::opt<int> UserSystemHeaderThresholdOption(
      "system-header-threshold", llvm::cl::cat(MyToolCategory));

  auto OptionsParser =
      clang::tooling::CommonOptionsParser::create(argc, argv, MyToolCategory);

  if (auto E = OptionsParser.takeError()) {
    llvm::errs() << "Problem with division " << llvm::toString(std::move(E))
                 << "\n";
    return 1;
  }

  clang::tooling::ClangTool Tool((*OptionsParser).getCompilations(),
                                 (*OptionsParser).getSourcePathList());

  DumpResources::init(UserIdOption.getValue(),
                      UserSystemHeaderThresholdOption.getValue());

  int returnValue =
      Tool.run(clang::tooling::newFrontendActionFactory<DumpAstAction>().get());

  DumpResources::finish();

  return returnValue;
}