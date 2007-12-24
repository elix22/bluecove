#include "BluetoothStackBlueZ.h"
#include "DiscoveryListener.h"
#include "BlueCoveBlueZ.h"
#include <bluetooth/bluetooth.h>
#include <bluetooth/hci.h>
#include <bluetooth/hci_lib.h>

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeGetDeviceID(JNIEnv *env, jobject thisObject)
{
	return hci_get_route(NULL);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeOpenDevice(JNIEnv *env, jobject thisObject, jint deviceID)
{
	return hci_open_dev(deviceID);
	printf("device openned\n");
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeCloseDevice(JNIEnv *env, jobject thisObject, jint dd)
{
	hci_close_dev(dd);
	printf("device closed\n");
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeGetDeviceBluetoothAddress(JNIEnv *env, jobject thisObject, jint dd)
{
	char* addressChars=new char[17];
	bdaddr_t address;
	int error=hci_read_bd_addr(dd,&address,TIMEOUT);
	ba2str(&address,addressChars);
	jstring addressString=env->NewStringUTF(addressChars);
	delete[] addressChars;
	return addressString;
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeGetDeviceName(JNIEnv *env, jobject thisObject, jint dd)
{
	char* name=new char[DEVICE_NAME_MAX_SIZE];
	jstring nameString=NULL;
	if(!hci_local_name(dd,100,name,TIMEOUT))
		nameString=env->NewStringUTF(name);
	delete[] name;
	return nameString;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeGetDeviceClass(JNIEnv *env, jobject thisObject, jint dd)
{
//	I am not 100% sure this implementation is correct but I believe it is.
//	it works with me.
//	the used test is to retrieve the local device class and then discover my device from another device
//	and compare the device class got from both.
	uint8_t deviceClass[3];
	int record=0;
	if(!hci_read_class_of_dev(dd,deviceClass,TIMEOUT))
		record=deviceClassBytesToInt(deviceClass);
	return record;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeSetLocalDeviceDiscoverable(JNIEnv *env, jobject thisObject, jint dd, jint mode)
{
	uint8_t lap[3];
	lap[0]=mode & 0xff;
	lap[1]=(mode & 0xff00)>>8;
	lap[2]=(mode & 0xff0000)>>16;
	return hci_write_current_iac_lap(dd,1,lap,TIMEOUT);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeGetLocalDeviceDiscoverable(JNIEnv *env, jobject thisObject, jint dd)
{
	uint8_t lap[3];
	uint8_t num_iac;
	int error=hci_read_current_iac_lap(dd,&num_iac,lap,TIMEOUT);
	if(error<0 || num_iac>1)
//	I don't know why to check for num_iac to be less than or equal to one but avetana to this.
	{
		env->ThrowNew(env->FindClass("java/lang/RuntimeException"),"Unable to retrieve the local discovery mode. It may be because you are not root");
		return 0;
	}
	return (lap[0] & 0xff) | ((lap[1] & 0xff) << 8) | ((lap[2] & 0xff) << 16);
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeGetRemoteDeviceName(JNIEnv *env, jobject thisObject, jint dd, jstring addressString)
{
	jboolean copy=JNI_FALSE;
	const char* addressChars=env->GetStringUTFChars(addressString,&copy);
	bdaddr_t address;
	str2ba(addressChars,&address);
	char name[DEVICE_NAME_MAX_SIZE];
	int error=hci_read_remote_name(dd,&address,sizeof(name),name,TIMEOUT);
	if(error<0)
	{
		env->ThrowNew(env->FindClass("java/io/IOException"),"Can not get remote device name");
		return NULL;
	}
	return env->NewStringUTF(name);
}
