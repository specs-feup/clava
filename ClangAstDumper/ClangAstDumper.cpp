//
// Created by JoaoBispo on 20/01/2017.
//

#include "ClangAstDumper.h"
#include "ClangAstDumperConstants.h"
#include "ClangNodes.h"

#include "clang/AST/AST.h"
#include "clang/Lex/Lexer.h"


#include <iostream>
#include <sstream>

#include <assert.h>

//#define DEBUG

using namespace clang;

ClangAstDumper::ClangAstDumper(ASTContext *Context, int id) : Context(Context), id(id), dataDumper(Context, id)  {};

// This method is equivalent to a VisitQualType() in ClangAstDumperTypes.cpp
void ClangAstDumper::VisitTypeTop(const QualType& T) {
    if(T.isNull()) {
        return;
    }

    // Check if QualType is the same as the underlying type
    if((void*) T.getTypePtr() == T.getAsOpaquePtr()) {

/*
        if(dumpType(T.getTypePtr())) {
            return;
        }
*/
        // TODO: AST dump method relies on visiting the nodes multiple times
        // For now, detect it to avoid visiting children more than once
        if(seenTypes.count(T.getTypePtr()) == 0) {
            TypeVisitor::Visit(T.getTypePtr());
        }


        dumpType(T.getTypePtr());

        return;
    }

    // Dump QualType
    /*
    if(dumpType(T)) {
        return;
    }
    */


    // Visit children
    // TODO: AST dump method relies on visiting the nodes multiple times
    // For now, detect it to avoid visiting children more than once

/*
    if(seenTypes.count(T.getAsOpaquePtr()) == 0) {
        visitChildren(T);
    }
*/



    //dumpType(T);

    if(dumpType(T)) {
        return;
    }

    visitChildren(T);
    dataDumper.dump(T);
    // Dump data
    //dataDumper.dump(clava::TypeNode::TYPE, T);

    // Visit underlying (unqualified) type
    //TypeVisitor::Visit(T.getTypePtr());
//llvm::errs() << "Opaque PTR: " << T.getAsOpaquePtr() << "\n";
//llvm::errs() << "Underlying PTR: " << T.getTypePtr() << "\n";

    //dumpType(T);

//    auto typeAddr = T.getTypePtrOrNull();
//    if(typeAddr == nullptr) {
//        return;
//    }


}

void ClangAstDumper::VisitTypeTop(const Type *T) {
    TypeVisitor::Visit(T);
}

void ClangAstDumper::VisitStmtTop(const Stmt *Node) {
    if(Node == nullptr) {
        return;
    }

    ConstStmtVisitor::Visit(Node);
}

void ClangAstDumper::VisitDeclTop(const Decl *Node) {
    ConstDeclVisitor::Visit(Node);
}

void ClangAstDumper::VisitAttrTop(const Attr *Node) {
    VisitAttr(Node);
}

//void ClangAstDumper::log(const char* name, const void* addr) {
void ClangAstDumper::log(std::string name, const void* addr) {
#ifdef DEBUG
    llvm::errs() << name << " " << addr << "\n";
#endif
}

void ClangAstDumper::log(const Decl* D) {
    log(clava::getClassName(D), D);
}

void ClangAstDumper::log(const Stmt* S) {
    log(clava::getClassName(S), S);
}

void ClangAstDumper::log(const Type* T) {
    log(clava::getClassName(T), T);
}

void ClangAstDumper::log(const Attr* A) {
    log(clava::getClassName(A), A);
}


std::string ClangAstDumper::loc2str(SourceLocation locStart, SourceLocation locEnd) {

    clang::SourceManager *sm = &Context->getSourceManager();
    clang::LangOptions lopt = Context->getLangOpts();

    clang::SourceLocation b(locStart), _e(locEnd);
    clang::SourceLocation e(clang::Lexer::getLocForEndOfToken(_e, 0, *sm, lopt));


    std::string bChars(sm->getCharacterData(b));
    std::string eChars(sm->getCharacterData(e));

    if(bChars == "<<<<INVALID BUFFER>>>>") {
        return "";
    }

    if(eChars == "<<<<INVALID BUFFER>>>>") {
        return "";
    }

    return std::string(sm->getCharacterData(b), sm->getCharacterData(e)-sm->getCharacterData(b));
}


void ClangAstDumper::dumpSourceRange(std::string id, SourceLocation startLoc, SourceLocation endLoc) {
    llvm::errs() << "<SourceRange Dump>\n";

    llvm::errs() << id << "\n";
    // All components of the source range will be dumped

    const SourceManager& SM = Context->getSourceManager();

    SourceLocation startSpellingLoc = SM.getSpellingLoc(startLoc);
    PresumedLoc startPLoc = SM.getPresumedLoc(startSpellingLoc);

    if (startPLoc.isInvalid()) {
        llvm::errs() << "<invalid>\n";
        /*
        llvm::errs() << "startLoc:\n";
        startLoc.dump(SM);
        llvm::errs() << "endLoc:\n";
        endLoc.dump(SM);
        assert(startLoc == endLoc);
         */
        return;
    }


    // Dump start location
    llvm::errs() << startPLoc.getFilename() << "\n";
    llvm::errs() << startPLoc.getLine() << "\n";
    llvm::errs() << startPLoc.getColumn() << "\n";

    if(startLoc == endLoc) {
        llvm::errs() << "<end>\n";
        return;
    }

    SourceLocation endSpellingLoc = SM.getSpellingLoc(endLoc);
    PresumedLoc endPLoc = SM.getPresumedLoc(endSpellingLoc);

    if(endPLoc.isInvalid()) {
        llvm::errs() << "<end>\n";
        return;
    }

    const char* endFilename = endPLoc.getFilename();
    if(!endFilename) {
        endFilename = startPLoc.getFilename();
    }

    unsigned int endLine = endPLoc.getLine();
    if(!endLine) {
        endLine = startPLoc.getLine();
    }

    unsigned int endCol = endPLoc.getColumn();
    if(!endCol) {
        endCol = startPLoc.getColumn();
    }

    // Dump end location
    llvm::errs() << endFilename << "\n";
    llvm::errs() << endLine << "\n";
    llvm::errs() << endCol << "\n";
}


std::string ClangAstDumper::getId(const void* addr) {
    std::stringstream ss;
    ss <<  addr << "_" << id;

    return ss.str();
}

std::string ClangAstDumper::toBoolString(int value) {
    return value ? "true" : "false";
}


const Type* getTypePtr(QualType T, std::string source)  {
    assert(!T.isNull() && "Cannot retrieve a NULL type pointer");

    return T.getTypePtr();
}

void ClangAstDumper::dumpVisitedChildren(const void *pointer, std::vector<std::string> children) {
    llvm::errs() << VISITED_CHILDREN << "\n";
    llvm::errs() << getId(pointer) << "\n";
    llvm::errs() << children.size() << "\n";

    for(auto child : children) {
        llvm::errs() << child << "\n";
    }
}

void ClangAstDumper::dumpIdToClassMap(const void* pointer, std::string className) {
    llvm::errs() << ID_TO_CLASS_MAP << "\n";
    llvm::errs() << getId(pointer) << "\n";
    llvm::errs() << className << "\n";
}

void ClangAstDumper::dumpTopLevelType(const QualType &type) {
    llvm::errs() << TOP_LEVEL_TYPES << "\n";
    clava::dump(type, id);
}


void ClangAstDumper::dumpTopLevelAttr(const Attr *attr) {
    llvm::errs() << TOP_LEVEL_TYPES << "\n";
    llvm::errs() << getId(attr) << "\n";
}



void ClangAstDumper::emptyChildren(const void *pointer) {
    std::vector<std::string> noChildren;
    dumpVisitedChildren(pointer, noChildren);
}


