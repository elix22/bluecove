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
#include "BlueCoveBlueZ.h"
#include <bluetooth/bluetooth.h>
#include <bluetooth/hci.h>
#include <bluetooth/hci_lib.h>

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeGetDeviceID
(JNIEnv *env, jobject thisObject) {
	int dev_id = hci_get_route(NULL);
	if (dev_id < 0) {
	    debug("hci_get_route : %i", dev_id);
	    throwBluetoothStateException(env, "Bluetooth Device is not available");
	    return 0;
	} else {
	    return dev_id;
    }
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeOpenDevice
(JNIEnv *env, jobject thisObject, jint deviceID) {
	int deviceDescriptor = hci_open_dev(deviceID);
	if (deviceDescriptor < 0) {
	    debug("hci_open_dev : %i", deviceDescriptor);
		throwBluetoothStateException(env, "HCI device open failed");
		return 0;
	}
	return deviceDescriptor;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeCloseDevice
(JNIEnv *env, jobject thisObject, jint deviceDescriptor) {
	hci_close_dev(deviceDescriptor);
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_getLocalDeviceBluetoothAddressImpl
(JNIEnv *env, jobject, jint deviceDescriptor) {
	bdaddr_t address;
	int error = hci_read_bd_addr(deviceDescriptor, &address, TIMEOUT);
	if (error != 0) {
	    switch (error) {
        case HCI_HARDWARE_FAILURE:
            throwBluetoothStateException(env, "Bluetooth Device is not available");
	    default:
	        throwBluetoothStateException(env, "Bluetooth Device is not ready. %d", error);
        }
	    return 0;
	}
	return deviceAddrToLong(&address);
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeGetDeviceName
(JNIEnv *env, jobject thisObject, jint deviceDescriptor) {
	char* name = new char[DEVICE_NAME_MAX_SIZE];
	jstring nameString = NULL;
	if (!hci_local_name(deviceDescriptor, 100, name, TIMEOUT)) {
		nameString = env->NewStringUTF(name);
	}
	delete[] name;
	return nameString;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeGetDeviceClass
(JNIEnv *env, jobject thisObject, jint deviceDescriptor) {
	uint8_t deviceClass[3];
	if (!hci_read_class_of_dev(deviceDescriptor, deviceClass, TIMEOUT)) {
		return deviceClassBytesToInt(deviceClass);
	} else {
	    return 0xff000000;
	}
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeSetLocalDeviceDiscoverable
(JNIEnv *env, jobject thisObject, jint deviceDescriptor, jint mode) {
	uint8_t lap[3];
	lap[0] = mode & 0xff;
	lap[1] = (mode & 0xff00)>>8;
	lap[2] = (mode & 0xff0000)>>16;
	return hci_write_current_iac_lap(deviceDescriptor, 1, lap, TIMEOUT);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeGetLocalDeviceDiscoverable
(JNIEnv *env, jobject thisObject, jint deviceDescriptor) {
	uint8_t lap[3];
	uint8_t num_iac;
	int error = hci_read_current_iac_lap(deviceDescriptor,&num_iac,lap,TIMEOUT);
    //M.S.	I don't know why to check for num_iac to be less than or equal to one but avetana to this.
	if ((error < 0) || (num_iac > 1)) {
		throwRuntimeException(env, "Unable to retrieve the local discovery mode. It may be because you are not root");
		return 0;
	}
	return (lap[0] & 0xff) | ((lap[1] & 0xff) << 8) | ((lap[2] & 0xff) << 16);
}

