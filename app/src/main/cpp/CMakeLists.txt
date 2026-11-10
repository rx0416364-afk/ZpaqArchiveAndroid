cmake_minimum_required(VERSION 3.22.1)
project(zpaqengine LANGUAGES CXX)

# The upstream command-line entry point is exposed to the JNI bridge as zpaq_main.
set_source_files_properties(zpaq.cpp PROPERTIES COMPILE_DEFINITIONS "main=zpaq_main")

add_library(
    zpaqengine
    SHARED
    zpaq.cpp
    libzpaq.cpp
    jni_bridge.cpp
)

target_compile_features(zpaqengine PRIVATE cxx_std_14)
target_compile_definitions(zpaqengine PRIVATE unix=1 NOJIT=1)
target_compile_options(zpaqengine PRIVATE -O3 -fexceptions -frtti -Wno-unused-parameter)

find_library(log-lib log)
target_link_libraries(zpaqengine PRIVATE ${log-lib})

target_compile_definitions(zpaqengine PRIVATE _LARGEFILE64_SOURCE=1 fseeko=fseeko64 ftello=ftello64)
