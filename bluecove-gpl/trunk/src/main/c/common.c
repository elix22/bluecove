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
#define CPP__FILE "common.cc"

#include "common.h"
#include <stdio.h>

const char* cRuntimeException = "java/lang/RuntimeException";
const char* cIOException = "java/io/IOException";
const char* cInterruptedIOException = "java/io/InterruptedIOException";
const char* cBluetoothStateException = "javax/bluetooth/BluetoothStateException";
const char* cBluetoothConnectionException = "javax/bluetooth/BluetoothConnectionException";
const char* cServiceRegistrationException = "javax/bluetooth/ServiceRegistrationException";

// --- Debug

bool nativeDebugCallbackEnabled = false;
static jclass nativeDebugListenerClass;
static jmethodID nativeDebugMethod = NULL;

void enableNativeDebug(JNIEnv *env, jobject loggerClass, jboolean on) {
	if (on) {
		if (nativeDebugCallbackEnabled) {
			return;
		}
		nativeDebugListenerClass = (jclass)env->NewGlobalRef(loggerClass);
		if (nativeDebugListenerClass != NULL) {
			nativeDebugMethod = env->GetStaticMethodID(nativeDebugListenerClass, "nativeDebugCallback", "(Ljava/lang/String;ILjava/lang/String;)V");
			if (nativeDebugMethod != NULL) {
				nativeDebugCallbackEnabled = true;
				debug("nativeDebugCallback ON");
			}
		}
	} else {
		nativeDebugCallbackEnabled = false;
	}
}

void callDebugListener(JNIEnv *env, const char* fileName, int lineN, const char *fmt, ...) {
	va_list ap;
	va_start(ap, fmt);
	{
		if ((env != NULL) && (nativeDebugCallbackEnabled)) {
			char msg[1064];
			vsnprintf(msg, 1064, fmt, ap);
			env->CallStaticVoidMethod(nativeDebugListenerClass, nativeDebugMethod, env->NewStringUTF(fileName), lineN, env->NewStringUTF(msg));
		}
	}
	va_end(ap);
}

void ndebug(const char *fmt, ...) {
	va_list ap;
	va_start(ap, fmt);
	if (nativeDebugCallbackEnabled) {
	    fprintf(stdout, "NATIVE:");
        vfprintf(stdout, fmt, ap);
        fprintf(stdout, "\n");
        fflush(stdout);
    }
    va_end(ap);
}

// --- Error handling

void vthrowException(JNIEnv *env, const char *name, const char *fmt, va_list ap) {
	char msg[1064];
    if (env == NULL) {
		return;
	}
    vsnprintf(msg, 1064, fmt, ap);
	if (env->ExceptionCheck()) {
		ndebug("ERROR: can't throw second exception %s(%s)", name, msg);
		return;
	}
	jclass cls = env->FindClass(name);
    /* if cls is NULL, an exception has already been thrown */
    if (cls != NULL) {
        env->ThrowNew(cls, msg);
        /* free the local ref */
        env->DeleteLocalRef(cls);
	} else {
	    debug("Can't find Exception %s", name);
		env->FatalError(name);
	}

}

void throwException(JNIEnv *env, const char *name, const char *fmt, ...) {
    va_list ap;
	va_start(ap, fmt);
	vthrowException(env, name, fmt, ap);
	va_end(ap);
}

void throwRuntimeException(JNIEnv *env, const char *fmt, ...) {
    va_list ap;
	va_start(ap, fmt);
	vthrowException(env, cRuntimeException, fmt, ap);
	va_end(ap);
}

void throwIOException(JNIEnv *env, const char *fmt, ...) {
	va_list ap;
	va_start(ap, fmt);
	vthrowException(env, cIOException, fmt, ap);
	va_end(ap);
}

void throwInterruptedIOException(JNIEnv *env, const char *fmt, ...) {
	va_list ap;
	va_start(ap, fmt);
	vthrowException(env, cInterruptedIOException, fmt, ap);
	va_end(ap);
}

void throwServiceRegistrationException(JNIEnv *env, const char *fmt, ...) {
    va_list ap;
	va_start(ap, fmt);
	vthrowException(env, cServiceRegistrationException, fmt, ap);
	va_end(ap);
}

void throwBluetoothStateException(JNIEnv *env, const char *fmt, ...) {
   va_list ap;
	va_start(ap, fmt);
	vthrowException(env, cBluetoothStateException, fmt, ap);
	va_end(ap);
}

void throwBluetoothConnectionException(JNIEnv *env, int error, const char *fmt, ...) {
    va_list ap;
	va_start(ap, fmt);

	char msg[1064];
	if (env == NULL) {
	    va_end(ap);
		return;
	}
	vsnprintf(msg, 1064, fmt, ap);

	if (env->ExceptionCheck()) {
		debug("ERROR: can't throw second exception %s(%s)", cBluetoothConnectionException, msg);
		va_end(ap);
		return;
	}
	jclass cls = env->FindClass(cBluetoothConnectionException);
    /* if cls is NULL, an exception has already been thrown */
    if (cls != NULL) {
		jmethodID methodID = env->GetMethodID(cls, "<init>", "(ILjava/lang/String;)V");
		if (methodID == NULL) {
			env->FatalError("Fail to get constructor for Exception");
		} else {
			jstring excMessage = env->NewStringUTF(msg);
			jthrowable obj = (jthrowable)env->NewObject(cls, methodID, error, excMessage);
			if (obj != NULL) {
				env->Throw(obj);
			} else {
				env->FatalError("Fail to create new Exception");
			}
		}
        /* free the local ref */
        env->DeleteLocalRef(cls);
	} else {
		env->FatalError(cBluetoothConnectionException);
	}

	va_end(ap);
}

// --- Interaction with java classes

bool isCurrentThreadInterrupted(JNIEnv *env, jobject peer) {
	jclass peerClass = env->GetObjectClass(peer);
	if (peerClass == NULL) {
		throwRuntimeException(env, "Fail to get Object Class");
		return true;
	}
	jmethodID aMethod = env->GetMethodID(peerClass, "isCurrentThreadInterruptedCallback", "()Z");
	if (aMethod == NULL) {
		throwRuntimeException(env, "Fail to get MethodID isCurrentThreadInterruptedCallback");
		return true;
	}
	if (env->CallBooleanMethod(peer, aMethod)) {
		throwInterruptedIOException(env, "thread interrupted");
		return true;
	}
	return env->ExceptionCheck();
}

jmethodID getGetMethodID(JNIEnv * env, jclass clazz, const char *name, const char *sig) {
    if (clazz == NULL) {
        throwRuntimeException(env, "Fail to get MethodID %s for NULL class", name);
	    return NULL;
    }
    jmethodID methodID = env->GetMethodID(clazz, name, sig);
    if (methodID == NULL) {
	    throwRuntimeException(env, "Fail to get MethodID %s", name);
	    return NULL;
	}
	return methodID;
}

void DeviceInquiryCallback_Init(DeviceInquiryCallback* callback) {
    callback->peer = NULL;
    callback->deviceDiscoveredCallbackMethod = NULL;
    callback->startedNotify = NULL;
    callback->startedNotifyNotifyMethod = NULL;
}

bool DeviceInquiryCallback_builDeviceInquiryCallbacks(JNIEnv * env, DeviceInquiryCallback* callback, jobject peer, jobject startedNotify) {
    jclass peerClass = env->GetObjectClass(peer);

	if (peerClass == NULL) {
		throwRuntimeException(env, "Fail to get Object Class");
		return false;
	}

	jmethodID deviceDiscoveredCallbackMethod = env->GetMethodID(peerClass, "deviceDiscoveredCallback", "(Ljavax/bluetooth/DiscoveryListener;JILjava/lang/String;Z)V");
	if (deviceDiscoveredCallbackMethod == NULL) {
		throwRuntimeException(env, "Fail to get MethodID deviceDiscoveredCallback");
		return false;
	}

	jclass notifyClass = env->GetObjectClass(startedNotify);
	if (notifyClass == NULL) {
		throwRuntimeException(env, "Fail to get Object Class");
		return false;
	}
	jmethodID notifyMethod = env->GetMethodID(notifyClass, "deviceInquiryStartedCallback", "()V");
	if (notifyMethod == NULL) {
		throwRuntimeException(env, "Fail to get MethodID deviceInquiryStartedCallback");
		return false;
	}

    callback->peer = peer;
    callback->deviceDiscoveredCallbackMethod = deviceDiscoveredCallbackMethod;
    callback->startedNotify = startedNotify;
    callback->startedNotifyNotifyMethod = notifyMethod;

	return true;
}

bool DeviceInquiryCallback_callDeviceInquiryStartedCallback(JNIEnv * env, DeviceInquiryCallback* callback) {
    if ((callback->startedNotify == NULL) || (callback->startedNotifyNotifyMethod == NULL)) {
        throwRuntimeException(env, "DeviceInquiryCallback not initialized");
        return false;
    }
    env->CallVoidMethod(callback->startedNotify, callback->startedNotifyNotifyMethod);
    if (env->ExceptionCheck()) {
        return false;
    } else {
        return true;
    }
}

bool DeviceInquiryCallback_callDeviceDiscovered(JNIEnv * env, DeviceInquiryCallback* callback, jobject listener, jlong deviceAddr, jint deviceClass, jstring name, jboolean paired) {
    if ((callback->peer == NULL) || (callback->deviceDiscoveredCallbackMethod == NULL)) {
        throwRuntimeException(env, "DeviceInquiryCallback not initialized");
        return false;
    }
    env->CallVoidMethod(callback->peer, callback->deviceDiscoveredCallbackMethod, listener, deviceAddr, deviceClass, name, paired);
	if (env->ExceptionCheck()) {
        return false;
    } else {
        return true;
    }
}

