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
 * @version $Id$
 */
#define CPP__FILE "BlueCoveBlueZ_SDPServer.cc"

#include "BlueCoveBlueZ.h"

#include <bluetooth/sdp_lib.h>

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_registerSDPServiceImpl
  (JNIEnv* env, jobject, jint deviceDescriptor, jbyteArray record) {
    bdaddr_t localAddr;
	int error = hci_read_bd_addr(deviceDescriptor, &localAddr, LOCALDEVICE_ACCESS_TIMEOUT);
	if (error != 0) {
        throwServiceRegistrationException(env, "Bluetooth Device is not ready. [%d] %s", errno, strerror(errno));
	    return 0;
	}

    sdp_session_t* session = sdp_connect(BDADDR_ANY, BDADDR_LOCAL, SDP_RETRY_IF_BUSY);
	if (!session) {
		throwServiceRegistrationException(env, "Can not open SDP session. [%d] %s", errno, strerror(errno));
		return 0;
	}

	int length = env->GetArrayLength(record);
	jbyte *bytes = env->GetByteArrayElements(record, 0);
    int flags = 0;
    uint32_t handle;
    int err = sdp_device_record_register_binary(session, &localAddr, (uint8_t*)bytes, length, flags, &handle);
    env->ReleaseByteArrayElements(record, bytes, 0);

    if (err != 0) {
        throwServiceRegistrationException(env, "Can not register SDP record. [%d] %s", errno, strerror(errno));
        return 0;
    }

	return (jlong)session;
}

/*
 * Class:     com_intel_bluetooth_BluetoothStackBlueZ
 * Method:    unregisterSDPServiceImpl
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_unregisterSDPServiceImpl
  (JNIEnv* env, jobject, jlong sdpSessionHandle) {
    if (sdp_close((sdp_session_t*)sdpSessionHandle) < 0) {
        throwServiceRegistrationException(env, "Can not close SDP session. [%d] %s", errno, strerror(errno));
    }
}