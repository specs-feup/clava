# TODO: Documentation
function(clava_weave ORIG_TARGET ASPECT)

	# get CMakeLists.txt dir
	get_target_property(ORIG_CMAKE_DIR ${ORIG_TARGET} SOURCE_DIR)
	#message(STATUS "ORIG_CMAKE_DIR: ${ORIG_CMAKE_DIR}")
	
	# get full path of aspect file
	set(ASPECT "${ORIG_CMAKE_DIR}/${ASPECT}")
	#message(STATUS "ASPECT: ${ASPECT}")

	# get woven directory path
	set(WOVEN_DIR "clava_${ORIG_TARGET}")
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
	#message(STATUS "ORIG_INCLUDES: ${ORIG_INCLUDES}")
	
	# process original includes list
	string(REGEX REPLACE "([^;]+)" "\\1" PROC_ORIG_INCLUDES "${ORIG_INCLUDES}")
	if(NOT "${CMAKE_HOST_SYSTEM}" MATCHES ".*Windows.*")
		string(REGEX REPLACE ";" ":" PROC_ORIG_INCLUDES "${PROC_ORIG_INCLUDES}")
	endif()
	message(STATUS "PROC_ORIG_INCLUDES: ${PROC_ORIG_INCLUDES}")
	
	
	# get original include directories
	#get_target_property(ORIG_INC_DIRS ${ORIG_TARGET} INCLUDE_DIRECTORIES)
	#message(STATUS "ORIG_INC_DIRS: ${ORIG_INC_DIRS}")
	
	# make the cmake configuration depend on the LARA file
	set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS "${ASPECT};${ORIG_SOURCES}")
	
	set(CLAVA_COMMAND "java -jar ${CLAVA_JAR_PATH} ${ASPECT} --cmake -b 2 -p ${PROC_ORIG_SOURCES} -of ${WOVEN_DIR}")
	
	# execute Clava (TODO: set correct standard from cmake) (TODO: set correct clava jar)
	execute_process(
		# -std c99 
		#COMMAND ${CLAVA_COMMAND}
		COMMAND java -jar "${CLAVA_JAR_PATH}" ${ASPECT} --cmake -b 2 -p "${PROC_ORIG_SOURCES}" -of "${WOVEN_DIR}" -ih "${PROC_ORIG_INCLUDES}"
		WORKING_DIRECTORY ${ORIG_CMAKE_DIR}
	)
	
	# read new sources
	if(EXISTS "${ORIG_CMAKE_DIR}/${WOVEN_DIR}/clava_generated_files.txt")
        file(READ "${ORIG_CMAKE_DIR}/${WOVEN_DIR}/clava_generated_files.txt" CLAVA_WOVEN_SOURCES)
        string(STRIP "${CLAVA_WOVEN_SOURCES}" CLAVA_WOVEN_SOURCES)
        set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS "${CLAVA_WOVEN_SOURCES}")

        #message(STATUS "CLAVA_WOVEN_SOURCES: ${CLAVA_WOVEN_SOURCES}")
	else()
		message(FATAL_ERROR "Could not find Clava file 'clava_generated_files.txt'")
	endif()
	
	# read new includes
	if(EXISTS "${ORIG_CMAKE_DIR}/${WOVEN_DIR}/clava_include_dirs.txt")
        file(READ "${ORIG_CMAKE_DIR}/${WOVEN_DIR}/clava_include_dirs.txt" CLAVA_INCLUDE_DIRS)
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