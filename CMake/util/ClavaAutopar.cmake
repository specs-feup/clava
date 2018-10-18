# Analyses for loops in source code for parallelization opportunities and adds OpenMP pragmas to loops it considers it can parallelize.
function(clava_weave_autopar ORIG_TARGET)
	message(STATUS "Attempting to auto-parallelize target '${ORIG_TARGET}'")
	
	# OpenMP Support
	find_package(OpenMP REQUIRED)
	
	set(AUTORPAR_ASPECT "${CLAVA_CMAKE_HOME}/util/ApplyAutopar.lara")

	# Generate HDF5 interfaces for current code
	clava_weave(${ORIG_TARGET} ${AUTORPAR_ASPECT}) 

    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${OpenMP_C_FLAGS}" PARENT_SCOPE)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${OpenMP_CXX_FLAGS}" PARENT_SCOPE)
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ${OpenMP_EXE_LINKER_FLAGS}" PARENT_SCOPE)

	#message(STATUS "OpenMP C flags: '${OpenMP_C_FLAGS}'")
	#message(STATUS "OpenMP C++ flags: '${OpenMP_CXX_FLAGS}'")
	#message(STATUS "OpenMP Linker flags: '${OpenMP_EXE_LINKER_FLAGS}'")

	
endfunction(clava_weave_autopar)