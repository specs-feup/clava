cmake_minimum_required(VERSION 3.2)
project(TranslationUnitErrorDumper)

#set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/bin")
#message(STATUS HELLO)

set(CMAKE_CXX_STANDARD 11)

#SET(CMAKE_C_COMPILER "clang")
#SET(CMAKE_CXX_COMPILER "clang++")

# Initialize Deps
include("../deps.cmake")

# Add Modules
#deps_find_package(llvm3.8 REQUIRED) # LLVM
deps_find_package(llvm7 REQUIRED) # LLVM

set(COMMON_CXX_FLAGS "-std=c++11 -O3 -Wall -Wno-unused-function")
#set(COMMON_CXX_FLAGS "-std=c++11 -g -O1 -Wall -Wno-unused-function")
#-D__STDC_CONSTANT_MACROS -D__STDC_LIMIT_MACROS

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
        main.cpp DiagnosticAction.cpp DiagnosticAction.h DiagnosticConsumer.cpp DiagnosticConsumer.h)

add_executable(TranslationUnitErrorDumper ${SOURCE_FILES})

# Add includes
target_include_directories(TranslationUnitErrorDumper
        SYSTEM PUBLIC ${DEPS_INCLUDES}
        )

# Add enums helper

# Add Libraries
target_link_libraries(TranslationUnitErrorDumper
        ${DEPS_LIBRARIES}
        )
