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

int main(int argc, const char *argv[])
{


    CommonOptionsParser OptionsParser(argc, argv, MyToolCategory);
    ClangTool Tool(OptionsParser.getCompilations(),
                   OptionsParser.getSourcePathList());

    // Making it static/global because I do not know how to create actions with arbitrary arguments using newFrontendActionFactory
    DumpResources::init();

    int returnCode = Tool.run(newFrontendActionFactory<DumpIncludesAction>().get());

    if(returnCode != 0) {
        std::cout << "Error while running IncludeFinderAction (return code "<< returnCode <<")";
        DumpResources::finish();
        return returnCode;
    }

    returnCode = Tool.run(newFrontendActionFactory<DumpAstAction>().get());

    DumpResources::finish();

    return returnCode;
}