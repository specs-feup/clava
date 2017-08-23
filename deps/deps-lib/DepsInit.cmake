#
# OPTIONS - can be set before calling the module
#  
#
# SET VARIABLES - variables set by this module
#  - DEPS_ENABLED: Flag that indicates this module was executed
#  - DEPS_COMPILER: The name of the compiler used to build the compiled libraries (e.g., gcc-4).
#  - DEPS_ENVIRONMENT: An id for the compilation platform of the compiled libraries
#  - DEPS_ARTIFACTS_DIR: The folder to where artifacts will be downloaded
#  - DEPS_JAR: name of support JAR executable
#  - DEPS_JAR_DIR: path to support jar
#  - DEPS_JAR_PROPS: Java properties file used by the support JAR
#
# Additionally, when including dependencies, the following variables should be set
#  - DEPS_INCLUDES: All include directories of dependencies
#  - DEPS_LIBRARIES: All libraries dependencies need for linking
#  - DEPS_DLLS_[WIN32 | UNIX]: Contains folders with dynamic libraries that should be copied to where the executable is
#

cmake_minimum_required(VERSION 3.2)

find_package(Java REQUIRED)

# Set flag, to signal that deps is enabled
set(DEPS_ENABLED 1)


## Define compiler name
if(NOT DEPS_COMPILER)
	# Check GCC
	if("${CMAKE_C_COMPILER_ID}" MATCHES "GNU")
		# Check GCC version
		if (CMAKE_CXX_COMPILER_VERSION VERSION_GREATER 5)
			set(DEPS_COMPILER "gcc5")
		elseif(CMAKE_CXX_COMPILER_VERSION VERSION_GREATER 4)
			set(DEPS_COMPILER "gcc4")
		else()
			message(FATAL_ERROR "-- [Deps] gcc version '${CMAKE_C_COMPILER_VERSION}' not supported (use gcc-4 or gcc-5)")		
		endif()
	else()
		message(FATAL_ERROR "-- [Deps] Compiler not supported: '${CMAKE_C_COMPILER_ID}'")
	endif()
endif()

# Define compilation environment
set(DEPS_ENVIRONMENT "${DEPS_COMPILER}")

message("-- [Deps] Setting compiled library environment to '${DEPS_ENVIRONMENT}'")


# Add the modules in the current path 
set(CMAKE_MODULE_PATH "${CMAKE_CURRENT_LIST_DIR};${CMAKE_CURRENT_LIST_DIR}/..;${CMAKE_MODULE_PATH}")

set(DEPS_ARTIFACTS_DIR "${CMAKE_CURRENT_LIST_DIR}/../artifacts")

# Load find_libraries
include("FindLibraries")

# Load deps_find_package
include("DepsFindPackage")

# Load make_package_target
include("MakePackageTarget")

# Load deps_resolve
include("DepsResolve")

# Set name of support jar
set(DEPS_JAR "deps-support.jar")

# Set path to support jar 
set(DEPS_JAR_DIR "${CMAKE_CURRENT_LIST_DIR}")

# Set support jar properties
set(DEPS_JAR_PROPS "${DEPS_JAR_DIR}/deps.properties")
