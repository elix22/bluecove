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
#include <jni.h>

#include <iostream>
using namespace std;

int deviceClassBytesToInt(uint8_t* deviceClass)
{
	return ((deviceClass[2] & 0xff)<<16)|((deviceClass[1] & 0xff)<<8)|(deviceClass[0] & 0xff);
}

void populateServiceRecord(JNIEnv *env,jobject serviceRecord,sdp_record_t* sdpRecord,sdp_list_t* attributeList)
{
	jclass serviceRecordImplClass=env->GetObjectClass(serviceRecord);
	jmethodID populateAttributeValueID=env->GetMethodID(serviceRecordImplClass,"populateAttributeValue","(ILjavax/bluetooth/DataElement;)V");

	for(;attributeList;attributeList=attributeList->next)
	{
		jint attributeID=*(uint16_t*)attributeList->data;
		sdp_data_t *data=sdp_data_get(sdpRecord,(uint16_t)attributeID);
		if(data)
		{
			//cout<<"data : "<<(int)data->attrId<<"\t"<<(int)data->dtd<<endl;
			//cout<<"data "<<(int)data->attrId<<endl;
			jobject dataElement=createDataElement(env,data);
			//cout<<"attribute id: "<<attributeID<<endl;
			env->CallVoidMethod(serviceRecord,populateAttributeValueID,attributeID,dataElement);
		}
	}
}

//int n=0;

jobject createDataElement(JNIEnv *env,sdp_data_t *data)
{
	//++n;
	//for(int i=0;i<n;i++)
	//	cout<<"\t";
	//cout<<"create data element "<<(int)data->dtd<<endl;
	jclass dataElementClass=env->FindClass("javax/bluetooth/DataElement");
	jmethodID constructorID;
	jobject dataElement=NULL;
	switch(data->dtd)
	{
		case SDP_DATA_NIL:
		{
			constructorID=env->GetMethodID(dataElementClass,"<init>","(I)V");
			dataElement=env->NewObject(dataElementClass,constructorID,DATA_ELEMENT_TYPE_NULL);
			break;
		}
		//-----------------------------------------//
		case SDP_BOOL:
		{
			jboolean boolean=data->val.uint8;
			constructorID=env->GetMethodID(dataElementClass,"<init>","(Z)V");
			dataElement=env->NewObject(dataElementClass,constructorID,boolean);
			break;
		}
		//-----------------------------------------//
		case SDP_UINT8:
		{
			jlong value=(jlong)data->val.uint8;
			constructorID=env->GetMethodID(dataElementClass,"<init>","(IJ)V");
			dataElement=env->NewObject(dataElementClass,constructorID,DATA_ELEMENT_TYPE_U_INT_1,value);
			break;
		}
		case SDP_UINT16:
		{
			jlong value=(jlong)data->val.uint16;
			constructorID=env->GetMethodID(dataElementClass,"<init>","(IJ)V");
			dataElement=env->NewObject(dataElementClass,constructorID,DATA_ELEMENT_TYPE_U_INT_2,value);
			break;
		}
		case SDP_UINT32:
		{
			jlong value=(jlong)data->val.uint32;
			constructorID=env->GetMethodID(dataElementClass,"<init>","(IJ)V");
			dataElement=env->NewObject(dataElementClass,constructorID,DATA_ELEMENT_TYPE_U_INT_4,value);
			break;
		}
		case SDP_INT8:
		{
			jlong value=(jlong)data->val.int8;
			constructorID=env->GetMethodID(dataElementClass,"<init>","(IJ)V");
			dataElement=env->NewObject(dataElementClass,constructorID,DATA_ELEMENT_TYPE_INT_1,value);
			break;
		}
		case SDP_INT16:
		{
			jlong value=(jlong)data->val.int16;
			constructorID=env->GetMethodID(dataElementClass,"<init>","(IJ)V");
			dataElement=env->NewObject(dataElementClass,constructorID,DATA_ELEMENT_TYPE_U_INT_2,value);
			break;
		}
		case SDP_INT32:
		{
			jlong value=(jlong)data->val.int32;
			constructorID=env->GetMethodID(dataElementClass,"<init>","(IJ)V");
			dataElement=env->NewObject(dataElementClass,constructorID,DATA_ELEMENT_TYPE_INT_4,value);
			break;
		}
		case SDP_INT64:
		{
			jlong value=(jlong)data->val.int64;
			constructorID=env->GetMethodID(dataElementClass,"<init>","(IJ)V");
			dataElement=env->NewObject(dataElementClass,constructorID,DATA_ELEMENT_TYPE_INT_8,value);
			break;
		}
		//-----------------------------------------//
		case SDP_UINT64:
		{
			uint64_t value=data->val.uint64;
			jbyte* bytes=(jbyte*)&value;
			reverseArray(bytes,sizeof(value));
			jbyteArray byteArray=env->NewByteArray(sizeof(value));
			env->SetByteArrayRegion(byteArray,0,sizeof(value),bytes);
			constructorID=env->GetMethodID(dataElementClass,"<init>","(ILjava/lang/Object;)V");
			dataElement=env->NewObject(dataElementClass,constructorID,DATA_ELEMENT_TYPE_U_INT_8,byteArray);
			break;
		}
		case SDP_UINT128:
		{
			uint128_t value=data->val.uint128;
			jbyte* bytes=(jbyte*)&value;
			reverseArray(bytes,sizeof(value));
			jbyteArray byteArray=env->NewByteArray(sizeof(value));
			env->SetByteArrayRegion(byteArray,0,sizeof(value),bytes);
			constructorID=env->GetMethodID(dataElementClass,"<init>","(ILjava/lang/Object;)V");
			dataElement=env->NewObject(dataElementClass,constructorID,DATA_ELEMENT_TYPE_U_INT_16,byteArray);
			break;
		}
		case SDP_INT128:
		{
			uint128_t value=data->val.int128;
			jbyte* bytes=(jbyte*)&value;
			reverseArray(bytes,sizeof(value));
			jbyteArray byteArray=env->NewByteArray(sizeof(value));
			env->SetByteArrayRegion(byteArray,0,sizeof(value),bytes);
			constructorID=env->GetMethodID(dataElementClass,"<init>","(ILjava/lang/Object;)V");
			dataElement=env->NewObject(dataElementClass,constructorID,DATA_ELEMENT_TYPE_INT_16,byteArray);
			break;
		}
		//-----------------------------------------//
		case SDP_URL_STR_UNSPEC:
		case SDP_URL_STR8:
		case SDP_URL_STR16:
		case SDP_URL_STR32:
		case SDP_TEXT_STR_UNSPEC:
		case SDP_TEXT_STR8:
		case SDP_TEXT_STR16:
		case SDP_TEXT_STR32:
		{
			char* str=data->val.str;
			constructorID=env->GetMethodID(dataElementClass,"<init>","(ILjava/lang/Object;)V");
			jstring string=env->NewStringUTF(str);
			dataElement=env->NewObject(dataElementClass,constructorID,data->dtd,string);
			break;
		}
		//-----------------------------------------//
		case SDP_UUID_UNSPEC:
		case SDP_UUID16:
		case SDP_UUID32:
		case SDP_UUID128:
		{
			jobject javaUUID=createJavaUUID(env,data->val.uuid);
			constructorID=env->GetMethodID(dataElementClass,"<init>","(ILjava/lang/Object;)V");
			dataElement=env->NewObject(dataElementClass,constructorID,DATA_ELEMENT_TYPE_UUID,javaUUID);
			break;
		}
		//-----------------------------------------//
		case SDP_SEQ_UNSPEC:
		case SDP_SEQ8:
		case SDP_SEQ16:
		case SDP_SEQ32:
		{
			sdp_data_t *newData=data->val.dataseq;
			constructorID=env->GetMethodID(dataElementClass,"<init>","(I)V");
			dataElement=env->NewObject(dataElementClass,constructorID,DATA_ELEMENT_TYPE_DATSEQ);
			jmethodID addElementID=env->GetMethodID(dataElementClass,"addElement","(Ljavax/bluetooth/DataElement;)V");
			for(;newData;newData=newData->next)
			{
				jobject newDataElement=createDataElement(env,newData);
				env->CallVoidMethod(dataElement,addElementID,newDataElement);
			}
			break;
		}
		case SDP_ALT_UNSPEC:
		case SDP_ALT8:
		case SDP_ALT16:
		case SDP_ALT32:
		{
			sdp_data_t *newData=data->val.dataseq;
			constructorID=env->GetMethodID(dataElementClass,"<init>","(I)V");
			dataElement=env->NewObject(dataElementClass,constructorID,DATA_ELEMENT_TYPE_DATALT);
			jmethodID addElementID=env->GetMethodID(dataElementClass,"addElement","(Ljavax/bluetooth/DataElement;)V");
			for(;newData;newData=newData->next)
			{
				jobject newDataElement=createDataElement(env,newData);
				env->CallVoidMethod(dataElement,addElementID,newDataElement);
			}
			break;
		}
		default:
		{
			cout<<"strange data type "<<(int)data->dtd<<endl;
			constructorID=env->GetMethodID(dataElementClass,"<init>","(I)V");
			dataElement=env->NewObject(dataElementClass,constructorID,DATA_ELEMENT_TYPE_NULL);
			break;
		}
	}
	//--n;
	return dataElement;
}

void reverseArray(jbyte* array,int length)
{
	for(int i=0;i<length/2;i++)
	{
		jbyte temp=array[i];
		array[i]=array[length-1-i];
		array[length-1-i]=temp;
	}
}

uuid_t getUuidFromJavaUUID(JNIEnv *env,jobject javaUUID)
{
	jboolean isCopy=JNI_FALSE;
	jbyteArray uuidValue=(jbyteArray)env->GetObjectField(javaUUID,env->GetFieldID(env->FindClass("javax/bluetooth/UUID"),"uuidValue","[B"));
	jbyte* bytes=env->GetByteArrayElements(uuidValue,&isCopy);
	uuid_t uuid;
	uuid.type=SDP_UUID128;
	memcpy(&uuid.value,bytes,128/8);
	return uuid;
}

jobject createJavaUUID(JNIEnv *env,uuid_t uuid)
{
	jboolean longUUID=true;//(uuid.type==SDP_UUID128);
	char uuidChars[32];
	sdp_uuid2strn(&uuid,uuidChars,sizeof(uuidChars));
	jstring uuidString=env->NewStringUTF(uuidChars);
	jclass uuidClass=env->FindClass("javax/bluetooth/UUID");
	jmethodID constructorID=env->GetMethodID(uuidClass,"<init>","(Ljava/lang/String;Z)V");
	return env->NewObject(uuidClass,constructorID,uuidString,longUUID);
}
