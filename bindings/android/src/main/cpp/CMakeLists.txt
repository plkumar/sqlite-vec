cmake_minimum_required(VERSION 3.22.1)

# Declares and names the project.
project("sqlite-vec")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
add_library(
    sqlite-vec
    SHARED
    sqlite-vec-jni.cpp
    sqlite-vec.c
)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.
find_library(
    log-lib
    log
)

find_library(
    android-lib
    android
)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
target_link_libraries(
    sqlite-vec
    ${log-lib}
    ${android-lib}
)

# Set compile definitions
target_compile_definitions(sqlite-vec PRIVATE
    SQLITE_VEC_STATIC=1
    SQLITE_THREADSAFE=1
    SQLITE_USE_URI=1
)

# Include directories
target_include_directories(sqlite-vec PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)
