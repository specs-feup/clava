cmake_minimum_required(VERSION 3.2)
project(ClangAstDumper)

#set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/bin")

# Initialize Deps
include("../deps.cmake")

# Add Modules
deps_find_package(llvm3.8 REQUIRED) # LLVM

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -O3 -static-libgcc -static-libstdc++")

#Enable debug
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DDEBUG")

# For Centos
# Set SYSTEM_PLATFORM in deps_config.cmake
if(SYSTEM_PLATFORM STREQUAL "Centos")
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D__STDC_CONSTANT_MACROS -D__STDC_LIMIT_MACROS")
endif()



set(SOURCE_FILES
		main.cpp
		ClangAst.cpp
		ClangAstDumper.cpp
		#ClangAstDumper.h
		ClangAstDumperTypes.cpp
		ClangAstDumperStmts.cpp
		ClangAstDumperDecls.cpp
		ClangAstDumperExtra.cpp
		TypeMarker.cpp
		#TypeMarker.h
		#ClangAstDumperConstants.h
		#TEst.h
		)

add_executable(ClangAstDumper ${SOURCE_FILES})

# Add includes
target_include_directories(ClangAstDumper
       PUBLIC ${DEPS_INCLUDES}
)

# Add Libraries
target_link_libraries(ClangAstDumper
        ${DEPS_LIBRARIES}
        )
