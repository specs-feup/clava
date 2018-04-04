//
// Created by JoaoBispo on 21/03/2018.
//

#include "ClangNodes.h"

using namespace clang;

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


void clava::dumpSourceRange(ASTContext *Context, SourceLocation startLoc, SourceLocation endLoc) {
    //llvm::errs() << "<SourceRange Dump>\n";

    //llvm::errs() << id << "\n";

    // All components of the source range will be dumped
    const SourceManager& SM = Context->getSourceManager();

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

const std::string clava::getId(const void* addr, int id) {
    std::stringstream ss;
    ss <<  addr << "_" << id;

    return ss.str();
}


void clava::dump(bool boolean) {
    llvm::errs() << boolean << "\n";
}

void clava::dump(int integer) {
    llvm::errs() << integer << "\n";
}

void clava::dump(unsigned int integer) {
    llvm::errs() << integer << "\n";
}

void clava::dump(const std::string& string) {
    llvm::errs() << string << "\n";
}

void clava::dump(const QualType& type, int id) {
    if(type.isNull()) {
        dump("nullptr");
    }

    dump(getId(type.getTypePtr(), id));
}
