cmake_minimum_required(VERSION 3.7)
project(LibdeflateJNI C)

set(CMAKE_C_STANDARD 11)

find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

set(LIBDEFLATE_BUILD_SHARED_LIB OFF CACHE BOOL "" FORCE)
set(LIBDEFLATE_DECOMPRESSION_SUPPORT ON CACHE BOOL "" FORCE)
set(LIBDEFLATE_ZLIB_SUPPORT ON CACHE BOOL "" FORCE)
set(LIBDEFLATE_GZIP_SUPPORT OFF CACHE BOOL "" FORCE)
set(LIBDEFLATE_BUILD_GZIP OFF CACHE BOOL "" FORCE)
add_subdirectory(libdeflate)

add_library(
        LibdeflateJNI SHARED org_by1337_btcp_common_io_zip_natives_LibdeflateZLibCompressor.c
        org_by1337_btcp_common_io_zip_natives_LibdeflateZLibDecompressor.c
)

target_link_libraries(${CMAKE_PROJECT_NAME} libdeflate_static)