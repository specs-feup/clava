#
# ARGUMENTS - Variables that need to be set
#  - DEPS_DLLS_WIN32: The 

cmake_minimum_required(VERSION 3.2)

# Copy DLL to executable folder
if(WIN32 AND NOT ("${DEPS_DLLS_WIN32}" STREQUAL ""))
    message(WARNING "-- [Deps] The executable will need the files in the folders '${DEPS_DLLS_WIN32}'")
	add_custom_command(TARGET ${CMAKE_PROJECT_NAME} POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_directory
        "${DEPS_DLLS_WIN32}"
        $<TARGET_FILE_DIR:${CMAKE_PROJECT_NAME}>)
endif()

if(UNIX AND NOT ("${DEPS_DLLS_UNIX}" STREQUAL ""))
    message(WARNING "-- [Deps] The executable will need the files in the folders '${DEPS_DLLS_WIN32}'")
endif()