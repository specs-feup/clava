cmake_minimum_required(VERSION 3.3)

# Generate files based on the source code of the current target, 
# add generated files as a new target that depends on the original target.
#
# Automatic code generation is disabled, only manually generated files in the
# given aspect are considered as generated code, i.e., calls to $file.write
#
# Parameter 1: The target that the LARA file will be applied to
# Parameter 2: Name of the new target that will represent the generated code
# Parameter 3: A LARA file
# Parameter ARGS (one value): A string with the aspect arguments e.g., "inputFile:'data.json', execute:true, iterations:10"
# Parameter FLAGS (multi-value): A list of flags that will be passed to Clava during execution
function(clava_generate ORIG_TARGET GENERATED_TARGET ASPECT) 

    set(options)
    set(oneValueArgs ASPECT_ARGS)
    set(multiValueArgs WEAVER_ARGS)

    cmake_parse_arguments(PARSE_ARGV 3 CLAVA_GENERATE "${options}" "${oneValueArgs}" "${multiValueArgs}")

	#message(STATUS "ASPECT_ARGS: " ${CLAVA_GENERATE_ASPECT_ARGS})
	#message(STATUS "WEAVER_ARGS: " ${CLAVA_GENERATE_WEAVER_ARGS})

	# Create aspect arguments
	if(NOT ("${CLAVA_GENERATE_ARGS}" STREQUAL ""))
		set(ASPECT_ARGS_FLAG "-av")
		set(ASPECT_ARGS "{${CLAVA_GENERATE_ARGS}}")	
	else()
		set(ASPECT_ARGS_FLAG "")
		set(ASPECT_ARGS "")	
	endif()

	
	# Aspect arguments
	#if(ARGC GREATER 3)
	#	set(ASPECT_ARGS_FLAG "-av")
	#	set(ASPECT_ARGS "{${ARGV3}}")
	#else()
	#	set(ASPECT_ARGS_FLAG "")
	#	set(ASPECT_ARGS "")
	#endif()
	
	#if(ARGC GREATER 4)
	#	set(GENERATED_TARGET_DEPENDENCIES "${ARGV4}")
	#else()
	#	set(GENERATED_TARGET_DEPENDENCIES "")
	#endif()
	
	# get build dir
	set(BUILD_DIR "${CMAKE_CURRENT_BINARY_DIR}")
	
	#message(STATUS "DEPENDENCIES: ${GENERATED_TARGET_DEPENDENCIES}")

	#message(STATUS "ASPECT ARGS: ${ASPECT_ARGS}")
	
	# get CMakeLists.txt dir
	clava_get_target_property(ORIG_CMAKE_DIR ${ORIG_TARGET} SOURCE_DIR)
	#message(STATUS "ORIG_CMAKE_DIR: ${ORIG_CMAKE_DIR}")
	
	# set absolute path of aspect file relative to the source folder, in case path is relative
	if(NOT IS_ABSOLUTE ${ASPECT})
		set(ASPECT "${ORIG_CMAKE_DIR}/${ASPECT}")	
	endif()

	#message(STATUS "ASPECT: ${ASPECT}")

	# get woven directory path
	#set(WOVEN_DIR_NAME "clava_${ORIG_TARGET}")
	#message(STATUS "WOVEN_DIR_NAME: ${WOVEN_DIR_NAME}")

	
	#set(WOVEN_DIR "${ORIG_CMAKE_DIR}/${WOVEN_DIR_NAME}")


	# woven dir name
	set(WOVEN_DIR_NAME "woven")
	
	# working dir
	#string(TIMESTAMP CURRENT_TIMESTAMP)
	#string(MAKE_C_IDENTIFIER ${CURRENT_TIMESTAMP} TIME_ID)
	set(WORKING_DIR "${BUILD_DIR}/${GENERATED_TARGET}")
	set(WOVEN_DIR "${WORKING_DIR}/${WOVEN_DIR_NAME}")
	file(MAKE_DIRECTORY ${WOVEN_DIR})
	#message(STATUS "WOVEN_DIR: ${WOVEN_DIR}")	
	
	# get original source files
	clava_get_target_property(ORIG_SOURCES ${ORIG_TARGET} SOURCES)
	#message(STATUS "ORIG_SOURCES: ${ORIG_SOURCES}")
	
	# process original source file list
	string(REGEX REPLACE "([^;]+)" "${ORIG_CMAKE_DIR}/\\1" PROC_ORIG_SOURCES "${ORIG_SOURCES}")
	
	# No longer needed, now Clava always uses ;
	#if(NOT "${CMAKE_HOST_SYSTEM}" MATCHES ".*Windows.*")
	#	string(REGEX REPLACE ";" ":" PROC_ORIG_SOURCES "${PROC_ORIG_SOURCES}")
	#endif()
	#message(STATUS "PROC_ORIG_SOURCES: ${PROC_ORIG_SOURCES}")
	
	
	# get original include folders
	clava_get_target_property(ORIG_INCLUDES ${ORIG_TARGET} INCLUDE_DIRECTORIES)
	set(INCLUDE_HEADERS_FLAG "-ih")
    if((${ORIG_INCLUDES} MATCHES "ORIG_INCLUDES-NOTFOUND") OR ("${ORIG_INCLUDES}" STREQUAL ""))
        set(INCLUDE_HEADERS_FLAG "")
        set(ORIG_INCLUDES "")
    endif()
	#message(STATUS "ORIG_INCLUDES: ${ORIG_INCLUDES}")

	
	# process original includes list
	string(REGEX REPLACE "([^;]+)" "\\1" PROC_ORIG_INCLUDES "${ORIG_INCLUDES}")
	#if(NOT "${CMAKE_HOST_SYSTEM}" MATCHES ".*Windows.*")
	#	string(REGEX REPLACE ";" ":" PROC_ORIG_INCLUDES "${PROC_ORIG_INCLUDES}")
	#endif()
	#message(STATUS "PROC_ORIG_INCLUDES: ${PROC_ORIG_INCLUDES}")
	
	
	
	# make the cmake configuration depend on the LARA file
	set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS "${ASPECT}")
	message(STATUS "ASPECT: ${ASPECT}")

	
	# mark Clava output directory as a target for 'make clean'
	set_property(DIRECTORY APPEND PROPERTY ADDITIONAL_MAKE_CLEAN_FILES "${WOVEN_DIR}")


	# use the source files as dependencies. Can only be files that can be built (e.g., no aspects)
	set(GENERATOR_DEPENDENCIES "${PROC_ORIG_SOURCES}")
	
	# if no dependencies given, use source files
	#if("${GENERATED_TARGET_DEPENDENCIES}" STREQUAL "")
	#	set(GENERATOR_DEPENDENCIES "${PROC_ORIG_SOURCES}")
	# otherwise, make generator depend on given files
	#else()
	#	set(GENERATOR_DEPENDENCIES "${GENERATED_TARGET_DEPENDENCIES}")
	#endif()
	#message(STATUS "clava_generate dependencies: ${GENERATOR_DEPENDENCIES}")


	# If generation has not run yet, run it for the first time, during configuration time
	if(NOT EXISTS "${WOVEN_DIR}/clava_generated_files.txt")
		message(STATUS "Generating source code for target '${GENERATED_TARGET}' for the first time")
		execute_process(
			COMMAND ${CLAVA_CMD} ${ASPECT} ${ASPECT_ARGS_FLAG} ${ASPECT_ARGS} --cmake -s -b 2 -p "${PROC_ORIG_SOURCES}" -o ${WORKING_DIR} -of ${WOVEN_DIR_NAME} ${INCLUDE_HEADERS_FLAG} "${PROC_ORIG_INCLUDES}" -ncg ${CLAVA_GENERATE_FLAGS}
			#WORKING_DIRECTORY ${ORIG_CMAKE_DIR} 
			#WORKING_DIRECTORY ${BUILD_DIR} 			
			WORKING_DIRECTORY ${WORKING_DIR} 			
		)	
	endif()
	
	add_custom_command(OUTPUT "${WOVEN_DIR}/clava_generated_files.txt"
		COMMAND ${CLAVA_CMD} ${ASPECT} ${ASPECT_ARGS_FLAG} ${ASPECT_ARGS} --cmake -s -b 2 -p "${PROC_ORIG_SOURCES}" -o ${WORKING_DIR} -of ${WOVEN_DIR_NAME} INCLUDE_HEADERS_FLAG "${PROC_ORIG_INCLUDES}" -ncg ${CLAVA_GENERATE_WEAVER_FLAGS}
		#WORKING_DIRECTORY ${ORIG_CMAKE_DIR} 
		#WORKING_DIRECTORY ${BUILD_DIR} 
		WORKING_DIRECTORY ${WORKING_DIR} 
		DEPENDS ${GENERATOR_DEPENDENCIES}
		COMMENT "Generating source code for target '${GENERATED_TARGET}'"
	)

	# checks if re-generation of target '${GENERATED_TARGET}' is required"
	set("${GENERATED_TARGET}_checker" "${GENERATED_TARGET}_dependencies")
	ADD_CUSTOM_TARGET("${GENERATED_TARGET}_checker"
		DEPENDS "${WOVEN_DIR}/clava_generated_files.txt" 
	)
	
	
	#message(STATUS "Clava stdout: ${CLAVA_STDOUT}")
	#message(STATUS "Clava stderr: ${CLAVA_STDERR}")	

	
	# read new sources
	if(EXISTS "${WOVEN_DIR}/clava_generated_files.txt")
        file(READ "${WOVEN_DIR}/clava_generated_files.txt" CLAVA_WOVEN_SOURCES)
        string(STRIP "${CLAVA_WOVEN_SOURCES}" CLAVA_WOVEN_SOURCES)
        set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS "${CLAVA_WOVEN_SOURCES}" )

        #message(STATUS "CLAVA_WOVEN_SOURCES: ${CLAVA_WOVEN_SOURCES}")
	else()
		message(FATAL_ERROR "Could not find Clava file 'clava_generated_files.txt'")
	endif()
	
	# read new includes
	if(EXISTS "${WOVEN_DIR}/clava_include_dirs.txt")
        file(READ "${WOVEN_DIR}/clava_include_dirs.txt" CLAVA_INCLUDE_DIRS)
        string(STRIP "${CLAVA_INCLUDE_DIRS}" CLAVA_INCLUDE_DIRS)

        #message(STATUS "CLAVA_INCLUDE_DIRS: ${CLAVA_INCLUDE_DIRS}")
	else()
		message(FATAL_ERROR "Could not find Clava file 'clava_include_dirs.txt'")
	endif()
	

	# add new sources
	add_library(${GENERATED_TARGET} "${CLAVA_WOVEN_SOURCES}")

	# add new include directories
	target_include_directories(${GENERATED_TARGET} PUBLIC "${CLAVA_INCLUDE_DIRS}")
	target_include_directories(${GENERATED_TARGET} PUBLIC "${ORIG_INCLUDES}")
	

	clava_get_target_property(GEN_INCLUDES ${GENERATED_TARGET} INCLUDE_DIRECTORIES)
	#message(STATUS "Generated Includes: ${GEN_INCLUDES}")
	

	add_dependencies(${GENERATED_TARGET} "${GENERATED_TARGET}_checker")

	# create variables with include directories of generate target
	set(${GENERATED_TARGET}_INCLUDE_DIRS "${CLAVA_INCLUDE_DIRS}" PARENT_SCOPE)
	#message(STATUS "Target '${GENERATED_TARGET}' include dirs: ${${GENERATED_TARGET}_INCLUDE_DIRS}")
	
	# include generated target as part of the link libraries
	set(${GENERATED_TARGET}_LIBRARIES ${GENERATED_TARGET} PARENT_SCOPE)
	#message(STATUS "Target '${GENERATED_TARGET}' libraries: ${${GENERATED_TARGET}_LIBRARIES}")
	
endfunction(clava_generate)
