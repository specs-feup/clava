//
// Created by JoaoBispo on 20/01/2017.
//

#include "../Clang/ClangNodes.h"
#include "ClangAstDumper.h"
#include "ClangAstDumperConstants.h"

#include "clang/AST/AST.h"

#include <iostream>
#include <sstream>

using namespace clang;

// #define OLD_OUTPUT

void ClangAstDumper::visitChildrenAndData(const Stmt *S) {
  // Visit children
  visitChildren(S);

  // Dump data
  dataDumper.dump(S);

  // Dump id
  dumpIdToClassMap(S, clava::getClassName(S));
}

void ClangAstDumper::visitChildrenAndData(const Expr *E) {
  // Visit children
  visitChildren(E);

  // Dump data
  dataDumper.dump(E);

  // Dump id
  dumpIdToClassMap(E, clava::getClassName(E));
}

/*
 * STMTS
 */

bool ClangAstDumper::dumpStmt(const Stmt *stmtAddr) {

  if (stmtAddr == nullptr) {
    return true;
  }

  if (seenStmts.count(stmtAddr) != 0) {
    return true;
  }

  log(stmtAddr);

  // A StmtDumper is created for each context,
  // no need to use id to disambiguate
  seenStmts.insert(stmtAddr);

  std::ostringstream extendedId;
  extendedId << stmtAddr << "_" << id;

#ifdef OLD_OUTPUT
  // Dump location
  dumpSourceRange(extendedId.str(), stmtAddr->getBeginLoc(),
                  stmtAddr->getEndLoc());
#endif

  return false;
}

void ClangAstDumper::VisitStmt(const Stmt *Node) {
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
