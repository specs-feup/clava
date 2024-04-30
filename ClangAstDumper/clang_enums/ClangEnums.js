laraImport("HeaderEnums");
laraImport("ClangEnum");
laraImport("weaver.Query");

/**
 * @class
 */
class ClangEnums {
  static _TYPE_H = new HeaderEnums("../../../include/clang/AST/Type.h", [
    new ClangEnum("Kind", "BUILTIN_KIND", undefined, ["LastKind"]).setOccurence(
      3
    ),
    new ClangEnum(
      "RefQualifierKind",
      "REFERENCE_QUALIFIER",
      (value) => ClangEnums._removePrefix(value, "RQ_"),
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
      (value) => ClangEnums._removePrefix(value, "ETK_"),
      undefined
    ),
    new ClangEnum(
      "TagTypeKind",
      "TAG_KIND",
      (value) => ClangEnums._removePrefix(value, "TTK_"),
      undefined
    ),
  ]);

  static _OPERATIONS_KINDS_H = new HeaderEnums(
    "../../../include/clang/AST/OperationKinds.h",
    [
      new ClangEnum(
        "CastKind",
        "CAST_KIND",
        (value) => ClangEnums._removePrefix(value, "CK_"),
        undefined
      ),
      new ClangEnum(
        "BinaryOperatorKind",
        "BINARY_OPERATOR_KIND",
        (value) => ClangEnums._removePrefix(value, "BO_"),
        undefined
      ),
      new ClangEnum(
        "UnaryOperatorKind",
        "UNARY_OPERATOR_KIND",
        (value) => ClangEnums._removePrefix(value, "UO_"),
        undefined
      ),
    ]
  );

  static _ATTR_KINDS_H = new HeaderEnums(
    "../../../include/clang/Basic/AttrKinds.h",
    [new ClangEnum("Kind", "ATTRIBUTES", undefined, undefined)]
  );

  static _SPECIFIERS_H = new HeaderEnums(
    "../../../include/clang/Basic/Specifiers.h",
    [
      new ClangEnum(
        "CallingConv",
        "CALLING_CONVENTION",
        (value) => ClangEnums._removePrefix(value, "CC_"),
        undefined
      ),
      new ClangEnum(
        "AccessSpecifier",
        "ACCESS_SPECIFIER",
        (value) => ClangEnums._removePrefix(value, "AS_").toUpperCase(),
        undefined
      ),
      new ClangEnum(
        "StorageClass",
        "STORAGE_CLASS",
        (value) => ClangEnums._removePrefix(value, "SC_"),
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
        (value) => ClangEnums._removePrefix(value, "TSK_"),
        undefined
      ),
    ]
  );

  static _EXCEPTION_SPECIFICATION_TYPE_H = new HeaderEnums(
    "../../../include/clang/Basic/ExceptionSpecificationType.h",
    [
      new ClangEnum(
        "ExceptionSpecificationType",
        "EXCEPTION_SPECIFICATION_TYPE",
        (value) => ClangEnums._removePrefix(value, "EST_"),
        undefined
      ),
    ]
  );

  static _LINKAGE_H = new HeaderEnums("../../../include/clang/Basic/Linkage.h", [
    new ClangEnum(
      "Linkage",
      "LINKAGE",
      (value) => ClangEnums._removePrefix(value, "EST_"),
      undefined
    ),
  ]);

  static _VISIBILITY_H = new HeaderEnums(
    "../../../include/clang/Basic/Visibility.h",
    [
      new ClangEnum(
        "Visibility",
        "VISIBILITY",
        (value) => ClangEnums._removeSuffix(value, "Visibility"),
        undefined
      ),
    ]
  );

  static _TEMPLATE_BASE_H = new HeaderEnums(
    "../../../include/clang/AST/TemplateBase.h",
    [new ClangEnum("ArgKind", "TEMPLATE_ARG_KIND", undefined, undefined)]
  );

  static _DECL_H = new HeaderEnums("../../../include/clang/AST/Decl.h", [
    new ClangEnum(
      "InitializationStyle",
      "INIT_STYLE",
      (value) => ClangEnums._INIT_STYLE_ENUMS[value],
      undefined
    ),
    new ClangEnum(
      "TLSKind",
      "TLS_KIND",
      (value) => ClangEnums._removePrefix(value, "TLS_").toUpperCase(),
      undefined
    ),
  ]);

  static _INIT_STYLE_ENUMS = {
    CInit: "CINIT",
    CallInit: "CALL_INIT",
    ListInit: "LIST_INIT",
  };

  static _EXPR_CXX_H = new HeaderEnums("../../../include/clang/AST/ExprCXX.h", [
    new ClangEnum(
      "InitializationStyle",
      "NEW_INIT_STYLE",
      (value) => ClangEnums._NEW_INIT_STYLE_ENUMS[value],
      undefined
    ).setOccurence(2),
    new ClangEnum(
      "ConstructionKind",
      "CONSTRUCTION_KIND",
      (value) => ClangEnums._removePrefix(value, "CK_"),
      undefined
    ),
  ]);

  static _NEW_INIT_STYLE_ENUMS = {
    NoInit: "NO_INIT",
    CallInit: "CALL_INIT",
    ListInit: "LIST_INIT",
  };

  static _TYPE_TRAITS_H = new HeaderEnums(
    "../../../include/clang/Basic/TypeTraits.h",
    [
      new ClangEnum(
        "UnaryExprOrTypeTrait",
        "UETT_KIND",
        (value) => ClangEnums._removePrefix(value, "UETT_"),
        ["UETT_Last"]
      ),
    ]
  );

  static _NESTED_NAME_SPECIFIER_H = new HeaderEnums(
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

  static _DECL_CXX_H = new HeaderEnums("../../../include/clang/AST/DeclCxx.h", [
    new ClangEnum(
      "LanguageIDs",
      "LINKAGE_LANGUAGE",
      (value) => ClangEnums._removePrefix(value, "lang_").toUpperCase(),
      undefined
    ),
  ]);

  static _LAMBDA_H = new HeaderEnums("../../../include/clang/Basic/Lambda.h", [
    new ClangEnum(
      "LambdaCaptureDefault",
      "LAMBDA_CAPTURE_DEFAULT",
      (value) => ClangEnums._removePrefix(value, "LCD_"),
      undefined
    ),
    new ClangEnum(
      "LambdaCaptureKind",
      "LAMBDA_CAPTURE_KIND",
      (value) => ClangEnums._removePrefix(value, "LCK_"),
      undefined
    ),
  ]);

  static _TEMPLATE_NAME_H = new HeaderEnums(
    "../../../include/clang/AST/TemplateName.h",
    [new ClangEnum("NameKind", "TEMPLATE_NAME_KIND", undefined, undefined)]
  );

  static _ATTR_H = new HeaderEnums("../../../include/clang/AST/Attr.h", [
    new ClangEnum(
      "VisibilityType",
      "VISIBILITY_ATTR_TYPE",
      undefined,
      undefined
    ),
  ]);

  static _EXPR_H = new HeaderEnums("../../../include/clang/AST/Expr.h", [
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

  static _HEADERS = [
    ClangEnums._TYPE_H,
    ClangEnums._OPERATIONS_KINDS_H,
    ClangEnums._ATTR_KINDS_H,
    ClangEnums._SPECIFIERS_H,
    ClangEnums._EXCEPTION_SPECIFICATION_TYPE_H,
    ClangEnums._LINKAGE_H,
    ClangEnums._VISIBILITY_H,
    ClangEnums._TEMPLATE_BASE_H,
    ClangEnums._DECL_H,
    ClangEnums._EXPR_CXX_H,
    ClangEnums._TYPE_TRAITS_H,
    ClangEnums._NESTED_NAME_SPECIFIER_H,
    ClangEnums._DECL_CXX_H,
    ClangEnums._LAMBDA_H,
    ClangEnums._TEMPLATE_NAME_H,
    ClangEnums._ATTR_H,
    ClangEnums._EXPR_H,
  ];

  /**
   * 
   * @param {string} headerFilename 
   * @returns {HeaderEnums}
   */
  static getHeader (headerFilename) {
    if (ClangEnums._HEADER_MAP === undefined) {
      ClangEnums._HEADER_MAP = ClangEnums.buildHeaderMap();
    }

    return ClangEnums._HEADER_MAP[headerFilename];
  };

  /**
   * 
   * @returns {Record<string, HeaderEnums>}
   */
  static buildHeaderMap() {
    const map = {};

    for (const header of ClangEnums._HEADERS) {
      map[header.getName()] = header;
    }

    return map;
  };

  /**
   * Mappers
   */
  /**
   * 
   * @param {string} enumValue 
   * @param {string} prefix 
   * @returns 
   */
  static _removePrefix(enumValue, prefix) {
    if (enumValue.startsWith(prefix)) {
      enumValue = enumValue.substring(prefix.length);
    }

    return enumValue;
  };

  /**
   * 
   * @param {string} enumValue 
   * @param {string} suffix 
   * @returns 
   */
  static _removeSuffix(enumValue, suffix) {
    if (enumValue.endsWith(suffix)) {
      enumValue = enumValue.substring(0, enumValue.length - suffix.length);
    }

    return enumValue;
  };
}
