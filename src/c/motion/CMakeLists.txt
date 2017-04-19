cmake_minimum_required(VERSION 2.8)
project(robotvision)
set(EXECUTABLE_OUTPUT_PATH bin)
set(LIBRARY_OUTPUT_PATH lib)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")

#ADD_DEFINITIONS(-DCPU_ONLY)
#ADD_DEFINITIONS("-g")
#find_package(OpenCV REQUIRED)

include_directories(
-I include -I main/include -I functions/include 
-I motion/include -I json/include -I test/decoder/include
-I external/include
  )

LINK_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR}/external/libs)

message (STATUS "LINK_LIBRARY_PATG=${JNI_INCLUDE_DIRS}/external/libs")

add_library(sengled_algorithm_base SHARED 
		motion/motionAction.cpp
		main/AlgorithmManager.cpp
		main/robot_vision.cpp
        functions/vibe-background-sequential.cpp		
		json/cJSON.c
        main/sengled_algorithm_base.c
)


target_link_libraries(sengled_algorithm_base 
  -lopencv_highgui -lopencv_video -lopencv_imgproc -lopencv_core -lopencv_ml -lopencv_objdetect 
  -lIlmImf -lIlmThread -lIlmImfUtil -lImath -lIex -lIexMath -ltiff -lHalf
  -llibjpeg -llibpng -llibjasper -lz -llzma -llapack -ltmglib -lblas_LINUX -lgfortran -lpthread ) 

