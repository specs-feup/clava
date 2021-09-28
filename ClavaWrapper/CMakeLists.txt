cmake_minimum_required(VERSION 3.16)
project(ClavaWrapper)

set(CMAKE_CXX_STANDARD 14)

add_executable(ClavaWrapper main.cpp)

target_link_libraries(ClavaWrapper gearman)
