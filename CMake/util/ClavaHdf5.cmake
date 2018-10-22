# Creates a target of generated code with HDF5 interfaces for the classes/structures found in the given target. 
#
# Parameter 1: The target with classes/structures
# Parameter 2: Name of the new target that will represent the generated code
# Parameter FILTER (one value): A string with a succession of attributes of records and patterns e.g., "rx_name: 'A', kind: 'class'". If attribute is prefixed by 'rx_', it is interpreted as a regex
function(clava_generate_hdf5 ORIG_TARGET GENERATED_TARGET)
	message(STATUS "Generating HDF5 support library for target '${ORIG_TARGET}'")

	set(options)
    set(oneValueArgs FILTER)
    set(multiValueArgs)

    cmake_parse_arguments(PARSE_ARGV 3 CLAVA_GENERATE_HDF5 "${options}" "${oneValueArgs}" "${multiValueArgs}")

	
	#if(ARGC GREATER 2)
	#	set(FILTER ${ARGV2})
	#else()
	#	set(FILTER "")	
	#endif()
	
	#if(ARGC GREATER 3)
	#	set(DEPENDENCIES ${ARGV3})
	#else()
	#	set(DEPENDENCIES "")	
	#endif()
	
	# This is imported by package Clava
	#find_package(Clava REQUIRED)
	
	# HDF5 Support
	find_package(HDF5 REQUIRED CXX)
	
	set(HDF5_ASPECT "${CLAVA_CMAKE_HOME}/util/ApplyHdf5.lara")
	set(HDF5_ASPECT_ARGS "filter: {${CLAVA_GENERATE_HDF5_FILTER}}")

	# Generate HDF5 interfaces for current code
	clava_generate(${ORIG_TARGET} ${GENERATED_TARGET} ${HDF5_ASPECT} ARGS ${HDF5_ASPECT_ARGS}) # If attribute is prefixed by 'rx_', it is interpreted as a regex 
	
	target_include_directories(${GENERATED_TARGET} PUBLIC ${HDF5_INCLUDE_DIRS})
	#target_link_libraries(${GENERATED_TARGET} ${HDF5_LIBRARIES} ${HDF5_CXX_LIBRARIES})
	#target_link_libraries(${GENERATED_TARGET} hdf5::hdf5-shared hdf5_cpp)
	#target_link_libraries(${GENERATED_TARGET} hdf5_cpp)
	target_link_libraries(${GENERATED_TARGET} ${HDF5_LIBRARIES})
	
	#message(STATUS "HDF5_C_LIBRARIES ${HDF5_C_LIBRARIES}")
	#message(STATUS "HDF5_CXX_LIBRARIES ${HDF5_CXX_LIBRARIES}")
	#message(STATUS "HDF5_LIBRARIES ${HDF5_LIBRARIES}")
	
	# update generated target variables
	
	# create variables with include directories of generate target
	list(APPEND ${GENERATED_TARGET}_INCLUDE_DIRS ${HDF5_INCLUDE_DIRS})
	set(${GENERATED_TARGET}_INCLUDE_DIRS ${${GENERATED_TARGET}_INCLUDE_DIRS} PARENT_SCOPE)
	#set(${GENERATED_TARGET}_INCLUDE_DIRS "${${GENERATED_TARGET}_INCLUDE_DIRS} ${HDF5_INCLUDE_DIRS}" PARENT_SCOPE)
	
	# include generated target as part of the link libraries
	#list(APPEND ${GENERATED_TARGET}_LIBRARIES hdf5::hdf5-shared hdf5_cpp)
	#list(APPEND ${GENERATED_TARGET}_LIBRARIES hdf5_cpp)	
	list(APPEND ${GENERATED_TARGET}_LIBRARIES ${HDF5_LIBRARIES})	
	set(${GENERATED_TARGET}_LIBRARIES ${${GENERATED_TARGET}_LIBRARIES} PARENT_SCOPE)
	#set(${GENERATED_TARGET}_LIBRARIES "${GENERATED_TARGET}_LIBRARIES hdf5::hdf5-shared hdf5_cpp" PARENT_SCOPE)
	
	#"filter: {rx_name: 'A', kind: 'class'}") # If attribute is prefixed by 'rx_', it is interpreted as a regex
	
	#set(HDF5_GENERATE_TARGET "${INPUT_TARGET}_clava_hdf5_generate")
	#set(HDF5_LIB_TARGET "${INPUT_TARGET}_clava_hdf5_lib")

	#clava_generate(${INPUT_TARGET} ${HDF5_LIB_TARGET} ${HDF5_GENERATE_TARGET})

	#add_dependencies(hdf5-generate ${HDF5_GENERATE_TARGET})
	
endfunction(clava_generate_hdf5)