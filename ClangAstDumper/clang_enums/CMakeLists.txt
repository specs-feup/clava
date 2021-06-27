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


set(COMMON_CXX_FLAGS "-std=c++14 -O3 -Wall -Wno-unused-function")


#Enable debug
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DDEBUG")

# For CentOS and macOS
# Set SYSTEM_PLATFORM in deps_config.cmake
if(SYSTEM_PLATFORM STREQUAL "Centos" OR SYSTEM_PLATFORM STREQUAL "MacOS")
  	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D__STDC_CONSTANT_MACROS -D__STDC_LIMIT_MACROS")
endif()


set(Clava_DIR "../../CMake")
find_package(Clava REQUIRED)

message(STATUS "LLVM DIR: ${LLVM_DIR}")

# Create target for clang/AST/Type.h
add_library(Type_H "${LLVM_DIR}/src/include/clang/AST/Type.h")

# Because it is a header file, it cannot figure it out alone
set_target_properties(Type_H PROPERTIES LINKER_LANGUAGE CXX)

# Add includes
target_include_directories(Type_H
       SYSTEM PUBLIC ${DEPS_INCLUDES}
)

# Add enums helper
clava_weave(Type_H "ClangEnumsGenerator.lara"  ARGS "header:'Type.h', outputFolder:${CMAKE_CURRENT_LIST_DIR}" JAVA_FLAGS -Xmx8000m)
