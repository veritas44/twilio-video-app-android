#ifndef VIDEO_ANDROID_COM_TWILIO_VIDEO_CONNECT_OPTIONS_H_
#define VIDEO_ANDROID_COM_TWILIO_VIDEO_CONNECT_OPTIONS_H_

#include <jni.h>
#include "video/connect_options.h"

#ifdef __cplusplus
extern "C" {
#endif

namespace twilio_video_jni {

JNIEXPORT jlong JNICALL Java_com_twilio_video_ConnectOptions_nativeCreate
        (JNIEnv *, jobject, jstring, jstring, jobjectArray, jobjectArray, jobject, jboolean, jlong);


}

#ifdef __cplusplus
}
#endif

#endif //  VIDEO_ANDROID_COM_TWILIO_VIDEO_CONNECT_OPTIONS_H_
