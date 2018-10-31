//
// Created by JoaoBispo on 21/03/2018.
//

#ifndef CLANGASTDUMPER_CLANGNODES_H
#define CLANGASTDUMPER_CLANGNODES_H

#include "clang/AST/AST.h"
#include "clang/AST/Decl.h"
#include "clang/AST/Stmt.h"
#include "clang/AST/Type.h"
#include "clang/AST/NestedNameSpecifier.h"
#include "clang/AST/TemplateName.h"


#include <string>
#include <sstream>
#include <vector>
#include <functional>

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
     * Returns the name of the class of the given Attribute.
     *
     * @param A
     * @return
     */
    const std::string getClassName(const Attr* A);

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
    const std::string getId(const Decl* addr, int id);
    //const std::string getId(Decl* addr, int id);
    const std::string getId(const Stmt* addr, int id);
    const std::string getId(const Expr* addr, int id);
    const std::string getId(const Type* addr, int id);
    const std::string getId(const QualType &addr, int id);
    const std::string getId(const Attr* addr, int id);

    /**
     * Should only be used internally by functions of this include.
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

    //llvm::raw_ostream stringStream();


    // Value dumpers
    void dump(bool boolean);
    void dump(int integer);
    void dump(double integer);
    void dump(unsigned int integer);
    void dump(size_t integer);
    void dump(const std::string& string);
    void dump(const char string[]);
    void dump(const std::vector<std::string> &strings);
    void dump(const std::vector<Attr*> &attributes, const int id);
    void dump(const QualType& type, int id);
    void dump(const Qualifiers& qualifiers, ASTContext* Context);
    void dump(NestedNameSpecifier* qualifier, ASTContext* Context);
    void dump(const TemplateArgument& templateArg, int id);
    void dump(const CXXBaseSpecifier& base, int id);
    //void dump(const TemplateName& templateName);
    void dump(std::function<void(llvm::raw_string_ostream&)> dumper);

    bool isSystemHeader(const Stmt* S, ASTContext* context);
    bool isSystemHeader(const Decl* S, ASTContext* context);
        //  void dump(llvm::raw_string_ostream llvmStringStream);




}

#endif //CLANGASTDUMPER_CLANGNODES_H
