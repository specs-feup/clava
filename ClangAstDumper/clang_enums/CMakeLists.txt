cmake_minimum_required(VERSION 3.2)
project(ClangEnumGenerator)

set(CLANG_VERSION 15)

SET(CMAKE_C_COMPILER "clang-${CLANG_VERSION}")
SET(CMAKE_CXX_COMPILER "clang++-${CLANG_VERSION}")

SET(Clang_DIR "/usr/lib/cmake/clang-${CLANG_VERSION}")
SET(LLVM_DIR "/usr/lib/llvm-${CLANG_VERSION}/lib/cmake/llvm")

# Add Modules
find_package(LLVM REQUIRED) # LLVM


set(COMMON_CXX_FLAGS "-O3 -Wall -Wno-unused-function")

set(Clava_DIR "../../CMake")
find_package(Clava REQUIRED)

message(STATUS "LLVM DIR: ${LLVM_DIR}")

# Create target for clang/AST/Type.h
add_library(EnumGen)

clava_weave(EnumGen "Main.js"  ARGS "llvmFolder:'${LLVM_DIR}', outputFolder:'${CMAKE_CURRENT_LIST_DIR}/..'" JAVA_FLAGS -Xmx8000m)
