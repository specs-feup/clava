//
// Created by JoaoBispo on 30/03/2018.
//

#include "../Clang/ClangNodes.h"
#include "../ClangEnums/ClangEnums.h"
#include "../ClavaDataDumper/ClavaDataDumper.h"

#include <bitset>
#include <limits>

const std::map<const std::string, clava::TypeNode> clava::TYPE_DATA_MAP = {
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
    {"TemplateSpecializationType",
     clava::TypeNode::TEMPLATE_SPECIALIZATION_TYPE},
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
    {"SubstTemplateTypeParmType",
     clava::TypeNode::SUBST_TEMPLATE_TYPE_PARM_TYPE},
    {"ComplexType", clava::TypeNode::COMPLEX_TYPE},
};

void clava::ClavaDataDumper::dump(const Type *T) {

  // Get classname
  const std::string classname = clava::getClassName(T);

  // Get corresponding TypeNode
  TypeNode typeNode = TYPE_DATA_MAP.count(classname) == 1
                          ? TYPE_DATA_MAP.find(classname)->second
                          : TypeNode::TYPE;

  dump(typeNode, T);
}

void clava::ClavaDataDumper::dump(clava::TypeNode typeNode, const Type *T) {
  // Dump header
  llvm::errs() << getDataName(typeNode) << "\n";
  llvm::errs() << clava::getId(T, id) << "\n";
  llvm::errs() << clava::getClassName(T) << "\n";

  switch (typeNode) {
  case clava::TypeNode::TYPE:
    DumpTypeData(T);
    break;
  case clava::TypeNode::BUILTIN_TYPE:
    DumpBuiltinTypeData(static_cast<const BuiltinType *>(T));
    break;
  case clava::TypeNode::POINTER_TYPE:
    DumpPointerTypeData(static_cast<const PointerType *>(T));
    break;
  case clava::TypeNode::FUNCTION_TYPE:
    DumpFunctionTypeData(static_cast<const FunctionType *>(T));
    break;
  case clava::TypeNode::FUNCTION_PROTO_TYPE:
    DumpFunctionProtoTypeData(static_cast<const FunctionProtoType *>(T));
    break;
  case clava::TypeNode::ARRAY_TYPE:
    DumpArrayTypeData(static_cast<const ArrayType *>(T));
    break;
  case clava::TypeNode::CONSTANT_ARRAY_TYPE:
    DumpConstantArrayTypeData(static_cast<const ConstantArrayType *>(T));
    break;
  case clava::TypeNode::VARIABLE_ARRAY_TYPE:
    DumpVariableArrayTypeData(static_cast<const VariableArrayType *>(T));
    break;
  case clava::TypeNode::DEPENDENT_SIZED_ARRAY_TYPE:
    DumpDependentSizedArrayTypeData(
        static_cast<const DependentSizedArrayType *>(T));
    break;
  case clava::TypeNode::TAG_TYPE:
    DumpTagTypeData(static_cast<const TagType *>(T));
    break;
  case clava::TypeNode::TYPE_WITH_KEYWORD:
    DumpTypeWithKeywordData(static_cast<const TypeWithKeyword *>(T));
    break;
  case clava::TypeNode::ELABORATED_TYPE:
    DumpElaboratedTypeData(static_cast<const ElaboratedType *>(T));
    break;
  case clava::TypeNode::TEMPLATE_TYPE_PARM_TYPE:
    DumpTemplateTypeParmTypeData(static_cast<const TemplateTypeParmType *>(T));
    break;
  case clava::TypeNode::TEMPLATE_SPECIALIZATION_TYPE:
    DumpTemplateSpecializationTypeData(
        static_cast<const TemplateSpecializationType *>(T));
    break;
  case clava::TypeNode::TYPEDEF_TYPE:
    DumpTypedefTypeData(static_cast<const TypedefType *>(T));
    break;
  case clava::TypeNode::ADJUSTED_TYPE:
    DumpAdjustedTypeData(static_cast<const AdjustedType *>(T));
    break;
  case clava::TypeNode::DECAYED_TYPE:
    DumpDecayedTypeData(static_cast<const DecayedType *>(T));
    break;
  case clava::TypeNode::DECLTYPE_TYPE:
    DumpDecltypeTypeData(static_cast<const DecltypeType *>(T));
    break;
  case clava::TypeNode::AUTO_TYPE:
    DumpAutoTypeData(static_cast<const AutoType *>(T));
    break;
  case clava::TypeNode::REFERENCE_TYPE:
    DumpReferenceTypeData(static_cast<const ReferenceType *>(T));
    break;
  case clava::TypeNode::PACK_EXPANSION_TYPE:
    DumpPackExpansionTypeData(static_cast<const PackExpansionType *>(T));
    break;
  case clava::TypeNode::TYPE_OF_EXPR_TYPE:
    DumpTypeOfExprTypeData(static_cast<const TypeOfExprType *>(T));
    break;
  case clava::TypeNode::ATTRIBUTED_TYPE:
    DumpAttributedTypeData(static_cast<const AttributedType *>(T));
    break;
  case clava::TypeNode::UNARY_TRANSFORM_TYPE:
    DumpUnaryTransformTypeData(static_cast<const UnaryTransformType *>(T));
    break;
  case clava::TypeNode::SUBST_TEMPLATE_TYPE_PARM_TYPE:
    DumpSubstTemplateTypeParmTypeData(
        static_cast<const SubstTemplateTypeParmType *>(T));
    break;
  case clava::TypeNode::COMPLEX_TYPE:
    DumpComplexTypeData(static_cast<const ComplexType *>(T));
    break;
  default:
    throw std::invalid_argument(
        "ClangDataDumper::dump(TypeNode): Case not implemented, '" +
        getName(typeNode) + "'");
  }
}

void clava::ClavaDataDumper::DumpTypeData(const Type *T) {
  Qualifiers noQualifiers;
  DumpTypeData(T, noQualifiers);
}

/**
 * Data dumper for Type. To be used by both top-level QualType nodes and
 * unqualified types.
 * @param T
 */
void clava::ClavaDataDumper::DumpTypeData(const Type *T,
                                          Qualifiers &qualifiers) {
  clava::dump(
      QualType::getAsString(T, qualifiers, Context->getPrintingPolicy()));

  if (T->isDependentType()) {
    clava::dump("DEPENDENT");
  } else if (T->isInstantiationDependentType()) {
    clava::dump("INSTANTIATION_DEPENDENT");
  } else {
    clava::dump("NONE");
  }

  clava::dump(T->isVariablyModifiedType());
  clava::dump(T->containsUnexpandedParameterPack());
  clava::dump(T->isFromAST());

  QualType singleStepDesugar =
      T->getLocallyUnqualifiedSingleStepDesugaredType();
  if (singleStepDesugar != QualType(T, 0)) {
    clava::dump(clava::getId(singleStepDesugar, id));
  } else {
    clava::dump(clava::getId(((const Type *)nullptr), id));
  }
}

// Dumps the same information as DumpTypeData, and after that, information about
// QualType
void clava::ClavaDataDumper::dump(const QualType &T) {
  // Dump header
  llvm::errs() << "<QualTypeData>"
               << "\n";
  llvm::errs() << clava::getId(T, id) << "\n";
  llvm::errs() << "QualType"
               << "\n";

  auto qualifiers = T.getQualifiers();

  // Base type data
  DumpTypeData(T.getTypePtr(), qualifiers);

  // Dump C99 qualifiers
  clava::dump(qualifiers, Context);

  // Dumps address space
  LangAS addrspace = T.getAddressSpace();
  switch (addrspace) {
  case LangAS::Default:
    clava::dump("NONE");
    break;
  case LangAS::opencl_global:
    clava::dump("GLOBAL");
    break;
  case LangAS::opencl_local:
    clava::dump("LOCAL");
    break;
  case LangAS::opencl_constant:
    clava::dump("CONSTANT");
    break;
  case LangAS::opencl_generic:
    clava::dump("GENERIC");
    break;
  case LangAS::opencl_private:
    clava::dump("PRIVATE");
    break;

  case LangAS::cuda_constant:
    clava::dump("CUDA_CONSTANT");
    break;
  case LangAS::cuda_device:
    clava::dump("CUDA_DEVICE");
    break;
  case LangAS::cuda_shared:
    clava::dump("CUDA_SHARED");
    break;

  default:
    clava::dump("DEFAULT");
  }

  if (isTargetAddressSpace(addrspace)) {
    clava::dump(toTargetAddressSpace(addrspace));
  } else {
    clava::dump((unsigned)0);
  }

  // Unqualified type
  clava::dump(clava::getId(T.getTypePtr(), id));

  // TODO: The following code is valid but the ASTParser hasn't been updated
  // to handle it. Removing it for now as it breaks the whole tool.
  // Single desugar step
  // clava::dump(clava::getId(
  //     T.getSingleStepDesugaredType(*const_cast<const ASTContext *>(Context)),
  //     id));
}

void clava::ClavaDataDumper::DumpBuiltinTypeData(const BuiltinType *T) {
  DumpTypeData(T);

  clava::dump(clava::BUILTIN_KIND, T->getKind());
  clava::dump(T->getName(Context->getPrintingPolicy()));
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

void clava::ClavaDataDumper::DumpFunctionProtoTypeData(
    const FunctionProtoType *T) {
  DumpFunctionTypeData(T);

  // Num parameters
  clava::dumpSize(T->getParamTypes().size());

  // Parameters types
  clava::dumpSize(T->getParamTypes().size());
  for (QualType paramType : T->getParamTypes()) {
    clava::dump(clava::getId(paramType, id));
  }

  auto info = T->getExtProtoInfo();
  clava::dump(info.HasTrailingReturn);
  clava::dump(info.Variadic);

  clava::dump(clava::REFERENCE_QUALIFIER[info.RefQualifier]);

  clava::dump(clava::EXCEPTION_SPECIFICATION_TYPE[info.ExceptionSpec.Type]);

  // Dump types array
  clava::dumpSize(info.ExceptionSpec.Exceptions.size());
  for (auto &exceptType : info.ExceptionSpec.Exceptions) {
    clava::dump(clava::getId(exceptType, id));
  }

  switch (info.ExceptionSpec.Type) {
  case EST_DependentNoexcept:
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
}

void clava::ClavaDataDumper::DumpTagTypeData(const TagType *T) {
  DumpTypeData(T);

  clava::dump(clava::getId(T->getDecl(), id));
}

void clava::ClavaDataDumper::DumpArrayTypeData(const ArrayType *T) {
  DumpTypeData(T);

  clava::dump(clava::ARRAY_SIZE_MODIFIER[T->getSizeModifier()]);

  // Dump C99 qualifiers of element type
  clava::dump(T->getIndexTypeQualifiers(), Context);
  clava::dump(clava::getId(T->getElementType(), id));
}

void clava::ClavaDataDumper::DumpConstantArrayTypeData(
    const ConstantArrayType *T) {
  // Hierarchy
  DumpArrayTypeData(T);

  SmallString<0> str;
  T->getSize().toString(str, 10, false);
  clava::dump(str);
}

void clava::ClavaDataDumper::DumpVariableArrayTypeData(
    const VariableArrayType *T) {
  // Hierarchy
  DumpArrayTypeData(T);

  clava::dump(clava::getId(T->getSizeExpr(), id));
}

void clava::ClavaDataDumper::DumpDependentSizedArrayTypeData(
    const DependentSizedArrayType *T) {
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

void clava::ClavaDataDumper::DumpTemplateTypeParmTypeData(
    const TemplateTypeParmType *T) {
  // Hierarchy
  DumpTypeData(T);

  clava::dump(T->getDepth());
  clava::dump(T->getIndex());
  clava::dump(T->isParameterPack());
  clava::dump(clava::getId(T->getDecl(), id));
}

void clava::ClavaDataDumper::DumpTemplateSpecializationTypeData(
    const TemplateSpecializationType *T) {
  // Hierarchy
  DumpTypeData(T);

  clava::dump(T->isTypeAlias());
  if (T->isTypeAlias()) {
    clava::dump(clava::getId(T->getAliasedType(), id));
  } else {
    clava::dump(clava::getId((const Type *)nullptr, id));
  }

  clava::dump([&T](llvm::raw_string_ostream &stream) {
    T->getTemplateName().dump(stream);
  });
  clava::dump(clava::getId(T->getTemplateName().getAsTemplateDecl(), id));

  int numArgs = T->template_arguments().size();
  clava::dump(numArgs);
  for (auto &arg : T->template_arguments()) {
    clava::dump(arg, id, Context);
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

  clava::dump(T->isSugared());
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

void clava::ClavaDataDumper::DumpPackExpansionTypeData(
    const PackExpansionType *T) {
  // Hierarchy
  DumpTypeData(T);

  if (T->getNumExpansions().has_value()) {
    clava::dump(T->getNumExpansions().value());
  } else {
    clava::dump(0);
  }

  clava::dump(clava::getId(T->getPattern(), id));
}

void clava::ClavaDataDumper::DumpTypeOfExprTypeData(const TypeOfExprType *T) {
  // Hierarchy
  DumpTypeData(T);

  clava::dump(T->isSugared());
  clava::dump(clava::getId(T->getUnderlyingExpr(), id));
}

void clava::ClavaDataDumper::DumpAttributedTypeData(const AttributedType *T) {
  // Hierarchy
  DumpTypeData(T);

  clava::dump(clava::getId(T->getModifiedType(), id));
  clava::dump(clava::getId(T->getEquivalentType(), id));
}

void clava::ClavaDataDumper::DumpUnaryTransformTypeData(
    const UnaryTransformType *T) {
  // Hierarchy
  DumpTypeData(T);

  clava::dump(clava::UTT_KIND[T->getUTTKind()]);
  clava::dump(clava::getId(T->getUnderlyingType(), id));
  clava::dump(clava::getId(T->getBaseType(), id));
}

void clava::ClavaDataDumper::DumpSubstTemplateTypeParmTypeData(
    const SubstTemplateTypeParmType *T) {
  // Hierarchy
  DumpTypeData(T);

  clava::dump(clava::getId(T->getReplacedParameter(), id));
  clava::dump(clava::getId(T->getReplacementType(), id));
}

void clava::ClavaDataDumper::DumpComplexTypeData(const ComplexType *T) {
  // Hierarchy
  DumpTypeData(T);

  clava::dump(clava::getId(T->getElementType(), id));
}
