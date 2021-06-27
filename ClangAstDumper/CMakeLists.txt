cmake_minimum_required(VERSION 3.2)
project(ClangAstDumper)

#set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/bin")
#message(STATUS HELLO)


#SET(CMAKE_C_COMPILER "clang")
#SET(CMAKE_CXX_COMPILER "clang++")

# Initialize Deps
include("../deps.cmake")

# Add Modules
#deps_find_package(llvm3.8 REQUIRED) # LLVM
#deps_find_package(llvm7 REQUIRED) # LLVM
deps_find_package(llvm12 REQUIRED) # LLVM

set(COMMON_CXX_FLAGS "-std=c++14 -O3 -Wall -Wno-unused-function")
#-D__STDC_CONSTANT_MACROS -D__STDC_LIMIT_MACROS -DVISIT_CHECK

#add_definitions(-DOLD_OUTPUT)

if(SYSTEM_PLATFORM STREQUAL "MacOS")
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${COMMON_CXX_FLAGS} -stdlib=libc++")
else()
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${COMMON_CXX_FLAGS} -static-libgcc -static-libstdc++")
endif()


#Enable debug
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DDEBUG")

# For CentOS and macOS
# Set SYSTEM_PLATFORM in deps_config.cmake
if(SYSTEM_PLATFORM STREQUAL "Centos" OR SYSTEM_PLATFORM STREQUAL "MacOS")
  	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D__STDC_CONSTANT_MACROS -D__STDC_LIMIT_MACROS")
endif()


OPTION(USE_CLAVA "Uses Clava during compilation")

# Enable Clava
if(USE_CLAVA)
#	message(STATUS "Using Clava in this project")
	find_package(Clava REQUIRED)
#else()
#    message(STATUS "Not using Clava")
endif(USE_CLAVA)
unset(USE_CLAVA CACHE) # So that the value is refreshed


set(SOURCE_FILES
		main.cpp
		ClangAst.cpp
		ClangAstDumper.cpp
		ClangAstDumperTypes.cpp
		ClangAstDumperStmts.cpp
		ClangAstDumperDecls.cpp
        ClangAstDumperExtra.cpp
		TypeMarker.cpp
        ClavaDataDumper.cpp
        ClavaDataDumperDecls.cpp
    	ChildrenVisitorDecls.cpp
    	ClangNodes.cpp
		ClavaDataDumperStmts.cpp
		ClavaDataDumperTypes.cpp
		ClangAstDumperExprs.cpp
		ChildrenVisitorStmts.cpp
		ClavaConstants.cpp
		ChildrenVisitorTypes.cpp
		ClangEnums.cpp
		ClangastDumperAttr.cpp
		ChildrenVisitorAttrs.cpp
		ClavaDataDumperAttrs.cpp
		ChildrenVisitorExtras.cpp
		ClavaDecl.cpp
		enums_Type_h.cpp
		)

add_executable(ClangAstDumper ${SOURCE_FILES})

# Add includes
target_include_directories(ClangAstDumper
       SYSTEM PUBLIC ${DEPS_INCLUDES}
)

# Add enums helper

# Add Libraries
target_link_libraries(ClangAstDumper
        ${DEPS_LIBRARIES}
)
