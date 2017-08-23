#
# Finds paths to all the libraries passed as variadic arguments. Saves the result 
# in a list.
#
# ARGUMENTS:
#   - var : the name of the list variable that will hold the paths
#   - path : where we search for libraries
#   - variadic arguments : names of the libraries
#
# EXAMPLE:
#   find_libraries(MY_LIST "/home/libs/" protobuf zmq)
#   saves the paths to protobuf.so and zmq.so in the variable MY_LIST
#

function(find_libraries var path)

    foreach(lib_name ${ARGN})
        
        find_library(TMP_${lib_name} "${lib_name}" PATHS "${path}" NO_DEFAULT_PATH)

        list(APPEND TMP_LIST
            ${TMP_${lib_name}}
        )
		
		#message("lib name: ${lib_name}")
		#message("lib:" ${TMP_${lib_name}})

    endforeach()

    set(${var} ${TMP_LIST} PARENT_SCOPE)

endfunction()
