//
// Created by JoaoBispo on 30/03/2018.
//

#include "ClavaDataDumper.h"
#include "ClangNodes.h"
#include "ClangEnums.h"

#include <bitset>
#include <limits>

const std::map<const std::string, clava::TypeNode > clava::TYPE_DATA_MAP = {
        {"BuiltinType", clava::TypeNode::BUILTIN_TYPE},
        {"PointerType", clava::TypeNode::POINTER_TYPE},
        {"FunctionProtoType", clava::TypeNode::FUNCTION_PROTO_TYPE},
        {"FunctionNoProtoType", clava::TypeNode::FUNCTION_TYPE},
        {"ConstantArrayType", clava::TypeNode::CONSTANT_ARRAY_TYPE},
        {"VariableArrayType", clava::TypeNode::VARIABLE_ARRAY_TYPE},
        {"IncompleteArrayType", clava::TypeNode::ARRAY_TYPE},
        {"DependentSizedArrayType", clava::TypeNode::DEPENDENT_SIZED_ARRAY_TYPE},
        {"RecordType", clava::TypeNode::TAG_TYPE},
        {"EnumType", clava::TypeNode::TAG_TYPE},
        {"ElaboratedType", clava::TypeNode::ELABORATED_TYPE},
        {"TemplateTypeParmType", clava::TypeNode::TEMPLATE_TYPE_PARM_TYPE},
        {"TemplateSpecializationType", clava::TypeNode::TEMPLATE_SPECIALIZATION_TYPE},
        {"TypedefType", clava::TypeNode::TYPEDEF_TYPE},
        {"DecayedType", clava::TypeNode::DECAYED_TYPE},
        {"DecltypeType", clava::TypeNode::DECLTYPE_TYPE},
        {"AutoType", clava::TypeNode::AUTO_TYPE},
        {"LValueReferenceType", clava::TypeNode::REFERENCE_TYPE},
        {"RValueReferenceType", clava::TypeNode::REFERENCE_TYPE},
        {"TypeOfExprType", clava::TypeNode::TYPE_OF_EXPR_TYPE},
        {"PackExpansionType", clava::TypeNode::PACK_EXPANSION_TYPE},
        {"UnaryTransformType", clava::TypeNode::UNARY_TRANSFORM_TYPE},
        {"AttributedType", clava::TypeNode::ATTRIBUTED_TYPE},
        {"SubstTemplateTypeParmType", clava::TypeNode::SUBST_TEMPLATE_TYPE_PARM_TYPE},
};

void clava::ClavaDataDumper::dump(const Type* T) {

    // Get classname
    const std::string classname = clava::getClassName(T);

    // Get corresponding TypeNode
    TypeNode typeNode = TYPE_DATA_MAP.count(classname) == 1 ? TYPE_DATA_MAP.find(classname)->second : TypeNode::TYPE;

    dump(typeNode, T);
}



void clava::ClavaDataDumper::dump(clava::TypeNode typeNode, const Type* T) {
    // Dump header
    llvm::errs() << getDataName(typeNode) << "\n";
    llvm::errs() << clava::getId(T, id) << "\n";
    //DumpHeader(getDataName(typeNode), T);

    switch(typeNode) {
        case clava::TypeNode::TYPE:
            DumpTypeData(T); break;
            //DumpTypeData(T->getCanonicalTypeUnqualified()); break;
//        case clava::TypeNode::QUAL_TYPE:
//            DumpQualTypeData(static_cast<const QualType *>(T)); break;
        case clava::TypeNode::BUILTIN_TYPE:
            DumpBuiltinTypeData(static_cast<const BuiltinType *>(T)); break;
        case clava::TypeNode::POINTER_TYPE:
            DumpPointerTypeData(static_cast<const PointerType *>(T)); break;
        case clava::TypeNode::FUNCTION_TYPE:
            DumpFunctionTypeData(static_cast<const FunctionType *>(T)); break;
        case clava::TypeNode::FUNCTION_PROTO_TYPE:
            DumpFunctionProtoTypeData(static_cast<const FunctionProtoType *>(T)); break;
        case clava::TypeNode::ARRAY_TYPE:
            DumpArrayTypeData(static_cast<const ArrayType *>(T)); break;
        case clava::TypeNode::CONSTANT_ARRAY_TYPE:
            DumpConstantArrayTypeData(static_cast<const ConstantArrayType *>(T)); break;
        case clava::TypeNode::VARIABLE_ARRAY_TYPE:
            DumpVariableArrayTypeData(static_cast<const VariableArrayType *>(T)); break;
        case clava::TypeNode::DEPENDENT_SIZED_ARRAY_TYPE:
            DumpDependentSizedArrayTypeData(static_cast<const DependentSizedArrayType *>(T)); break;
        case clava::TypeNode::TAG_TYPE:
            DumpTagTypeData(static_cast<const TagType *>(T)); break;
        case clava::TypeNode::TYPE_WITH_KEYWORD:
            DumpTypeWithKeywordData(static_cast<const TypeWithKeyword *>(T)); break;
        case clava::TypeNode::ELABORATED_TYPE:
            DumpElaboratedTypeData(static_cast<const ElaboratedType *>(T)); break;
        case clava::TypeNode::TEMPLATE_TYPE_PARM_TYPE:
            DumpTemplateTypeParmTypeData(static_cast<const TemplateTypeParmType *>(T)); break;
        case clava::TypeNode::TEMPLATE_SPECIALIZATION_TYPE:
            DumpTemplateSpecializationTypeData(static_cast<const TemplateSpecializationType *>(T)); break;
        case clava::TypeNode::TYPEDEF_TYPE:
            DumpTypedefTypeData(static_cast<const TypedefType *>(T)); break;
        case clava::TypeNode::ADJUSTED_TYPE:
            DumpAdjustedTypeData(static_cast<const AdjustedType *>(T)); break;
        case clava::TypeNode::DECAYED_TYPE:
            DumpDecayedTypeData(static_cast<const DecayedType *>(T)); break;
        case clava::TypeNode::DECLTYPE_TYPE:
            DumpDecltypeTypeData(static_cast<const DecltypeType *>(T)); break;
        case clava::TypeNode::AUTO_TYPE:
            DumpAutoTypeData(static_cast<const AutoType *>(T)); break;
        case clava::TypeNode::REFERENCE_TYPE:
            DumpReferenceTypeData(static_cast<const ReferenceType *>(T)); break;
        case clava::TypeNode::PACK_EXPANSION_TYPE:
            DumpPackExpansionTypeData(static_cast<const PackExpansionType *>(T)); break;
        case clava::TypeNode::TYPE_OF_EXPR_TYPE:
            DumpTypeOfExprTypeData(static_cast<const TypeOfExprType *>(T)); break;
        case clava::TypeNode::ATTRIBUTED_TYPE:
            DumpAttributedTypeData(static_cast<const AttributedType *>(T)); break;
        case clava::TypeNode::UNARY_TRANSFORM_TYPE:
            DumpUnaryTransformTypeData(static_cast<const UnaryTransformType  *>(T)); break;
        case clava::TypeNode::SUBST_TEMPLATE_TYPE_PARM_TYPE:
            DumpSubstTemplateTypeParmTypeData(static_cast<const SubstTemplateTypeParmType  *>(T)); break;

//         case clava::TypeNode::RECORD_TYPE:
//            DumpRecordTypeData(static_cast<const RecordType *>(T)); break;
        default: throw std::invalid_argument("ClangDataDumper::dump(TypeNode): Case not implemented, '"+ getName(typeNode) +"'");
    }
}




void clava::ClavaDataDumper::DumpTypeData(const Type *T) {
    Qualifiers noQualifiers;
    DumpTypeData(T, noQualifiers);
}



//void clava::ClavaDataDumper::DumpTypeData(const Type *T, Qualifiers &qualifiers) {
/**
 * Data dumper for Type. To be used by both top-level QualType nodes and unqualified types.
 * @param T
 */
//void clava::ClavaDataDumper::DumpTypeData(const QualType &T) {
//void clava::ClavaDataDumper::DumpTypeData(const QualType &T) {
void clava::ClavaDataDumper::DumpTypeData(const Type *T, Qualifiers &qualifiers) {
    //QualType canonicalType = T->getCanonicalTypeInternal();
    //SplitQualType T_split = canonicalType.split();

    //clava::dump(QualType::getAsString(T, qualifiers));
    //clava::dump(QualType::getAsString(T_split));

    // typeAsString
    //SplitQualType T_split = T.split();
    //clava::dump(QualType::getAsString(T_split));
    clava::dump(QualType::getAsString(T, qualifiers));

    // sugar
    //QualType SingleStepDesugar = T->getLocallyUnqualifiedSingleStepDesugaredType();
    //bool hasSugar = SingleStepDesugar != T->unqu;

    /*
    const Type *singleStepDesugar = T->getUnqualifiedDesugaredType();
    bool hasSugar = singleStepDesugar != T;
    clava::dump(hasSugar);
    */

    QualType singleStepDesugar = T->getLocallyUnqualifiedSingleStepDesugaredType();
    bool hasSugar = singleStepDesugar != QualType(T, 0);
    clava::dump(hasSugar);

    if(T->isDependentType()) {
        clava::dump("DEPENDENT");
    } else if (T->isInstantiationDependentType()) {
        clava::dump("INSTANTIATION_DEPENDENT");
    } else {
        clava::dump("NONE");
    }

    clava::dump(T->isVariablyModifiedType());
    clava::dump(T->containsUnexpandedParameterPack());
    clava::dump(T->isFromAST());



    //if(singleStepDesugar != T) {
    if(singleStepDesugar != QualType(T, 0)) {
        clava::dump(clava::getId(singleStepDesugar, id));
    } else {
        clava::dump(clava::getId(((const Type*) nullptr), id));
    }


    //clava::dump(clava::getId(T->getUnqualifiedDesugaredType(), id));


}



// Dumps the same information as DumpTypeData, and after that, information about QualType
void clava::ClavaDataDumper::dump(const QualType& T) {
    // Dump header
    llvm::errs() << "<QualTypeData>" << "\n";
    llvm::errs() << clava::getId(T, id) << "\n";
    //DumpHeader("<QualTypeData>", T.getAsOpaquePtr());

    auto qualifiers = T.getQualifiers();

    // Base type data
    //DumpTypeData(T.getCanonicalType());
    DumpTypeData(T.getTypePtr(), qualifiers);



    // Dump C99 qualifiers
    clava::dump(qualifiers, Context);
/*
    auto c99Qualifiers = qualifiers.getCVRQualifiers();
    const int numBits = std::numeric_limits<decltype(c99Qualifiers)>::digits;
    size_t numSetBits = std::bitset<numBits>(c99Qualifiers).count();

    // Dumps the number of C99 qualifiers, and then the name of the qualifiers
    clava::dump((int) numSetBits);
    if(qualifiers.hasConst()) {clava::dump("CONST");}
    if(qualifiers.hasRestrict()) {
        if(Context->getPrintingPolicy().LangOpts.C99)
            clava::dump("RESTRICT_C99");
        else
            clava::dump("RESTRICT");
    }
    if(qualifiers.hasVolatile()) {clava::dump("VOLATILE");}
*/
    // Dumps address space
    unsigned addrspace = T.getAddressSpace();
    if(addrspace) {
        switch(addrspace) {
            case LangAS::opencl_global: clava::dump("GLOBAL"); break;
            case LangAS::opencl_local: clava::dump("LOCAL"); break;
            case LangAS::opencl_constant: clava::dump("CONSTANT"); break;
            case LangAS::opencl_generic: clava::dump("GENERIC"); break;
            default: clava::dump("DEFAULT");
        }
    } else {
        clava::dump("NONE");
    }
    clava::dump(addrspace);

    // Unqualified type
    clava::dump(clava::getId(T.getTypePtr(), id));

/*    DumpTypeData(T.getTypePtr());

    DumpTypeData(const_cast<Type*>(static_cast<Type*>(T.getAsOpaquePtr())));
    llvm::errs() << "Opaque PTR dump:\n";
    static_cast<Type*>(T.getAsOpaquePtr())->dump();
    llvm::errs() << "Type PTR dump:\n";
    T.getTypePtr()->dump();
    // Dump info about constant, volatile
    */
    // Dump qualifiers as string
    //llvm::errs() << T.getQualifiers().getAsString() << "\n";

}

//void clava::ClavaDataDumper::DumpQualTypeData(const QualType *T) {

//}



void clava::ClavaDataDumper::DumpBuiltinTypeData(const BuiltinType *T) {
    DumpTypeData(T);

    clava::dump(clava::BUILTIN_KIND[T->getKind()]);
    clava::dump((const std::string&) T->getName(Context->getPrintingPolicy()));

}

void clava::ClavaDataDumper::DumpPointerTypeData(const PointerType *T) {
    DumpTypeData(T);

    clava::dump(clava::getId(T->getPointeeType(), id));
}

void clava::ClavaDataDumper::DumpFunctionTypeData(const FunctionType *T) {
    DumpTypeData(T);

    clava::dump(T->isConst());
    clava::dump(T->isVolatile());
    clava::dump(T->isRestrict());

    auto extInfo = T->getExtInfo();
    clava::dump(extInfo.getNoReturn());
    clava::dump(extInfo.getProducesResult());
    clava::dump(extInfo.getHasRegParm());
    clava::dump(extInfo.getHasRegParm() ? extInfo.getRegParm() : 0);
    clava::dump(clava::CALLING_CONVENTION[extInfo.getCC()]);

    clava::dump(clava::getId(T->getReturnType(), id));
}

void clava::ClavaDataDumper::DumpFunctionProtoTypeData(const FunctionProtoType *T) {
    DumpFunctionTypeData(T);




    // Num parameters
    clava::dump(T->getParamTypes().size());

    // Parameters types
    clava::dump(T->getParamTypes().size());
    for (QualType paramType : T->getParamTypes()) {
        clava::dump(clava::getId(paramType, id));
    }


    auto info = T->getExtProtoInfo();
    clava::dump(info.HasTrailingReturn);
    clava::dump(info.Variadic);

    //clava::dump(T->isConst());
    //clava::dump(T->isVolatile());
    //clava::dump(T->isRestrict());
    clava::dump(clava::REFERENCE_QUALIFIER[info.RefQualifier]);

    clava::dump(clava::EXCEPTION_SPECIFICATION_TYPE[info.ExceptionSpec.Type]);
    // Dump types array
    clava::dump(info.ExceptionSpec.Exceptions.size());
    for(auto& exceptType : info.ExceptionSpec.Exceptions) {
        clava::dump(clava::getId(exceptType, id));
    }

    switch(info.ExceptionSpec.Type) {
        case EST_ComputedNoexcept:
            clava::dump(clava::getId(info.ExceptionSpec.NoexceptExpr, id));
            break;
        case EST_Unevaluated:
            clava::dump(clava::getId(info.ExceptionSpec.SourceDecl, id));
            break;
        case EST_Uninstantiated:
            clava::dump(clava::getId(info.ExceptionSpec.SourceDecl, id));
            clava::dump(clava::getId(info.ExceptionSpec.SourceTemplate, id));
            break;
        default:
            // No additional information required
            break;
    }

    //clava::dump(clava::getId(info.ExceptionSpec.NoexceptExpr, id));
    //info.ExceptionSpec.SourceDecl
/*
        FunctionProtoType::ExtProtoInfo EPI = T->getExtProtoInfo();
        switch (EPI.ExceptionSpec.Type) {
            default: break;
            case EST_Unevaluated:
                OS << " noexcept-unevaluated " << EPI.ExceptionSpec.SourceDecl;
                break;
            case EST_Uninstantiated:
                OS << " noexcept-uninstantiated " << EPI.ExceptionSpec.SourceTemplate;
                break;
        }
*/

}


void clava::ClavaDataDumper::DumpTagTypeData(const TagType *T) {
    DumpTypeData(T);

    clava::dump(clava::getId(T->getDecl(), id));

    /*
    TagDecl* tagDecl = T->getDecl()->getDefinition();
    if(tagDecl != nullptr) {
        clava::dump(clava::getId(tagDecl, id));
    } else {
        clava::dump(clava::getId(T->getDecl(), id));
    }
     */
/*
    // Get classname
    const std::string classname = clava::getClassName(T);

    // Get corresponding TypeNode
    TypeNode typeNode = TYPE_DATA_MAP.count(classname) == 1 ? TYPE_DATA_MAP.find(classname)->second : TypeNode::TYPE;

    llvm::errs() << "CLASSNAME: " + classname + " WITH TYPE " << clava::getName(typeNode) << "\n";
*/
}

/*
void clava::ClavaDataDumper::DumpRecordTypeData(const RecordType *T) {
    DumpTagTypeData(T);

}
*/


void clava::ClavaDataDumper::DumpArrayTypeData(const ArrayType *T) {
    DumpTypeData(T);

    clava::dump(clava::ARRAY_SIZE_MODIFIER[T->getSizeModifier()]);
    //clava::dump(QualType::getAsString(T->getArrayElementTypeNoTypeQual(), T->getIndexTypeQualifiers()));
    //clava::dump(T->getIndexTypeQualifiers().getAsString());
    // Dump C99 qualifiers of element type
    clava::dump(T->getIndexTypeQualifiers(), Context);
    clava::dump(clava::getId(T->getElementType(), id));
}

void clava::ClavaDataDumper::DumpConstantArrayTypeData(const ConstantArrayType *T) {
    // Hierarchy
    DumpArrayTypeData(T);

    //clava::dump(T->getSize().VAL);
    clava::dump(T->getSize().toString(10, false));

}

void clava::ClavaDataDumper::DumpVariableArrayTypeData(const VariableArrayType *T) {
    // Hierarchy
    DumpArrayTypeData(T);

    clava::dump(clava::getId(T->getSizeExpr(), id));
}

void clava::ClavaDataDumper::DumpDependentSizedArrayTypeData(const DependentSizedArrayType *T) {
    // Hierarchy
    DumpArrayTypeData(T);

    clava::dump(clava::getId(T->getSizeExpr(), id));
}

void clava::ClavaDataDumper::DumpTypeWithKeywordData(const TypeWithKeyword *T) {
    // Hierarchy
    DumpTypeData(T);

    clava::dump(clava::ELABORATED_TYPE_KEYWORD[T->getKeyword()]);
}

void clava::ClavaDataDumper::DumpElaboratedTypeData(const ElaboratedType *T) {
    // Hierarchy
    DumpTypeWithKeywordData(T);

    clava::dump(T->getQualifier(), Context);
    clava::dump(clava::getId(T->getNamedType(), id));

}

void clava::ClavaDataDumper::DumpTemplateTypeParmTypeData(const TemplateTypeParmType *T) {
    // Hierarchy
    DumpTypeData(T);

    clava::dump(T->getDepth());
    clava::dump(T->getIndex());
    clava::dump(T->isParameterPack());
    clava::dump(clava::getId(T->getDecl(), id));

}

void clava::ClavaDataDumper::DumpTemplateSpecializationTypeData(const TemplateSpecializationType *T) {
    // Hierarchy
    DumpTypeData(T);

    clava::dump(T->isTypeAlias());
    if(T->isTypeAlias()) {
        clava::dump(clava::getId(T->getAliasedType(), id));
    } else {
        clava::dump(clava::getId((const Type*) nullptr, id));
    }

    clava::dump([&T](llvm::raw_string_ostream& stream){T->getTemplateName().dump(stream);});

    int numArgs = T->getNumArgs();
    clava::dump(numArgs);
    for(int i=0; i<numArgs; i++) {
        clava::dump(T->getArg(i), id);
    }


}

void clava::ClavaDataDumper::DumpTypedefTypeData(const TypedefType *T) {
    // Hierarchy
    DumpTypeData(T);

    clava::dump(clava::getId(T->getDecl(), id));
}

void clava::ClavaDataDumper::DumpAdjustedTypeData(const AdjustedType *T) {
    // Hierarchy
    DumpTypeData(T);

    clava::dump(clava::getId(T->getOriginalType(), id));
    clava::dump(clava::getId(T->getAdjustedType(), id));
}

void clava::ClavaDataDumper::DumpDecayedTypeData(const DecayedType *T) {
    // Hierarchy
    DumpAdjustedTypeData(T);


    clava::dump(clava::getId(T->getDecayedType(), id));
    clava::dump(clava::getId(T->getPointeeType(), id));
}

void clava::ClavaDataDumper::DumpDecltypeTypeData(const DecltypeType *T) {
    // Hierarchy
    DumpTypeData(T);


    clava::dump(clava::getId(T->getUnderlyingExpr(), id));
}

void clava::ClavaDataDumper::DumpAutoTypeData(const AutoType *T) {
    // Hierarchy
    DumpTypeData(T);


    clava::dump(clava::getId(T->getDeducedType(), id));
}


void clava::ClavaDataDumper::DumpReferenceTypeData(const ReferenceType *T) {
    // Hierarchy
    DumpTypeData(T);


    clava::dump(clava::getId(T->getPointeeType(), id));
}


void clava::ClavaDataDumper::DumpPackExpansionTypeData(const PackExpansionType *T) {
    // Hierarchy
    DumpTypeData(T);

    if (T->getNumExpansions().hasValue()) {
        clava::dump(T->getNumExpansions().getValue());
    } else {
        clava::dump(0);
    }

    clava::dump(clava::getId(T->getPattern(), id));
    /*
    if (!T->isSugared()) {
        clava::dump(clava::getId(T->getPattern(), id));
    } else {
        clava::dump(clava::getId((const Type*) nullptr, id));
    }
     */

}


void clava::ClavaDataDumper::DumpTypeOfExprTypeData(const TypeOfExprType *T) {
    // Hierarchy
    DumpTypeData(T);

    clava::dump(clava::getId(T->getUnderlyingExpr(), id));
}


void clava::ClavaDataDumper::DumpAttributedTypeData(const AttributedType *T) {
    // Hierarchy
    DumpTypeData(T);

    clava::dump(clava::getId(T->getModifiedType(), id));
    clava::dump(clava::getId(T->getEquivalentType(), id));
}


void clava::ClavaDataDumper::DumpUnaryTransformTypeData(const UnaryTransformType  *T) {
    // Hierarchy
    DumpTypeData(T);

    clava::dump(clava::UTT_KIND[T->getUTTKind()]);
    clava::dump(clava::getId(T->getUnderlyingType(), id));
    clava::dump(clava::getId(T->getBaseType(), id));
}

void clava::ClavaDataDumper::DumpSubstTemplateTypeParmTypeData(const SubstTemplateTypeParmType  *T) {
    // Hierarchy
    DumpTypeData(T);

    clava::dump(clava::getId(T->getReplacedParameter(), id));
    clava::dump(clava::getId(T->getReplacementType(), id));
}

