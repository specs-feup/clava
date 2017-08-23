#
# Calls find_package and updates variables DEPS_INCLUDES and DEPS_LIBRARIES.
#
# Using a macro so that variables from find_package are exported.
#
# ARGUMENTS:
#	- Receives the same arguments as find_package. The first argument must be the name of the library
#
# EXAMPLE:
#   deps_find_package(HashTable REQUIRED)
#   Looks for a library 'HashTable' in the current hosts list
#


macro(deps_find_package lib_name)

	if(NOT DEPS_ENABLED)
		message(FATAL_ERROR "'Deps' is not enabled, include 'deps.cmake' first")
	endif()

	# Call Find package
	find_package(${lib_name} ${ARGN})

	# Update DEPS_INCLUDES
	list(APPEND DEPS_INCLUDES
		${${lib_name}_INCLUDES}
	)
	
	# Update DEPS_LIBRARIES
	list(APPEND DEPS_LIBRARIES 
		${${lib_name}_LIBRARIES}
	)

endmacro()
