#.rst:
# FindClava
# ---------
#
# Locate or download the Clava C++ weaver for LARA DSL executable.
# See: https://web.fe.up.pt/~specs/projects/lara/doku.php?id=lara:documentation
#
# This module defines the following variables
# ::
#   CLAVA_JAR - Absolute path to clava.jar executable
#   CLAVA_CMAKE_HOME - Absolute path to the Clava CMake folder
#
# Module parameters:
#
# LOCAL_CLAVA
# If defined, search for Clava in path specified in this parameter
#
#

# Check for Java JRE
find_package(Java COMPONENTS Runtime REQUIRED)

# Save current Clava folder
set(CLAVA_CMAKE_HOME ${CMAKE_CURRENT_LIST_DIR})
message(STATUS "Clava home: ${CLAVA_CMAKE_HOME}")

# Check if installation file with JAR path exists
if(LOCAL_CLAVA)

elseif(EXISTS "${CMAKE_CURRENT_LIST_DIR}/clava-installation-jar.txt")
	file(READ "${CMAKE_CURRENT_LIST_DIR}/clava-installation-jar.txt" CLAVA_JAR_PATH) 
	message(STATUS "Found Clava: " ${CLAVA_JAR_PATH})
else()
	# Set default URL for clava.jar
	set(CLAVA_JAR_URL "http://specs.fe.up.pt/tools/clava.jar")
	set(CLAVA_JAR_PATH "${CMAKE_CURRENT_BINARY_DIR}/clava.jar")
	set(DOWNLOADED_CLAVA true)
endif()

# Download clava by default
if(DOWNLOADED_CLAVA)

    # Check if Clava has been already downloaded
    if(EXISTS ${CLAVA_JAR_PATH})
        message(STATUS "Found Clava at: " ${CLAVA_JAR_PATH})
    else()
        message(STATUS "Downloading Clava")
        file(DOWNLOAD
                ${CLAVA_JAR_URL}
                ${CLAVA_JAR_PATH}
                INACTIVITY_TIMEOUT 30
                STATUS DOWN_STATUS)
        list(GET DOWN_STATUS 0 status_code)
        list(GET DOWN_STATUS 1 status_string)

        if(${status_code} OR 0)
            message(SEND_ERROR "Cannot download Clava: ${status_code} - ${status_string}")
        endif()
        message(STATUS "Downloaded Clava to: ${CLAVA_JAR_PATH}")
    endif()

elseif(LOCAL_CLAVA)
    message(STATUS "Using local Clava at: ${LOCAL_CLAVA}")
    set(CLAVA_JAR_PATH ${LOCAL_CLAVA})
endif()

if(NOT EXISTS ${CLAVA_JAR_PATH})
    message(SEND_ERROR "File ${CLAVA_JAR_PATH} does not exits")
    set(CLAVA_JAR_NOTFOUND)
endif()

# Set the result variables
set(CLAVA_JAR ${CLAVA_JAR_PATH})
set(CLAVA_JAR_FOUND)

# Add current folder to the modules path
#set (CMAKE_MODULE_PATH "${CMAKE_MODULE_PATH};${CMAKE_CURRENT_LIST_DIR}")

# Add ApplyLARA, to make function apply_lara_aspect available
#include(${CMAKE_CURRENT_LIST_DIR}/ApplyLARA.cmake RESULT_VARIABLE LARA_RESULT)
include(${CMAKE_CURRENT_LIST_DIR}/ApplyLARA.cmake)

# Make utility functions available
include(${CMAKE_CURRENT_LIST_DIR}/ClavaUtils.cmake)

### Base functions

# Add function 'clava_generate'
include(${CMAKE_CURRENT_LIST_DIR}/clava/ClavaGenerate.cmake)

# Add function 'clava_weave'
include(${CMAKE_CURRENT_LIST_DIR}/clava/ClavaWeave.cmake)

### Utility functions

# Add function 'clava_generate_hdf5'
include(${CMAKE_CURRENT_LIST_DIR}/util/ClavaHdf5.cmake)
# Add function 'clava_weave_autopar'
include(${CMAKE_CURRENT_LIST_DIR}/util/ClavaAutopar.cmake)
