//
// Created by JoaoBispo on 05/04/2018.
//
#include "ClangAstDumper.h"
#include "ClavaConstants.h"
#include "ClangNodes.h"

#include <string>

const std::map<const std::string, clava::TypeNode > ClangAstDumper::TYPE_CHILDREN_MAP = {
        {"FunctionProtoType", clava::TypeNode::FUNCTION_PROTO_TYPE},
        {"FunctionNoProtoType", clava::TypeNode::FUNCTION_TYPE},
        {"ConstantArrayType", clava::TypeNode::ARRAY_TYPE},
        {"DependentSizedArrayType", clava::TypeNode::DEPENDENT_SIZED_ARRAY_TYPE},
        {"IncompleteArrayType", clava::TypeNode::ARRAY_TYPE},
        {"VariableArrayType", clava::TypeNode::VARIABLE_ARRAY_TYPE},
        {"PointerType", clava::TypeNode::POINTER_TYPE},
        {"EnumType", clava::TypeNode::TAG_TYPE},
        {"RecordType", clava::TypeNode::TAG_TYPE},
        {"ElaboratedType", clava::TypeNode::ELABORATED_TYPE},
        {"LValueReferenceType", clava::TypeNode::REFERENCE_TYPE},
        {"RValueReferenceType", clava::TypeNode::REFERENCE_TYPE},
        {"InjectedClassNameType", clava::TypeNode::INJECTED_CLASS_NAME_TYPE},
        {"TemplateTypeParmType", clava::TypeNode::TEMPLATE_TYPE_PARM_TYPE},
        {"SubstTemplateTypeParmType", clava::TypeNode::SUBST_TEMPLATE_TYPE_PARM_TYPE},
        {"TemplateSpecializationType", clava::TypeNode::TEMPLATE_SPECIALIZATION_TYPE},
        {"TypedefType", clava::TypeNode::TYPEDEF_TYPE},
        {"DecayedType", clava::TypeNode::DECAYED_TYPE},
        {"DecltypeType", clava::TypeNode::DECLTYPE_TYPE},
        {"AutoType", clava::TypeNode::AUTO_TYPE},
};

void ClangAstDumper::visitChildren(const Type* T) {
    // Get classname
    const std::string classname = clava::getClassName(T);

    // Get corresponding ExprNode
    clava::TypeNode typeNode = TYPE_CHILDREN_MAP.count(classname) == 1 ? TYPE_CHILDREN_MAP.find(classname)->second :
                               clava::TypeNode::TYPE;

    visitChildren(typeNode, T);
}

void ClangAstDumper::visitChildren(clava::TypeNode typeNode, const Type* T) {

    std::vector<std::string> visitedChildren;

    switch(typeNode) {
        case clava::TypeNode::TYPE:
            VisitTypeChildren(T, visitedChildren); break;
        case clava::TypeNode::FUNCTION_TYPE:
            VisitFunctionTypeChildren(static_cast<const FunctionType *>(T), visitedChildren); break;
        case clava::TypeNode::FUNCTION_PROTO_TYPE:
            VisitFunctionProtoTypeChildren(static_cast<const FunctionProtoType *>(T), visitedChildren); break;
        case clava::TypeNode::ARRAY_TYPE:
            VisitArrayTypeChildren(static_cast<const ArrayType *>(T), visitedChildren); break;
        case clava::TypeNode::VARIABLE_ARRAY_TYPE:
            VisitVariableArrayTypeChildren(static_cast<const VariableArrayType *>(T), visitedChildren); break;
        case clava::TypeNode::DEPENDENT_SIZED_ARRAY_TYPE:
            VisitDependentSizedArrayTypeChildren(static_cast<const DependentSizedArrayType *>(T), visitedChildren); break;
        case clava::TypeNode::POINTER_TYPE:
            VisitPointerTypeChildren(static_cast<const PointerType *>(T), visitedChildren); break;
        case clava::TypeNode::TAG_TYPE:
            VisitTagTypeChildren(static_cast<const TagType *>(T), visitedChildren); break;
        case clava::TypeNode::ELABORATED_TYPE:
            VisitElaboratedTypeChildren(static_cast<const ElaboratedType *>(T), visitedChildren); break;
        case clava::TypeNode::REFERENCE_TYPE:
            VisitReferenceTypeChildren(static_cast<const ReferenceType *>(T), visitedChildren); break;
        case clava::TypeNode::INJECTED_CLASS_NAME_TYPE:
            VisitInjectedClassNameTypeChildren(static_cast<const InjectedClassNameType *>(T), visitedChildren); break;
        case clava::TypeNode::TEMPLATE_TYPE_PARM_TYPE:
            VisitTemplateTypeParmTypeChildren(static_cast<const TemplateTypeParmType *>(T), visitedChildren); break;
        //case clava::TypeNode::TYPEDEF_TYPE:
        //    VisitTypedefTypeChildren(static_cast<const TypedefType *>(T), visitedChildren); break;
        case clava::TypeNode::SUBST_TEMPLATE_TYPE_PARM_TYPE:
            VisitSubstTemplateTypeParmTypeChildren(static_cast<const SubstTemplateTypeParmType *>(T), visitedChildren); break;
        case clava::TypeNode::TEMPLATE_SPECIALIZATION_TYPE:
            VisitTemplateSpecializationTypeChildren(static_cast<const TemplateSpecializationType *>(T), visitedChildren); break;
        case clava::TypeNode::TYPEDEF_TYPE:
            VisitTypedefTypeChildren(static_cast<const TypedefType *>(T), visitedChildren); break;
        case clava::TypeNode::ADJUSTED_TYPE:
            VisitAdjustedTypeChildren(static_cast<const AdjustedType *>(T), visitedChildren); break;
        case clava::TypeNode::DECAYED_TYPE:
            VisitDecayedTypeChildren(static_cast<const DecayedType *>(T), visitedChildren); break;
        case clava::TypeNode::DECLTYPE_TYPE:
            VisitDecltypeTypeChildren(static_cast<const DecltypeType *>(T), visitedChildren); break;
        case clava::TypeNode::AUTO_TYPE:
            VisitAutoTypeChildren(static_cast<const AutoType *>(T), visitedChildren); break;

        default: throw std::invalid_argument("ChildrenVisitorTypes::visitChildren(TypeNode): Case not implemented, '"+clava::getName(typeNode)+"'");

    }

    //llvm::errs() << "Dumping visited children for " << clava::getId(T, id) << "\n";
    dumpVisitedChildren(T, visitedChildren);
}


void ClangAstDumper::visitChildren(const QualType &T) {
    std::vector<std::string> visitedChildren;

    // Visit underlying (unqualified) type
    //TypeVisitor::Visit(T.getTypePtr());

    //addChild(T.getTypePtr(), visitedChildren);

    // QualType might associate with any type node, do not add type pointer as child

    //llvm::errs() << "QUAL TYPE: " << T.getAsOpaquePtr() << " -> " << T.getTypePtr() << "\n";
    VisitTypeTop(T.getTypePtr());


    //VisitTypeTop(T.getTypePtr());
    //visitedChildren.push_back(clava::getId(T.getTypePtr(), id));

    dumpVisitedChildren(T.getAsOpaquePtr(), visitedChildren);
}


void ClangAstDumper::VisitTypeChildren(const Type *T, std::vector<std::string> &visitedChildren) {

    // If has sugar, visit desugared type
    //const Type *singleStepDesugar = T->getUnqualifiedDesugaredType();
    QualType singleStepDesugar = T->getLocallyUnqualifiedSingleStepDesugaredType();

    //if(singleStepDesugar != T) {
    if(singleStepDesugar != QualType(T, 0)) {
        VisitTypeTop(singleStepDesugar);
        //addChild(singleStepDesugar, visitedChildren);
        //VisitTypeTop(singleStepDesugar);
        //visitedChildren.push_back(clava::getId(singleStepDesugar, id));
    }

}

//     TypeVisitor::Visit(T.getTypePtr());

void ClangAstDumper::VisitFunctionTypeChildren(const FunctionType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Return type
    addChild(T->getReturnType(), visitedChildren);
    //VisitTypeTop(T->getReturnType());
    //visitedChildren.push_back(clava::getId(T->getReturnType(), id));
}

void ClangAstDumper::VisitFunctionProtoTypeChildren(const FunctionProtoType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitFunctionTypeChildren(T, visitedChildren);

    // Parameters types
    for (QualType paramType : T->getParamTypes()) {
        addChild(paramType, visitedChildren);
//        VisitTypeTop(paramType);
//        visitedChildren.push_back(clava::getId(paramType, id));
    }

    const auto& exptSpec = T->getExtProtoInfo().ExceptionSpec;

    // Visit exception types
    for(auto& exceptType : exptSpec.Exceptions) {
        VisitTypeTop(exceptType);
    }



    // Visit noexcept expression, if present
    VisitStmtTop(exptSpec.NoexceptExpr);

    // Visit source decl, if present
    VisitDeclTop(exptSpec.SourceDecl);

    // Visit source template, if present
    VisitDeclTop(exptSpec.SourceTemplate);



    //llvm::errs() << "TEST:" << clava::getClassName(T->getExtProtoInfo().ExceptionSpec.NoexceptExpr);
}

void ClangAstDumper::VisitTagTypeChildren(const TagType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Just visit decl
    //llvm::errs() << "VISITING DECL: " << clava::getId(T->getDecl(), id) << "\n";
    VisitDeclTop(T->getDecl());
    //VisitDeclTop(T->getDecl()->getCanonicalDecl());
    /*
    TagDecl* tagDecl = T->getDecl()->getDefinition();
    if(tagDecl != nullptr) {
        VisitDeclTop(tagDecl);
    } else {
        VisitDeclTop(T->getDecl());
    }
     */

}

void ClangAstDumper::VisitArrayTypeChildren(const ArrayType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Element type
    addChild(T->getElementType(), visitedChildren);
    //VisitTypeTop(T->getElementType());
    //visitedChildren.push_back(clava::getId(T->getElementType(), id));

    //llvm::errs() << "ARRAY TYPE: " << T << "\n";
    //llvm::errs() << "ELEMENT TYPE: " <<T->getElementType().getTypePtr() << "\n";
}

void ClangAstDumper::VisitVariableArrayTypeChildren(const VariableArrayType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitArrayTypeChildren(T, visitedChildren);


    // Visit and add size expression
    addChild(T->getSizeExpr(), visitedChildren);
    //VisitStmtTop(T->getSizeExpr());
    //visitedChildren.push_back(clava::getId(T->getSizeExpr(), id));
}

void ClangAstDumper::VisitDependentSizedArrayTypeChildren(const DependentSizedArrayType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitArrayTypeChildren(T, visitedChildren);


    // Visit and add size expression
    VisitStmtTop(T->getSizeExpr());
}

void ClangAstDumper::VisitPointerTypeChildren(const PointerType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);


    //llvm::errs() << "POINTER TYPE ADDING " << clava::getId(T->getPointeeType(), id) << "\n";

    // Visit pointee
    //VisitTypeTop(T->getPointeeType());
    addChild(T->getPointeeType(), visitedChildren);
    //VisitTypeTop(T->getPointeeType());
    //visitedChildren.push_back(clava::getId(T->getPointeeType(), id));
}

void ClangAstDumper::VisitElaboratedTypeChildren(const ElaboratedType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Visit named type
    addChild(T->getNamedType(), visitedChildren);
//    VisitTypeTop(T->getNamedType());
//    visitedChildren.push_back(clava::getId(T->getNamedType(), id));
}

void ClangAstDumper::VisitReferenceTypeChildren(const ReferenceType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    //addChild(T->getPointeeTypeAsWritten(), visitedChildren);
    VisitTypeTop(T->getPointeeTypeAsWritten());


    //llvm::errs() << "REFERENCE TYPE POINTEE TYPE AS WRITTEN: " << clava::getId(T->getPointeeTypeAsWritten(), id) <<  "\n";
//llvm::errs() << "REFERENCE TYPE POINTEE TYPE: " << clava::getId(T->getPointeeType(), id) <<  "\n";
    // Visit pointee and pointee as written type

//    VisitTypeTop(T->getPointeeTypeAsWritten());
//    visitedChildren.push_back(clava::getId(T->getPointeeTypeAsWritten(), id));

//    VisitTypeTop(T->getPointeeType());
//    visitedChildren.push_back(clava::getId(T->getPointeeType(), id));
}

void ClangAstDumper::VisitInjectedClassNameTypeChildren(const InjectedClassNameType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Visit decl
    VisitDeclTop(T->getDecl());
}


void ClangAstDumper::VisitTemplateTypeParmTypeChildren(const TemplateTypeParmType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Visit decl
    VisitDeclTop(T->getDecl());
}

/*
void ClangAstDumper::VisitTypedefTypeChildren(const TypedefType *T, std::vector<std::string> &visitedChildren) {
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);


};
*/

void ClangAstDumper::VisitSubstTemplateTypeParmTypeChildren(const SubstTemplateTypeParmType *T, std::vector<std::string> &visitedChildren){
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    addChild(T->getReplacedParameter(), visitedChildren);
    addChild(T->getReplacementType(), visitedChildren);
};

void ClangAstDumper::VisitTemplateSpecializationTypeChildren(const TemplateSpecializationType *T, std::vector<std::string> &visitedChildren){

    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Visit each argument
    for(int i=0; i<T->getNumArgs(); i++) {
        VisitTemplateArgChildren(T->getArg(i));
    }

    // Visit type alias
    if(T->isTypeAlias()) {
        VisitTypeTop(T->getAliasedType());
    }


};

void ClangAstDumper::VisitTypedefTypeChildren(const TypedefType *T, std::vector<std::string> &visitedChildren){
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    VisitDeclTop(T->getDecl());
};


void ClangAstDumper::VisitAdjustedTypeChildren(const AdjustedType *T, std::vector<std::string> &visitedChildren){
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    // Original type
    VisitTypeTop(T->getOriginalType());

    // Adjusted type
    VisitTypeTop(T->getAdjustedType());
};

void ClangAstDumper::VisitDecayedTypeChildren(const DecayedType *T, std::vector<std::string> &visitedChildren){
    // Hierarchy
    VisitAdjustedTypeChildren(T, visitedChildren);

    VisitTypeTop(T->getDecayedType());

    VisitTypeTop(T->getPointeeType());
};

void ClangAstDumper::VisitDecltypeTypeChildren(const DecltypeType *T, std::vector<std::string> &visitedChildren){
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    VisitStmtTop(T->getUnderlyingExpr());
};

void ClangAstDumper::VisitAutoTypeChildren(const AutoType *T, std::vector<std::string> &visitedChildren){
    // Hierarchy
    VisitTypeChildren(T, visitedChildren);

    VisitTypeTop(T->getDeducedType());
};








