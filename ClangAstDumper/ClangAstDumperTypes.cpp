//
// Created by JoaoBispo on 20/01/2017.
//

#include "ClangAstDumper.h"
#include "ClangAstDumperConstants.h"
#include "ClangNodes.h"

#include "clang/AST/AST.h"


using namespace clang;

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

    llvm::errs() << "TYPE_BEGIN\n";
    typeAddr->dump();
    llvm::errs() << "TYPE_END\n";

    dumpIdToClassMap(typeAddr, clava::getClassName(typeAddr));


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

    llvm::errs() << "TYPE_BEGIN\n";
    type.dump();
    llvm::errs() << "TYPE_END\n";

    dumpIdToClassMap(typeAddr, "QualType");



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

    // Visit children
    visitChildren(clava::TypeNode::TYPE, T);

    // Dump data
    dataDumper.dump(clava::TypeNode::TYPE, T);
}



void ClangAstDumper::VisitPointerType(const PointerType *T) {
    if(dumpType(T)) {
        return;
    }

    // Visit child
    VisitTypeTop(T->getPointeeType().getTypePtr());
}


void ClangAstDumper::VisitTemplateSpecializationType(const TemplateSpecializationType *T){
    // If already parsed, return
    if(seenTypes.count(T) != 0) {
        return;
    }

    // Visit template types
    for (auto &Arg : *T) {
        // currently only supports Type kind
        if(Arg.getKind() != 1) {
            continue;
        }

        const Type* argType = Arg.getAsType().getTypePtrOrNull();
        if(argType != nullptr) {
            VisitTypeTop(argType);
        } else {
            llvm::errs() << "VisitTemplateSpecializationType: arg type is null\n";
        }

    }

    // Dump template names
    llvm::errs() << "TEMPLATE_NAME_BEGIN\n";
    llvm::errs() << "Template_type:" << T  << "_" << id << "\n";
    for (auto &Arg : *T) {
        Arg.print(Context->getPrintingPolicy(), llvm::errs());
        llvm::errs() << "\n";
    }
    llvm::errs() << "TEMPLATE_NAME_END\n";

    // Dump template arguments types
    llvm::errs() << "TEMPLATE_ARGUMENT_TYPES_BEGIN\n";
    llvm::errs() << "Template_type:" << T  << "_" << id << "\n";
    for (auto &Arg : *T) {
        // Currently only supports Type kind
        if(Arg.getKind() != 1) {
            continue;
        }

        const Type* argType = Arg.getAsType().getTypePtrOrNull();
        if(argType != nullptr) {
            llvm::errs() << "Template_arg:" << argType  << "_" << id << "\n";
        }  else {
            llvm::errs() << "VisitTemplateSpecializationType: template arg type is null\n";
        }

    }
    llvm::errs() << "TEMPLATE_ARGUMENT_TYPES_END\n";




    // Visit type alias
    if(T->isTypeAlias()) {
        const Type* aliasedType = T->getAliasedType().getTypePtrOrNull();
        if(aliasedType != nullptr) {
            VisitTypeTop(aliasedType);
        } else {
            llvm::errs() << "VisitTemplateSpecializationType: aliased type is null\n";
        }

    }

    if(T->isSugared()) {
        VisitTypeTop(T->getUnqualifiedDesugaredType());
    }

    // Dump type, in the end
    if(dumpType(T)) {
        return;
    }

}

void ClangAstDumper::VisitFunctionProtoType(const FunctionProtoType *T) {
    if(dumpType(T)) {
        return;
    }

    // Return type
    VisitTypeTop(T->getReturnType().getTypePtr());

    // Parameters type
    for (QualType PT : T->getParamTypes()) {
        // QUALTYPE EXP
        VisitTypeTop(PT.getTypePtr());
        //VisitTypeTop(PT);
        //VisitTypeTop(PT.getAsOpaquePtr());
    }

    auto EI = T->getExtInfo();

    // Dump template names
    llvm::errs() << FUNCTION_PROTO_TYPE_EXCEPTION << "\n";
    llvm::errs() << getId(T) << "\n";
    auto EPI = T->getExtProtoInfo();

    llvm::errs() << EPI.ExceptionSpec.Type << "\n";

    llvm::errs() << (EPI.ExceptionSpec.NoexceptExpr != nullptr) << "\n";

    if(EPI.ExceptionSpec.NoexceptExpr != nullptr) {
        auto noexceptExpr = EPI.ExceptionSpec.NoexceptExpr;
        llvm::errs() <<  loc2str(noexceptExpr->getLocStart(), noexceptExpr->getLocEnd()) << "\n";
    }

}


void ClangAstDumper::VisitTypedefType(const TypedefType *T) {
    if(dumpType(T)) {
        return;
    }

    VisitTypeTop(T->getDecl()->getUnderlyingType().getTypePtr());

}


void ClangAstDumper::VisitElaboratedType(const ElaboratedType *T) {
    if(dumpType(T)) {
        return;
    }

    VisitTypeTop(T->getNamedType().getTypePtr());
}


void ClangAstDumper::VisitLValueReferenceType(const LValueReferenceType *T) {
    if(dumpType(T)) {
        return;
    }

    VisitTypeTop(T->getPointeeType().getTypePtr());
}


void ClangAstDumper::VisitDependentSizedArrayType(const DependentSizedArrayType *T) {

    if(dumpType(T)) {
        return;
    }

    // Element type
    VisitTypeTop(T->getElementType().getTypePtr());

    // Size expression (can be null)
    if(T->getSizeExpr()) {
        VisitStmtTop(T->getSizeExpr());
    }
}


