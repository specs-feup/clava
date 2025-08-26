# toolchain-mingw.cmake

set(CMAKE_SYSTEM_NAME Windows)
set(CMAKE_SYSTEM_PROCESSOR x86_64)  # Change to x86 if targeting 32-bit

# Cross-compiler location
set(CROSS_PREFIX x86_64-w64-mingw32)  # Change to i686-w64-mingw32 for 32-bit

set(CMAKE_C_COMPILER ${CROSS_PREFIX}-gcc)
set(CMAKE_CXX_COMPILER ${CROSS_PREFIX}-g++)
set(CMAKE_RC_COMPILER ${CROSS_PREFIX}-windres)

# Set CMake tools
set(CMAKE_FIND_ROOT_PATH /usr/${CROSS_PREFIX} /usr/${CROSS_PREFIX}/lib)

# Prefer cross-compiled libraries over system libraries
set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE ONLY)
