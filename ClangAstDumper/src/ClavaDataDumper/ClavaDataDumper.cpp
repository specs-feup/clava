//
// Created by JoaoBispo on 18/03/2018.
//

#include "ClavaDataDumper.h"

#include "../Clang/ClangNodes.h"
#include "../Clava/ClavaConstants.h"

#include <sstream>

clava::ClavaDataDumper::ClavaDataDumper(ASTContext *Context, int id)
    : Context(Context), id(id){};

const std::string clava::ClavaDataDumper::getDataName(std::string nodeName) {
    return "<" + nodeName + "Data>";
}

const std::string clava::ClavaDataDumper::getDataName(DeclNode node) {
    return getDataName(clava::getName(node));
}
const std::string clava::ClavaDataDumper::getDataName(StmtNode node) {
    return getDataName(clava::getName(node));
}

const std::string clava::ClavaDataDumper::getDataName(TypeNode node) {
    return getDataName(clava::getName(node));
}

const std::string clava::ClavaDataDumper::getDataName(AttrNode node) {
    return getDataName(clava::getName(node));
}
