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
#include <bluetooth/sdp.h>
#include <bluetooth/sdp_lib.h>
#include <vector>
using std::vector;

#include <iostream>
using namespace std;

JNIEXPORT jlongArray JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeSearchServices(JNIEnv *env, jobject thisObject, jobjectArray uuidSet, jlong remoteDeviceAddress)
{
	// convert uuid set from java array to bluez sdp_list_t
	jsize uuidSetSize=env->GetArrayLength(uuidSet);
	sdp_list_t *uuidList=NULL;
	for(jsize i=0;i<uuidSetSize;i++)
	{
		jobject uuidObject=env->GetObjectArrayElement(uuidSet,i);
		uuid_t uuid=getUuidFromJavaUUID(env,uuidObject);
		uuidList=sdp_list_append(uuidList,&uuid);
	}

	const uint16_t max_rec_num=256;
	sdp_list_t *rsp_list=NULL;

	// convert remote device address from jlong to bluez bdaddr_t
	bdaddr_t remoteAddress;
	memcpy(remoteAddress.b,&remoteDeviceAddress,sizeof(remoteAddress));

	// connect to the device to retrieve services
	sdp_session_t *session=sdp_connect(BDADDR_ANY,&remoteAddress,SDP_RETRY_IF_BUSY);

	// if connection is not established throw an exception
	if(session==NULL)
	{
		char addressChars[17];
		ba2str(&remoteAddress,addressChars);
		char message[]="Can not retrieve service records from remote device name with address ";
		char* messageWithAddress=new char[sizeof(message)+sizeof(addressChars)];
		strcpy(messageWithAddress,message);
		strcat(messageWithAddress,addressChars);
		env->ThrowNew(env->FindClass("com/intel/bluetooth/SearchServicesDeviceNotReachableException"),messageWithAddress);
		sdp_list_free(uuidList,NULL);
		return NULL;
	}

	// then ask the device for service record handles
	int error=sdp_service_search_req(session,uuidList,max_rec_num,&rsp_list);

	// again, if there is an error retrieving service records, throw an exception
	if(error)
	{
		char addressChars[17];
		ba2str(&remoteAddress,addressChars);
		char message[]="Can not retrieve service records from remote device name with address ";
		char* messageWithAddress=new char[sizeof(message)+sizeof(addressChars)];
		strcpy(messageWithAddress,message);
		strcat(messageWithAddress,addressChars);
		env->ThrowNew(env->FindClass("com/intel/bluetooth/SearchServicesException"),messageWithAddress);
		sdp_list_free(uuidList,NULL);
		return NULL;
	}

	// convert retrieved records from linked list to vector (to retrieve its size to put it finally in java array)
	vector<uint32_t> serviceHandles;
	sdp_list_t* handle=rsp_list;
	for(;handle;handle=handle->next)
	{
		uint32_t record=*(uint32_t*)handle->data;
		serviceHandles.push_back(record);
	}

	// convert the vertor to a java array
	jsize numOfHandles=(jsize)serviceHandles.size();
	jlongArray handlesArray=env->NewLongArray(numOfHandles);
	for(int i=0;i<numOfHandles;i++)
		env->SetLongArrayRegion(handlesArray,i,1,(jlong*)&serviceHandles[i]);

	// clear and return
	sdp_list_free(uuidList,NULL);
	sdp_list_free(rsp_list,NULL);
	sdp_close(session);

	return handlesArray;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativePopulateServiceRecordAttributeValues(JNIEnv *env, jobject thisObject, jlong remoteDeviceAddressLong, jlong handle, jintArray attrIDs, jobject serviceRecord)
{
	bdaddr_t remoteDeviceAddress;
	memcpy(remoteDeviceAddress.b,&remoteDeviceAddressLong,sizeof(bdaddr_t));
	sdp_session_t *session=sdp_connect(BDADDR_ANY,&remoteDeviceAddress,SDP_RETRY_IF_BUSY);
//	cout<<"1"<<endl;

	sdp_list_t *attr_list=NULL;
	jboolean isCopy=JNI_FALSE;
	jint* ids=env->GetIntArrayElements(attrIDs,&isCopy);
	for(int i=0;i<env->GetArrayLength(attrIDs);i++)
	{
		uint16_t* id=new uint16_t;
		*id=(uint16_t)ids[i];
//		cout<<"id:"<<ids[i]<<"\t"<<*id<<endl;
		attr_list=sdp_list_append(attr_list,id);
	}
//	cout<<"2"<<endl;
//	sdp_list_t* atrli=attr_list;
//	cout<<"list : ";
//	while(atrli)
//	{
//		uint16_t* atrid=(uint16_t*)atrli->data;
//		cout<<*atrid<<" ";
//		atrli=atrli->next;
//	}
//	cout<<endl<<endl;

	sdp_record_t *sdpRecord=sdp_service_attr_req(session,(uint32_t)handle,SDP_ATTR_REQ_INDIVIDUAL,attr_list);
//	cout<<"2.5"<<endl;
	if(!sdpRecord)
	{
//		cout<<"will return false"<<endl<<endl;
		sdp_list_free(attr_list,NULL);
		sdp_close(session);
		return JNI_FALSE;
	}
//	cout<<"3"<<endl;
	populateServiceRecord(env,serviceRecord,sdpRecord,attr_list);
	//cout<<"attributes populated"<<endl;

//	cout<<"4"<<endl;
	sdp_record_free(sdpRecord);
	sdp_list_free(attr_list,NULL);
	sdp_close(session);

//	cout<<"5"<<endl<<endl;
	return JNI_TRUE;
}
