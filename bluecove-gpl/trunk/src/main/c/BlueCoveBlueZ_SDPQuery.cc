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

	jmethodID serviceDiscoveredCallback = getGetMethodID(env, peerClass, "serviceDiscoveredCallback", "(Lcom/intel/bluetooth/SearchServicesThread;JJ)Z");
	if (serviceDiscoveredCallback == NULL) {
		return SERVICE_SEARCH_ERROR;
	}

    SDPQueryData data;

	// convert uuid set from java array to bluez sdp_list_t
	jsize uuidSetSize = env->GetArrayLength(uuidValues);
	for(jsize i = 0; i < uuidSetSize; i++) {
		jbyteArray byteArray = (jbyteArray)env->GetObjectArrayElement(uuidValues, i);
		uuid_t* uuid =  (uuid_t*)malloc(sizeof(uuid_t));
		convertUUIDByteArrayToUUID(env, byteArray, uuid);
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
	jlong sdpSession = (jlong)data.session;
    Edebug("runSearchServicesImpl session %p %li", data.session, sdpSession);

	// Notify java about found services
	int serviceCount = 0;
	sdp_list_t* handle = data.rsp_list;
	for(; handle; handle = handle->next) {
		uint32_t record = *(uint32_t*)handle->data;
		jlong recordHandle = record;
		Edebug("runSearchServicesImpl serviceRecordHandle %li", recordHandle);
		jboolean isTerminated = env->CallBooleanMethod(peer, serviceDiscoveredCallback, searchServicesThread, sdpSession, recordHandle);
        if (env->ExceptionCheck()) {
            return SERVICE_SEARCH_ERROR;
        } else if (isTerminated) {
            return SERVICE_SEARCH_TERMINATED;
        }
        serviceCount ++;
	}
	debug("runSearchServicesImpl found %i", serviceCount);
	return SERVICE_SEARCH_COMPLETED;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_populateServiceRecordAttributeValuesImpl
  (JNIEnv *env, jobject peer, jlong remoteDeviceAddressLong, jlong sdpSession, jlong handle, jintArray attrIDs, jobject serviceRecord) {
	SDPQueryData data;
	sdp_session_t* session = (sdp_session_t*)sdpSession;
	if (session != NULL) {
	    debug("populateServiceRecordAttributeValuesImpl connected %p, recordHandle %li", session, handle);
	} else {
	    debug("populateServiceRecordAttributeValuesImpl connects, recordHandle %li", handle);
	    bdaddr_t remoteAddress;
	    longToDeviceAddr(remoteDeviceAddressLong, &remoteAddress);
	    session = sdp_connect(BDADDR_ANY, &remoteAddress, SDP_RETRY_IF_BUSY);
	    if (session == NULL) {
	        debug("populateServiceRecordAttributeValuesImpl can't connect");
	        return JNI_FALSE;
	    }
	    // Close session on exit
	    data.session = session;
    }

	sdp_list_t *attr_list = NULL;
	jboolean isCopy = JNI_FALSE;
	jint* ids = env->GetIntArrayElements(attrIDs, &isCopy);
	for(int i = 0; i < env->GetArrayLength(attrIDs); i++) {
		uint16_t* id = (uint16_t*)malloc(sizeof(uint16_t));
		*id=(uint16_t)ids[i];
//		cout<<"id:"<<ids[i]<<"\t"<<*id<<endl;
		attr_list = sdp_list_append(attr_list,id);
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

	sdp_record_t *sdpRecord = sdp_service_attr_req(session, (uint32_t)handle, SDP_ATTR_REQ_INDIVIDUAL, attr_list);
	if (!sdpRecord) {
        debug("sdp_service_attr_req return error");
		sdp_list_free(attr_list, free);
		return JNI_FALSE;
	}
	populateServiceRecord(env, serviceRecord, sdpRecord, attr_list);
	sdp_record_free(sdpRecord);
	sdp_list_free(attr_list, free);
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
	jmethodID constructorID = getGetMethodID(env, uuidClass, "<init>", "(Ljava/lang/String;Z)V");
	if (constructorID == NULL) {
	    return NULL;
	}
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
			constructorID = getGetMethodID(env, dataElementClass, "<init>", "(I)V");
			if (constructorID == NULL) {
			    break;
			}
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_NULL);
			break;
		}
		case SDP_BOOL:
		{
			jboolean boolean = data->val.uint8;
			constructorID = getGetMethodID(env, dataElementClass, "<init>", "(Z)V");
			if (constructorID == NULL) {
			    break;
			}
			dataElement = env->NewObject(dataElementClass, constructorID, boolean);
			break;
		}
		case SDP_UINT8:
		{
			jlong value = (jlong)data->val.uint8;
			constructorID = getGetMethodID(env, dataElementClass, "<init>", "(IJ)V");
			if (constructorID == NULL) {
			    break;
			}
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_U_INT_1, value);
			break;
		}
		case SDP_UINT16:
		{
			jlong value = (jlong)data->val.uint16;
			constructorID = getGetMethodID(env, dataElementClass, "<init>", "(IJ)V");
			if (constructorID == NULL) {
			    break;
			}
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_U_INT_2, value);
			break;
		}
		case SDP_UINT32:
		{
			jlong value = (jlong)data->val.uint32;
			constructorID = getGetMethodID(env, dataElementClass, "<init>", "(IJ)V");
			if (constructorID == NULL) {
			    break;
			}
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_U_INT_4, value);
			break;
		}
		case SDP_INT8:
		{
			jlong value = (jlong)data->val.int8;
			constructorID = getGetMethodID(env, dataElementClass,"<init>","(IJ)V");
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_INT_1, value);
			break;
		}
		case SDP_INT16:
		{
			jlong value = (jlong)data->val.int16;
			constructorID = getGetMethodID(env, dataElementClass, "<init>", "(IJ)V");
			if (constructorID == NULL) {
			    break;
			}
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_INT_2, value);
			break;
		}
		case SDP_INT32:
		{
			jlong value = (jlong)data->val.int32;
			constructorID = getGetMethodID(env, dataElementClass, "<init>", "(IJ)V");
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_INT_4, value);
			break;
		}
		case SDP_INT64:
		{
			jlong value = (jlong)data->val.int64;
			constructorID = getGetMethodID(env, dataElementClass, "<init>", "(IJ)V");
			if (constructorID == NULL) {
			    break;
			}
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_INT_8, value);
			break;
		}
		case SDP_UINT64:
		{
			Edebug("SDP_UINT64");
			uint64_t value = data->val.uint64;
			jbyte* bytes = (jbyte*)&value;
			reverseArray(bytes, sizeof(value));
			jbyteArray byteArray = env->NewByteArray(sizeof(value));
			env->SetByteArrayRegion(byteArray, 0, sizeof(value), bytes);
			constructorID = getGetMethodID(env, dataElementClass, "<init>", "(ILjava/lang/Object;)V");
			if (constructorID == NULL) {
			    break;
			}
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_U_INT_8, byteArray);
			break;
		}
		case SDP_UINT128:
		{
			Edebug("SDP_UINT128");
			uint128_t value = data->val.uint128;
			jbyte* bytes = (jbyte*)&value;
			reverseArray(bytes, sizeof(value));
			jbyteArray byteArray = env->NewByteArray(sizeof(value));
			env->SetByteArrayRegion(byteArray, 0, sizeof(value), bytes);
			constructorID = getGetMethodID(env, dataElementClass, "<init>", "(ILjava/lang/Object;)V");
			if (constructorID == NULL) {
			    break;
			}
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
			constructorID = getGetMethodID(env, dataElementClass, "<init>", "(ILjava/lang/Object;)V");
			if (constructorID == NULL) {
			    break;
			}
			dataElement=env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_INT_16, byteArray);
			break;
		}
		case SDP_URL_STR_UNSPEC:
		case SDP_URL_STR8:
		case SDP_URL_STR16:
		case SDP_URL_STR32:
		{
			Edebug("SDP_URL");
			char* str = data->val.str;
			constructorID = getGetMethodID(env, dataElementClass, "<init>", "(ILjava/lang/Object;)V");
			if (constructorID == NULL) {
			    break;
			}
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
			constructorID = getGetMethodID(env, dataElementClass, "<init>", "(ILjava/lang/Object;)V");
			if (constructorID == NULL) {
			    break;
			}
			jstring string = env->NewStringUTF(str);
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_STRING, string);
			break;
		}
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
			constructorID = getGetMethodID(env, dataElementClass, "<init>", "(ILjava/lang/Object;)V");
			if (constructorID == NULL) {
			    break;
			}
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_UUID, javaUUID);
			break;
		}
		case SDP_SEQ_UNSPEC:
		case SDP_SEQ8:
		case SDP_SEQ16:
		case SDP_SEQ32:
		{
			Edebug("SDP_SEQ");
			sdp_data_t *newData = data->val.dataseq;
			constructorID = getGetMethodID(env, dataElementClass, "<init>", "(I)V");
			if (constructorID == NULL) {
			    break;
			}
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_DATSEQ);
			jmethodID addElementID = getGetMethodID(env, dataElementClass, "addElement", "(Ljavax/bluetooth/DataElement;)V");
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
			constructorID = getGetMethodID(env, dataElementClass, "<init>", "(I)V");
			if (constructorID == NULL) {
			    break;
			}
			dataElement = env->NewObject(dataElementClass, constructorID, DATA_ELEMENT_TYPE_DATALT);
			jmethodID addElementID = getGetMethodID(env, dataElementClass, "addElement", "(Ljavax/bluetooth/DataElement;)V");
			for(; newData; newData = newData->next) {
				jobject newDataElement = createDataElement(env, newData);
				if (newDataElement == NULL) {
				    break;
				}
				env->CallVoidMethod(dataElement, addElementID, newDataElement);
			    if (env->ExceptionCheck()) {
		            break;
		        }
			}
			break;
		}
		default:
		{
		    debug("strange data type 0x%x", data->dtd);
			constructorID = getGetMethodID(env, dataElementClass, "<init>", "(I)V");
			if (constructorID == NULL) {
			    break;
			}
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
	debug("populateServiceRecord");
	jmethodID populateAttributeValueID = getGetMethodID(env, serviceRecordImplClass, "populateAttributeValue", "(ILjavax/bluetooth/DataElement;)V");
	if (populateAttributeValueID == NULL) {
	    return;
	}
	int attrCount = 0;
	for(; attributeList; attributeList = attributeList->next) {
		jint attributeID=*(uint16_t*)attributeList->data;
		sdp_data_t *data = sdp_data_get(sdpRecord, (uint16_t)attributeID);
		if (data) {
			jobject dataElement = createDataElement(env, data);
			if (env->ExceptionCheck()) {
		        break;
		    }
			if (dataElement == NULL) {
			    break;
			}
			env->CallVoidMethod(serviceRecord, populateAttributeValueID, attributeID, dataElement);
			if (env->ExceptionCheck()) {
		         break;
		    }
		    attrCount ++;
		}
	}
	Edebug("attrCount %i", attrCount);
}

void debugDataElement(JNIEnv *env, sdp_data_t *data, int ident);

void debugDataElementSequence(JNIEnv *env, sdp_data_t *seqData, int ident) {
    for(; seqData; seqData = seqData->next) {
	    debugDataElement(env, seqData, ident + 1);
    }
}

void debugDataElement(JNIEnv *env, sdp_data_t *data, int ident) {
    char t[40];
    memset(t, ' ', sizeof(t));
    t[ident] = '\0';

    switch (data->dtd) {
		case SDP_DATA_NIL:
		    debug("%sSDP_DATA_NIL %i", t, data->dtd);
			break;
		case SDP_BOOL:
		    debug("%sSDP_BOOL %i", t, data->val.uint8);
			break;
		case SDP_UINT8:
		    debug("%sSDP_UINT8 %i", t, data->val.uint8);
			break;
		case SDP_UINT16:
		    debug("%sSDP_UINT16 %i", t, data->val.uint16);
			break;
		case SDP_UINT32:
		    debug("%sSDP_UINT32 %i", t, data->val.uint32);
			break;
		case SDP_INT8:
		    debug("%sSDP_INT8 %i", t, data->val.int8);
			break;
		case SDP_INT16:
		    debug("%sSDP_INT16 %i", t, data->val.int16);
			break;
		case SDP_INT32:
		    debug("%sSDP_INT32 %i", t, data->val.int32);
			break;
		case SDP_INT64:
		    debug("%sSDP_INT64 %i", t, data->val.int64);
			break;
		case SDP_UINT64: {
		    uint64_t value = data->val.uint64;
		    debug("%sSDP_UINT64 ...", t);
		    break;
		}
		case SDP_UINT128: {
		    uint128_t value = data->val.uint128;
		    debug("%sSDP_UINT128 ...", t);
		    break;
		}
		case SDP_INT128: {
			uint128_t value = data->val.int128;
			debug("%sSDP_INT128 ...", t);
			break;
		}
		case SDP_URL_STR_UNSPEC:
		    debug("%sSDP_URL_STR_UNSPEC %s", t, data->val.str);
		    break;
		case SDP_URL_STR8:
		    debug("%sSDP_URL_STR8 %s", t, data->val.str);
		    break;
		case SDP_URL_STR16:
		    debug("%sSDP_URL_STR16 %s", t, data->val.str);
		    break;
		case SDP_URL_STR32:
		    debug("%sSDP_URL_STR32 %s", t, data->val.str);
		    break;
		case SDP_TEXT_STR_UNSPEC:
		    debug("%sSDP_TEXT_STR_UNSPEC %s", t, data->val.str);
		    break;
		case SDP_TEXT_STR8:
		    debug("%sSDP_TEXT_STR8 %s", t, data->val.str);
		    break;
		case SDP_TEXT_STR16:
		    debug("%sSDP_TEXT_STR16 %s", t, data->val.str);
		    break;
		case SDP_TEXT_STR32:
		    debug("%sSDP_TEXT_STR32 %s", t, data->val.str);
		    break;
		case SDP_UUID_UNSPEC:
		    debug("%sSDP_UUID_UNSPEC ...", t);
		    break;
		case SDP_UUID16:
		    debug("%sSDP_UUID16 %.4x", t, data->val.uuid.value.uuid16);
		    break;
		case SDP_UUID32:
		    debug("%sSDP_UUID32 %.8x", t, data->val.uuid.value.uuid32);
		    break;
		case SDP_UUID128:
		    debug("%sSDP_UUID128 ...", t);
		    break;
		case SDP_SEQ_UNSPEC:
		    debug("%sSDP_SEQ_UNSPEC", t);
			debugDataElementSequence(env, data->val.dataseq, ident);
			break;
		case SDP_SEQ8:
		    debug("%sSDP_SEQ8", t);
			debugDataElementSequence(env, data->val.dataseq, ident);
			break;
		case SDP_SEQ16:
		    debug("%sSDP_SEQ16", t);
			debugDataElementSequence(env, data->val.dataseq, ident);
			break;
		case SDP_SEQ32:
			debug("%sSDP_SEQ32", t);
			debugDataElementSequence(env, data->val.dataseq, ident);
			break;
		case SDP_ALT_UNSPEC:
		case SDP_ALT8:
		case SDP_ALT16:
		case SDP_ALT32: {
		    debug("%sSDP_ALT", t);
			sdp_data_t *seqData = data->val.dataseq;
			for(; seqData; seqData = seqData->next) {
				debugDataElement(env, seqData, ident + 1);
			}
			break;
		}
		default: {
		    debug("%sstrange data type 0x%x", t, data->dtd);
			break;
		}
	}
}

void debugServiceRecord(JNIEnv *env, sdp_record_t* sdpRecord) {
    if (sdpRecord == NULL) {
        debug("sdpRecord is NULL");
        return;
    }
    debug("sdpRecord.handle", sdpRecord->handle);

    if (sdpRecord->attrlist == NULL) {
        debug("sdpRecord.attrlist is NULL");
        return;
    }
    sdp_list_t *list = sdpRecord->attrlist;
    for (; list; list = list->next) {
        sdp_data_t * sdpdata = (sdp_data_t *)list->data;
		debug("AttrID: 0x%x", sdpdata->attrId);
		debugDataElement(env, sdpdata, 1);
    }
}
