# Check for Clava
find_package(Clava REQUIRED)

# Add function 'clava_hdf5'
include(${CMAKE_CURRENT_LIST_DIR}/hdf5/ClavaHdf5.cmake)

