/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2008 Mina Shokry
 *  Copyright (C) 2007 Vlad Skarzhevskyy
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
 * @version $Id: BlueCoveBlueZ_Discovery.cc 1525 2008-01-14 22:18:27Z skarzhevskyy $
 */

#include "common.h"
#include <stdio.h>

const char* cRuntimeException = "java/lang/RuntimeException";
const char* cIOException = "java/io/IOException";
const char* cInterruptedIOException = "java/io/InterruptedIOException";
const char* cBluetoothStateException = "javax/bluetooth/BluetoothStateException";
const char* cBluetoothConnectionException = "javax/bluetooth/BluetoothConnectionException";
const char* cServiceRegistrationException = "javax/bluetooth/ServiceRegistrationException";

// Error handling

void vthrowException(JNIEnv *env, const char *name, const char *fmt, va_list ap) {
	char msg[1064];
	vsnprintf(msg, 1064, fmt, ap);
    if (env == NULL) {
		return;
	}
	if (env->ExceptionCheck()) {
		//debugss("ERROR: can't throw second exception %s(%s)", name, msg);
		return;
	}
	jclass cls = env->FindClass(name);
    /* if cls is NULL, an exception has already been thrown */
    if (cls != NULL) {
        env->ThrowNew(cls, msg);
        /* free the local ref */
        env->DeleteLocalRef(cls);
	} else {
	    //debugs("Can't find Exception %s", name);
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
	vsnprintf(msg, 1064, fmt, ap);

	if (env == NULL) {
	    va_end(ap);
		return;
	}
	if (env->ExceptionCheck()) {
		//debugss("ERROR: can't throw second exception %s(%s)", cBluetoothConnectionException, msg);
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

// Interaction with java classes

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
		throwException(env, cInterruptedIOException, "thread interrupted");
		return true;
	}
	return env->ExceptionCheck();
}

DeviceInquiryCallback::DeviceInquiryCallback() {
    this->peer = NULL;
    this->deviceDiscoveredCallbackMethod = NULL;
    this->startedNotify = NULL;
    this->startedNotifyNotifyMethod = NULL;
}

bool DeviceInquiryCallback::builDeviceInquiryCallbacks(JNIEnv * env, jobject peer, jobject startedNotify) {
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

    this->peer = peer;
    this->deviceDiscoveredCallbackMethod = deviceDiscoveredCallbackMethod;
    this->startedNotify = startedNotify;
    this->startedNotifyNotifyMethod = notifyMethod;

	return true;
}

bool DeviceInquiryCallback::callDeviceInquiryStartedCallback(JNIEnv * env) {
    if ((this->startedNotify == NULL) || (this->startedNotifyNotifyMethod == NULL)) {
        throwRuntimeException(env, "DeviceInquiryCallback not initialized");
        return false;
    }
    env->CallVoidMethod(this->startedNotify, this->startedNotifyNotifyMethod);
    if (env->ExceptionCheck()) {
        return false;
    } else {
        return true;
    }
}

bool DeviceInquiryCallback::callDeviceDiscovered(JNIEnv * env, jobject listener, jlong deviceAddr, jint deviceClass, jstring name, jboolean paired) {
    if ((this->peer == NULL) || (this->deviceDiscoveredCallbackMethod == NULL)) {
        throwRuntimeException(env, "DeviceInquiryCallback not initialized");
        return false;
    }
    env->CallVoidMethod(this->peer, this->deviceDiscoveredCallbackMethod, listener, deviceAddr, deviceClass, name, paired);
	if (env->ExceptionCheck()) {
        return false;
    } else {
        return true;
    }
}

