# extract_enums function
# Extracts the enum values from a given header file.
#
# Parameters:
# CURRENT_TARGET - A CMake target
# INCLUDE_DIRECTORIES_FOUND - Output list where the include directories will be stored
#
function(extract_enums BASE_FOLDER HEADER_FILENAME) 
#CURRENT_TARGET
# Create target for clang/AST/Type.h
set(TARGET_NAME "enums_${HEADER_FILENAME}")

add_library(${TARGET_NAME} "${LLVM_DIR}/src/include/clang/AST/Type.h")

set_property(TARGET ${TARGET_NAME} PROPERTY CXX_STANDARD 14)

# Because it is a header file, it cannot figure it out alone
set_target_properties(${TARGET_NAME} PROPERTIES LINKER_LANGUAGE CXX)

# Add includes
target_include_directories(${TARGET_NAME}
       SYSTEM PUBLIC ${DEPS_INCLUDES}
)

# Add enums helper
clava_weave(${TARGET_NAME} "ClangEnumsGenerator.lara"  ARGS "header:'${HEADER_FILENAME}', outputFolder:'${CMAKE_CURRENT_LIST_DIR}/..'" JAVA_FLAGS -Xmx8000m)

endfunction(extract_enums)
