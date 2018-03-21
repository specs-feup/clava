//
// Created by JoaoBispo on 21/03/2018.
//

#include "ClangNodes.h"


const std::string clava::getClassName(const Decl* D) {
    const std::string kindName = D->getDeclKindName();
    return kindName + "Decl";
}

const std::string clava::getClassName(const Stmt* S) {
    const std::string className = S->getStmtClassName();
    return className;
}

const std::string clava::getClassName(const Type* T) {
    const std::string kindName = T->getTypeClassName();
    return kindName + "Type";
}