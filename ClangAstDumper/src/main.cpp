//
// Created by JoaoBispo on 01/05/2016.
//

#include "ClangAst.h"

#include <clang/Tooling/CommonOptionsParser.h>
#include <clang/Tooling/Tooling.h>

#include <iostream>


using namespace clang::tooling;

static llvm::cl::OptionCategory MyToolCategory("my-tool options");
static llvm::cl::opt<int> UserIdOption("id", llvm::cl::cat(MyToolCategory));
static llvm::cl::opt<int> UserSystemHeaderThresholdOption("system-header-threshold", llvm::cl::cat(MyToolCategory));

int main(int argc, const char *argv[])
{


    // Errs is the main way we dump information, we tested if making it buffered improved performance
    // but could not detect a significant difference
    //llvm::errs().SetBuffered();
    auto OptionsParser = CommonOptionsParser::create(argc, argv, MyToolCategory);
    if (auto E = OptionsParser.takeError()) {
        // We must consume the error. Typically one of:
        // - return the error to our caller
        // - toString(), when logging
        // - consumeError(), to silently swallow the error
        // - handleErrors(), to distinguish error types
        llvm::errs() << "Problem while creating options parser: " << toString(std::move(E)) << "\n";
        return -1;
    }

    //CommonOptionsParser OptionsParser = *parserResult;
    ClangTool Tool((*OptionsParser).getCompilations(),
                   (*OptionsParser).getSourcePathList());


    /*
    for(auto source : OptionsParser.getSourcePathList()) {
        llvm::errs() << "SOURCE:" << source << "\n";
    }
     */

    // Making it static/global because I do not know how to create actions with arbitrary arguments using newFrontendActionFactory
    DumpResources::init(UserIdOption.getValue(), UserSystemHeaderThresholdOption.getValue());

    int includesReturnCode = Tool.run(newFrontendActionFactory<DumpIncludesAction>().get());

    int dumpReturnCode = Tool.run(newFrontendActionFactory<DumpAstAction>().get());

    DumpResources::finish();


    int returnCode = 0;

    if(includesReturnCode != 0) {
        std::cout << "Error while running IncludeFinderAction (return code "<< includesReturnCode <<")";
        returnCode = 1;
    }

    if(dumpReturnCode != 0) {
        std::cout << "Error while running DumpAstAction (return code "<< dumpReturnCode <<")";
        returnCode = 1;
    }

    //std::cout << "llvm:errs() buffer size: " << llvm::errs().GetBufferSize() << "\n";


    return returnCode;
}