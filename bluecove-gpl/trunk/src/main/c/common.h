/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2008 Mina Shokry
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Author: vlads
 * Created on Java 15, 2008, extracted from bluecove code
 *
 * @version $Id$
 */

#ifndef _BLUECOVE_COMMON_H
#define _BLUECOVE_COMMON_H

#include <jni.h>

typedef unsigned char bool;
enum {true = 1, false = 0};

// --- Debug
#define STD_DEBUG
#define EXT_DEBUG

void enableNativeDebug(JNIEnv * env, jobject loggerClass, jboolean on);

void callDebugListener(JNIEnv *env, const char* fileName, int lineN, const char *fmt, ...);

#ifdef STD_DEBUG
// This can be used in JNI functions. The message would be sent to java code
#define debug(...) callDebugListener(env, CPP__FILE, __LINE__, __VA_ARGS__);
#else
#define debug(...)
#endif

#ifdef EXT_DEBUG
#define Edebug(...) callDebugListener(env, CPP__FILE, __LINE__, __VA_ARGS__);
#else
#define Edebug(...)
#endif

// This will use stdout and can be used in native function callbacks
void ndebug(const char *fmt, ...);

// --- Error handling

void throwException(JNIEnv *env, const char *name, const char *fmt, ...);
void throwRuntimeException(JNIEnv *env, const char *fmt, ...);
void throwIOException(JNIEnv *env, const char *fmt, ...);
void throwInterruptedIOException(JNIEnv *env, const char *fmt, ...);
void throwServiceRegistrationException(JNIEnv *env, const char *fmt, ...);
void throwBluetoothStateException(JNIEnv *env, const char *fmt, ...);
void throwBluetoothConnectionException(JNIEnv *env, int error, const char *fmt, ...);

// --- Interaction with java classes

jmethodID getGetMethodID(JNIEnv * env, jclass clazz, const char *name, const char *sig);

bool isCurrentThreadInterrupted(JNIEnv *env, jobject peer);

struct DeviceInquiryCallback {
    jobject peer;
    jmethodID deviceDiscoveredCallbackMethod;

    jobject startedNotify;
    jmethodID startedNotifyNotifyMethod;
};

void DeviceInquiryCallback_Init(struct DeviceInquiryCallback* callback);
bool DeviceInquiryCallback_builDeviceInquiryCallbacks(JNIEnv * env, struct DeviceInquiryCallback* callback, jobject peer, jobject startedNotify);
bool DeviceInquiryCallback_callDeviceInquiryStartedCallback(JNIEnv * env, struct DeviceInquiryCallback* callback);
bool DeviceInquiryCallback_callDeviceDiscovered(JNIEnv * env, struct DeviceInquiryCallback* callback, jobject listener, jlong deviceAddr, jint deviceClass, jstring name, jboolean paired);


#endif  /* _BLUECOVE_COMMON_H */

