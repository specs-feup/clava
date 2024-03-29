cmake_minimum_required(VERSION 3.2)
project(ClangEnumGenerator)

# Initialize Deps
include("../../deps.cmake")

# Add Modules
#deps_find_package(llvm3.8 REQUIRED) # LLVM
#deps_find_package(llvm7 REQUIRED) # LLVM
deps_find_package(llvm12 REQUIRED) # LLVM

# Set base llvm dir
set(LLVM_DIR ${llvm12_DIR})

set(COMMON_CXX_FLAGS "-O3 -Wall -Wno-unused-function")

set(Clava_DIR "../../CMake")
find_package(Clava REQUIRED)

message(STATUS "LLVM DIR: ${LLVM_DIR}")

# Make enum extractor available
include(${CMAKE_CURRENT_LIST_DIR}/ExtractEnums.cmake)
     
#extract_enums("${LLVM_DIR}/src/include/clang/AST" "Type.h")
#extract_enums("${LLVM_DIR}/src/include/clang/AST" "OperationKinds.h")
#extract_enums("${LLVM_DIR}/src/include/clang/Basic" "AttrKinds.h")
extract_enums("${LLVM_DIR}/src/include/clang/Basic" "Specifiers.h")
#extract_enums("${LLVM_DIR}/src/include/clang/Basic" "ExceptionSpecificationType.h")
#extract_enums("${LLVM_DIR}/src/include/clang/Basic" "Linkage.h")
#extract_enums("${LLVM_DIR}/src/include/clang/Basic" "Visibility.h")
#extract_enums("${LLVM_DIR}/src/include/clang/AST" "TemplateBase.h")
#extract_enums("${LLVM_DIR}/src/include/clang/AST" "Decl.h")
#extract_enums("${LLVM_DIR}/src/include/clang/AST" "ExprCXX.h")
#extract_enums("${LLVM_DIR}/src/include/clang/Basic" "TypeTraits.h")
#extract_enums("${LLVM_DIR}/src/include/clang/AST" "NestedNameSpecifier.h")
#extract_enums("${LLVM_DIR}/src/include/clang/AST" "DeclCxx.h")
#extract_enums("${LLVM_DIR}/src/include/clang/Basic" "Lambda.h")
#extract_enums("${LLVM_DIR}/src/include/clang/AST" "TemplateName.h")
#extract_enums("${LLVM_DIR}/src/include/clang/AST" "Attr.h")
#extract_enums("${LLVM_DIR}/src/include/clang/AST" "Expr.h")

 
# Create target for clang/AST/Type.h
#add_library(Type_H "${LLVM_DIR}/src/include/clang/AST/Type.h")

#set_property(TARGET Type_H PROPERTY CXX_STANDARD 14)

# Because it is a header file, it cannot figure it out alone
#set_target_properties(Type_H PROPERTIES LINKER_LANGUAGE CXX)

# Add includes
#target_include_directories(Type_H
#       SYSTEM PUBLIC ${DEPS_INCLUDES}
#)

# Add enums helper
#clava_weave(Type_H "ClangEnumsGenerator.lara"  ARGS "header:'Type.h', outputFolder:'${CMAKE_CURRENT_LIST_DIR}/..'" JAVA_FLAGS -Xmx8000m)
