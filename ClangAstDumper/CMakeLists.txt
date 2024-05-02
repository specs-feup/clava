cmake_minimum_required(VERSION 3.14)
project(ClangAstDumper)

set(CLANG_VERSION 14)

SET(CMAKE_C_COMPILER "clang-${CLANG_VERSION}")
SET(CMAKE_CXX_COMPILER "clang++-${CLANG_VERSION}")

#SET(CMAKE_FIND_PACKAGE_SORT_ORDER NATURAL)
#SET(CMAKE_FIND_PACKAGE_SORT_DIRECTION DEC)

SET(Clang_DIR "/usr/lib/cmake/clang-${CLANG_VERSION}")
SET(LLVM_DIR "/usr/lib/llvm-${CLANG_VERSION}/lib/cmake/llvm")


find_package(Clang REQUIRED CONFIG)
include_directories("/usr/lib/clang/${CLANG_VERSION}/include/")
include_directories("/usr/lib/llvm-${CLANG_VERSION}/include/")


set(SOURCE_FILES
	src/plugin.cpp
	src/Clang/ClangAst.cpp
	src/Clang/ClangNodes.cpp
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
	src/ClangEnums/ClangEnums.cpp
	src/ClangEnums/enums_Type_h.cpp
	src/ClangEnums/enums_OperationKinds_h.cpp
	src/ClangEnums/enums_AttrKinds_h.cpp
	src/ClangEnums/enums_Specifiers_h.cpp
	src/ClangEnums/enums_ExceptionSpecificationType_h.cpp
	src/ClangEnums/enums_Linkage_h.cpp
	src/ClangEnums/enums_Visibility_h.cpp
	src/ClangEnums/enums_TemplateBase_h.cpp
	src/ClangEnums/enums_Decl_h.cpp
	src/ClangEnums/enums_ExprCXX_h.cpp
	src/ClangEnums/enums_TypeTraits_h.cpp
	src/ClangEnums/enums_NestedNameSpecifier_h.cpp
	src/ClangEnums/enums_DeclCXX_h.cpp
	src/ClangEnums/enums_Lambda_h.cpp
	src/ClangEnums/enums_TemplateName_h.cpp
	src/ClangEnums/enums_Attr_h.cpp
	src/ClangEnums/enums_Expr_h.cpp
	)

add_library(MyPlugin SHARED ${SOURCE_FILES})

target_compile_features(MyPlugin PRIVATE cxx_std_17)
