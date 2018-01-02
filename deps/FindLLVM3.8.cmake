cmake_minimum_required(VERSION 3.2)

# Download library
deps_resolve("llvm3.8" LIB_DIR)

# LLVM requires C++11
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

if(UNIX)
	# Unix version uses pre-build clang binaries, which disable rtti
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -pthread -fno-rtti")
endif()


#LLVM includes
list(APPEND llvm3.8_INCLUDES
	"${LIB_DIR}/src/include"
	"${LIB_DIR}/build/include"
)

# Clang includes
list(APPEND llvm3.8_INCLUDES
	"${LIB_DIR}/src/tools/clang/include"
	"${LIB_DIR}/build/tools/clang/include"
)

# LLVM + Clang libs
# Using link_directories because of circular dependencies in Clang libraries.
# Ideally we would use 'find_libraries'
LINK_DIRECTORIES("${LIB_DIR}/build/lib")

#find_libraries(llvm3.8_LIBRARIES "${LIB_DIR}/build/lib" 
list(APPEND llvm3.8_LIBRARIES 
	
	# Clang libraries, they have circular dependencies, so they are inside a group
	-Wl,--start-group
        clangAnalysis
        #clangApplyReplacements
        clangARCMigrate
        clangAST
        clangASTMatchers
        clangBasic
        clangCodeGen
        clangDriver
        clangDynamicASTMatchers
        clangEdit
        clangFormat
        clangFrontend
        clangFrontendTool
        clangIndex
        clangLex
        clangParse
        #clangQuery
        #clangRename
        clangRewrite
        clangRewriteFrontend
        clangSema
        clangSerialization
        clangStaticAnalyzerCheckers
        clangStaticAnalyzerCore
        clangStaticAnalyzerFrontend
        #clangTidy
        #clangTidyCERTModule
        #clangTidyCppCoreGuidelinesModule
        #clangTidyGoogleModule
        #clangTidyLLVMModule
        #clangTidyMiscModule
        #clangTidyModernizeModule
        #clangTidyPerformanceModule
        #clangTidyReadabilityModule
        #clangTidyUtils
        clangTooling
        clangToolingCore
	-Wl,--end-group
		
	# LLVM libraries, as given by 'llvm-config --libs'
	LLVMLTO
	LLVMObjCARCOpts
	LLVMMIRParser
	LLVMSymbolize
	LLVMDebugInfoPDB
	LLVMDebugInfoDWARF
	LLVMTableGen
	LLVMLineEditor
	LLVMOrcJIT
	LLVMCppBackendCodeGen
	LLVMCppBackendInfo
	LLVMARMDisassembler
	LLVMARMCodeGen
	LLVMARMAsmParser
	LLVMARMDesc
	LLVMARMInfo
	LLVMARMAsmPrinter
	LLVMLibDriver
	LLVMOption
	LLVMX86Disassembler
	LLVMX86AsmParser
	LLVMX86CodeGen
	LLVMSelectionDAG
	LLVMAsmPrinter
	LLVMX86Desc
	LLVMMCDisassembler
	LLVMX86Info
	LLVMX86AsmPrinter
	LLVMX86Utils
	LLVMMCJIT
	LLVMPasses
	LLVMipo
	LLVMVectorize
	LLVMLinker
	LLVMIRReader
	LLVMAsmParser
	LLVMDebugInfoCodeView
	LLVMInterpreter
	LLVMExecutionEngine
	LLVMRuntimeDyld
	LLVMCodeGen
	LLVMTarget
	LLVMScalarOpts
	LLVMInstCombine
	LLVMInstrumentation
	LLVMProfileData
	LLVMObject
	LLVMMCParser
	LLVMTransformUtils
	LLVMMC
	LLVMBitWriter
	LLVMBitReader
	LLVMAnalysis
	LLVMCore
	LLVMSupport
)
	
	
	
	

if(UNIX)
	list(APPEND llvm3.8_LIBRARIES 
		rt
		tinfo
		z
		m
		dl
	
		# Needed by LLVMSupport
		# CentOS does not support this, you can comment this
		#c++abi
	)
endif()
