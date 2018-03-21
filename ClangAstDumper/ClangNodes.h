//
// Created by JoaoBispo on 21/03/2018.
//

#ifndef CLANGASTDUMPER_CLANGNODES_H
#define CLANGASTDUMPER_CLANGNODES_H

#include "clang/AST/Decl.h"
#include "clang/AST/Stmt.h"
#include "clang/AST/Type.h"

#include <string>

using namespace clang;

namespace clava {
    const std::string getClassName(const Decl* D);
    const std::string getClassName(const Stmt* S);
    const std::string getClassName(const Type* T);
}

#endif //CLANGASTDUMPER_CLANGNODES_H
