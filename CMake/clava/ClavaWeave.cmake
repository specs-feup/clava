# TODO: Documentation
function(clava_weave ORIG_TARGET ASPECT)

	# Aspect arguments
	if(ARGC GREATER 2)
		set(ASPECT_ARGS_FLAG "-av")
		set(ASPECT_ARGS "{${ARGV2}}")
	else()
		set(ASPECT_ARGS_FLAG "")
		set(ASPECT_ARGS "")
	endif()

	# get build dir
	set(BUILD_DIR "${CMAKE_CURRENT_BINARY_DIR}")
	#message(STATUS "BUILD DIR: ${BUILD_DIR}") 

	# woven dir name
	set(WOVEN_DIR_NAME "woven")
	
	# working dir
	string(TIMESTAMP CURRENT_TIMESTAMP)
	string(MAKE_C_IDENTIFIER ${CURRENT_TIMESTAMP} TIME_ID)
	set(WORKING_DIR "${BUILD_DIR}/${ORIG_TARGET}_${TIME_ID}")
	set(WOVEN_DIR "${WORKING_DIR}/${WOVEN_DIR_NAME}")

	file(MAKE_DIRECTORY ${WOVEN_DIR})
	#message(STATUS "Woven dir: ${WOVEN_DIR}")
	
	#set(ORIG_CMAKE_DIR "${WORKING_DIR}")
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

	# get original source files
	get_target_property(ORIG_SOURCES ${ORIG_TARGET} SOURCES)
	#message(STATUS "ORIG_SOURCES: ${ORIG_SOURCES}")
	
	# initialize processed sources
	set(PROC_ORIG_SOURCES "")
	foreach(SOURCE ${ORIG_SOURCES})
		if(IS_ABSOLUTE ${SOURCE})
			set(PROC_SOURCE "${SOURCE}")	
		else()
			set(PROC_SOURCE "${ORIG_CMAKE_DIR}/${SOURCE}")			
		endif()
		
		LIST(APPEND PROC_ORIG_SOURCES "${PROC_SOURCE}")
	endforeach()
	
	#message(STATUS "NEW PROC_ORIG_SOURCES: ${PROC_ORIG_SOURCES}")
	# process original source file list
	#string(REGEX REPLACE "([^;]+)" "${ORIG_CMAKE_DIR}/\\1" PROC_ORIG_SOURCES "${ORIG_SOURCES}")
	#message(STATUS "PROC_ORIG_SOURCES V1: ${PROC_ORIG_SOURCES}")
	if(NOT "${CMAKE_HOST_SYSTEM}" MATCHES ".*Windows.*")
		string(REGEX REPLACE ";" ":" PROC_ORIG_SOURCES "${PROC_ORIG_SOURCES}")
	endif()
	#message(STATUS "PROC_ORIG_SOURCES: ${PROC_ORIG_SOURCES}")
	
	
	# get original include folders
	get_target_property(ORIG_INCLUDES ${ORIG_TARGET} INCLUDE_DIRECTORIES)
	#message(STATUS "ORIG_INCLUDES: '${ORIG_INCLUDES}'")

	set(INCLUDE_HEADERS_FLAG "-ih")
    if((${ORIG_INCLUDES} MATCHES "ORIG_INCLUDES-NOTFOUND") OR ("${ORIG_INCLUDES}" STREQUAL ""))
        set(INCLUDE_HEADERS_FLAG "")
        set(ORIG_INCLUDES "")
    endif()
	
	# process original includes list
	string(REGEX REPLACE "([^;]+)" "\\1" PROC_ORIG_INCLUDES "${ORIG_INCLUDES}")
	if(NOT "${CMAKE_HOST_SYSTEM}" MATCHES ".*Windows.*")
		string(REGEX REPLACE ";" ":" PROC_ORIG_INCLUDES "${PROC_ORIG_INCLUDES}")
	endif()
	#message(STATUS "PROC_ORIG_INCLUDES: ${PROC_ORIG_INCLUDES}")
	
	#set(WOVEN_DIR "${ORIG_CMAKE_DIR}/${WOVEN_DIR_NAME}")
	#set(WOVEN_DIR "${ORIG_CMAKE_DIR}/${WOVEN_DIR_NAME}")

	
	# get original include directories
	#get_target_property(ORIG_INC_DIRS ${ORIG_TARGET} INCLUDE_DIRECTORIES)
	#message(STATUS "ORIG_INC_DIRS: ${ORIG_INC_DIRS}")
	
	# make the cmake configuration depend on the LARA file
	set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS "${ASPECT};${ORIG_SOURCES}")
	
	# mark Clava output directory as a target for 'make clean'
	set_property(DIRECTORY APPEND PROPERTY ADDITIONAL_MAKE_CLEAN_FILES "${WOVEN_DIR}")
	
	#set(CLAVA_COMMAND "java -jar ${CLAVA_JAR_PATH} ${ASPECT} --cmake -b 2 -p ${PROC_ORIG_SOURCES} -of ${WOVEN_DIR_NAME}")
	
	# execute Clava (TODO: set correct standard from cmake) (TODO: set correct clava jar)
	execute_process(
		# -std c99 
		#COMMAND ${CLAVA_COMMAND}
		COMMAND java -jar "${CLAVA_JAR_PATH}" ${ASPECT} ${ASPECT_ARGS_FLAG} ${ASPECT_ARGS} --cmake -b 2 -p "${PROC_ORIG_SOURCES}" -of "${WOVEN_DIR_NAME}" ${INCLUDE_HEADERS_FLAG} ${PROC_ORIG_INCLUDES}
		#WORKING_DIRECTORY ${ORIG_CMAKE_DIR}
		WORKING_DIRECTORY ${WORKING_DIR}
	)
		
	#-of "${WOVEN_DIR_NAME}"
		
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
	

	# set new sources
	set_target_properties(${ORIG_TARGET}
		PROPERTIES SOURCES "${CLAVA_WOVEN_SOURCES}"
	)
	
	# set new include directories
	set_target_properties(${ORIG_TARGET}
		PROPERTIES INCLUDE_DIRECTORIES "${CLAVA_INCLUDE_DIRS}"
	)	
	
endfunction(clava_weave)
