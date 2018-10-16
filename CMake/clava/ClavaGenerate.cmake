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
	
		message(STATUS "DEPENDENCIES: ${GENERATED_TARGET_DEPENDENCIES}")

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
	set(WOVEN_DIR_NAME "clava_${ORIG_TARGET}")
	#message(STATUS "WOVEN_DIR_NAME: ${WOVEN_DIR_NAME}")

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
	#message(STATUS "ORIG_INCLUDES: ${ORIG_INCLUDES}")

	
	# process original includes list
	string(REGEX REPLACE "([^;]+)" "\\1" PROC_ORIG_INCLUDES "${ORIG_INCLUDES}")
	if(NOT "${CMAKE_HOST_SYSTEM}" MATCHES ".*Windows.*")
		string(REGEX REPLACE ";" ":" PROC_ORIG_INCLUDES "${PROC_ORIG_INCLUDES}")
	endif()
	#message(STATUS "PROC_ORIG_INCLUDES: ${PROC_ORIG_INCLUDES}")
	
	set(WOVEN_DIR "${ORIG_CMAKE_DIR}/${WOVEN_DIR_NAME}")
	
	# get original include directories
	#get_target_property(ORIG_INC_DIRS ${ORIG_TARGET} INCLUDE_DIRECTORIES)
	#message(STATUS "ORIG_INC_DIRS: ${ORIG_INC_DIRS}")
	
	# make the cmake configuration depend on the LARA file
	#set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS "${ASPECT};${ORIG_SOURCES}")
	set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS "${ASPECT}")
	
	# mark Clava output directory as a target for 'make clean'
	set_property(DIRECTORY APPEND PROPERTY ADDITIONAL_MAKE_CLEAN_FILES "${WOVEN_DIR}")
	
	#set(CLAVA_COMMAND "java -jar ${CLAVA_JAR_PATH} ${ASPECT} --cmake -b 2 -p ${PROC_ORIG_SOURCES} -of ${WOVEN_DIR_NAME}")
	
	# execute Clava (TODO: set correct standard from cmake) (TODO: set correct clava jar)
	#message(STATUS "Running command: java -jar ${CLAVA_JAR_PATH} ${ASPECT} ${ASPECT_ARGS_FLAG} ${ASPECT_ARGS} --cmake -b 2 -p ${PROC_ORIG_SOURCES} -of ${WOVEN_DIR_NAME} -ih ${PROC_ORIG_INCLUDES} -ncg")

	# if file with generated sources does not exist, run process
	#if(NOT EXISTS "${WOVEN_DIR}/clava_generated_files.txt")
	#	set(EXECUTE_CLAVA TRUE)
	# if file exists, check time-stamp
	#else()
	#	set(EXECUTE_CLAVA FALSE)
	#endif()
	
	#if(EXECUTE_CLAVA)
	#	execute_process(
	#		COMMAND java -jar "${CLAVA_JAR_PATH}" ${ASPECT} ${ASPECT_ARGS_FLAG} ${ASPECT_ARGS} --cmake -b 2 -p "${PROC_ORIG_SOURCES}" -of "${WOVEN_DIR_NAME}" -ih "${PROC_ORIG_INCLUDES}" -ncg
	#		WORKING_DIRECTORY ${ORIG_CMAKE_DIR} 
	#	)
	#endif()

	# if no dependencies given, always run generator
	if("${GENERATED_TARGET_DEPENDENCIES}" STREQUAL "")
		add_custom_target("${GENERATED_TARGET}_always_run"
			COMMAND java -jar "${CLAVA_JAR_PATH}" ${ASPECT} ${ASPECT_ARGS_FLAG} ${ASPECT_ARGS} --cmake -b 2 -p "${PROC_ORIG_SOURCES}" -of "${WOVEN_DIR_NAME}" -ih "${PROC_ORIG_INCLUDES}" -ncg
			WORKING_DIRECTORY ${ORIG_CMAKE_DIR} 
			COMMENT "Generating source code for target '${GENERATED_TARGET}'"
		)	
	# otherwise, make generator depend on given files
	else()
		add_custom_command(OUTPUT "${WOVEN_DIR}/clava_generated_files.txt"
			COMMAND java -jar "${CLAVA_JAR_PATH}" ${ASPECT} ${ASPECT_ARGS_FLAG} ${ASPECT_ARGS} --cmake -b 2 -p "${PROC_ORIG_SOURCES}" -of "${WOVEN_DIR_NAME}" -ih "${PROC_ORIG_INCLUDES}" -ncg
			WORKING_DIRECTORY ${ORIG_CMAKE_DIR} 
			DEPENDS ${GENERATED_TARGET_DEPENDENCIES}
			COMMENT "Generating source code for target '${GENERATED_TARGET}'"
		)
	
		# checks if re-generation of target '${GENERATED_TARGET}' is required"
		set("${GENERATED_TARGET}_checker" "${GENERATED_TARGET}_dependencies")
		ADD_CUSTOM_TARGET("${GENERATED_TARGET}_checker"
			DEPENDS "${WOVEN_DIR}/clava_generated_files.txt" 
		)
	endif()
	

	
	
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

	# add original target includes
#	set_target_properties(${GENERATED_TARGET}
#		PROPERTIES INCLUDE_DIRECTORIES "${ORIG_INCLUDES}"
#	)
	# add new include directories
	target_include_directories(${GENERATED_TARGET} PUBLIC "${CLAVA_INCLUDE_DIRS}")
	target_include_directories(${GENERATED_TARGET} PUBLIC "${ORIG_INCLUDES}")

	get_target_property(GEN_INCLUDES ${GENERATED_TARGET} INCLUDE_DIRECTORIES)
	message(STATUS "Generated Includes: ${GEN_INCLUDES}")
	
	#set(GENERATED_TARGET_DEPENDENCIES "${GENERATED_TARGET}_dependencies")
	#add_custom_target(${GENERATED_TARGET_DEPENDENCIES} DEPENDS "include/test_lib.h")
	
	#target_link_libraries(${GENERATED_TARGET} ${ORIG_TARGET})
	#add_dependencies(${GENERATED_TARGET} ${ASPECT})
	
	#add_dependencies(${GENERATED_TARGET} ${ORIG_TARGET})
	#message(STATUS "Target ${GENERATED_TARGET} depends on ${GENERATED_TARGET_DEPENDENCIES}")
	#add_dependencies(${GENERATED_TARGET} ${GENERATED_TARGET_DEPENDENCIES})

	#get the file timestamp of the 'source' resource file
    #FILE(TIMESTAMP "${GENERATED_TARGET}.timestamp" CLAVA_GENERATE_TIMESTAMP)
    #FILE(TIMESTAMP "${WOVEN_DIR}/clava_include_dirs.txt" CLAVA_GENERATE_TIMESTAMP)
	#message(STATUS "Timestamp: ${CLAVA_GENERATE_TIMESTAMP}")

	
	#add_custom_command(OUTPUT ${GENERATED_TARGET}.timestamp
	#	COMMAND java -jar "${CLAVA_JAR_PATH}" ${ASPECT} ${ASPECT_ARGS_FLAG} ${ASPECT_ARGS} --cmake -b 2 -p "${PROC_ORIG_SOURCES}" -of "${WOVEN_DIR_NAME}" -ih "${PROC_ORIG_INCLUDES}" -ncg
	#	COMMAND ${CMAKE_COMMAND} -E touch ${GENERATED_TARGET}.timestamp
	#	WORKING_DIRECTORY ${ORIG_CMAKE_DIR} 
	#	DEPENDS "include/test_lib.h"
	#	COMMENT "Generating source code for target '${GENERATED_TARGET}'"
	#)
	
	#set("${GENERATED_TARGET}_checker" "${GENERATED_TARGET}_dependencies")
	#ADD_CUSTOM_TARGET("${GENERATED_TARGET}_checker"
	#	DEPENDS ${GENERATED_TARGET}.timestamp 
    #   COMMENT "Checking if re-generation of target '${GENERATED_TARGET}' is required"
	#)
	if("${GENERATED_TARGET_DEPENDENCIES}" STREQUAL "")
		add_dependencies(${GENERATED_TARGET} "${GENERATED_TARGET}_always_run")
	else()
		add_dependencies(${GENERATED_TARGET} "${GENERATED_TARGET}_checker")
	endif()

	
	# if no given dependencies, make generated target depend on the original target
	#if("${GENERATED_TARGET_DEPENDENCIES}" STREQUAL "")
		#add_dependencies(${GENERATED_TARGET} ${ORIG_TARGET})
	# otherwise, make generated target depend on checker target
	#else()
	#	add_dependencies(${GENERATED_TARGET} "${GENERATED_TARGET}_checker")
	#endif()
	

	
endfunction(clava_generate)
