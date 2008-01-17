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
#define CPP__FILE "BlueCoveBlueZ_Discovery.cc"

#include "BlueCoveBlueZ.h"
#include <bluetooth/bluetooth.h>
#include <bluetooth/hci.h>
#include <bluetooth/hci_lib.h>

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_runDeviceInquiryImpl
(JNIEnv *env, jobject peer, jobject startedNotify, jint deviceID, jint deviceDescriptor, jint accessCode, jint inquiryLength, jint maxResponses, jobject listener) {
	DeviceInquiryCallback callback;
    if (!callback.builDeviceInquiryCallbacks(env, peer, startedNotify)) {
        return INQUIRY_ERROR;
    }
    if (!callback.callDeviceInquiryStartedCallback(env)) {
		return INQUIRY_ERROR;
	}
	int max_rsp = maxResponses;
	inquiry_info *ii = NULL;
	int num_rsp = hci_inquiry(deviceID, inquiryLength, max_rsp, NULL, &ii, accessCode);
	int rc = INQUIRY_COMPLETED;
	if (num_rsp < 0) {
		rc = INQUIRY_ERROR;
	} else {
	    for(int i = 0; i < num_rsp; i++) {
		    bdaddr_t* address = &(ii+i)->bdaddr;
		    jlong addressLong = deviceAddrToLong(address);
		    uint8_t *dev_class = (ii+i)->dev_class;
		    int deviceClass = deviceClassBytesToInt(dev_class);

            jboolean paired = false; // TODO

            jstring name = NULL; // Names are stored in RemoteDeviceHelper and can be reused.

		    if (!callback.callDeviceDiscovered(env, listener, addressLong, deviceClass, name, paired)) {
			    rc = INQUIRY_ERROR;
			    break;
		    }
		}
	}
	free(ii);
	return rc;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_deviceInquiryCancelImpl
(JNIEnv *env, jobject peer, jint deviceDescriptor) {
    int err = hci_send_cmd(deviceDescriptor, OGF_LINK_CTL, OCF_INQUIRY_CANCEL, 0, NULL);
    return (err == 0);
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_getRemoteDeviceFriendlyNameImpl
(JNIEnv *env, jobject peer, jint deviceDescriptor, jlong remoteAddress) {
	bdaddr_t address;
    longToDeviceAddr(remoteAddress, &address);
	char name[DEVICE_NAME_MAX_SIZE];
	int error = hci_read_remote_name(deviceDescriptor, &address, sizeof(name), name, READ_REMOTE_NAME_TIMEOUT);
	if (error < 0) {
		throwIOException(env, "Can not get remote device name");
		return NULL;
	}
	return env->NewStringUTF(name);
}
