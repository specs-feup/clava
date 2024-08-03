#include <clang/Frontend/FrontendPluginRegistry.h>

#include "Clang/ClangAst.h"

// Register the plugin with Clang
const static FrontendPluginRegistry::Add<DumpAstAction>
    DumpAst("DumpAst", "Dumps the AST information to feed ClangStreamParserV2");
