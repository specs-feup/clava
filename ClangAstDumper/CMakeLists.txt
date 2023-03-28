cmake_minimum_required(VERSION 3.14)
project(ClangAstDumper)

set(LLVM_VERSION 14)

SET(CMAKE_C_COMPILER "clang-14")
SET(CMAKE_CXX_COMPILER "clang++-14")

find_package(Clang REQUIRED CONFIG)
include_directories(/usr/lib/llvm-14/include)


set(SOURCE_FILES
		src/newmain.cpp
		src/ClangAst.cpp
    	src/ClangNodes.cpp
		src/TypeMarker.cpp
        src/ClavaDataDumper.cpp
		src/ClavaDataDumperAttrs.cpp
        src/ClavaDataDumperDecls.cpp
		src/ClavaDataDumperStmts.cpp
		src/ClavaDataDumperTypes.cpp
		src/ClavaConstants.cpp
		src/ClavaDecl.cpp
		src/ChildrenVisitorAttrs.cpp
    	src/ChildrenVisitorDecls.cpp
		src/ChildrenVisitorExtras.cpp
		src/ChildrenVisitorStmts.cpp
		src/ChildrenVisitorTypes.cpp
		src/ClangAstDumper.cpp
		src/ClangAstDumperAttr.cpp
		src/ClangAstDumperDecls.cpp
		src/ClangAstDumperExprs.cpp
        src/ClangAstDumperExtra.cpp
		src/ClangAstDumperStmts.cpp
		src/ClangAstDumperTypes.cpp
		src/ClangEnums.cpp
		src/enums_Type_h.cpp
		src/enums_OperationKinds_h.cpp
		src/enums_AttrKinds_h.cpp
		src/enums_Specifiers_h.cpp
		src/enums_ExceptionSpecificationType_h.cpp
		src/enums_Linkage_h.cpp
		src/enums_Visibility_h.cpp
		src/enums_TemplateBase_h.cpp
		src/enums_Decl_h.cpp
		src/enums_ExprCXX_h.cpp
		src/enums_TypeTraits_h.cpp
		src/enums_NestedNameSpecifier_h.cpp
		src/enums_DeclCxx_h.cpp
		src/enums_Lambda_h.cpp
		src/enums_TemplateName_h.cpp
		src/enums_Attr_h.cpp
		src/enums_Expr_h.cpp
		)

add_library(MyPlugin SHARED ${SOURCE_FILES})

target_compile_features(MyPlugin PRIVATE cxx_std_17)
