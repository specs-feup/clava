//
// Created by JoaoBispo on 20/01/2017.
//

#include "ClangAstDumper.h"
#include "../Clang/ClangNodes.h"
#include "../ClangEnums/ClangEnums.h"
#include "ClangAstDumperConstants.h"

#include "clang/Lex/Lexer.h"

#include "clang/Basic/SourceManager.h"

#include <iostream>
#include <sstream>

#include <assert.h>

// #define DEBUG

// #define VISIT_CHECK

using namespace clang;

ClangAstDumper::ClangAstDumper(ASTContext *Context, int id,
                               int systemHeaderThreshold)
    : Context(Context), id(id), systemHeaderThreshold(systemHeaderThreshold),
      dataDumper(Context, id){};

// This method is equivalent to a VisitQualType() in ClangAstDumperTypes.cpp
void ClangAstDumper::VisitTypeTop(const QualType &T) {

  if (T.isNull()) {
    return;
  }

  // Check if QualType is the same as the underlying type
  if ((void *)T.getTypePtr() == T.getAsOpaquePtr()) {
#ifdef VISIT_CHECK
    clava::dump(TOP_VISIT_START);
    clava::dump(clava::getId(T.getTypePtr(), id));
#endif

    // TODO: AST dump method relies on visiting the nodes multiple times
    // For now, detect it to avoid visiting children more than once
    if (seenTypes.count(T.getTypePtr()) == 0) {
      TypeVisitor::Visit(T.getTypePtr());
    }

    dumpType(T.getTypePtr());
#ifdef VISIT_CHECK
    clava::dump(TOP_VISIT_END);
    clava::dump(clava::getId(T.getTypePtr(), id));
#endif
    return;
  }

  if (dumpType(T)) {
    return;
  }

#ifdef VISIT_CHECK
  clava::dump(TOP_VISIT_START);
  clava::dump(clava::getId(T, id));
#endif

  visitChildren(T);
  dataDumper.dump(T);
  dumpIdToClassMap(T.getAsOpaquePtr(), "QualType");

#ifdef VISIT_CHECK
  clava::dump(TOP_VISIT_END);
  clava::dump(clava::getId(T, id));
#endif
}

void ClangAstDumper::VisitTypeTop(const Type *T) {
  if (T == nullptr) {
    return;
  }

#ifdef VISIT_CHECK
  clava::dump(TOP_VISIT_START);
  clava::dump(clava::getId(T, id));
#endif

  TypeVisitor::Visit(T);

#ifdef VISIT_CHECK
  clava::dump(TOP_VISIT_END);
  clava::dump(clava::getId(T, id));
#endif
}

void ClangAstDumper::VisitStmtTop(const Stmt *Node) {
  if (Node == nullptr) {
    return;
  }

#ifdef VISIT_CHECK
  clava::dump(TOP_VISIT_START);
  clava::dump(clava::getId(Node, id));
#endif

  ConstStmtVisitor::Visit(Node);

#ifdef VISIT_CHECK
  clava::dump(TOP_VISIT_END);
  clava::dump(clava::getId(Node, id));
#endif
}

void ClangAstDumper::VisitDeclTop(const Decl *Node) {
  if (Node == nullptr) {
    return;
  }

#ifdef VISIT_CHECK
  clava::dump(TOP_VISIT_START);
  clava::dump(clava::getId(Node, id));
#endif

  ConstDeclVisitor::Visit(Node);

#ifdef VISIT_CHECK
  clava::dump(TOP_VISIT_END);
  clava::dump(clava::getId(Node, id));
#endif
}

void ClangAstDumper::VisitAttrTop(const Attr *Node) {
  if (Node == nullptr) {
    return;
  }

#ifdef VISIT_CHECK
  clava::dump(TOP_VISIT_START);
  clava::dump(clava::getId(Node, id));
#endif

  VisitAttr(Node);

#ifdef VISIT_CHECK
  clava::dump(TOP_VISIT_END);
  clava::dump(clava::getId(Node, id));
#endif
}

void ClangAstDumper::log(std::string name, const void *addr) {
#ifdef DEBUG
  llvm::errs() << name << " " << addr << "\n";
#endif
}

void ClangAstDumper::log(const Decl *D) { log(clava::getClassName(D), D); }

void ClangAstDumper::log(const Stmt *S) { log(clava::getClassName(S), S); }

void ClangAstDumper::log(const Type *T) { log(clava::getClassName(T), T); }

void ClangAstDumper::log(const Attr *A) { log(clava::getClassName(A), A); }

std::string ClangAstDumper::loc2str(SourceLocation locStart,
                                    SourceLocation locEnd) {

  clang::SourceManager *sm = &Context->getSourceManager();
  clang::LangOptions lopt = Context->getLangOpts();

  clang::SourceLocation b(locStart), _e(locEnd);
  clang::SourceLocation e(clang::Lexer::getLocForEndOfToken(_e, 0, *sm, lopt));

  std::string bChars(sm->getCharacterData(b));
  std::string eChars(sm->getCharacterData(e));

  if (bChars == "<<<<INVALID BUFFER>>>>") {
    return "";
  }

  if (eChars == "<<<<INVALID BUFFER>>>>") {
    return "";
  }

  return std::string(sm->getCharacterData(b),
                     sm->getCharacterData(e) - sm->getCharacterData(b));
}

void ClangAstDumper::dumpSourceRange(std::string id, SourceLocation startLoc,
                                     SourceLocation endLoc) {
  llvm::errs() << "<SourceRange Dump>\n";

  llvm::errs() << id << "\n";
  // All components of the source range will be dumped

  const SourceManager &SM = Context->getSourceManager();

  SourceLocation startSpellingLoc = SM.getSpellingLoc(startLoc);
  PresumedLoc startPLoc = SM.getPresumedLoc(startSpellingLoc);

  if (startPLoc.isInvalid()) {
    llvm::errs() << "<invalid>\n";
    return;
  }

  // Dump start location
  llvm::errs() << startPLoc.getFilename() << "\n";
  llvm::errs() << startPLoc.getLine() << "\n";
  llvm::errs() << startPLoc.getColumn() << "\n";

  if (startLoc == endLoc) {
    llvm::errs() << "<end>\n";
    return;
  }

  SourceLocation endSpellingLoc = SM.getSpellingLoc(endLoc);
  PresumedLoc endPLoc = SM.getPresumedLoc(endSpellingLoc);

  if (endPLoc.isInvalid()) {
    llvm::errs() << "<end>\n";
    return;
  }

  const char *endFilename = endPLoc.getFilename();
  if (!endFilename) {
    endFilename = startPLoc.getFilename();
  }

  unsigned int endLine = endPLoc.getLine();
  if (!endLine) {
    endLine = startPLoc.getLine();
  }

  unsigned int endCol = endPLoc.getColumn();
  if (!endCol) {
    endCol = startPLoc.getColumn();
  }

  // Dump end location
  llvm::errs() << endFilename << "\n";
  llvm::errs() << endLine << "\n";
  llvm::errs() << endCol << "\n";
}

std::string ClangAstDumper::toBoolString(int value) {
  return value ? "true" : "false";
}

const Type *getTypePtr(QualType T, std::string source) {
  assert(!T.isNull() && "Cannot retrieve a NULL type pointer");

  return T.getTypePtr();
}

void ClangAstDumper::dumpVisitedChildren(const void *pointer,
                                         std::vector<std::string> children) {
  llvm::errs() << VISITED_CHILDREN << "\n";
  // If node has children, pointer will not be null
  llvm::errs() << clava::getId(pointer, id) << "\n";
  llvm::errs() << children.size() << "\n";

  for (auto child : children) {
    llvm::errs() << child << "\n";
  }
}

void ClangAstDumper::dumpIdToClassMap(const void *pointer,
                                      std::string className) {
  llvm::errs() << ID_TO_CLASS_MAP << "\n";
  llvm::errs() << clava::getId(pointer, id) << "\n";
  llvm::errs() << className << "\n";
}

void ClangAstDumper::dumpTopLevelType(const QualType &type) {
  llvm::errs() << TOP_LEVEL_TYPES << "\n";
  clava::dump(type, id);
}

void ClangAstDumper::dumpTopLevelAttr(const Attr *attr) {
  llvm::errs() << TOP_LEVEL_ATTRIBUTES << "\n";
  llvm::errs() << clava::getId(attr, id) << "\n";
}

void ClangAstDumper::emptyChildren(const void *pointer) {
  std::vector<std::string> noChildren;
  dumpVisitedChildren(pointer, noChildren);
}

const void ClangAstDumper::addChild(const Decl *addr,
                                    std::vector<std::string> &children) {

  // Do not add child if goes above system header threshold
  if (systemHeaderThreshold > 0 &&
      currentSystemHeaderLevel > systemHeaderThreshold) {
    return;
  }

  std::string clavaId = clava::getId(addr, id);

#ifdef VISIT_CHECK
  if (addr != nullptr) {
    clava::dump(VISIT_START);
    clava::dump(clavaId);
  }
#endif

  VisitDeclTop(addr);
  children.push_back(clavaId);

#ifdef VISIT_CHECK
  if (addr != nullptr) {
    clava::dump(VISIT_END);
    clava::dump(clavaId);
  }
#endif
};

const void ClangAstDumper::addChildren(DeclContext::decl_range decls,
                                       std::vector<std::string> &children) {

  for (auto decl = decls.begin(), endDecl = decls.end(); decl != endDecl;
       ++decl) {

    // Ignore decls that are not in the source code
    if (decl->isImplicit()) {
      continue;
    }

    if (*decl == nullptr) {
      continue;
    }

    addChild(*decl, children);
  }
};

const void ClangAstDumper::addChild(const Stmt *addr,
                                    std::vector<std::string> &children) {

  // Do not add child if goes above system header threshold
  if (systemHeaderThreshold > 0 &&
      currentSystemHeaderLevel > systemHeaderThreshold) {
    return;
  }

  std::string clavaId = clava::getId(addr, id);

#ifdef VISIT_CHECK
  if (addr != nullptr) {
    clava::dump(VISIT_START);
    clava::dump(clavaId);
  }
#endif

  VisitStmtTop(addr);
  children.push_back(clavaId);

#ifdef VISIT_CHECK
  if (addr != nullptr) {
    clava::dump(VISIT_END);
    clava::dump(clavaId);
  }
#endif
};

const void ClangAstDumper::addChild(const Expr *addr,
                                    std::vector<std::string> &children) {

  // Do not add child if goes above system header threshold
  if (systemHeaderThreshold > 0 &&
      currentSystemHeaderLevel > systemHeaderThreshold) {
    return;
  }

  std::string clavaId = clava::getId(addr, id);

#ifdef VISIT_CHECK
  if (addr != nullptr) {
    clava::dump(VISIT_START);
    clava::dump(clavaId);
  }
#endif

  VisitStmtTop(addr);
  children.push_back(clavaId);

#ifdef VISIT_CHECK
  if (addr != nullptr) {
    clava::dump(VISIT_END);
    clava::dump(clavaId);
  }
#endif
};

const void ClangAstDumper::addChild(const Type *addr,
                                    std::vector<std::string> &children) {

  // Do not add child if goes above system header threshold
  if (systemHeaderThreshold > 0 &&
      currentSystemHeaderLevel > systemHeaderThreshold) {
    return;
  }

  std::string clavaId = clava::getId(addr, id);

#ifdef VISIT_CHECK
  if (addr != nullptr) {
    clava::dump(VISIT_START);
    clava::dump(clavaId);
  }
#endif

  VisitTypeTop(addr);
  children.push_back(clavaId);

#ifdef VISIT_CHECK
  if (addr != nullptr) {
    clava::dump(VISIT_END);
    clava::dump(clavaId);
  }
#endif
};

const void ClangAstDumper::addChild(const QualType &addr,
                                    std::vector<std::string> &children) {

  // Do not add child if goes above system header threshold
  if (systemHeaderThreshold > 0 &&
      currentSystemHeaderLevel > systemHeaderThreshold) {
    return;
  }

  std::string clavaId = clava::getId(addr, id);

#ifdef VISIT_CHECK
  clava::dump(VISIT_START);
  clava::dump(clavaId);
#endif

  VisitTypeTop(addr);
  children.push_back(clavaId);

#ifdef VISIT_CHECK
  clava::dump(VISIT_END);
  clava::dump(clavaId);
#endif
};

const void ClangAstDumper::addChild(const Attr *addr,
                                    std::vector<std::string> &children) {

  // Do not add child if goes above system header threshold
  if (systemHeaderThreshold > 0 &&
      currentSystemHeaderLevel > systemHeaderThreshold) {
    return;
  }

  std::string clavaId = clava::getId(addr, id);

#ifdef VISIT_CHECK
  if (addr != nullptr) {
    clava::dump(VISIT_START);
    clava::dump(clavaId);
  }
#endif

  VisitAttrTop(addr);
  children.push_back(clavaId);

#ifdef VISIT_CHECK
  if (addr != nullptr) {
    clava::dump(VISIT_END);
    clava::dump(clavaId);
  }
#endif
};

void ClangAstDumper::VisitTemplateArgument(
    const TemplateArgument &templateArg) {
  switch (templateArg.getKind()) {
  case TemplateArgument::ArgKind::Type:
    VisitTypeTop(templateArg.getAsType());
    break;
  case TemplateArgument::ArgKind::Expression:
    VisitStmtTop(templateArg.getAsExpr());
    break;
  case TemplateArgument::ArgKind::Pack:
    // Do nothing
    break;
  case TemplateArgument::ArgKind::Integral:
    // Do nothing
    break;
  case TemplateArgument::ArgKind::Template:
    VisitTemplateName(templateArg.getAsTemplate());
    break;
  default:
    throw std::invalid_argument(
        "ClangAstDumper::VisitTemplateArgument(): Case not implemented, '" +
        clava::TEMPLATE_ARG_KIND[templateArg.getKind()] + "'");
  }
};

void ClangAstDumper::VisitTemplateName(const TemplateName &templateName) {

  switch (templateName.getKind()) {
  case TemplateName::NameKind::Template:
    VisitDeclTop(templateName.getAsTemplateDecl());
    break;
  case TemplateName::NameKind::QualifiedTemplate:
    VisitDeclTop(templateName.getAsQualifiedTemplateName()->getTemplateDecl());
    break;
  case TemplateName::NameKind::SubstTemplateTemplateParm:
    VisitDeclTop(templateName.getAsSubstTemplateTemplateParm()->getParameter());
    VisitTemplateName(
        templateName.getAsSubstTemplateTemplateParm()->getReplacement());
  default:
    throw std::invalid_argument(
        "ClangAstDumper::VisitTemplateArgument(): TemplateName case not "
        "implemented, '" +
        clava::TEMPLATE_NAME_KIND[templateName.getKind()] + "'");
  }
};