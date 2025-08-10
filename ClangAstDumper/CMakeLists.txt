cmake_minimum_required(VERSION 3.14)
project(ClangAstDumper)

set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

set(CLANG_VERSION 16)

# Set base folder in Windows
#
# Convention for MinGW packages that were downloaded from https://repo.msys2.org/mingw/mingw64/
# Download matching versions of clang and llvm packages, extract them to a folder (e.g. mingw64-clang-<version>)
# You have to download:
# mingw-w64-x86_64-llvm-<version>
# mingw-w64-x86_64-llvm-libs-<version>
# mingw-w64-x86_64-clang-<version>
# mingw-w64-x86_64-clang-tools-extra-<version>
#
# The executable will still need the dynamic LLVM library to run
# It can be found in c:/mingw64-clang-${CLANG_VERSION}/bin/libLLVM-${CLANG_VERSION}.dll
#
#
# For creating a zip with libcxx, you can use Winlibs (https://winlibs.com/#download-release), find a release close the Clang version, and use the following folders, in the same order (it is important:
#  mingw64/lib/clang/${CLANG_VERSION}/include
#  mingw64/include/c++/%GCCVER%
#  mingw64/include/c++/%GCCVER%/x86_64-w64-mingw32
#  mingw64/x86_64-w64-mingw32/include
#  mingw64/include
#
# Also download from https://repo.msys2.org/mingw/mingw64/ and include:
# mingw-w64-x86_64-openmp-<version> // OpenMP
#
if(WIN32)
	SET(MINGW_BASEDIR "c:/mingw64-clang-${CLANG_VERSION}")
endif()

# Set compiler
if(WIN32)
    # Often these do not work... replace with direct invocation in command line
	# E.g., cmake -DCMAKE_C_COMPILER="c:/mingw64-clang-16/bin/clang.exe" -DCMAKE_CXX_COMPILER="C:/mingw64-clang-16/bin/clang++.exe" -DCMAKE_LINKER="C:/mingw64-clang-16/bin/lld-link.exe" ..
	SET(CMAKE_C_COMPILER "${MINGW_BASEDIR}/bin/clang.exe")
	SET(CMAKE_CXX_COMPILER "${MINGW_BASEDIR}/bin/clang++.exe")
	SET(CMAKE_LINKER "${MINGW_BASEDIR}/bin/lld-link.exe")
else()
	SET(CMAKE_C_COMPILER "clang-${CLANG_VERSION}")
	SET(CMAKE_CXX_COMPILER "clang++-${CLANG_VERSION}")
endif()

# Set libraries
if(WIN32)
	SET(Clang_DIR "${MINGW_BASEDIR}/lib/cmake/clang")
	SET(LLVM_DIR "${MINGW_BASEDIR}/lib/cmake/llvm")
elseif(APPLE)
	# Point CMake to Homebrew's LLVM and Clang config packages
	set(LLVM_DIR   "/usr/local/opt/llvm@${CLANG_VERSION}/lib/cmake/llvm"   CACHE PATH "Path to LLVMConfig.cmake")
	set(Clang_DIR  "/usr/local/opt/llvm@${CLANG_VERSION}/lib/cmake/clang"  CACHE PATH "Path to ClangConfig.cmake")
else()
	SET(Clang_DIR "/usr/lib/cmake/clang-${CLANG_VERSION}")
	SET(LLVM_DIR "/usr/lib/llvm-${CLANG_VERSION}/lib/cmake/llvm")
endif()

add_compile_options(-Wsuggest-override)

find_package(LLVM REQUIRED CONFIG)
find_package(Clang REQUIRED CONFIG)


# Helpful metadata
message(STATUS "Found LLVM ${LLVM_PACKAGE_VERSION}")
message(STATUS "Using LLVMConfig.cmake in: ${LLVM_DIR}")
message(STATUS "Clang folder: ${Clang_DIR}")



# Set the LLVM and Clang include directories
include_directories(${LLVM_INCLUDE_DIRS})	
include_directories(${CLANG_INCLUDE_DIRS})

#include_directories("/usr/lib/llvm-${CLANG_VERSION}/include/")
#include_directories("/usr/lib/clang/${CLANG_VERSION}/include/")

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
    COMMAND node clang_enums/Main.mjs ${LLVM_DIR} src/
    WORKING_DIRECTORY ${CMAKE_SOURCE_DIR})

# Only build plugin if not on Windows or MacOS
if(NOT WIN32 AND NOT APPLE)
	add_library(plugin MODULE ${SOURCE_FILES} src/plugin.cpp)
	target_compile_features(plugin PRIVATE cxx_std_20)
	add_dependencies(plugin clang_enums)
endif()



add_executable(tool ${SOURCE_FILES} src/tool.cpp)
target_compile_features(tool PRIVATE cxx_std_20)
add_dependencies(tool clang_enums)


message(STATUS "SYSTEM PROCESSOR: '${CMAKE_SYSTEM_PROCESSOR}'")
if(${CMAKE_SYSTEM_PROCESSOR} MATCHES "arm") 
	set(PROCESSOR_PREFIX "ARM")
else()
	set(PROCESSOR_PREFIX "X86")
endif()


#set_property(TARGET tool PROPERTY INTERPROCEDURAL_OPTIMIZATION OFF)


if(WIN32)
	target_link_directories(tool PRIVATE "${MINGW_BASEDIR}/lib")
	target_link_libraries(tool
			clang-cpp
			LLVM
	)  
else()
#	set(LLVM_LINK_LLVM_DYLIB OFF)
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
    #LLVMDlltoolDriver
    LLVMCoverage
    #LLVMLineEditor
    #LLVMXCoreDisassembler
    #LLVMXCoreCodeGen
    #LLVMXCoreDesc
    #LLVMXCoreInfo
    LLVM${PROCESSOR_PREFIX}Disassembler
    LLVM${PROCESSOR_PREFIX}AsmParser
    LLVM${PROCESSOR_PREFIX}CodeGen
    LLVM${PROCESSOR_PREFIX}Desc
    LLVM${PROCESSOR_PREFIX}Info
    #LLVMARMUtils
    #LLVMAArch64Utils
    LLVMOrcJIT
    LLVMMCJIT
    LLVMJITLink
    #LLVMOrcTargetProcess
    #LLVMOrcShared
    #LLVMInterpreter
    LLVMExecutionEngine
    LLVMRuntimeDyld
    LLVMSymbolize
    LLVMDebugInfoPDB
    LLVMDebugInfoGSYM
    LLVMOption
    LLVMObjectYAML
    #LLVMMCA
    #LLVMMCDisassembler
    LLVMLTO
    #LLVMPasses
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
    #LLVMAsmPrinter
    LLVMDebugInfoDWARF
    LLVMSelectionDAG
    LLVMCodeGen
    LLVMIRReader
    #LLVMAsmParser
    LLVMInterfaceStub
    LLVMFileCheck
    LLVMFuzzMutate
    LLVMTarget
    #LLVMScalarOpts
    #LLVMInstCombine
    #LLVMAggressiveInstCombine
    LLVMTransformUtils
    LLVMBitWriter
    LLVMAnalysis
    #LLVMProfileData
    LLVMObject
    #LLVMTextAPI
    #LLVMMCParser
    #LLVMMC
    #LLVMDebugInfoCodeView
    #LLVMDebugInfoMSF
    #LLVMBitReader
    #LLVMCore
    #LLVMRemarks
    #LLVMBitstreamReader
    #LLVMBinaryFormat
    #LLVMTableGen
    #LLVMSupport
    #LLVMDemangle
)
endif()

# If on MacOS, change the expected location of libLLVM.dylib and libzstd.1.dylib
# To check changes, use 'otool -L tool'
if(APPLE)
	add_custom_command (
		TARGET tool POST_BUILD
		COMMAND install_name_tool -change /usr/local/opt/llvm@${CLANG_VERSION}/lib/libLLVM.dylib @executable_path/libLLVM.dylib "$<TARGET_FILE:tool>"
		COMMAND install_name_tool -change /usr/local/opt/zstd/lib/libzstd.1.dylib @executable_path/libzstd.1.dylib  "$<TARGET_FILE:tool>"
	)
endif()