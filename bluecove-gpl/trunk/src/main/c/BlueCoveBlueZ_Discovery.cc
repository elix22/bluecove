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

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeRunDeviceInquiry(JNIEnv *env, jobject thisObject, jint deviceID, jint dd, jint len, jint accessCode, jobject devicesVector)
{
	int max_rsp=255;
	inquiry_info *ii=new inquiry_info[max_rsp];
	int num_rsp=hci_inquiry(deviceID,len,max_rsp,NULL,&ii,accessCode);
	if(num_rsp<0)
		return INQUIRY_ERROR;
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
