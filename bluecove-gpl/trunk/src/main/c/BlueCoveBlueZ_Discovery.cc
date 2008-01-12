#include "BluetoothStackBlueZ.h"
#include "DiscoveryListener.h"
#include "BlueCoveBlueZ.h"
#include <bluetooth/bluetooth.h>
#include <bluetooth/hci.h>
#include <bluetooth/hci_lib.h>

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeRunDeviceInquiry(JNIEnv *env, jobject thisObject, jint deviceID, jint dd, jint len, jint accessCode, jobject devicesVector)
{
	int max_rsp=255;
	inquiry_info *ii=new inquiry_info[max_rsp];
	int num_rsp=hci_inquiry(deviceID,len,max_rsp,NULL,&ii,accessCode);
	if(num_rsp<0)
		return javax_bluetooth_DiscoveryListener_INQUIRY_ERROR;
	for(int i=0;i<num_rsp;i++)
	{
		bdaddr_t* address=&(ii+i)->bdaddr;
		jlong addressLong=0;
		for(int j=0;j<sizeof(address->b);j++)
			addressLong=(addressLong<<8)|address->b[sizeof(address->b)-j-1];
		char name[DEVICE_NAME_MAX_SIZE];
		hci_read_remote_name(dd,address,sizeof(name),name,TIMEOUT);
		uint8_t *dev_class=(ii+i)->dev_class;
		int record=deviceClassBytesToInt(dev_class);
				
		jclass bluetoothStackBlueZClass=env->GetObjectClass(thisObject);
		jmethodID createReportedDeviceID=env->GetMethodID(bluetoothStackBlueZClass,"createReportedDevice","(Ljava/lang/String;JI)Lcom/intel/bluetooth/BluetoothStackBlueZ$ReportedDevice;");
		jobject reportedDeviceObject=env->CallObjectMethod(thisObject,createReportedDeviceID,env->NewStringUTF(name),addressLong,(jint)record);
		
		jclass vectorClass=env->GetObjectClass(devicesVector);
		jmethodID addElementID=env->GetMethodID(vectorClass,"addElement","(Ljava/lang/Object;)V");
		env->CallVoidMethod(devicesVector,addElementID,reportedDeviceObject);
	}
}
