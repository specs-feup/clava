# target_include_directories_recursive function
# Collects all include directories of a target, recursively traversing its linked libraries.
#
# Parameters:
# CURRENT_TARGET - A CMake target
# INCLUDE_DIRECTORIES_FOUND - Output list where the include directories will be stored
#
function(target_include_directories_recursive CURRENT_TARGET INCLUDE_DIRECTORIES_FOUND)
	# Initialize output variables
	#set(${INCLUDE_DIRECTORIES_FOUND} "" PARENT_SCOPE)

	#message(STATUS "Calling 'target_include_directories_recursive' with target ${CURRENT_TARGET}")

	if(NOT TARGET ${CURRENT_TARGET})
		#message(STATUS "Could not find target ${CURRENT_TARGET}") 
		return()
	endif()
	
	# Get original include folders
	clava_get_target_property(ORIG_INCLUDES ${CURRENT_TARGET} INCLUDE_DIRECTORIES)
	#message(STATUS "ORIG_INCLUDES of ${CURRENT_TARGET}: '${ORIG_INCLUDES}'")
	
	# If not found, set to empty list
	if(("${ORIG_INCLUDES}" MATCHES "ORIG_INCLUDES-NOTFOUND"))
		set(ORIG_INCLUDES "")
	endif()
	
	# Add to output variables
	set(TARGET_INCLUDES "")
	list(APPEND TARGET_INCLUDES ${ORIG_INCLUDES})
	#message(STATUS "Added original includes: ${TARGET_INCLUDES}")


	# Add includes of target link libraries
	clava_get_target_property(ORIG_LINK_LIBRARIES "${CURRENT_TARGET}" LINK_LIBRARIES)	
	#message(STATUS "ORIG_LINK_LIBRARIES of ${CURRENT_TARGET}: ${ORIG_LINK_LIBRARIES}")		
	
	# If not found, set to empty list
	if("${ORIG_LINK_LIBRARIES}" MATCHES "ORIG_LINK_LIBRARIES-NOTFOUND")
		set(ORIG_LINK_LIBRARIES "")
	endif()
	
	# Add include directories of linked targets
	foreach(ORIG_LINK_LIB IN LISTS ORIG_LINK_LIBRARIES)
		#message(STATUS "ORIG_LINK_LIB: ${ORIG_LINK_LIB}")	
		target_include_directories_recursive(${ORIG_LINK_LIB} CHILD_INCLUDES)
		list(APPEND TARGET_INCLUDES ${CHILD_INCLUDES})
		#message(STATUS "Current includes: ${TARGET_INCLUDES}")	
	endforeach(ORIG_LINK_LIB)	

	#message(STATUS "Include dirs found at the end: ${${INCLUDE_DIRECTORIES_FOUND}}")
	
	# Set output variable
	#set(${INCLUDE_DIRECTORIES_FOUND} ${${INCLUDE_DIRECTORIES_FOUND}} PARENT_SCOPE)
	set(${INCLUDE_DIRECTORIES_FOUND} ${TARGET_INCLUDES} PARENT_SCOPE)
	
#	if(CALL_AGAIN)
#		message(STATUS "Calling include directores again")
#		target_include_directories_recursive(A_TARGET INCLUDE_DIRECTORIES_FOUND false)
#	else()
#		message(STATUS "Second call of include directores")
#	endif()

endfunction(target_include_directories_recursive)


# Gets the Clava standard flag automatically from the provided target
#
# Parameter 1: _target - The target that we will query
# Parameter 2: _std - The variable that will hold the standard flag string (output parameter)
function(clava_get_target_std _target _std)
	get_property(_cxxs TARGET ${_target} PROPERTY CXX_STANDARD)
	get_property(_cxxe TARGET ${_target} PROPERTY CXX_EXTENSIONS)
	get_property(_cs TARGET ${_target} PROPERTY C_STANDARD)
	get_property(_ce TARGET ${_target} PROPERTY C_EXTENSIONS)
	
	if(${_cxxs})
		# C++ is on
		if(("${_cxxe}" STREQUAL ON) OR ("${_cxxe}" STREQUAL ""))
			# gnu is on
			set(${_std} "-std gnu++${_cxxs}" PARENT_SCOPE)
		else()
			# gnu is off
			set(${_std} "-std c++${_cxxs}" PARENT_SCOPE)
		endif()
	elseif(${_cs})
		# C is on
		if(("${_ce}" STREQUAL ON) OR ("${_ce}" STREQUAL ""))
			# gnu is on
			set(${_std} "-std gnu${_cs}" PARENT_SCOPE)
		else()
			# gnu is off
			set(${_std} "-std c${_cs}" PARENT_SCOPE)
		endif()
	else()
		# neither standard is explicitly defined, assume the user will provide one
		set(${_std} "" PARENT_SCOPE)
	endif()
endfunction(clava_get_target_std)


# Gets all the targets from the subdirectories of the provided targets
# 
# Parameter 1: _result - A list with all the targets found (output parameter)
# Parameter 2: _dir - The directory where the subdirectory search will start
function(clava_get_all_targets _result _dir)
	get_property(_sub_dirs DIRECTORY "${_dir}" PROPERTY SUBDIRECTORIES)
	foreach(_sub_dir IN LISTS _sub_dirs)
		clava_get_all_targets(${_result} "${_sub_dir}")
	endforeach()
	get_property(_sub_targets DIRECTORY "${_dir}" PROPERTY BUILDSYSTEM_TARGETS)
	set(${_result} ${${_result}} ${_sub_targets} PARENT_SCOPE)
endfunction(clava_get_all_targets)

# Wrapper around get_target_property that takes into account special case INTERFACE_LIBRARY
# 
# Parameter 1: _var - variable where value of the property will be stored
# Parameter 2: _target - the target where we will read the property from
# Parameter 3: _property - the target property to read
function(clava_get_target_property _var _target _property)

	get_target_property(_TARGET_TYPE ${_target} TYPE)
	if(_TARGET_TYPE STREQUAL "INTERFACE_LIBRARY")
		#unset(_var)
		set(_value "")
	else()
		get_target_property(_value ${_target} ${_property})
	endif()

	set(${_var} ${_value} PARENT_SCOPE)

endfunction(clava_get_target_property)