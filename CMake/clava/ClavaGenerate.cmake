# 
function(clava_generate INPUT_TARGET GENERATED_TARGET GENERATION_COMMAND_TARGET)

	# Split into generation (calling command) and generated library
	# Adds generation as dependency of 

	message(STATUS "Input Target: ${INPUT_TARGET}")
	message(STATUS "Generate Target: ${GENERATED_TARGET}")


	
	add_custom_target(
		${GENERATION_COMMAND_TARGET}
   )
	
	add_custom_command(
    TARGET
        ${GENERATION_COMMAND_TARGET}
	POST_BUILD
    COMMAND
		#clava --help
        cmake -E touch ${GENERATED_TARGET}.stamp
    COMMENT
        "Generating HDF5 library"
    VERBATIM
    )	
	
	get_property(TARGET_SOURCES TARGET ${INPUT_TARGET} PROPERTY SOURCES)
	message(STATUS "sources: ${TARGET_SOURCES}")
	get_property(TARGET_INCLUDES TARGET ${INPUT_TARGET} PROPERTY INCLUDE_DIRECTORIES)	
	message(STATUS "includes: ${TARGET_INCLUDES}")	
	
	add_library(${GENERATED_TARGET} "H:/Work_Shared/2018-06-15 CMake HDF5 with Clava/generated/src/generated.cpp")

	target_include_directories(${GENERATED_TARGET} PUBLIC "H:/Work_Shared/2018-06-15 CMake HDF5 with Clava/generated/src/")

	get_property(GENERATED_SOURCES TARGET ${GENERATED_TARGET} PROPERTY SOURCES)
	message(STATUS "generated sources: ${GENERATED_SOURCES}")	
	
	get_property(GENERATED_INCLUDES TARGET ${GENERATED_TARGET} PROPERTY INCLUDE_DIRECTORIES)
	message(STATUS "generated includes: ${GENERATED_INCLUDES}")	
	
	add_dependencies(${GENERATED_TARGET} ${GENERATION_COMMAND_TARGET})
	add_dependencies(${INPUT_TARGET} ${GENERATED_TARGET})
	

	
endfunction(clava_generate)