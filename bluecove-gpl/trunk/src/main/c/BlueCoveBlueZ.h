// 
// File:   BlueCoveBlueZ.h
// Author: mina
//
// Created on December 24, 2007, 4:17 PM
//

#ifndef _BLUECOVEBLUEZ_H
#define	_BLUECOVEBLUEZ_H

#include "BluetoothStackBlueZ.h"
#include "DiscoveryListener.h"
#include "BlueCoveBlueZ.h"
#include <bluetooth/bluetooth.h>
#include <bluetooth/hci.h>
#include <bluetooth/hci_lib.h>
#include <bluetooth/sdp.h>
#include <bluetooth/sdp_lib.h>
#include <jni.h>

const int TIMEOUT=5000;
const int DEVICE_NAME_MAX_SIZE=248;

int deviceClassBytesToInt(uint8_t* deviceClass);

void populateServiceRecord(JNIEnv* env,jobject serviceRecrod,sdp_record_t* sdpRecord,sdp_list_t* attributeList);
jobject createDataElement(JNIEnv* env,sdp_data_t* data);
void reverseArray(jbyte* array,int length);
uuid_t getUuidFromJavaUUID(JNIEnv *env,jobject javaUUID);
jobject createJavaUUID(JNIEnv *env,uuid_t uuid);

#endif	/* _BLUECOVEBLUEZ_H */

