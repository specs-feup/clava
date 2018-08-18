//
// Created by JoaoBispo on 06/04/2018.
//

#include "ClangEnums.h"

/* Clava Enums */

/*
extern const std::string clava::CXX_CTOR_INIT_KIND[] {
        "ANY_MEMBER_INITIALIZER",
        "BASE_INITIALIZER",
        "DELEGATING_INITIALIZER"
};
*/

/* Clang Enums */

const std::string clava::BUILTIN_KIND[] = {
        "Void",
        "Bool",
        "Char_U",
        "UChar",
        "WChar_U",
        "Char16",
        "Char32",
        "UShort",
        "UInt",
        "ULong",
        "ULongLong",
        "UInt128",
        "Char_S",
        "SChar",
        "WChar_S",
        "Short",
        "Int",
        "Long",
        "LongLong",
        "Int128",
        "Half",
        "Float",
        "Double",
        "LongDouble",
        "NullPtr",
        "ObjCId",
        "ObjCClass",
        "ObjCSel",
        "OCLImage1d",
        "OCLImage1dArray",
        "OCLImage1dBuffer",
        "OCLImage2d",
        "OCLImage2dArray",
        "OCLImage2dDepth",
        "OCLImage2dArrayDepth",
        "OCLImage2dMSAA",
        "OCLImage2dArrayMSAA",
        "OCLImage2dMSAADepth",
        "OCLImage2dArrayMSAADepth",
        "OCLImage3d",
        "OCLSampler",
        "OCLEvent",
        "OCLClkEvent",
        "OCLQueue",
        "OCLNDRange",
        "OCLReserveID",
        "Dependent",
        "Overload",
        "BoundMember",
        "PseudoObject",
        "UnknownAny",
        "BuiltinFn"
};


const std::string clava::CAST_KIND[]{
        "DEPENDENT",
        "BIT_CAST",
        "L_VALUE_BIT_CAST",
        "L_VALUE_TO_R_VALUE",
        "NO_OP",
        "BASE_TO_DERIVED",
        "DERIVED_TO_BASE",
        "UNCHECKED_DERIVED_TO_BASE",
        "DYNAMIC",
        "TO_UNION",
        "ARRAY_TO_POINTER_DECAY",
        "FUNCTION_TO_POINTER_DECAY",
        "NULL_TO_POINTER",
        "NULL_TO_MEMBER_POINTER",
        "BASE_TO_DERIVED_MEMBER_POINTER",
        "DERIVED_TO_BASE_MEMBER_POINTER",
        "MEMBER_POINTER_TO_BOOLEAN",
        "REINTERPRET_MEMBER_POINTER",
        "USER_DEFINED_CONVERSION",
        "CONSTRUCTOR_CONVERSION",
        "INTEGRAL_TO_POINTER",
        "POINTER_TO_INTEGRAL",
        "POINTER_TO_BOOLEAN",
        "TO_VOID",
        "VECTOR_SPLAT",
        "INTEGRAL_CAST",
        "INTEGRAL_TO_BOOLEAN",
        "INTEGRAL_TO_FLOATING",
        "FLOATING_TO_INTEGRAL",
        "FLOATING_TO_BOOLEAN",
        "BOOLEAN_TO_SIGNED_INTEGRAL",
        "FLOATING_CAST",
        "C_POINTER_TO_OBJ_C_POINTER_CAST",
        "BLOCK_POINTER_TO_OBJ_C_POINTER_CAST",
        "ANY_POINTER_TO_BLOCK_POINTER_CAST",
        "OBJ_C_OBJECT_L_VALUE_CAST",
        "FLOATING_REAL_TO_COMPLEX",
        "FLOATING_COMPLEX_TO_REAL",
        "FLOATING_COMPLEX_TO_BOOLEAN",
        "FLOATING_COMPLEX_CAST",
        "FLOATING_COMPLEX_TO_INTEGRAL_COMPLEX",
        "INTEGRAL_REAL_TO_COMPLEX",
        "INTEGRAL_COMPLEX_TO_REAL",
        "INTEGRAL_COMPLEX_TO_BOOLEAN",
        "INTEGRAL_COMPLEX_CAST",
        "INTEGRAL_COMPLEX_TO_FLOATING_COMPLEX",
        "ARC_PRODUCE_OBJECT",
        "ARC_CONSUME_OBJECT",
        "ARC_RECLAIM_RETURNED_OBJECT",
        "ARC_EXTEND_BLOCK_OBJECT",
        "ATOMIC_TO_NON_ATOMIC",
        "NON_ATOMIC_TO_ATOMIC",
        "COPY_AND_AUTORELEASE_BLOCK_OBJECT",
        "BUILTIN_FN_TO_FN_PTR",
        "ZERO_TO_OCL_EVENT",
        "ADDRESS_SPACE_CONVERSION"
};


const std::string clava::ATTRIBUTES[] = {
        "Annotate",
        "CFConsumed",
        "CarriesDependency",
        "NSConsumed",
        "PassObjectSize",

        "AMDGPUNumSGPR",
        "AMDGPUNumVGPR",
        "ARMInterrupt",
        "AcquireCapability",
        "AcquiredAfter",
        "AcquiredBefore",
        "AlignMac68k",
        "Aligned",
        "AlwaysInline",
        "AnalyzerNoReturn",
        "ArcWeakrefUnavailable",
        "ArgumentWithTypeTag",
        "AsmLabel",
        "AssertCapability",
        "AssertExclusiveLock",
        "AssertSharedLock",
        "AssumeAligned",
        "Availability",
        "Blocks",
        "C11NoReturn",
        "CDecl",
        "CFAuditedTransfer",
        "CFReturnsNotRetained",
        "CFReturnsRetained",
        "CFUnknownTransfer",
        "CUDAConstant",
        "CUDADevice",
        "CUDAGlobal",
        "CUDAHost",
        "CUDAInvalidTarget",
        "CUDALaunchBounds",
        "CUDAShared",
        "CXX11NoReturn",
        "CallableWhen",
        "Capability",
        "CapturedRecord",
        "Cleanup",
        "Cold",
        "Common",
        "Const",
        "Constructor",
        "Consumable",
        "ConsumableAutoCast",
        "ConsumableSetOnRead",
        "DLLExport",
        "DLLImport",
        "Deprecated",
        "Destructor",
        "DisableTailCalls",
        "EnableIf",
        "ExclusiveTrylockFunction",
        "FastCall",
        "Final",
        "FlagEnum",
        "Flatten",
        "Format",
        "FormatArg",
        "GNUInline",
        "GuardedBy",
        "GuardedVar",
        "Hot",
        "IBAction",
        "IBOutlet",
        "IBOutletCollection",
        "InitPriority",
        "IntelOclBicc",
        "InternalLinkage",
        "LockReturned",
        "LocksExcluded",
        "MSABI",
        "MSInheritance",
        "MSNoVTable",
        "MSP430Interrupt",
        "MSStruct",
        "MSVtorDisp",
        "MaxFieldAlignment",
        "MayAlias",
        "MinSize",
        "Mips16",
        "MipsInterrupt",
        "NSConsumesSelf",
        "NSReturnsAutoreleased",
        "NSReturnsNotRetained",
        "NSReturnsRetained",
        "Naked",
        "NoAlias",
        "NoCommon",
        "NoDebug",
        "NoDuplicate",
        "NoInline",
        "NoInstrumentFunction",
        "NoMips16",
        "NoReturn",
        "NoSanitize",
        "NoSplitStack",
        "NoThreadSafetyAnalysis",
        "NoThrow",
        "NonNull",
        "NotTailCalled",
        "OMPThreadPrivateDecl",
        "ObjCBridge",
        "ObjCBridgeMutable",
        "ObjCBridgeRelated",
        "ObjCException",
        "ObjCExplicitProtocolImpl",
        "ObjCIndependentClass",
        "ObjCMethodFamily",
        "ObjCNSObject",
        "ObjCPreciseLifetime",
        "ObjCRequiresPropertyDefs",
        "ObjCRequiresSuper",
        "ObjCReturnsInnerPointer",
        "ObjCRootClass",
        "OpenCLKernel",
        "OptimizeNone",
        "Override",
        "Ownership",
        "Packed",
        "ParamTypestate",
        "Pascal",
        "Pcs",
        "PtGuardedBy",
        "PtGuardedVar",
        "Pure",
        "ReleaseCapability",
        "ReqdWorkGroupSize",
        "RequiresCapability",
        "Restrict",
        "ReturnTypestate",
        "ReturnsNonNull",
        "ReturnsTwice",
        "ScopedLockable",
        "Section",
        "SelectAny",
        "Sentinel",
        "SetTypestate",
        "SharedTrylockFunction",
        "StdCall",
        "SysVABI",
        "TLSModel",
        "Target",
        "TestTypestate",
        "ThisCall",
        "TransparentUnion",
        "TryAcquireCapability",
        "TypeTagForDatatype",
        "TypeVisibility",
        "Unavailable",
        "Unused",
        "Used",
        "Uuid",
        "VecReturn",
        "VecTypeHint",
        "VectorCall",
        "Visibility",
        "WarnUnused",
        "WarnUnusedResult",
        "Weak",
        "WeakImport",
        "WeakRef",
        "WorkGroupSizeHint",
        "X86ForceAlignArgPointer",

        "Alias",
        "AlignValue",
        "FallThrough",
        "InitSeg",
        "LoopHint",
        "Mode",
        "ObjCBoxable",
        "ObjCDesignatedInitializer",
        "ObjCRuntimeName",
        "OpenCLImageAccess",
        "Overloadable",
        "Thread"
};


const std::string clava::CALLING_CONVENTION[]{
        "C",
        "X86StdCall",
        "X86FastCall",
        "X86ThisCall",
        "X86VectorCall",
        "X86Pascal",
        "X86_64Win64",
        "X86_64SysV",
        "AAPCS",
        "AAPCS_VFP",
        "IntelOclBicc",
        "SpirFunction",
        "SpirKernel"
};

const std::string clava::REFERENCE_QUALIFIER[]{
        "None",
        "LValue",
        "RValue"
};


extern const std::string clava::EXCEPTION_SPECIFICATION_TYPE[]{

        "None",             ///< no exception specification
        "DynamicNone",      ///< throw()
        "Dynamic",          ///< throw(T1, T2)
        "MSAny",            ///< Microsoft throw(...) extension
        "BasicNoexcept",    ///< noexcept
        "ComputedNoexcept", ///< noexcept(expression)
        "Unevaluated",      ///< not evaluated yet, for special member function
        "Uninstantiated",   ///< not instantiated yet
        "Unparsed"          ///< not parsed yet

};


extern const std::string clava::LINKAGE[]{
        "NoLinkage",
        "InternalLinkage",
        "UniqueExternalLinkage",
        "VisibleNoLinkage",
        "ExternalLinkage"
};


extern const std::string clava::VISIBILITY[]{
        "Hidden",
        "Protected",
        "Default"
};

extern const std::string clava::ARRAY_SIZE_MODIFIER[] {
        "Normal",
        "Static",
        "Star"
};


extern const std::string clava::TEMPLATE_ARG_KIND[] {
        "Null",
        "Type",
        "Declaration",
        "NullPtr",
        "Integral",
        "Template",
        "TemplateExpansion",
        "Expression",
        "Pack"
};

extern const std::string clava::TAG_KIND[] {
        "STRUCT",
        "INTERFACE",
        "UNION",
        "CLASS",
        "ENUM"
};

extern const std::string clava::ACCESS_SPECIFIER[] {
        "PUBLIC",
        "PROTECTED",
        "PRIVATE",
        "NONE"
};

extern const std::string clava::INIT_STYLE[] {
        "CINIT",
        "CALL_INIT",
        "LIST_INIT"
};


extern const std::string clava::NEW_INIT_STYLE[]{
        "NO_INIT",
        "CALL_INIT",
        "LIST_INIT"
};


extern const std::string clava::STORAGE_CLASS[] {
        "NONE",
        "EXTERN",
        "STATIC",
        "PRIVATE_EXTERN",
        "AUTO",
        "REGISTER"
};

extern const std::string clava::TLS_KIND[] {
        "NONE",
        "STATIC",
        "DYNAMIC"
};

extern const std::string clava::ELABORATED_TYPE_KEYWORD[] {
        "STRUCT",
        "INTERFACE",
        "UNION",
        "CLASS",
        "ENUM",
        "TYPENAME",
        "NONE"
};

extern const std::string clava::CONSTRUCTION_KIND[]{
        "Complete",
        "NonVirtualBase",
        "VirtualBase",
        "Delegating"
};

extern const std::string clava::BINARY_OPERATOR_KIND[]{
        "PTR_MEM_D",
        "PTR_MEM_I",
        "MUL",
        "DIV",
        "REM",
        "ADD",
        "SUB",
        "SHL",
        "SHR",
        "LT",
        "GT",
        "LE",
        "GE",
        "EQ",
        "NE",
        "AND",
        "XOR",
        "OR",
        "L_AND",
        "L_OR",
        "ASSIGN",
        "MUL_ASSIGN",
        "DIV_ASSIGN",
        "REM_ASSIGN",
        "ADD_ASSIGN",
        "SUB_ASSIGN",
        "SHL_ASSIGN",
        "SHR_ASSIGN",
        "AND_ASSIGN",
        "XOR_ASSIGN",
        "OR_ASSIGN",
        "COMMA"
};

extern const std::string clava::UTT_KIND[]{
    "EnumUnderlyingType"
};


extern const std::string clava::UNARY_OPERATOR_KIND[]{
        "POST_INC",
        "POST_DEC",
        "PRE_INC",
        "PRE_DEC",
        "ADDR_OF",
        "DEREF",
        "PLUS",
        "MINUS",
        "NOT",
        "L_NOT",
        "REAL",
        "IMAG",
        "EXTENSION",
        "COAWAIT"
};

extern const std::string clava::UETT_KIND[]{
        "SIZE_OF",
        "ALIGN_OF",
        "VEC_STEP",
        "OPEN_MP_REQUIRED_SIMD_ALIGN"
};


extern const std::string clava::NESTED_NAMED_SPECIFIER[]{
        "IDENTIFIER",
        "NAMESPACE",
        "NAMESPACE_ALIAS",
        "TYPE_SPEC",
        "TYPE_SPEC_WITH_TEMPLATE",
        "GLOBAL",
        "SUPER"
};


extern const std::string clava::OFFSET_OF_NODE_KIND[]{
        "ARRAY",
        "FIELD",
        "IDENTIFIER",
        "BASE"
};


extern const std::string clava::LINKAGE_LANGUAGE[]{
    "C",
    "CXX"
};


extern const std::string clava::LAMBDA_CAPTURE_DEFAULT[]{
        "NONE",
        "BY_COPY",
        "BY_REF"
};

extern const std::string clava::LAMBDA_CAPTURE_KIND[] {
        "THIS",
        "BY_COPY",
        "BY_REF",
        "VLA_TYPE"
};

extern const std::string clava::PREDEFINED_ID_TYPE[]{
        "FUNC",
        "FUNCTION",
        "L_FUNCTION",
        "FUND_D_NAME",
        "FUNC_SIG",
        "PRETTY_FUNCTION",
        "PRETTY_FUNCTION_NO_VIRTUAL"
};

//const std::string BUILTIN_KIND[] {};