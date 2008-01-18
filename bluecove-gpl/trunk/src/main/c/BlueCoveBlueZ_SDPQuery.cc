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
#define CPP__FILE "BlueCoveBlueZ_SDPQuery.cc"

#include "BlueCoveBlueZ.h"
#include <bluetooth/bluetooth.h>
#include <bluetooth/hci.h>
#include <bluetooth/hci_lib.h>
#include <bluetooth/sdp.h>
#include <bluetooth/sdp_lib.h>

// Used to free memory automaticaly
class SDPQueryData {
public:
    sdp_list_t *uuidList;
    sdp_list_t *rsp_list;
    sdp_session_t *session;

    SDPQueryData();
    ~SDPQueryData();
};

void populateServiceRecord(JNIEnv *env, jobject serviceRecord, sdp_record_t* sdpRecord, sdp_list_t* attributeList);

SDPQueryData::SDPQueryData() {
    uuidList = NULL;
    rsp_list = NULL;
    session = NULL;
}

SDPQueryData::~SDPQueryData() {
    sdp_list_free(uuidList, free);
    sdp_list_free(rsp_list, free);
    if (session != NULL) {
	    sdp_close(session);
    }
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_runSearchServicesImpl
  (JNIEnv *env, jobject peer, jobject searchServicesThread, jobjectArray uuidValues, jlong remoteDeviceAddressLong) {

    // Prepare serviceDiscoveredCallback
    jclass peerClass = env->GetObjectClass(peer);
	if (peerClass == NULL) {
		throwRuntimeException(env, "Fail to get Object Class");
		return SERVICE_SEARCH_ERROR;
	}

	jmethodID serviceDiscoveredCallback = env->GetMethodID(peerClass, "serviceDiscoveredCallback", "(Lcom/intel/bluetooth/SearchServicesThread;JJ)Z");
	if (serviceDiscoveredCallback == NULL) {
		throwRuntimeException(env, "Fail to get MethodID serviceDiscoveredCallback");
		return SERVICE_SEARCH_ERROR;
	}

    SDPQueryData data;

	// convert uuid set from java array to bluez sdp_list_t
	jsize uuidSetSize = env->GetArrayLength(uuidValues);
	for(jsize i = 0; i < uuidSetSize; i++) {
		jbyteArray byteArray = (jbyteArray)env->GetObjectArrayElement(uuidValues, i);
		uuid_t* uuid =  (uuid_t*)malloc(sizeof(uuid_t));
		convertUUIDByteArryaToUUID(env, byteArray, uuid);
		data.uuidList = sdp_list_append(data.uuidList, uuid);
	}

	// convert remote device address from jlong to bluez bdaddr_t
	bdaddr_t remoteAddress;
	longToDeviceAddr(remoteDeviceAddressLong, &remoteAddress);

	// connect to the device to retrieve services
	data.session = sdp_connect(BDADDR_ANY, &remoteAddress, SDP_RETRY_IF_BUSY);

	// if connection is not established throw an exception
	if (data.session == NULL) {
		return SERVICE_SEARCH_DEVICE_NOT_REACHABLE;
	}

	const uint16_t max_rec_num = 256;

	// then ask the device for service record handles
	int error = sdp_service_search_req(data.session, data.uuidList, max_rec_num, &(data.rsp_list));
	if (error) {
	    debug("sdp_service_search_req error %i", error);
		return SERVICE_SEARCH_ERROR;
	}

	// Notify java about found services
	sdp_list_t* handle = data.rsp_list;
	for(; handle; handle = handle->next) {
		uint32_t record = *(uint32_t*)handle->data;
		jboolean isTerminated = env->CallBooleanMethod(peer, serviceDiscoveredCallback, searchServicesThread, (jlong)record, (jlong)(data.session));
        if (env->ExceptionCheck()) {
            return SERVICE_SEARCH_ERROR;
        } else if (isTerminated) {
            return SERVICE_SEARCH_TERMINATED;
        }
	}
	return SERVICE_SEARCH_COMPLETED;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_populateServiceRecordAttributeValuesImpl
  (JNIEnv *env, jobject, jlong remoteDeviceAddressLong, jlong sdpSession, jlong handle, jintArray attrIDs, jobject serviceRecord) {

	SDPQueryData data;
	sdp_session_t* session = NULL;
	if (sdpSession == 0) {
	    session = (sdp_session_t*) sdpSession;
	} else {
	    bdaddr_t remoteAddress;
	    longToDeviceAddr(remoteDeviceAddressLong, &remoteAddress);
	    session = sdp_connect(BDADDR_ANY, &remoteAddress, SDP_RETRY_IF_BUSY);
	    if (session == NULL) {
	        return JNI_FALSE;
	    }
	    // Close session on exit
	    data.session = session;
    }
//	cout<<"1"<<endl;

	sdp_list_t *attr_list = NULL;
	jboolean isCopy = JNI_FALSE;
	jint* ids = env->GetIntArrayElements(attrIDs,&isCopy);
	for(int i=0; i < env->GetArrayLength(attrIDs); i++) {
		uint16_t* id = (uint16_t*)malloc(sizeof(uint16_t));
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

	sdp_record_t *sdpRecord = sdp_service_attr_req(session,(uint32_t)handle,SDP_ATTR_REQ_INDIVIDUAL,attr_list);
	if (!sdpRecord) {
        debug("sdp_service_attr_req return error");
		sdp_list_free(attr_list,NULL);
		return JNI_FALSE;
	}
//	cout<<"3"<<endl;
	populateServiceRecord(env, serviceRecord, sdpRecord, attr_list);
	//cout<<"attributes populated"<<endl;

//	cout<<"4"<<endl;
	sdp_record_free(sdpRecord);
	sdp_list_free(attr_list, NULL);

//	cout<<"5"<<endl<<endl;
	return JNI_TRUE;
}

char b2hex(int i) {
    static char hex[] = "0123456789abcdef";
    return hex[i];
}

jobject createJavaUUID(JNIEnv *env, uuid_t uuid) {
	jboolean shortUUID = true;
	const int strSize = 32;
	char uuidChars[strSize + 1];

	switch (uuid.type) {
	case SDP_UUID16:
		snprintf(uuidChars, strSize, "%.4x", uuid.value.uuid16);
		break;
	case SDP_UUID32:
		snprintf(uuidChars, strSize, "%.8x", uuid.value.uuid32);
		break;
	case SDP_UUID128: {
	    shortUUID = false;
	    int j = 0;
	    for(int i = 0; i < 16; i++) {
	        uuidChars[j++] = b2hex((uuid.value.uuid128.data[i]  >> 4) & 0xf);
	        uuidChars[j++] = b2hex(uuid.value.uuid128.data[i] & 0xf);
	    }
	    uuidChars[j] = 0;
		break;
	}
	default:
		return NULL;
	}

	jstring uuidString = env->NewStringUTF(uuidChars);
	jclass uuidClass = env->FindClass("javax/bluetooth/UUID");
	jmethodID constructorID = env->GetMethodID(uuidClass, "<init>", "(Ljava/lang/String;Z)V");
	return env->NewObject(uuidClass, constructorID, uuidString, shortUUID);
}

jobject createDataElement(JNIEnv *env, sdp_data_t *data) {
	Edebug("createDataElement 0x%x", data->dtd);
	jclass dataElementClass = env->FindClass("javax/bluetooth/DataElement");
	jmethodID constructorID;
	jobject dataElement = NULL;
	switch (data->dtd) {
		case SDP_DATA_NIL:
		{
			constructorID = env->GetMethodID(dataElementClass, "<init>", "(I)V");
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_NULL);
			break;
		}
		case SDP_BOOL:
		{
			jboolean boolean = data->val.uint8;
			constructorID = env->GetMethodID(dataElementClass, "<init>", "(Z)V");
			dataElement = env->NewObject(dataElementClass, constructorID, boolean);
			break;
		}
		case SDP_UINT8:
		{
			jlong value = (jlong)data->val.uint8;
			constructorID = env->GetMethodID(dataElementClass, "<init>", "(IJ)V");
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_U_INT_1, value);
			break;
		}
		case SDP_UINT16:
		{
			jlong value = (jlong)data->val.uint16;
			constructorID = env->GetMethodID(dataElementClass, "<init>", "(IJ)V");
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_U_INT_2, value);
			break;
		}
		case SDP_UINT32:
		{
			jlong value = (jlong)data->val.uint32;
			constructorID = env->GetMethodID(dataElementClass, "<init>", "(IJ)V");
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_U_INT_4, value);
			break;
		}
		case SDP_INT8:
		{
			jlong value = (jlong)data->val.int8;
			constructorID = env->GetMethodID(dataElementClass,"<init>","(IJ)V");
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_INT_1, value);
			break;
		}
		case SDP_INT16:
		{
			jlong value = (jlong)data->val.int16;
			constructorID = env->GetMethodID(dataElementClass, "<init>", "(IJ)V");
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_U_INT_2, value);
			break;
		}
		case SDP_INT32:
		{
			jlong value = (jlong)data->val.int32;
			constructorID = env->GetMethodID(dataElementClass, "<init>", "(IJ)V");
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_INT_4, value);
			break;
		}
		case SDP_INT64:
		{
			jlong value = (jlong)data->val.int64;
			constructorID = env->GetMethodID(dataElementClass, "<init>", "(IJ)V");
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_INT_8, value);
			break;
		}
		//-----------------------------------------//
		case SDP_UINT64:
		{
			Edebug("SDP_UINT64");
			uint64_t value = data->val.uint64;
			jbyte* bytes = (jbyte*)&value;
			reverseArray(bytes, sizeof(value));
			jbyteArray byteArray = env->NewByteArray(sizeof(value));
			env->SetByteArrayRegion(byteArray, 0, sizeof(value), bytes);
			constructorID = env->GetMethodID(dataElementClass, "<init>", "(ILjava/lang/Object;)V");
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_U_INT_8, byteArray);
			break;
		}
		case SDP_UINT128:
		{
			Edebug("SDP_UINT128");
			uint128_t value=data->val.uint128;
			jbyte* bytes=(jbyte*)&value;
			reverseArray(bytes, sizeof(value));
			jbyteArray byteArray = env->NewByteArray(sizeof(value));
			env->SetByteArrayRegion(byteArray, 0, sizeof(value), bytes);
			constructorID = env->GetMethodID(dataElementClass, "<init>", "(ILjava/lang/Object;)V");
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_U_INT_16, byteArray);
			break;
		}
		case SDP_INT128:
		{
            Edebug("SDP_INT128");
			uint128_t value = data->val.int128;
			jbyte* bytes = (jbyte*)&value;
			reverseArray(bytes, sizeof(value));
			jbyteArray byteArray = env->NewByteArray(sizeof(value));
			env->SetByteArrayRegion(byteArray, 0, sizeof(value), bytes);
			constructorID = env->GetMethodID(dataElementClass, "<init>", "(ILjava/lang/Object;)V");
			dataElement=env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_INT_16, byteArray);
			break;
		}
		//-----------------------------------------//
		case SDP_URL_STR_UNSPEC:
		case SDP_URL_STR8:
		case SDP_URL_STR16:
		case SDP_URL_STR32:
		{
			Edebug("SDP_URL");
			char* str = data->val.str;
			constructorID = env->GetMethodID(dataElementClass, "<init>", "(ILjava/lang/Object;)V");
			jstring string = env->NewStringUTF(str);
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_URL, string);
			break;
		}
		case SDP_TEXT_STR_UNSPEC:
		case SDP_TEXT_STR8:
		case SDP_TEXT_STR16:
		case SDP_TEXT_STR32:
		{
			Edebug("SDP_TEXT");
			char* str = data->val.str;
			constructorID = env->GetMethodID(dataElementClass, "<init>", "(ILjava/lang/Object;)V");
			jstring string = env->NewStringUTF(str);
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_STRING, string);
			break;
		}
		//-----------------------------------------//
		case SDP_UUID_UNSPEC:
		case SDP_UUID16:
		case SDP_UUID32:
		case SDP_UUID128:
		{
		    Edebug("SDP_UUID");
			jobject javaUUID = createJavaUUID(env, data->val.uuid);
			if (javaUUID == NULL) {
			    debug("fail to create UUID");
			    break;
			}
			constructorID = env->GetMethodID(dataElementClass, "<init>", "(ILjava/lang/Object;)V");
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_UUID, javaUUID);
			break;
		}
		//-----------------------------------------//
		case SDP_SEQ_UNSPEC:
		case SDP_SEQ8:
		case SDP_SEQ16:
		case SDP_SEQ32:
		{
			Edebug("SDP_SEQ");
			sdp_data_t *newData = data->val.dataseq;
			constructorID = env->GetMethodID(dataElementClass, "<init>", "(I)V");
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_DATSEQ);
			jmethodID addElementID = env->GetMethodID(dataElementClass, "addElement", "(Ljavax/bluetooth/DataElement;)V");
			for(; newData; newData = newData->next) {
				jobject newDataElement = createDataElement(env, newData);
				if (newDataElement != NULL) {
				    env->CallVoidMethod(dataElement, addElementID, newDataElement);
			    }
			    if (env->ExceptionCheck()) {
		            break;
		        }
			}
			break;
		}
		case SDP_ALT_UNSPEC:
		case SDP_ALT8:
		case SDP_ALT16:
		case SDP_ALT32:
		{
		    Edebug("SDP_ALT");
			sdp_data_t *newData = data->val.dataseq;
			constructorID = env->GetMethodID(dataElementClass, "<init>", "(I)V");
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_DATALT);
			jmethodID addElementID = env->GetMethodID(dataElementClass, "addElement", "(Ljavax/bluetooth/DataElement;)V");
			for(; newData; newData = newData->next) {
				jobject newDataElement = createDataElement(env, newData);
				if (newDataElement != NULL) {
				    env->CallVoidMethod(dataElement, addElementID, newDataElement);
			    }
			    if (env->ExceptionCheck()) {
		            break;
		        }
			}
			break;
		}
		default:
		{
			//cout<<"strange data type "<<(int)data->dtd<<endl;
			constructorID = env->GetMethodID(dataElementClass, "<init>", "(I)V");
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_NULL);
			break;
		}
	}
	if (dataElement != NULL) {
	    Edebug("dataElement created 0x%x", data->dtd);
    }
    if (env->ExceptionCheck()) {
        ndebug("Exception in data element creation 0x%x", data->dtd);
    }
	return dataElement;
}

void populateServiceRecord(JNIEnv *env, jobject serviceRecord, sdp_record_t* sdpRecord, sdp_list_t* attributeList) {
	jclass serviceRecordImplClass = env->GetObjectClass(serviceRecord);
	jmethodID populateAttributeValueID = env->GetMethodID(serviceRecordImplClass, "populateAttributeValue", "(ILjavax/bluetooth/DataElement;)V");
	for(; attributeList; attributeList = attributeList->next) {
		jint attributeID=*(uint16_t*)attributeList->data;
		sdp_data_t *data = sdp_data_get(sdpRecord, (uint16_t)attributeID);
		if (data) {
			jobject dataElement = createDataElement(env, data);
			if (dataElement != NULL) {
			    env->CallVoidMethod(serviceRecord, populateAttributeValueID, attributeID, dataElement);
		    }
		    if (env->ExceptionCheck()) {
		        break;
		    }
		}
	}
}

