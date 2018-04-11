//
// Created by JoaoBispo on 21/03/2018.
//

#ifndef CLANGASTDUMPER_CLANGNODES_H
#define CLANGASTDUMPER_CLANGNODES_H

#include "clang/AST/AST.h"
#include "clang/AST/Decl.h"
#include "clang/AST/Stmt.h"
#include "clang/AST/Type.h"

#include <string>
#include <sstream>

using namespace clang;

namespace clava {
    /**
     * Returns the name of the class of the given Decl.
     * @param D
     * @return
     */
    const std::string getClassName(const Decl* D);

    /**
     * Returns the name of the class of the given Stmt.
     * @param S
     * @return
     */
    const std::string getClassName(const Stmt* S);

    /**
     * Returns the name of the class of the given Type.
     *
     * @param T
     * @return
     */
    const std::string getClassName(const Type* T);

    /**
     * Dumps the source range.
     *
     * @param Context
     * @param startLoc
     * @param endLoc
     */
    void dumpSourceRange(ASTContext *Context, SourceLocation startLoc, SourceLocation endLoc);

    /**
     * Dumps info related to the source code (original source location, if it is a macro, spelling source location...)
     * @param Context
     * @param startLoc
     * @param endLoc
     */
    void dumpSourceInfo(ASTContext *Context, SourceLocation startLoc, SourceLocation endLoc);

    /**
     * Builds a string id.
     *
     * @param addr
     * @param id
     * @return
     */
    const std::string getId(const void* addr, int id);

    /**
     *
     * @param Context
     * @param sourceRange
     * @return the source code corresponding to the given sourceRange
     */
    const std::string getSource(ASTContext *Context, SourceRange sourceRange);



    // Value dumpers
    void dump(bool boolean);
    void dump(int integer);
    void dump(unsigned int integer) ;
    void dump(const std::string& string);
    void dump(const char string[]);
    void dump(const QualType& type, int id);

}

#endif //CLANGASTDUMPER_CLANGNODES_H
