#
# Bootstraps Deps
#
cmake_minimum_required(VERSION 3.2)

#Initialize deps
if(NOT DEPS_ENABLED)
	include("${CMAKE_CURRENT_LIST_DIR}/deps_config.cmake")
	include("${CMAKE_CURRENT_LIST_DIR}/deps/deps-lib/DepsInit.cmake")
endif()
