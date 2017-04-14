#!/bin/bash

base_path=$(cd `dirname $0`/..; pwd)

g++ -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -O0 -g3 -Wall -c -fmessage-length=0 -v -fPIC -MMD -MP -MF"com_sengled_media_jni_JNIFunction.d" -MT"com_sengled_media_jni_JNIFunction.d" -o "com_sengled_media_jni_JNIFunction.o" "./com_sengled_media_jni_JNIFunction.cpp"


g++ -shared -o "liblog4c.so"  ./com_sengled_media_jni_JNIFunction.o 
