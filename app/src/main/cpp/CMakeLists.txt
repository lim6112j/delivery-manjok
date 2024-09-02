cmake_minimum_required(VERSION 3.22.1)
set(pathPROJECT /Users/byeongcheollim/AndroidStudioProjects/jijigi)
set(pathOPENCV /Users/byeongcheollim/workspace/OpenCV-android-sdk/sdk)
set(pathLIBOPENCV_JAVA /Users/byeongcheollim/AndroidStudioProjects/jijigi/opencv/native/libs/${ANDROID_ABI}/libopencv_java4.so)
project(jijigi)
#[[
set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

]]
include_directories(${pathOPENCV}/native/jni/include)
# dummy target to bring libc++_shared.so into packages
add_library(
        ${CMAKE_PROJECT_NAME}
        SHARED
        ${pathPROJECT}/app/src/main/cpp/native-lib.cpp
)
add_library(
        lib_opencv SHARED IMPORTED
)
set_target_properties(
        lib_opencv PROPERTIES IMPORTED_LOCATION ${pathLIBOPENCV_JAVA}
)
find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)
target_link_libraries( # Specifies the target library.
        ${CMAKE_PROJECT_NAME}

        lib_opencv

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
