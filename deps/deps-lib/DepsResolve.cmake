#
# Resolves a library, downloading the needed files from a host.
#
# ARGUMENTS:
#   - lib_name : the name of the library to resolve
#   - lib_dir : this variable will be used to store the value of the base folder of the downloaded artifacts for the given library
#
# EXAMPLE:
#   deps_resolve("HashTable")
#   Looks for a library 'HashTable' in the current hosts list
#


function(deps_resolve lib_name lib_dir)

	# Set to CMAKE_SYSTEM_NAME, if not defined
	if(DEFINED SYSTEM_PLATFORM)
		message(STATUS "Using custom platform '${SYSTEM_PLATFORM}'")
	else()
		set(SYSTEM_PLATFORM "${CMAKE_SYSTEM_NAME}")
	endif()

	if(NOT DEPS_ENABLED)
		message(FATAL_ERROR "'Deps' is not enabled, include 'deps.cmake' first")
	endif()
	
	# Check if folder for current system and compiler exist
	set(current_lib "${DEPS_ARTIFACTS_DIR}/${lib_name}-${SYSTEM_PLATFORM}-${DEPS_ENVIRONMENT}")
	set(${lib_dir} ${current_lib} PARENT_SCOPE)

	if ("${current_lib}" STREQUAL "")
		message( FATAL_ERROR "Could not set artifacts folder for library ${lib_name}")
	endif()
	
	if(NOT EXISTS "${current_lib}/")
		message("-- [Deps] Resolving dependency '${lib_name}'...")
		
		execute_process(COMMAND ${Java_JAVA_EXECUTABLE} -jar ${DEPS_JAR_DIR}/${DEPS_JAR} resolve ${DEPS_JAR_PROPS} ${lib_name} ${SYSTEM_PLATFORM} ${DEPS_ENVIRONMENT} ${DEPS_ARTIFACTS_DIR}
		WORKING_DIRECTORY ${CMAKE_CURRENT_LIST_DIR}
		RESULT_VARIABLE JAVA_RESULT
		OUTPUT_VARIABLE JAVA_OUTPUT
		ERROR_VARIABLE JAVA_OUTPUT)
	
		if(NOT ${JAVA_RESULT})
			#message("-- [Deps] Dependency resolved ${JAVA_OUTPUT}")
		else()
			message( FATAL_ERROR "Could not retrieve library '${lib_name}'. Reason:\n${JAVA_OUTPUT}" )
		endif()
	endif()

endfunction()
