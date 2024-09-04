//
// Created by JoaoBispo on 31/03/2018.
//

#include "../Clang/ClangNodes.h"
#include "ClangAstDumper.h"
#include "ClangAstDumperConstants.h"

#include "clang/AST/AST.h"

#include <iostream>
#include <sstream>

using namespace clang;

// EXPR

void ClangAstDumper::VisitExpr(const Expr *Node) {
  if (dumpStmt(Node)) {
    return;
  }

  bool isSystemHeader = clava::isSystemHeader(Node, Context);
  if (isSystemHeader) {
    currentSystemHeaderLevel++;
  }

  visitChildrenAndData(Node);

  if (isSystemHeader) {
    currentSystemHeaderLevel--;
  }
}
