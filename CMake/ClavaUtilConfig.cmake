#.rst:
# FindClavaUtil
# ---------

# Check for Clava
find_package(Clava REQUIRED)

# Add function 'clava_generate_hdf5'
include(${CMAKE_CURRENT_LIST_DIR}/util/ClavaHdf5.cmake)

