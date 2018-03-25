//
// Created by JoaoBispo on 18/03/2018.
//

#include "InfoDumper.h"

#include "ClangNodes.h"

#include <sstream>

InfoDumper::InfoDumper(int id) : id(id)  {};

void InfoDumper::DumpHeader(const std::string tag, const void *pointer) {
    llvm::errs() << tag << "\n";
    llvm::errs() << getId(pointer) << "\n";
}

void InfoDumper::DumpHeader(const Decl* D) {
    DumpHeader("<" + clava::getClassName(D) + "Data>", D);
}

void InfoDumper::DumpHeader(const Stmt* S) {
    DumpHeader("<" + clava::getClassName(S) + "Data>", S);
}

void InfoDumper::DumpHeader(const Type* T) {
    DumpHeader("<" + clava::getClassName(T) + "Data>", T);
}

std::string InfoDumper::getId(const void* addr) {
    std::stringstream ss;
    ss <<  addr << "_" << id;

    return ss.str();
}

void InfoDumper::dump(bool boolean) {
    llvm::errs() << boolean << "\n";
}


void InfoDumper::dump(int integer) {
    llvm::errs() << integer << "\n";
}

void InfoDumper::dump(std::string string) {
    llvm::errs() << string << "\n";
}


