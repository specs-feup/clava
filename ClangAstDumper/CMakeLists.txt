cmake_minimum_required(VERSION 3.14)
project(ClangAstDumper)

set(CLANG_VERSION 16)

set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

SET(CMAKE_C_COMPILER "clang-${CLANG_VERSION}")
SET(CMAKE_CXX_COMPILER "clang++-${CLANG_VERSION}")

SET(Clang_DIR "/usr/lib/cmake/clang-${CLANG_VERSION}")
SET(LLVM_DIR "/usr/lib/llvm-${CLANG_VERSION}/lib/cmake/llvm")

add_compile_options(-Wsuggest-override)

find_package(LLVM REQUIRED CONFIG)
find_package(Clang REQUIRED CONFIG)

message(STATUS "Found LLVM ${LLVM_PACKAGE_VERSION}")
message(STATUS "Using LLVMConfig.cmake in: ${LLVM_DIR}")

# Set the LLVM and Clang include directories
include_directories(${LLVM_INCLUDE_DIRS})
#include_directories("/usr/lib/llvm-${CLANG_VERSION}/include/")
#include_directories(${CLANG_INCLUDE_DIRS})
include_directories("/usr/lib/clang/${CLANG_VERSION}/include/")

# Add definitions needed for LLVM and Clang
add_definitions(${LLVM_DEFINITIONS})
add_definitions(${CLANG_DEFINITIONS})

file (GLOB ENUM_FILES CONFIGURE_DEPENDS
    "src/ClangEnums/enums_*.h"
    "src/ClangEnums/enums_*.cpp")

set(SOURCE_FILES
    src/Clang/ClangAst.cpp
    src/Clang/ClangNodes.cpp
    src/ClangEnums/ClangEnums.cpp
    src/TypeMarker/TypeMarker.cpp
    src/Clava/ClavaConstants.cpp
    src/Clava/ClavaDecl.cpp
    src/ClavaDataDumper/ClavaDataDumper.cpp
    src/ClavaDataDumper/ClavaDataDumperAttrs.cpp
    src/ClavaDataDumper/ClavaDataDumperDecls.cpp
    src/ClavaDataDumper/ClavaDataDumperStmts.cpp
    src/ClavaDataDumper/ClavaDataDumperTypes.cpp
    src/ChildrenVisitor/ChildrenVisitorAttrs.cpp
    src/ChildrenVisitor/ChildrenVisitorDecls.cpp
    src/ChildrenVisitor/ChildrenVisitorExtras.cpp
    src/ChildrenVisitor/ChildrenVisitorStmts.cpp
    src/ChildrenVisitor/ChildrenVisitorTypes.cpp
    src/ClangAstDumper/ClangAstDumper.cpp
    src/ClangAstDumper/ClangAstDumperAttr.cpp
    src/ClangAstDumper/ClangAstDumperDecls.cpp
    src/ClangAstDumper/ClangAstDumperExprs.cpp
    src/ClangAstDumper/ClangAstDumperExtra.cpp
    src/ClangAstDumper/ClangAstDumperStmts.cpp
    src/ClangAstDumper/ClangAstDumperTypes.cpp
    ${ENUM_FILES}
    )

add_custom_target (
    clang_enums
    COMMAND node --experimental-default-type="module" clang_enums/Main.js ${CLANG_VERSION} src/
    WORKING_DIRECTORY ${CMAKE_SOURCE_DIR})



add_library(plugin MODULE ${SOURCE_FILES} src/plugin.cpp)
target_compile_features(plugin PRIVATE cxx_std_20)
add_dependencies(plugin clang_enums)

add_executable(tool ${SOURCE_FILES} src/tool.cpp)
target_compile_features(tool PRIVATE cxx_std_20)
add_dependencies(tool clang_enums)

message(STATUS "SYSTEM PROCESSOR: '${CMAKE_SYSTEM_PROCESSOR}'")
if(${CMAKE_SYSTEM_PROCESSOR} MATCHES "arm") 
	set(PROCESSOR_PREFIX "ARM")
else()
	set(PROCESSOR_PREFIX "X86")
endif()

target_link_libraries( tool
    clangAnalysis
    clangAPINotes
    clangARCMigrate
    clangAST
    clangASTMatchers
    clangBasic
    #clangChangeNamespace
    clangCodeGen
    clangCrossTU
    clangDependencyScanning
    clangDirectoryWatcher		
    #clangDoc
    clangDriver
    clangDynamicASTMatchers
    clangEdit
    clangFormat
    clangFrontend
    clangFrontendTool
    clangHandleCXX
    clangHandleLLVM
    clangIndex
    clangIndexSerialization		
    clangLex
    #clangMove
    clangParse
    #clangReorderFields
    clangRewrite
    clangRewriteFrontend
    clangSema
    clangSerialization
    clangStaticAnalyzerCheckers
    clangStaticAnalyzerCore
    clangStaticAnalyzerFrontend
    clangTooling
    clangToolingASTDiff
    clangToolingCore
    clangToolingInclusions
    clangToolingRefactoring
    clangToolingSyntax		
    clangTransformer
    #DynamicLibraryLib
        
    # LLVM libraries, as given by 'llvm-config --libs'
    LLVMWindowsManifest
    LLVMXRay
    LLVMLibDriver
    LLVMDlltoolDriver
    LLVMCoverage
    #LLVMLineEditor
    LLVMXCoreDisassembler
    LLVMXCoreCodeGen
    LLVMXCoreDesc
    LLVMXCoreInfo
    LLVM${PROCESSOR_PREFIX}Disassembler
    LLVM${PROCESSOR_PREFIX}AsmParser
    LLVM${PROCESSOR_PREFIX}CodeGen
    LLVM${PROCESSOR_PREFIX}Desc
    LLVM${PROCESSOR_PREFIX}Info
    LLVMARMUtils
    LLVMAArch64Utils
    LLVMOrcJIT
    LLVMMCJIT
    LLVMJITLink
    LLVMOrcTargetProcess
    LLVMOrcShared
    LLVMInterpreter
    LLVMExecutionEngine
    LLVMRuntimeDyld
    LLVMSymbolize
    LLVMDebugInfoPDB
    LLVMDebugInfoGSYM
    LLVMOption
    LLVMObjectYAML
    LLVMMCA
    LLVMMCDisassembler
    LLVMLTO
    LLVMPasses
    LLVMCFGuard
    LLVMCoroutines
    LLVMObjCARCOpts
    LLVMipo
    LLVMVectorize
    LLVMLinker
    LLVMInstrumentation
    LLVMFrontendOpenMP
    LLVMFrontendOpenACC
    LLVMExtensions
    LLVMDWARFLinker
    LLVMGlobalISel
    LLVMMIRParser
    LLVMAsmPrinter
    LLVMDebugInfoDWARF
    LLVMSelectionDAG
    LLVMCodeGen
    LLVMIRReader
    LLVMAsmParser
    LLVMInterfaceStub
    LLVMFileCheck
    LLVMFuzzMutate
    LLVMTarget
    LLVMScalarOpts
    LLVMInstCombine
    LLVMAggressiveInstCombine
    LLVMTransformUtils
    LLVMBitWriter
    LLVMAnalysis
    LLVMProfileData
    LLVMObject
    LLVMTextAPI
    LLVMMCParser
    LLVMMC
    LLVMDebugInfoCodeView
    LLVMDebugInfoMSF
    LLVMBitReader
    LLVMCore
    LLVMRemarks
    LLVMBitstreamReader
    LLVMBinaryFormat
    LLVMTableGen
    LLVMSupport
    LLVMDemangle
)