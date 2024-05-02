//
// Created by JoaoBispo on 20/01/2017.
//

#include "ClangAstDumper.h"
#include "ClangAstDumperConstants.h"
#include "../Clang/ClangNodes.h"

#include "clang/AST/AST.h"


using namespace clang;

//#define OLD_OUTPUT

void ClangAstDumper::visitChildrenAndData(const Type *T) {

    //llvm::errs() << "VISITING TYPE CHILDREN: " << T << "\n";

    // Visit children
    visitChildren(T);
    //llvm::errs() << "DUMPING TYPE DATA: " << T << "\n";
    // Dump data
    dataDumper.dump(T);

    // Dump id
    dumpIdToClassMap(T, clava::getClassName(T));
}

/*
 * TYPES
 */

bool ClangAstDumper::dumpType(const Type* typeAddr) {
    if(typeAddr == nullptr) {
        return true;
    }

    if(seenTypes.count(typeAddr) != 0) {
        return true;
    }

    log(typeAddr);

    // Dump type if it has not appeared yet
    // A TypeDumper is created for each context,
    // no need to use id to disambiguate
    seenTypes.insert((void*)typeAddr);

    return false;
}

bool ClangAstDumper::dumpType(const QualType& type) {
    // QUALTYPE EXP
    void* typeAddr = type.getAsOpaquePtr();
    //const void* typeAddr = &type;


    if(seenTypes.count(typeAddr) != 0) {
    //if(seenTypes.count(&type) != 0) {
        return true;
    }

    log("QualType", typeAddr);

    // Dump type if it has not appeared yet
    // A TypeDumper is created for each context,
    // no need to use id to disambiguate
    seenTypes.insert((void*)typeAddr);

#ifdef OLD_OUTPUT
    llvm::errs() << "TYPE_BEGIN\n";
    type.dump();
    llvm::errs() << "TYPE_END\n";
#endif
    //dumpIdToClassMap(typeAddr, "QualType");



    return false;
}


/**
 * Generic method.
 *
 * @param T
 */
void ClangAstDumper::VisitType(const Type *T){
    if(dumpType(T)) {
        return;
    }

    visitChildrenAndData(T);
}

/*
void ClangAstDumper::VisitPointerType(const PointerType *T) {
    if(dumpType(T)) {
        return;
    }

    visitChildrenAndData(T);
#ifdef OLD_OUTPUT
    // Visit child
    VisitTypeTop(T->getPointeeType().getTypePtr());
#endif
}


void ClangAstDumper::VisitFunctionProtoType(const FunctionProtoType *T) {
    if(dumpType(T)) {
        return;
    }

    visitChildrenAndData(T);

}


void ClangAstDumper::VisitTypedefType(const TypedefType *T) {
    if(dumpType(T)) {
        return;
    }

    visitChildrenAndData(T);
#ifdef OLD_OUTPUT
    VisitTypeTop(T->getDecl()->getUnderlyingType().getTypePtr());
#endif
}


void ClangAstDumper::VisitElaboratedType(const ElaboratedType *T) {
    if(dumpType(T)) {
        return;
    }

    visitChildrenAndData(T);
#ifdef OLD_OUTPUT
    VisitTypeTop(T->getNamedType().getTypePtr());
#endif
}


void ClangAstDumper::VisitLValueReferenceType(const LValueReferenceType *T) {
    if(dumpType(T)) {
        return;
    }

    visitChildrenAndData(T);
#ifdef OLD_OUTPUT
    VisitTypeTop(T->getPointeeType().getTypePtr());
#endif
}


void ClangAstDumper::VisitDependentSizedArrayType(const DependentSizedArrayType *T) {

    if(dumpType(T)) {
        return;
    }

    visitChildrenAndData(T);
#ifdef OLD_OUTPUT
    // Element type
    VisitTypeTop(T->getElementType().getTypePtr());

    // Size expression (can be null)
    if(T->getSizeExpr()) {
        VisitStmtTop(T->getSizeExpr());
    }
#endif
}

*/
