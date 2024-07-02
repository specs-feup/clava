cmake_minimum_required(VERSION 3.14)
project(ClangAstDumper)

set(CLANG_VERSION 12)

SET(CMAKE_C_COMPILER "clang-${CLANG_VERSION}")
SET(CMAKE_CXX_COMPILER "clang++-${CLANG_VERSION}")

SET(Clang_DIR "/usr/lib/cmake/clang-${CLANG_VERSION}")
SET(LLVM_DIR "/usr/lib/llvm-${CLANG_VERSION}/lib/cmake/llvm")


find_package(Clang REQUIRED CONFIG)
include_directories("/usr/lib/clang/${CLANG_VERSION}/include/")
include_directories("/usr/lib/llvm-${CLANG_VERSION}/include/")

file (GLOB ENUM_FILES CONFIGURE_DEPENDS
	"src/ClangEnums/enums_*.h"
	"src/ClangEnums/enums_*.cpp")

set(SOURCE_FILES
	src/plugin.cpp
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

add_library(MyPlugin MODULE ${SOURCE_FILES})
target_compile_features(MyPlugin PRIVATE cxx_std_20)


add_custom_target (
	clang_enums
	COMMAND node --experimental-default-type="module" clang_enums/Main.js ${CLANG_VERSION} src/
	WORKING_DIRECTORY ${CMAKE_SOURCE_DIR})
add_dependencies(MyPlugin clang_enums)