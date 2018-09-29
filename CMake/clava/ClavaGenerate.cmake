cmake_minimum_required(VERSION 3.3)

# Generate files based on the source code of the current target, 
# add generated files as a new target that depends on the original target.
#
# Automatic code generation is disabled, only manually generated files in the
# given aspect are considered as generated code, i.e., calls to $file.write
#
# TODO: Add variable for aspect arguments
# e.g., -av "{inputFile:'data.json',execute:true,iterations:10}"
function(clava_generate ORIG_TARGET GENERATED_TARGET ASPECT) 


	if(ARGC GREATER 3)
		set(ASPECT_ARGS_FLAG "-av")
		set(ASPECT_ARGS "{${ARGV3}}")
	else()
		set(ASPECT_ARGS_FLAG "")
		set(ASPECT_ARGS "")
	endif()

	#message(STATUS "ASPECT ARGS: ${ASPECT_ARGS}")
	
	# get CMakeLists.txt dir
	get_target_property(ORIG_CMAKE_DIR ${ORIG_TARGET} SOURCE_DIR)
	#message(STATUS "ORIG_CMAKE_DIR: ${ORIG_CMAKE_DIR}")
	
	# get full path of aspect file
	set(ASPECT "${ORIG_CMAKE_DIR}/${ASPECT}")
	#message(STATUS "ASPECT: ${ASPECT}")

	# get woven directory path
	set(WOVEN_DIR_NAME "clava_${ORIG_TARGET}")
	#message(STATUS "WOVEN_DIR_NAME: ${WOVEN_DIR_NAME}")

	# get original source files
	get_target_property(ORIG_SOURCES ${ORIG_TARGET} SOURCES)


	# add include directories in Clava sources, in order to compile header information
	#set(CLAVA_SOURCES 
	#	"${ORIG_SOURCES}"
	#	"${ORIG_INCLUDES}")
	#message(STATUS "CLAVA SOURCES: ${CLAVA_SOURCES}")	
	
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
	set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS "${ASPECT};${ORIG_SOURCES}")
	
	# mark Clava output directory as a target for 'make clean'
	set_property(DIRECTORY APPEND PROPERTY ADDITIONAL_MAKE_CLEAN_FILES "${WOVEN_DIR}")
	
	#set(CLAVA_COMMAND "java -jar ${CLAVA_JAR_PATH} ${ASPECT} --cmake -b 2 -p ${PROC_ORIG_SOURCES} -of ${WOVEN_DIR_NAME}")
	
	# execute Clava (TODO: set correct standard from cmake) (TODO: set correct clava jar)
	#message(STATUS "Running command: java -jar ${CLAVA_JAR_PATH} ${ASPECT} ${ASPECT_ARGS_FLAG} ${ASPECT_ARGS} --cmake -b 2 -p ${PROC_ORIG_SOURCES} -of ${WOVEN_DIR_NAME} -ih ${PROC_ORIG_INCLUDES} -ncg")
	execute_process(
		# -std c99 
		#COMMAND ${CLAVA_COMMAND}
		COMMAND java -jar "${CLAVA_JAR_PATH}" ${ASPECT} ${ASPECT_ARGS_FLAG} ${ASPECT_ARGS} --cmake -b 2 -p "${PROC_ORIG_SOURCES}" -of "${WOVEN_DIR_NAME}" -ih "${PROC_ORIG_INCLUDES}" -ncg
		WORKING_DIRECTORY ${ORIG_CMAKE_DIR} 
		#OUTPUT_VARIABLE ${CLAVA_STDOUT}
		#ERROR_VARIABLE ${CLAVA_STDERR}		
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

	# add original target includes
#	set_target_properties(${GENERATED_TARGET}
#		PROPERTIES INCLUDE_DIRECTORIES "${ORIG_INCLUDES}"
#	)
	# add new include directories
	target_include_directories(${GENERATED_TARGET} PUBLIC "${CLAVA_INCLUDE_DIRS}")
	target_include_directories(${GENERATED_TARGET} PUBLIC "${ORIG_INCLUDES}")

	get_target_property(GEN_INCLUDES ${GENERATED_TARGET} INCLUDE_DIRECTORIES)
	message(STATUS "Generated Includes: ${GEN_INCLUDES}")
	
	add_dependencies("${GENERATED_TARGET}" "${ORIG_TARGET}")

	
endfunction(clava_generate)
