cmake_minimum_required(VERSION 3.22.1)

project("sqlite-vec")

# Find the required libraries
find_library(log-lib log)
find_library(android-lib android)

# Include directories
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

# Create the sqlite-vec library
add_library(sqlite-vec SHARED
    sqlite-vec.c
    sqlite-vec-jni.c
)

# Link libraries
target_link_libraries(sqlite-vec
    ${log-lib}
    ${android-lib}
)

# Compiler flags
target_compile_options(sqlite-vec PRIVATE
    -DSQLITE_CORE
    -DSQLITE_VEC_STATIC
    -fvisibility=hidden
    -Wall
    -Wextra
    -O3
)
