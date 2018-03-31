//
// Created by JoaoBispo on 18/03/2018.
//

#include "ClavaDataDumper.h"

#include "ClangNodes.h"

#include <sstream>

clava::ClavaDataDumper::ClavaDataDumper(ASTContext *Context, int id) : Context(Context), id(id)  {};

void clava::ClavaDataDumper::DumpHeader(const std::string tag, const void *pointer) {
    llvm::errs() << tag << "\n";
    llvm::errs() << getId(pointer) << "\n";
}

/*
void InfoDumper::DumpHeader(const Decl* D) {
    DumpHeader("<" + clava::getClassName(D) + "Data>", D);
}
 */

/*
void InfoDumper::DumpHeader(const Stmt* S) {
    DumpHeader("<" + clava::getClassName(S) + "Data>", S);
}

void InfoDumper::DumpHeader(const Type* T) {
    DumpHeader("<" + clava::getClassName(T) + "Data>", T);
}
 */

std::string clava::ClavaDataDumper::getId(const void* addr) {
    std::stringstream ss;
    ss <<  addr << "_" << id;

    return ss.str();
}

void clava::ClavaDataDumper::dump(bool boolean) {
    llvm::errs() << boolean << "\n";
}


void clava::ClavaDataDumper::dump(int integer) {
    llvm::errs() << integer << "\n";
}

void clava::ClavaDataDumper::dump(std::string string) {
    llvm::errs() << string << "\n";
}


