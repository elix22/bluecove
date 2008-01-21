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
#define CPP__FILE "BlueCoveBlueZ_RFCOMMServer.cc"

#include "BlueCoveBlueZ.h"

#include <sys/socket.h>
#include <sys/unistd.h>
#include <bluetooth/sdp_lib.h>
#include <bluetooth/rfcomm.h>


int dynamic_bind_rc(int sock, struct sockaddr_rc *sockaddr, uint8_t *port) {
	int err;
	for(*port=1;*port<=31;*port++) {
		sockaddr->rc_channel=*port;
		err=bind(sock,(struct sockaddr *)sockaddr,sizeof(sockaddr));
		if(!err)
			break;
	}
	if(*port==31) {
		err=-1;
	}
	return err;
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeOpenSession(JNIEnv *env, jobject thisObject) {
	sdp_session_t* session=sdp_connect(BDADDR_ANY,BDADDR_LOCAL,SDP_RETRY_IF_BUSY);
	if(!session)
		throwIOException(env,"can not open session to local device.");
	return (jlong)session;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeCloseSession(JNIEnv *env, jobject thisObject, jlong session) {
	sdp_close((sdp_session_t*)session);
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeOpenSocket(JNIEnv *env, jobject thisObject, jint type, jint protocol) {
	int socketHandler=socket(AF_BLUETOOTH,type,protocol);
	if(!socketHandler)
		throwIOException(env,"can not open socket.");
	return (jlong)socketHandler;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeCloseSocket(JNIEnv *env, jobject thisObject, jlong socketHandler) {
	close((int)socketHandler);
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeCreateRFCOMMServiceRecord(JNIEnv *env, jobject thisObject,
		jlong socketHandler, jbyteArray uuid, jstring name, jboolean authorize, jboolean authenticate, jboolean encrypt, jboolean master) {
	uuid_t rootUUID,l2capUUID,rfcommUUID,serviceUUID;
	sdp_uuid16_create(&rootUUID,PUBLIC_BROWSE_GROUP);
	sdp_uuid16_create(&l2capUUID,L2CAP_UUID);
	sdp_uuid16_create(&rfcommUUID,RFCOMM_UUID);
	convertUUIDByteArrayToUUID(env,uuid,&serviceUUID);

	sdp_record_t* serviceRecord=sdp_record_alloc();
	sdp_set_service_id(serviceRecord,serviceUUID);

	sdp_list_t *rootList,*rfcommList,*l2capList,*protocolList,*accessProtocolList;
	rootList=sdp_list_append(NULL,&rootUUID);
	l2capList=sdp_list_append(NULL,&l2capUUID);
	rfcommList=sdp_list_append(NULL,&rfcommUUID);

	sockaddr_rc socketAddress;
	socketAddress.rc_family = AF_BLUETOOTH;
	socketAddress.rc_bdaddr=*BDADDR_ANY;
	int error=dynamic_bind_rc((int)socketHandler,&socketAddress,&socketAddress.rc_channel);
	if(error) {
		throwIOException(env,"can not find channel for service");
		return 0;
	}

	sdp_data_t* channel=sdp_data_alloc(SDP_UINT8,&socketAddress.rc_channel);
	rfcommList=sdp_list_append(rfcommList,channel);

	// TODO find a way to attach flags to the service record (authorize, authenticate, encrypt, master);

	sdp_set_browse_groups(serviceRecord,rootList);
	protocolList=sdp_list_append(NULL,l2capList);
	protocolList=sdp_list_append(protocolList,rfcommList);
	accessProtocolList=sdp_list_append(NULL,protocolList);

	sdp_set_access_protos(serviceRecord,accessProtocolList);
	jboolean isCopy=JNI_FALSE;
	const char* nameChars=env->GetStringUTFChars(name,&isCopy);
	sdp_set_info_attr(serviceRecord,nameChars,"BlueCove","service offered by BlueCove");

	sdp_data_free(channel);
	sdp_list_free(rootList,NULL);
	sdp_list_free(l2capList,NULL);
	sdp_list_free(rfcommList,NULL);
	sdp_list_free(protocolList,NULL);
	sdp_list_free(accessProtocolList,NULL);

	return (jlong)serviceRecord;
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeRegisterServiceRecord(JNIEnv *env, jobject thisObject, jlong session, jlong serviceRecord) {
	sdp_record_t* sdpRecord=(sdp_record_t*)serviceRecord;
	if(sdp_record_register((sdp_session_t*)session,sdpRecord,SDP_RECORD_PERSIST)) {
		throwIOException(env,"can not register service record");
		return 0;
	}
	sdp_data_t* handleData=sdp_data_get(sdpRecord,SDP_ATTR_RECORD_HANDLE);
	jlong handle=handleData->val.uint32;
	return handle;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeUnregisterServiceRecord(JNIEnv *, jobject, jlong session, jlong serviceRecord) {
	sdp_record_unregister((sdp_session_t*)session,(sdp_record_t*)serviceRecord);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeListen(JNIEnv *env, jobject thisObject, jlong socket) {
	return (jint)listen((int)socket,1);
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeAccept(JNIEnv *env, jobject thisObject, jlong socket) {
	sockaddr_rc remoteAddress;
	socklen_t size;
	return (jlong)accept((int)socket,(sockaddr*)&remoteAddress,&size);
}
