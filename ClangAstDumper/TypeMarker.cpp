//
// Created by JoaoBispo on 01/02/2017.
//

#include "TypeMarker.h"

#include "clang/AST/AST.h"


TypeMarker::TypeMarker(int id, std::set<const Type*> &seenTypes) : id(id), seenTypes(seenTypes) {};

void TypeMarker::markType(const Type *T) {
    seenTypes.insert(T);
}


void TypeMarker::VisitType(const Type *T) {
    markType(T);
}

void TypeMarker::VisitTypedefType(const TypedefType *T) {
    markType(T);
    Visit(T->getDecl()->getUnderlyingType().getTypePtr());
}