cmake_minimum_required(VERSION 3.2)

# Download library
deps_resolve("llvm7" LIB_DIR)

# LLVM requires C++11
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

if(UNIX)
	# Unix version uses pre-build clang binaries, which disable rtti
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -pthread -fno-rtti")
endif()


#LLVM includes
list(APPEND llvm7_INCLUDES
	"${LIB_DIR}/src/include"
	"${LIB_DIR}/build/include"
)

# Clang includes
list(APPEND llvm7_INCLUDES
	"${LIB_DIR}/src/tools/clang/include"
	"${LIB_DIR}/build/tools/clang/include"
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

#find_libraries(llvm7_LIBRARIES "${LIB_DIR}/build/lib" 
list(APPEND llvm7_LIBRARIES 
	
	# Clang libraries, they have circular dependencies, so they are inside a group
  ${LINKER_GROUP_START}
		clangAnalysis
		#clangApplyReplacements
		clangARCMigrate
		clangAST
		clangASTMatchers
		clangBasic
		clangChangeNamespace
		clangCodeGen
		clangCrossTU
		clangDaemon
		clangDoc
		clangDriver
		clangDynamicASTMatchers
		clangEdit
		clangFormat
		clangFrontend
		clangFrontendTool
		clangHandleCXX
		clangHandleLLVM
		clangIncludeFixer
		clangIncludeFixerPlugin
		clangIndex
		clangLex
		clangMove
		clangParse
		#clangQuery
		clangReorderFields
		clangRewrite
		clangRewriteFrontend
		clangSema
		clangSerialization
		clangStaticAnalyzerCheckers
		clangStaticAnalyzerCore
		clangStaticAnalyzerFrontend
		#clangTidy
		#clangTidyAbseilModule
		#clangTidyAndroidModule
		#clangTidyBoostModule
		#clangTidyBugproneModule
		#clangTidyCERTModule
		#clangTidyCppCoreGuidelinesModule
		#clangTidyFuchsiaModule
		#clangTidyGoogleModule
		#clangTidyHICPPModule
		#clangTidyLLVMModule
		#clangTidyMiscModule
		#clangTidyModernizeModule
		#clangTidyMPIModule
		#clangTidyObjCModule
		#clangTidyPerformanceModule
		#clangTidyPlugin
		#clangTidyPortabilityModule
		#clangTidyReadabilityModule
		#clangTidyUtils
		#clangTidyZirconModule
		clangTooling
		clangToolingASTDiff
		clangToolingCore
		clangToolingInclusions
		clangToolingRefactor
		#DynamicLibraryLib
		#findAllSymbols    
  ${LINKER_GROUP_END}
		
	# LLVM libraries, as given by 'llvm-config --libs'
	LLVMLTO
	LLVMPasses
	LLVMObjCARCOpts
	LLVMSymbolize
	LLVMDebugInfoPDB
	LLVMDebugInfoDWARF
	LLVMFuzzMutate
	LLVMTableGen
	LLVMDlltoolDriver
	LLVMLineEditor
	LLVMOrcJIT
	LLVMCoverage
	LLVMMIRParser
	LLVMTestingSupport
	LLVMObjectYAML
	LLVMLibDriver
	LLVMOption
	gtest_main
	gtest
	LLVMWindowsManifest
	LLVMX86Disassembler
	LLVMX86AsmParser
	LLVMX86CodeGen
	LLVMGlobalISel
	LLVMSelectionDAG
	LLVMAsmPrinter
	LLVMX86Desc
	LLVMMCDisassembler
	LLVMX86Info
	LLVMX86AsmPrinter
	LLVMX86Utils
	LLVMMCJIT
	LLVMInterpreter
	LLVMExecutionEngine
	LLVMRuntimeDyld
	LLVMCodeGen
	LLVMTarget
	LLVMCoroutines
	LLVMipo
	LLVMInstrumentation
	LLVMVectorize
	LLVMScalarOpts
	LLVMLinker
	LLVMIRReader
	LLVMAsmParser
	LLVMInstCombine
	LLVMBitWriter
	LLVMAggressiveInstCombine
	LLVMTransformUtils
	LLVMAnalysis
	LLVMProfileData
	LLVMObject
	LLVMMCParser
	LLVMMC
	LLVMDebugInfoCodeView
	LLVMDebugInfoMSF
	LLVMBitReader
	LLVMCore
	LLVMBinaryFormat
	LLVMSupport
	LLVMDemangle
)
	

if(UNIX AND NOT APPLE)
  # Generic UNIX dependencies
	list(APPEND llvm7_LIBRARIES 
		rt
		tinfo
		z
		m
		dl
	)
elseif(APPLE)
  # macOS dependencies
  list(APPEND llvm7_LIBRARIES 
		z
		m
		dl
		ncurses	
	)
elseif(WIN32)
  # Windows dependencies
  list(APPEND llvm7_LIBRARIES 
		version
		z
	)	
endif()

