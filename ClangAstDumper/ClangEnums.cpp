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
/*
const std::string clava::BUILTIN_KIND[] = {
        "OCLImage1dRO",
        "OCLImage1dArrayRO",
        "OCLImage1dBufferRO",
        "OCLImage2dRO",
        "OCLImage2dArrayRO",
        "OCLImage2dDepthRO",
        "OCLImage2dArrayDepthRO",
        "OCLImage2dMSAARO",
        "OCLImage2dArrayMSAARO",
        "OCLImage2dMSAADepthRO",
        "OCLImage2dArrayMSAADepthRO",
        "OCLImage3dRO",
        "OCLImage1dWO",
        "OCLImage1dArrayWO",
        "OCLImage1dBufferWO",
        "OCLImage2dWO",
        "OCLImage2dArrayWO",
        "OCLImage2dDepthWO",
        "OCLImage2dArrayDepthWO",
        "OCLImage2dMSAAWO",
        "OCLImage2dArrayMSAAWO",
        "OCLImage2dMSAADepthWO",
        "OCLImage2dArrayMSAADepthWO",
        "OCLImage3dWO",
        "OCLImage1dRW",
        "OCLImage1dArrayRW",
        "OCLImage1dBufferRW",
        "OCLImage2dRW",
        "OCLImage2dArrayRW",
        "OCLImage2dDepthRW",
        "OCLImage2dArrayDepthRW",
        "OCLImage2dMSAARW",
        "OCLImage2dArrayMSAARW",
        "OCLImage2dMSAADepthRW",
        "OCLImage2dArrayMSAADepthRW",
        "OCLImage3dRW",
        "OCLIntelSubgroupAVCMcePayload",
        "OCLIntelSubgroupAVCImePayload",
        "OCLIntelSubgroupAVCRefPayload",
        "OCLIntelSubgroupAVCSicPayload",
        "OCLIntelSubgroupAVCMceResult",
        "OCLIntelSubgroupAVCImeResult",
        "OCLIntelSubgroupAVCRefResult",
        "OCLIntelSubgroupAVCSicResult",
        "OCLIntelSubgroupAVCImeResultSingleRefStreamout",
        "OCLIntelSubgroupAVCImeResultDualRefStreamout",
        "OCLIntelSubgroupAVCImeSingleRefStreamin",
        "OCLIntelSubgroupAVCImeDualRefStreamin",
        "SveInt8",
        "SveInt16",
        "SveInt32",
        "SveInt64",
        "SveUint8",
        "SveUint16",
        "SveUint32",
        "SveUint64",
        "SveFloat16",
        "SveFloat32",
        "SveFloat64",
        "SveBFloat16",
        "SveInt8x2",
        "SveInt16x2",
        "SveInt32x2",
        "SveInt64x2",
        "SveUint8x2",
        "SveUint16x2",
        "SveUint32x2",
        "SveUint64x2",
        "SveFloat16x2",
        "SveFloat32x2",
        "SveFloat64x2",
        "SveBFloat16x2",
        "SveInt8x3",
        "SveInt16x3",
        "SveInt32x3",
        "SveInt64x3",
        "SveUint8x3",
        "SveUint16x3",
        "SveUint32x3",
        "SveUint64x3",
        "SveFloat16x3",
        "SveFloat32x3",
        "SveFloat64x3",
        "SveBFloat16x3",
        "SveInt8x4",
        "SveInt16x4",
        "SveInt32x4",
        "SveInt64x4",
        "SveUint8x4",
        "SveUint16x4",
        "SveUint32x4",
        "SveUint64x4",
        "SveFloat16x4",
        "SveFloat32x4",
        "SveFloat64x4",
        "SveBFloat16x4",
        "SveBool",
        "VectorQuad",
        "VectorPair",
        "Void",
        "Bool",
        "Char_U",
        "UChar",
        "WChar_U",
        "Char8",
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
        "ShortAccum",
        "Accum",
        "LongAccum",
        "UShortAccum",
        "UAccum",
        "ULongAccum",
        "ShortFract",
        "Fract",
        "LongFract",
        "UShortFract",
        "UFract",
        "ULongFract",
        "SatShortAccum",
        "SatAccum",
        "SatLongAccum",
        "SatUShortAccum",
        "SatUAccum",
        "SatULongAccum",
        "SatShortFract",
        "SatFract",
        "SatLongFract",
        "SatUShortFract",
        "SatUFract",
        "SatULongFract",
        "Half",
        "Float",
        "Double",
        "LongDouble",
        "Float16",
        "BFloat16",
        "Float128",
        "NullPtr",
        "ObjCId",
        "ObjCClass",
        "ObjCSel",
        "OCLSampler",
        "OCLEvent",
        "OCLClkEvent",
        "OCLQueue",
        "OCLReserveID",
        "Dependent",
        "Overload",
        "BoundMember",
        "PseudoObject",
        "UnknownAny",
        "BuiltinFn",
        "ARCUnbridgedCast",
        "IncompleteMatrixIdx",
        "OMPArraySection",
        "OMPArrayShaping",
        "OMPIterator"
};
*/

const std::string clava::CAST_KIND[]{
        "Dependent",
        "BitCast",
        "LValueBitCast",
        "LValueToRValue",
        "NoOp",
        "BaseToDerived",
        "DerivedToBase",
        "UncheckedDerivedToBase",
        "Dynamic",
        "ToUnion",
        "ArrayToPointerDecay",
        "FunctionToPointerDecay",
        "NullToPointer",
        "NullToMemberPointer",
        "BaseToDerivedMemberPointer",
        "DerivedToBaseMemberPointer",
        "MemberPointerToBoolean",
        "ReinterpretMemberPointer",
        "UserDefinedConversion",
        "ConstructorConversion",
        "IntegralToPointer",
        "PointerToIntegral",
        "PointerToBoolean",
        "ToVoid",
        "VectorSplat",
        "IntegralCast",
        "IntegralToBoolean",
        "IntegralToFloating",
        "FloatingToIntegral",
        "FloatingToBoolean",
        "BooleanToSignedIntegral",
        "FloatingCast",
        "CPointerToObjCPointerCast",
        "BlockPointerToObjCPointerCast",
        "AnyPointerToBlockPointerCast",
        "ObjCObjectLValueCast",
        "FloatingRealToComplex",
        "FloatingComplexToReal",
        "FloatingComplexToBoolean",
        "FloatingComplexCast",
        "FloatingComplexToIntegralComplex",
        "IntegralRealToComplex",
        "IntegralComplexToReal",
        "IntegralComplexToBoolean",
        "IntegralComplexCast",
        "IntegralComplexToFloatingComplex",
        "ARCProduceObject",
        "ARCConsumeObject",
        "ARCReclaimReturnedObject",
        "ARCExtendBlockObject",
        "AtomicToNonAtomic",
        "NonAtomicToAtomic",
        "CopyAndAutoreleaseBlockObject",
        "BuiltinFnToFnPtr",
        "ZeroToOCLEvent",
        "ZeroToOCLQueue",
        "AddressSpaceConversion",
        "IntToOCLSampler"
};


const std::string clava::ATTRIBUTES[] = {
        "FallThrough",
        "Suppress",
        "SwiftContext",
        "SwiftErrorResult",
        "SwiftIndirectResult",
        "Annotate",
        "CFConsumed",
        "CarriesDependency",
        "NSConsumed",
        "NonNull",
        "PassObjectSize",
        "AMDGPUFlatWorkGroupSize",
        "AMDGPUNumSGPR",
        "AMDGPUNumVGPR",
        "AMDGPUWavesPerEU",
        "ARMInterrupt",
        "AVRInterrupt",
        "AVRSignal",
        "AcquireCapability",
        "AcquiredAfter",
        "AcquiredBefore",
        "AlignMac68k",
        "Aligned",
        "AllocAlign",
        "AllocSize",
        "AlwaysInline",
        "AnalyzerNoReturn",
        "AnyX86Interrupt",
        "AnyX86NoCallerSavedRegisters",
        "AnyX86NoCfCheck",
        "ArcWeakrefUnavailable",
        "ArgumentWithTypeTag",
        "Artificial",
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
        "CPUDispatch",
        "CPUSpecific",
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
        "CodeSeg",
        "Cold",
        "Common",
        "Const",
        "Constructor",
        "Consumable",
        "ConsumableAutoCast",
        "ConsumableSetOnRead",
        "Convergent",
        "DLLExport",
        "DLLImport",
        "Deprecated",
        "Destructor",
        "DiagnoseIf",
        "DisableTailCalls",
        "EmptyBases",
        "EnableIf",
        "EnumExtensibility",
        "ExclusiveTrylockFunction",
        "ExternalSourceSymbol",
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
        "LTOVisibilityPublic",
        "LayoutVersion",
        "LifetimeBound",
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
        "MicroMips",
        "MinSize",
        "MinVectorWidth",
        "Mips16",
        "MipsInterrupt",
        "MipsLongCall",
        "MipsShortCall",
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
        "NoMicroMips",
        "NoMips16",
        "NoReturn",
        "NoSanitize",
        "NoSplitStack",
        "NoStackProtector",
        "NoThreadSafetyAnalysis",
        "NoThrow",
        "NotTailCalled",
        "OMPCaptureNoInit",
        "OMPDeclareTargetDecl",
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
        "ObjCSubclassingRestricted",
        "OpenCLIntelReqdSubGroupSize",
        "OpenCLKernel",
        "OpenCLUnrollHint",
        "OptimizeNone",
        "Override",
        "Ownership",
        "Packed",
        "ParamTypestate",
        "Pascal",
        "Pcs",
        "PragmaClangBSSSection",
        "PragmaClangDataSection",
        "PragmaClangRodataSection",
        "PragmaClangTextSection",
        "PreserveAll",
        "PreserveMost",
        "PtGuardedBy",
        "PtGuardedVar",
        "Pure",
        "RISCVInterrupt",
        "RegCall",
        "ReleaseCapability",
        "ReqdWorkGroupSize",
        "RequireConstantInit",
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
        "SwiftCall",
        "SysVABI",
        "TLSModel",
        "Target",
        "TestTypestate",
        "ThisCall",
        "TransparentUnion",
        "TrivialABI",
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
        "XRayInstrument",
        "XRayLogArgs",
        "AbiTag",
        "Alias",
        "AlignValue",
        "IFunc",
        "InitSeg",
        "LoopHint",
        "Mode",
        "NoEscape",
        "OMPCaptureKind",
        "OMPDeclareSimdDecl",
        "OMPReferencedVar",
        "ObjCBoxable",
        "ObjCDesignatedInitializer",
        "ObjCRuntimeName",
        "ObjCRuntimeVisible",
        "OpenCLAccess",
        "Overloadable",
        "RenderScriptKernel",
        "Thread"
};


const std::string clava::CALLING_CONVENTION[]{
        "C",
        "X86StdCall",
        "X86FastCall",
        "X86ThisCall",
        "X86VectorCall",
        "X86Pascal",
        "Win64",
        "X86_64SysV",
        "X86RegCall",
        "AAPCS",
        "AAPCS_VFP",
        "IntelOclBicc",
        "SpirFunction",
        "OpenCLKernel",
        "Swift",
        "PreserveMost",
        "PreserveAll"
};
/*
const std::string clava::REFERENCE_QUALIFIER[]{
        "None",
        "LValue",
        "RValue"
};
*/

extern const std::string clava::EXCEPTION_SPECIFICATION_TYPE[]{

        "None",             ///< no exception specification
        "DynamicNone",      ///< throw()
        "Dynamic",          ///< throw(T1, T2)
        "MSAny",            ///< Microsoft throw(...) extension
        "BasicNoexcept",    ///< noexcept
        "DependentNoexcept", ///< noexcept(expression), value-dependent
        "NoexceptFalse",    ///< noexcept(expression), evals to 'false'
        "NoexceptTrue",     ///< noexcept(expression), evals to 'true'
        "Unevaluated",      ///< not evaluated yet, for special member function
        "Uninstantiated",   ///< not instantiated yet
        "Unparsed"          ///< not parsed yet

};


extern const std::string clava::LINKAGE[]{
        "NoLinkage",
        "InternalLinkage",
        "UniqueExternalLinkage",
        "VisibleNoLinkage",
        "ModuleInternalLinkage",
        "ModuleLinkage",
        "ExternalLinkage"
};


extern const std::string clava::VISIBILITY[]{
        "Hidden",
        "Protected",
        "Default"
};

/*
extern const std::string clava::ARRAY_SIZE_MODIFIER[] {
        "Normal",
        "Static",
        "Star"
};
*/

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

/*
extern const std::string clava::TAG_KIND[] {
        "STRUCT",
        "INTERFACE",
        "UNION",
        "CLASS",
        "ENUM"
};
*/

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
/*
extern const std::string clava::ELABORATED_TYPE_KEYWORD[] {
        "STRUCT",
        "INTERFACE",
        "UNION",
        "CLASS",
        "ENUM",
        "TYPENAME",
        "NONE"
};
*/
extern const std::string clava::CONSTRUCTION_KIND[]{
        "Complete",
        "NonVirtualBase",
        "VirtualBase",
        "Delegating"
};

extern const std::string clava::BINARY_OPERATOR_KIND[]{
        "PtrMemD",
        "PtrMemI",
        "Mul",
        "Div",
        "Rem",
        "Add",
        "Sub",
        "Shl",
        "Shr",
        "Cmp",
        "LT",
        "GT",
        "LE",
        "GE",
        "EQ",
        "NE",
        "And",
        "Xor",
        "Or",
        "LAnd",
        "LOr",
        "Assign",
        "MulAssign",
        "DivAssign",
        "RemAssign",
        "AddAssign",
        "SubAssign",
        "ShlAssign",
        "ShrAssign",
        "AndAssign",
        "XorAssign",
        "OrAssign",
        "Comma"
};
/*
extern const std::string clava::UTT_KIND[]{
    "EnumUnderlyingType"
};
*/

extern const std::string clava::UNARY_OPERATOR_KIND[]{
        "PostInc",
        "PostDec",
        "PreInc",
        "PreDec",
        "AddrOf",
        "Deref",
        "Plus",
        "Minus",
        "Not",
        "LNot",
        "Real",
        "Imag",
        "Extension",
        "Coawait"
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
        "This",
        "StarThis",
        "ByCopy",
        "ByRef",
        "VLAType"
};

extern const std::string clava::PREDEFINED_ID_TYPE[]{
        "Func",
        "Function",
        "LFunction",
        "FuncDName",
        "FuncSig",
        "LFuncSig",
        "PrettyFunction",
        "PrettyFunctionNoVirtual"
};

extern const std::string clava::STRING_KIND[]{
        "ASCII",
        "WIDE",
        "UTF8",
        "UTF16",
        "UTF32"
};


extern const std::string clava::TEMPLATE_NAME_KIND[]{
        "Template",
        "OverloadedTemplate",
        "QualifiedTemplate",
        "DependentTemplate",
        "SubstTemplateTemplateParm",
        "SubstTemplateTemplateParmPack"
};

extern const std::string clava::DESIGNATOR_KIND[] {
    "Field",
    "Array",
    "ArrayRange"
};

extern const std::string clava::VISIBILITY_ATTR_TYPE[] {
    "Default",
    "Hidden",
    "Protected"
};


extern const std::string clava::EXPLICIT_SPEC_KIND[] {
    "ResolvedFalse",
    "ResolvedTrue",
    "Unresolved"
};




//const std::string BUILTIN_KIND[] {};