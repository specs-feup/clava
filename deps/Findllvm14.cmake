cmake_minimum_required(VERSION 3.2)

# Download library
deps_resolve("llvm14" LIB_DIR)

# Expose LLVM library
set("llvm14_DIR" ${LIB_DIR})

# LLVM requires C++11
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

if(UNIX)
	# Unix version uses pre-build clang binaries, which disable rtti
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -pthread -fno-rtti")
endif()


#LLVM and Clang includes
list(APPEND llvm14_INCLUDES
	"${LIB_DIR}/src/include"
	"${LIB_DIR}/build/include"
)


# LLVM + Clang libs
# Using link_directories because of circular dependencies in Clang libraries.
# Ideally we would use 'find_libraries'
LINK_DIRECTORIES("${LIB_DIR}/build/lib")

set(LINKER_GROUP_START "-Wl,--start-group")
set(LINKER_GROUP_END "-Wl,--end-group")

if(APPLE)
  # Apple Clang linker does not understand groups
  unset(LINKER_GROUP_START)
  unset(LINKER_GROUP_END)
endif()

#message("SYSTEM PROCESSOR: '${CMAKE_SYSTEM_PROCESSOR}'")
if(${CMAKE_SYSTEM_PROCESSOR} MATCHES "arm") 
	set(PROCESSOR_PREFIX "ARM")
else()
	set(PROCESSOR_PREFIX "X86")
endif()



#find_libraries(llvm14_LIBRARIES "${LIB_DIR}/build/lib" 
list(APPEND llvm14_LIBRARIES 
	
	# Clang libraries, they have circular dependencies, so they are inside a group
  ${LINKER_GROUP_START}
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
  ${LINKER_GROUP_END}
		
	# LLVM libraries, as given by 'llvm-config --libs'
    LLVMWindowsManifest
    LLVMXRay
    LLVMLibDriver
    LLVMDlltoolDriver
    LLVMCoverage
    LLVMLineEditor
    LLVM${PROCESSOR_PREFIX}Disassembler
    LLVM${PROCESSOR_PREFIX}AsmParser
    LLVM${PROCESSOR_PREFIX}CodeGen
    LLVM${PROCESSOR_PREFIX}Desc
    LLVM${PROCESSOR_PREFIX}Info
    LLVMOrcJIT
    LLVMMCJIT
    LLVMJITLink
    LLVMInterpreter
    LLVMExecutionEngine
    LLVMRuntimeDyld
    LLVMOrcTargetProcess
    LLVMOrcShared
    LLVMDWP
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
    LLVMDebugInfoMSF
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
    LLVMDebugInfoDWARF
    LLVMObject
    LLVMTextAPI
    LLVMMCParser
    LLVMMC
    LLVMDebugInfoCodeView
    LLVMBitReader
    LLVMCore
    LLVMRemarks
    LLVMBitstreamReader
    LLVMBinaryFormat
    LLVMTableGen
    LLVMSupport
    LLVMDemangle	
)




if(UNIX AND NOT APPLE)
  # Generic UNIX dependencies
	list(APPEND llvm14_LIBRARIES 
		rt
		tinfo
		z
		m
		dl
	)
elseif(APPLE)
  # macOS dependencies
  list(APPEND llvm14_LIBRARIES 
		c++abi
		#stdc++
		z
		m
		dl
		ncurses	
	)
elseif(WIN32)
  # Windows dependencies
  list(APPEND llvm14_LIBRARIES 
		version
		z
	)	
endif()

