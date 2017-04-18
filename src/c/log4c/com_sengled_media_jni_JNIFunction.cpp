/*
 * com_sengled_media_jni_JNIFunction.cpp
 *
 *  Created on: Apr 11, 2017
 *      Author: chenxh
 */


#include "com_sengled_media_jni_JNIFunction.h"
#include <string.h>
#include <stdio.h>

static JavaVM* LOG4C_VM = NULL;
static jclass LOG4C_CLASS = NULL;
static jmethodID LOG4C_METHOD_ID = 0;

void LOG4C_Funtion (int level, const char* chars) {
	jstring info;
	jint version = JNI_VERSION_1_8;
	JavaVMAttachArgs* args = NULL;

	JNIEnv *env;
	jint got;

	got = LOG4C_VM->GetEnv((void**)&env, version);
	switch(got){
	case JNI_OK:
		info = env->NewStringUTF(chars);
		env->CallStaticVoidMethod(LOG4C_CLASS, LOG4C_METHOD_ID, level, info);
		env->DeleteLocalRef(info);
		break;
	case JNI_EDETACHED:
		got = LOG4C_VM->AttachCurrentThreadAsDaemon((void**)&env, (void*)args);
		if (JNI_OK == got) {
			info = env->NewStringUTF(chars);
			env->CallStaticVoidMethod(LOG4C_CLASS, LOG4C_METHOD_ID, level, info);
			env->DeleteLocalRef(info);
			LOG4C_VM->DetachCurrentThread();
		}
		break;
	}
}

JNIEXPORT jlong JNICALL Java_com_sengled_media_jni_JNIFunction_getLog4CFunction
  (JNIEnv *env, jobject logger) {
	jlong rst = NULL;
	jint result = env->GetJavaVM(&LOG4C_VM);
	if (JNI_OK != result) {
		return result;
	}


	jclass loggerClass = env->GetObjectClass(logger);
	LOG4C_METHOD_ID = env->GetStaticMethodID(loggerClass, "log", "(ILjava/lang/String;)V");

	if (!LOG4C_METHOD_ID) {
		goto end;
	}

	if (NULL != LOG4C_CLASS) {
		env->DeleteGlobalRef(LOG4C_CLASS);
	}

	LOG4C_CLASS = (jclass)env->NewGlobalRef(loggerClass);
	rst = (jlong)LOG4C_Funtion;

end:
	env->DeleteLocalRef(loggerClass);
	return rst;
}
JNIEXPORT jint JNICALL Java_com_sengled_media_jni_JNIFunction_invokeLog4CFunction
(JNIEnv *env, jobject, jlong funcPtr, jint level, jstring log) {
	void (*funtion) (int level, const char* chars);
	funtion = (void(*)(int level, const char* chars))funcPtr;

	jboolean copy = JNI_FALSE;
	const char* chars = env->GetStringUTFChars(log, &copy);
	funtion(level, chars);
	env->ReleaseStringUTFChars(log, chars);

	return 1;
}

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = NULL;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_8) != JNI_OK) {
    	printf("log: %s\r", "I need JNI_VERSION_1_8!");
        goto bail;
    }
    result = JNI_VERSION_1_8;

    bail:
    return result;
}

