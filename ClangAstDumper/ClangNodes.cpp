//
// Created by JoaoBispo on 21/03/2018.
//

#include "ClangNodes.h"
#include "ClangEnums.h"

#include "clang/Lex/Lexer.h"

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

const std::string clava::getClassName(const Attr* A) {
    const std::string kindName =  clava::ATTRIBUTES[A->getKind()];
    return kindName + "Attr";
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

void clava::dumpSourceInfo(ASTContext *Context, SourceLocation begin, SourceLocation end) {

    // Original source range
    clava::dumpSourceRange(Context, begin, end);

    // If it is a macro
    bool isMacro = begin.isMacroID() || end.isMacroID();
    clava::dump(isMacro);

    // Spelling location, if macro
    if(isMacro) {
        clava::dumpSourceRange(Context, Context->getSourceManager().getSpellingLoc(begin), Context->getSourceManager().getSpellingLoc(end));
    }

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

void clava::dump(const char string[]) {
    dump(std::string(string));
}

void clava::dump(const std::vector<std::string> &strings) {
    // Number of attributes
    dump((unsigned int)strings.size());

    // Dump each attribute address
    for(auto string : strings) {
        dump(string);
    }
}

void clava::dump(const std::vector<Attr*> &attributes, const int id) {
    // Number of attributes
    dump((unsigned int)attributes.size());

    // Dump each attribute address
    for(auto attr : attributes) {
        dump(clava::getId(attr, id));
        dump(attr->isImplicit());
        dump(attr->isInherited());
        dump(attr->isLateParsed());
        dump(attr->isPackExpansion());
    }
/*
    // Dump each attribute
    for(auto attr : attributes) {
        dump(clava::ATTRIBUTES[attr->getKind()]);
        dump(attr->isImplicit());
        dump(attr->isInherited());
        dump(attr->isLateParsed());
        dump(attr->isPackExpansion());
    }
*/
}


void clava::dump(const QualType& type, int id) {
    if(type.isNull()) {
        dump("nullptr");
    }

    //dump(getId(type.getTypePtr(), id));
    // QUALTYPE EXP
    //dump(getId(type.getAsOpaquePtr(), id));
    dump(getId(type.getAsOpaquePtr(), id));

    // Check if QualType is the same as the underlying type
    /*
    if((void*) type.getTypePtr() == type.getAsOpaquePtr()) {

        llvm::errs() << "QualType " << getId(type.getAsOpaquePtr(), id) << " opaque ptr and type ptr are the same\n";
    } else {
        llvm::errs() << "QualType " << getId(type.getAsOpaquePtr(), id) << " type ptr is different -> " << getId(type.getTypePtr(), id) << "\n";
    }
     */

    // Dump QualType
    /*
    if(dumpType(T)) {
        return;
    }
    */




}


/**
 * Taken from: https://stackoverflow.com/questions/11083066/getting-the-source-behind-clangs-ast
 * @param Context
 * @param start
 * @param end
 * @return
 */
const std::string clava::getSource(ASTContext *Context, SourceRange sourceRange) {
    //const SourceManager &sm = Context->getSourceManager();

  /*

    clang::SourceLocation b(start), _e(end);

    clang::SourceLocation e(clang::Lexer::getLocForEndOfToken(_e, 0, sm, Context->getLangOpts()));

    return std::string(sm.getCharacterData(b),
                                sm.getCharacterData(e)-sm.getCharacterData(b));
*/
    // (T, U) => "T,,"

    const SourceManager &sm = Context->getSourceManager();


    SourceLocation begin = sourceRange.getBegin();
    SourceLocation end = sourceRange.getEnd();
    if (begin.isMacroID()) {
        //llvm::errs() << "Begin is macro:" << begin.printToString(sm) << "\n";
        begin = sm.getSpellingLoc(begin);
        //llvm::errs() << "Begin spelling loc:" << begin.printToString(sm) << "\n";
    } else {
        //llvm::errs() << "Begin is not macro:" << begin.printToString(sm) << "\n";
    }

    if (end.isMacroID()) {
        //llvm::errs() << "End is macro:" << end.printToString(sm) << "\n";
        end = sm.getSpellingLoc(end);
        //llvm::errs() << "End spelling loc:" << end.printToString(sm) << "\n";
    } else {
        //llvm::errs() << "End is not macro:" << begin.printToString(sm) << "\n";
    }



    std::string text = Lexer::getSourceText(CharSourceRange::getTokenRange(begin, end), sm, LangOptions(), 0);
    if (text.size() > 0 && (text.at(text.size()-1) == ',')) //the text can be ""
        return Lexer::getSourceText(CharSourceRange::getCharRange(begin, end), sm, LangOptions(), 0);
    return text;


}