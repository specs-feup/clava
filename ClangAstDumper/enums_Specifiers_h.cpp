#include "ClangEnums.h"

extern const std::string clava::CALLING_CONVENTION[] = {
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
        "SwiftAsync",
        "PreserveMost",
        "PreserveAll",
        "AArch64VectorCall",
};

extern const std::string clava::ACCESS_SPECIFIER[] = {
        "PUBLIC",
        "PROTECTED",
        "PRIVATE",
        "NONE",
};

extern const std::string clava::STORAGE_CLASS[] = {
        "None",
        "Extern",
        "Static",
        "PrivateExtern",
        "Auto",
        "Register",
};

extern const std::string clava::EXPLICIT_SPEC_KIND[] = {
        "ResolvedFalse",
        "ResolvedTrue",
        "Unresolved",
};

extern const std::string clava::TEMPLATE_SPECIALIZATION_KIND[] = {
        "Undeclared",
        "ImplicitInstantiation",
        "ExplicitSpecialization",
        "ExplicitInstantiationDeclaration",
        "ExplicitInstantiationDefinition",
};

