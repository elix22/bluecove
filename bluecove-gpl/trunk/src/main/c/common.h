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
 * @version $Id$
 */

#ifndef _BLUECOVE_COMMON_H
#define	_BLUECOVE_COMMON_H

#include <jni.h>

// Error handling

void throwException(JNIEnv *env, const char *name, const char *fmt, ...);

void throwRuntimeException(JNIEnv *env, const char *fmt, ...);

void throwIOException(JNIEnv *env, const char *fmt, ...);

void throwServiceRegistrationException(JNIEnv *env, const char *fmt, ...);

void throwBluetoothStateException(JNIEnv *env, const char *fmt, ...);

void throwBluetoothConnectionException(JNIEnv *env, int error, const char *fmt, ...);

// Interaction with java classes

bool isCurrentThreadInterrupted(JNIEnv *env, jobject peer);

class DeviceInquiryCallback {
private:
    jobject peer;
    jmethodID deviceDiscoveredCallbackMethod;

    jobject startedNotify;
    jmethodID startedNotifyNotifyMethod;

public:
    DeviceInquiryCallback();
    bool builDeviceInquiryCallbacks(JNIEnv * env, jobject peer, jobject startedNotify);
    bool callDeviceInquiryStartedCallback(JNIEnv * env);
    bool callDeviceDiscovered(JNIEnv * env, jobject listener, jlong deviceAddr, jint deviceClass, jstring name, jboolean paired);
};

#endif	/* _BLUECOVE_COMMON_H */

