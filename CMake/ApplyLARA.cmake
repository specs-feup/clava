#.rst:
# ApplyLARA
# ---------
#
# This module provides integration with Clava C++ weaver for LARA DSL.
# See: https://web.fe.up.pt/~specs/projects/lara/doku.php?id=lara:documentation
#
# This module defines the following varriables
# ::
#   LARA_WORKING_DIR
#   Holds all artefacts and logs produced by Clava, relative to the current binary dir.
#
# This module uses FindClava CMake module, see its definition.
#
# Usage:
# 1. Add directory containing this module file to the CMAKE_MODULE_PATH.
# 2. Include the module using ``include(ApplyLARA)``
# 3. Call ``apply_lara_aspect`` function with appropriate parameters

# Removed 'find', this file is automatically included when calling find_package(Clava)
# Find Clava
#find_package(Clava REQUIRED)

set(LARA_WORKING_DIR ${CMAKE_CURRENT_BINARY_DIR}/lara)

# Include the working directory, such that generated headers can be included in the rest of the parent project
# TODO: Disabled, check where this is used
#include_directories(${LARA_WORKING_DIR})

# apply_lara_aspect function
# Used to define custom build step for Clava execution.
#
# Parameters:
# LARA_INPUT_FILES - Path to a folder with source code (sub)tree used as input
# LARA_GENERATED_FILES - List of files generated by Clava by applying the selected LARA Aspect
# LARA_SYSTEM_INCLUDES - List of folders included to Clava clang-based frontend as system headers
# LARA_ASPECT - Path to *.lara file containing aspect to be applied to the code
# LARA_CONFIG - Path to *.clava XML file containing configuration of Clava
# LARA_INCLUDE_DIR - Additional *.js files to be included by LARA interpreter
#
function(apply_lara_aspect LARA_INPUT_FILES LARA_GENERATED_FILES LARA_SYSTEM_INCLUDES LARA_ASPECT LARA_CONFIG LARA_INCLUDE_DIR)

        string(REGEX REPLACE "[\n\ ]+" ":" CLAVA_INPUT ${LARA_INPUT_FILES})
        message(STATUS "Adding LARA build step")
        message(STATUS "Generated files: ${LARA_GENERATED_FILES}")

        # Append the input directory
        include_directories(${LARA_INPUT_FILES})

        add_custom_command(
            PRE_BUILD
            OUTPUT ${LARA_GENERATED_FILES}
            WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
			
            COMMAND ${CLAVA_CMD} #${Java_JAVA_EXECUTABLE} -Djava.awt.headless=true -jar ${CLAVA_JAR} 
			${LARA_ASPECT}
            -p ${CLAVA_INPUT}
            -is ${LARA_SYSTEM_INCLUDES}
            -o ${CMAKE_CURRENT_BINARY_DIR}
                -i ${LARA_INCLUDE_DIR}
                -c ${LARA_CONFIG}
            DEPENDS ${LARA_ASPECT} ${LARA_INPUT_FILES} ${LARA_CONFIG}
            VERBATIM
            COMMENT "Applying LARA aspects"
            USES_TERMINAL)

endfunction(apply_lara_aspect)