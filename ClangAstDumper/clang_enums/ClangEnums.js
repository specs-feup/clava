import ClangEnum from "./ClangEnum.js";
import HeaderEnums from "./HeaderEnums.js";

const _TYPE_H = new HeaderEnums("../../../include/clang/AST/Type.h", [
  new ClangEnum("Kind", "BUILTIN_KIND", undefined, ["LastKind"]).setOccurence(
    3
  ),
  new ClangEnum(
    "RefQualifierKind",
    "REFERENCE_QUALIFIER",
    (value) => _removePrefix(value, "RQ_"),
    undefined
  ),
  new ClangEnum(
    "ArraySizeModifier",
    "ARRAY_SIZE_MODIFIER",
    undefined,
    undefined
  ),
  new ClangEnum("UTTKind", "UTT_KIND", undefined, undefined),
  new ClangEnum(
    "ElaboratedTypeKeyword",
    "ELABORATED_TYPE_KEYWORD",
    (value) => _removePrefix(value, "ETK_"),
    undefined
  ),
  new ClangEnum(
    "TagTypeKind",
    "TAG_KIND",
    (value) => _removePrefix(value, "TTK_"),
    undefined
  ),
]);

const _OPERATIONS_KINDS_H = new HeaderEnums(
  "../../../include/clang/AST/OperationKinds.h",
  [
    new ClangEnum(
      "CastKind",
      "CAST_KIND",
      (value) => _removePrefix(value, "CK_"),
      undefined
    ),
    new ClangEnum(
      "BinaryOperatorKind",
      "BINARY_OPERATOR_KIND",
      (value) => _removePrefix(value, "BO_"),
      undefined
    ),
    new ClangEnum(
      "UnaryOperatorKind",
      "UNARY_OPERATOR_KIND",
      (value) => _removePrefix(value, "UO_"),
      undefined
    ),
  ]
);

const _ATTR_KINDS_H = new HeaderEnums(
  "../../../include/clang/Basic/AttrKinds.h",
  [new ClangEnum("Kind", "ATTRIBUTES", undefined, undefined)]
);

const _SPECIFIERS_H = new HeaderEnums(
  "../../../include/clang/Basic/Specifiers.h",
  [
    new ClangEnum(
      "CallingConv",
      "CALLING_CONVENTION",
      (value) => _removePrefix(value, "CC_"),
      undefined
    ),
    new ClangEnum(
      "AccessSpecifier",
      "ACCESS_SPECIFIER",
      (value) => _removePrefix(value, "AS_").toUpperCase(),
      undefined
    ),
    new ClangEnum(
      "StorageClass",
      "STORAGE_CLASS",
      (value) => _removePrefix(value, "SC_"),
      undefined
    ),
    new ClangEnum(
      "ExplicitSpecKind",
      "EXPLICIT_SPEC_KIND",
      undefined,
      undefined
    ),
    new ClangEnum(
      "TemplateSpecializationKind",
      "TEMPLATE_SPECIALIZATION_KIND",
      (value) => _removePrefix(value, "TSK_"),
      undefined
    ),
  ]
);

const _EXCEPTION_SPECIFICATION_TYPE_H = new HeaderEnums(
  "../../../include/clang/Basic/ExceptionSpecificationType.h",
  [
    new ClangEnum(
      "ExceptionSpecificationType",
      "EXCEPTION_SPECIFICATION_TYPE",
      (value) => _removePrefix(value, "EST_"),
      undefined
    ),
  ]
);

const _LINKAGE_H = new HeaderEnums("../../../include/clang/Basic/Linkage.h", [
  new ClangEnum(
    "Linkage",
    "LINKAGE",
    (value) => _removePrefix(value, "EST_"),
    undefined
  ),
]);

const _VISIBILITY_H = new HeaderEnums(
  "../../../include/clang/Basic/Visibility.h",
  [
    new ClangEnum(
      "Visibility",
      "VISIBILITY",
      (value) => _removeSuffix(value, "Visibility"),
      undefined
    ),
  ]
);

const _TEMPLATE_BASE_H = new HeaderEnums(
  "../../../include/clang/AST/TemplateBase.h",
  [new ClangEnum("ArgKind", "TEMPLATE_ARG_KIND", undefined, undefined)]
);

const _DECL_H = new HeaderEnums("../../../include/clang/AST/Decl.h", [
  new ClangEnum(
    "InitializationStyle",
    "INIT_STYLE",
    (value) => _INIT_STYLE_ENUMS[value],
    undefined
  ),
  new ClangEnum(
    "TLSKind",
    "TLS_KIND",
    (value) => _removePrefix(value, "TLS_").toUpperCase(),
    undefined
  ),
]);

const _INIT_STYLE_ENUMS = {
  CInit: "CINIT",
  CallInit: "CALL_INIT",
  ListInit: "LIST_INIT",
};

const _EXPR_CXX_H = new HeaderEnums("../../../include/clang/AST/ExprCXX.h", [
  new ClangEnum(
    "InitializationStyle",
    "NEW_INIT_STYLE",
    (value) => _NEW_INIT_STYLE_ENUMS[value],
    undefined
  ).setOccurence(2),
  new ClangEnum(
    "ConstructionKind",
    "CONSTRUCTION_KIND",
    (value) => _removePrefix(value, "CK_"),
    undefined
  ),
]);

const _NEW_INIT_STYLE_ENUMS = {
  NoInit: "NO_INIT",
  CallInit: "CALL_INIT",
  ListInit: "LIST_INIT",
};

const _TYPE_TRAITS_H = new HeaderEnums(
  "../../../include/clang/Basic/TypeTraits.h",
  [
    new ClangEnum(
      "UnaryExprOrTypeTrait",
      "UETT_KIND",
      (value) => _removePrefix(value, "UETT_"),
      ["UETT_Last"]
    ),
  ]
);

const _NESTED_NAME_SPECIFIER_H = new HeaderEnums(
  "../../../include/clang/AST/NestedNameSpecifier.h",
  [
    new ClangEnum(
      "SpecifierKind",
      "NESTED_NAMED_SPECIFIER",
      undefined,
      undefined
    ),
  ]
);

const _DECL_CXX_H = new HeaderEnums("../../../include/clang/AST/DeclCXX.h", [
  new ClangEnum(
    "LanguageIDs",
    "LINKAGE_LANGUAGE",
    (value) => _removePrefix(value, "lang_").toUpperCase(),
    undefined
  ),
]);

const _LAMBDA_H = new HeaderEnums("../../../include/clang/Basic/Lambda.h", [
  new ClangEnum(
    "LambdaCaptureDefault",
    "LAMBDA_CAPTURE_DEFAULT",
    (value) => _removePrefix(value, "LCD_"),
    undefined
  ),
  new ClangEnum(
    "LambdaCaptureKind",
    "LAMBDA_CAPTURE_KIND",
    (value) => _removePrefix(value, "LCK_"),
    undefined
  ),
]);

const _TEMPLATE_NAME_H = new HeaderEnums(
  "../../../include/clang/AST/TemplateName.h",
  [new ClangEnum("NameKind", "TEMPLATE_NAME_KIND", undefined, undefined)]
);

const _ATTR_H = new HeaderEnums("../../../include/clang/AST/Attr.h", [
  new ClangEnum("VisibilityType", "VISIBILITY_ATTR_TYPE", undefined, undefined),
]);

const _EXPR_H = new HeaderEnums("../../../include/clang/AST/Expr.h", [
  new ClangEnum(
    "Kind",
    "OFFSET_OF_NODE_KIND",
    (value) => value.toUpperCase(),
    undefined,
    "OffsetOfNode"
  ).setOccurence(10),
  new ClangEnum(
    "IdentKind",
    "PREDEFINED_ID_TYPE",
    undefined,
    undefined,
    "PredefinedExpr"
  ),
  new ClangEnum(
    "StringKind",
    "STRING_KIND",
    (value) => value.toUpperCase(),
    undefined
  ),
  //new ClangEnum('Kind', 'DESIGNATOR_KIND', undefined, undefined, 'Designator')
]);

/**




- clang/Ast/Expr.h
Kind, OFFSET_OF_NODE_KIND (class OffsetOfNode)
IdentKind, PREDEFINED_ID_TYPE
StringKind, STRING_KIND
Kind, DESIGNATOR_KIND (class Designator)
 

 */

export const Headers = [
  _TYPE_H,
  _OPERATIONS_KINDS_H,
  _ATTR_KINDS_H,
  _SPECIFIERS_H,
  _EXCEPTION_SPECIFICATION_TYPE_H,
  _LINKAGE_H,
  _VISIBILITY_H,
  _TEMPLATE_BASE_H,
  _DECL_H,
  _EXPR_CXX_H,
  _TYPE_TRAITS_H,
  _NESTED_NAME_SPECIFIER_H,
  _DECL_CXX_H,
  _LAMBDA_H,
  _TEMPLATE_NAME_H,
  _ATTR_H,
  _EXPR_H,
];

/**
 * Mappers
 */
/**
 *
 * @param {string} enumValue
 * @param {string} prefix
 * @returns
 */
function _removePrefix(enumValue, prefix) {
  if (enumValue.startsWith(prefix)) {
    enumValue = enumValue.substring(prefix.length);
  }

  return enumValue;
}

/**
 *
 * @param {string} enumValue
 * @param {string} suffix
 * @returns
 */
function _removeSuffix(enumValue, suffix) {
  if (enumValue.endsWith(suffix)) {
    enumValue = enumValue.substring(0, enumValue.length - suffix.length);
  }

  return enumValue;
}
