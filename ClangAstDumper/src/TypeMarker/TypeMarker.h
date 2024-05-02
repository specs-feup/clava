//
// Created by JoaoBispo on 01/02/2017.
//

#ifndef CLANGASTDUMPER_TYPEMARKER_H
#define CLANGASTDUMPER_TYPEMARKER_H

#include "clang/AST/TypeVisitor.h"

#include <set>

using namespace clang;

class TypeMarker : public clang::TypeVisitor<TypeMarker> {

  private:
    int id;
    std::set<const Type*> &seenTypes;
    void markType(const Type *T);

public:
    explicit TypeMarker(int id, std::set<const Type*> &seenTypes);
    void VisitType(const Type *T);
    void VisitTypedefType(const TypedefType *T);
};

#endif //CLANGASTDUMPER_TYPEMARKER_H
