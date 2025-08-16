//
// Created by JoaoBispo on 20/01/2017.
//

#include "../Clang/ClangNodes.h"
#include "ClangAstDumper.h"
#include "ClangAstDumperConstants.h"

#include "clang/AST/AST.h"

using namespace clang;

// #define OLD_OUTPUT

void ClangAstDumper::visitChildrenAndData(const Type *T) {
  // Visit children
  visitChildren(T);

  // Dump data
  dataDumper.dump(T);

  // Dump id
  dumpIdToClassMap(T, clava::getClassName(T));
}

/*
 * TYPES
 */

bool ClangAstDumper::dumpType(const Type *typeAddr) {
  if (typeAddr == nullptr) {
    return true;
  }

  if (seenTypes.count(typeAddr) != 0) {
    return true;
  }

  log(typeAddr);

  // Dump type if it has not appeared yet
  // A TypeDumper is created for each context,
  // no need to use id to disambiguate
  seenTypes.insert((void *)typeAddr);

  return false;
}

bool ClangAstDumper::dumpType(const QualType &type) {
  // QUALTYPE EXP
  void *typeAddr = type.getAsOpaquePtr();

  if (seenTypes.count(typeAddr) != 0) {
    return true;
  }

  log("QualType", typeAddr);

  // Dump type if it has not appeared yet
  // A TypeDumper is created for each context,
  // no need to use id to disambiguate
  seenTypes.insert((void *)typeAddr);

#ifdef OLD_OUTPUT
  llvm::errs() << "TYPE_BEGIN\n";
  type.dump();
  llvm::errs() << "TYPE_END\n";
#endif

  return false;
}

/**
 * Generic method.
 *
 * @param T
 */
void ClangAstDumper::VisitType(const Type *T) {
  if (dumpType(T)) {
    return;
  }

  visitChildrenAndData(T);
}
