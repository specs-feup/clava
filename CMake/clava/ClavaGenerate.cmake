cmake_minimum_required(VERSION 3.3)

# Generate files based on the source code of the current target, 
# add generated files as a new target that depends on the original target.
#
# Automatic code generation is disabled, only manually generated files in the
# given aspect are considered as generated code, i.e., calls to $file.write
#
# Parameter 1: ORIG_TARGET
# Parameter 2: GENERATED_TARGET 
# Parameter 3: ASPECT
# Optional parameter: Aspect arguments e.g., "inputFile:'data.json', execute:true, iterations:10"
# Optional parameter: List of sources that trigger the generation of code. If empty, code is generated everytime ORIG_TARGET is modified
function(clava_generate ORIG_TARGET GENERATED_TARGET ASPECT) 

	# Aspect arguments
	if(ARGC GREATER 3)
		set(ASPECT_ARGS_FLAG "-av")
		set(ASPECT_ARGS "{${ARGV3}}")
	else()
		set(ASPECT_ARGS_FLAG "")
		set(ASPECT_ARGS "")
	endif()
	
	if(ARGC GREATER 4)
		set(GENERATED_TARGET_DEPENDENCIES "${ARGV4}")
	else()
		set(GENERATED_TARGET_DEPENDENCIES "")
	endif()
	
	# get build dir
	set(BUILD_DIR "${CMAKE_CURRENT_BINARY_DIR}")
	
	#message(STATUS "DEPENDENCIES: ${GENERATED_TARGET_DEPENDENCIES}")

	#message(STATUS "ASPECT ARGS: ${ASPECT_ARGS}")
	
	# get CMakeLists.txt dir
	get_target_property(ORIG_CMAKE_DIR ${ORIG_TARGET} SOURCE_DIR)
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
	#message(STATUS "WOVEN_DIR: ${WOVEN_DIR}")	
	
	# get original source files
	get_target_property(ORIG_SOURCES ${ORIG_TARGET} SOURCES)
	#message(STATUS "ORIG_SOURCES: ${ORIG_SOURCES}")
	
	# process original source file list
	string(REGEX REPLACE "([^;]+)" "${ORIG_CMAKE_DIR}/\\1" PROC_ORIG_SOURCES "${ORIG_SOURCES}")
	if(NOT "${CMAKE_HOST_SYSTEM}" MATCHES ".*Windows.*")
		string(REGEX REPLACE ";" ":" PROC_ORIG_SOURCES "${PROC_ORIG_SOURCES}")
	endif()
	#message(STATUS "PROC_ORIG_SOURCES: ${PROC_ORIG_SOURCES}")
	
	
	# get original include folders
	get_target_property(ORIG_INCLUDES ${ORIG_TARGET} INCLUDE_DIRECTORIES)
	set(INCLUDE_HEADERS_FLAG "-ih")
    if((${ORIG_INCLUDES} MATCHES "ORIG_INCLUDES-NOTFOUND") OR ("${ORIG_INCLUDES}" STREQUAL ""))
        set(INCLUDE_HEADERS_FLAG "")
        set(ORIG_INCLUDES "")
    endif()
	#message(STATUS "ORIG_INCLUDES: ${ORIG_INCLUDES}")

	
	# process original includes list
	string(REGEX REPLACE "([^;]+)" "\\1" PROC_ORIG_INCLUDES "${ORIG_INCLUDES}")
	if(NOT "${CMAKE_HOST_SYSTEM}" MATCHES ".*Windows.*")
		string(REGEX REPLACE ";" ":" PROC_ORIG_INCLUDES "${PROC_ORIG_INCLUDES}")
	endif()
	#message(STATUS "PROC_ORIG_INCLUDES: ${PROC_ORIG_INCLUDES}")
	
	
	
	# make the cmake configuration depend on the LARA file
	set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS "${ASPECT}")
	
	# mark Clava output directory as a target for 'make clean'
	set_property(DIRECTORY APPEND PROPERTY ADDITIONAL_MAKE_CLEAN_FILES "${WOVEN_DIR}")


	# if no dependencies given, use source files
	if("${GENERATED_TARGET_DEPENDENCIES}" STREQUAL "")
		set(GENERATOR_DEPENDENCIES "${PROC_ORIG_SOURCES}")
	# otherwise, make generator depend on given files
	else()
		set(GENERATOR_DEPENDENCIES "${GENERATED_TARGET_DEPENDENCIES}")
	endif()
	#message(STATUS "clava_generate dependencies: ${GENERATOR_DEPENDENCIES}")


	# If generation has not run yet, run it for the first time, during configuration time
	if(NOT EXISTS "${WOVEN_DIR}/clava_generated_files.txt")
		message(STATUS "Generating source code for target '${GENERATED_TARGET}' for the first time")
		execute_process(
			COMMAND java -jar "${CLAVA_JAR_PATH}" ${ASPECT} ${ASPECT_ARGS_FLAG} ${ASPECT_ARGS} --cmake -s -b 2 -p "${PROC_ORIG_SOURCES}" -o ${WORKING_DIR} -of ${WOVEN_DIR_NAME} ${INCLUDE_HEADERS_FLAG} ${PROC_ORIG_INCLUDES} -ncg
			#WORKING_DIRECTORY ${ORIG_CMAKE_DIR} 
			#WORKING_DIRECTORY ${BUILD_DIR} 			
			WORKING_DIRECTORY ${WORKING_DIR} 			
		)	
	endif()
	
	add_custom_command(OUTPUT "${WOVEN_DIR}/clava_generated_files.txt"
		COMMAND java -jar "${CLAVA_JAR_PATH}" ${ASPECT} ${ASPECT_ARGS_FLAG} ${ASPECT_ARGS} --cmake -s -b 2 -p "${PROC_ORIG_SOURCES}" -o ${WORKING_DIR} -of ${WOVEN_DIR_NAME} INCLUDE_HEADERS_FLAG ${PROC_ORIG_INCLUDES} -ncg
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
        set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS "${CLAVA_WOVEN_SOURCES}")

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
	

	get_target_property(GEN_INCLUDES ${GENERATED_TARGET} INCLUDE_DIRECTORIES)
	#message(STATUS "Generated Includes: ${GEN_INCLUDES}")
	

	add_dependencies(${GENERATED_TARGET} "${GENERATED_TARGET}_checker")

	# create variables with include directories of generate target
	set(${GENERATED_TARGET}_INCLUDE_DIRS "${CLAVA_INCLUDE_DIRS}" PARENT_SCOPE)
	#message(STATUS "Target '${GENERATED_TARGET}' include dirs: ${${GENERATED_TARGET}_INCLUDE_DIRS}")
	
	# include generated target as part of the link libraries
	set(${GENERATED_TARGET}_LIBRARIES ${GENERATED_TARGET} PARENT_SCOPE)
	#message(STATUS "Target '${GENERATED_TARGET}' libraries: ${${GENERATED_TARGET}_LIBRARIES}")
	
endfunction(clava_generate)
