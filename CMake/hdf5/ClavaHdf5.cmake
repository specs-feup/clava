# Target that calls all HDF5 generator jobs (just the generation)
add_custom_target(hdf5-generate)

# Adds an HDF5 job based on the given target
function(clava_hdf5 INPUT_TARGET)
	message(STATUS "Adding Clava HDF5 support for target '${INPUT_TARGET}'")

	set(HDF5_GENERATE_TARGET "${INPUT_TARGET}_clava_hdf5_generate")
	set(HDF5_LIB_TARGET "${INPUT_TARGET}_clava_hdf5_lib")

	clava_generate(${INPUT_TARGET} ${HDF5_LIB_TARGET} ${HDF5_GENERATE_TARGET})

	add_dependencies(hdf5-generate ${HDF5_GENERATE_TARGET})
	
endfunction(clava_hdf5)