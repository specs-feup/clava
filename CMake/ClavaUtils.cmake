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
	get_target_property(ORIG_INCLUDES ${CURRENT_TARGET} INCLUDE_DIRECTORIES)
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
	get_target_property(ORIG_LINK_LIBRARIES "${CURRENT_TARGET}" LINK_LIBRARIES)
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