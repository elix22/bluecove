/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Eric Wagner
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
 
 /*
  * Simple main function to launch the JVM and start a bluecove test up while source
  * level debugging the native code.
  */
#include "blueCovejnilib.h"
#include <stdio.h>
#include <JavaVM/jni.h>
#include <pthread.h>

int main(int argc, const char * argv[]) {
	JavaVM	*jvm;
	JNIEnv	*env;
	jint	err;
	jclass	cls, stringCls;
	JavaVMInitArgs vm_args={0};
	JavaVMOption	options[3];
	options[0].optionString = "-Djava.compiler=NONE";
	
	/* set this to the path location specific to your OS tha contains the java code
	 * which interfaces with your native library */
	 
	options[1].optionString = "-Djava.class.path=/Users/work/Documents/workspace/BlueCoveOSX/bin";
	options[2].optionString = "-verbose:jni";
	
	err = JNI_GetDefaultJavaVMInitArgs(&vm_args);
		vm_args.version = JNI_VERSION_1_2;
	vm_args.options = options;
	
	/* set this to 3 if you want to see the linker actions logged to the console */
	vm_args.nOptions = 2;
	/* load and initialize the JVM */
	err = JNI_CreateJavaVM(&jvm, (void**) &env, &vm_args);
	/* trick the Native Library load class into thinking its already loaded the library
		If you don't do this it'll try to load a library and it may succeed in loading an
		old version from somewhere. This ensures only the current code is being debugged */
	jclass		nativeLLClass = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/NativeLibLoader"));


	jfieldID	aField = JAVA_ENV_CHECK(GetStaticFieldID(env, nativeLLClass, "triedToLoadAlredy", "Z"));
	JAVA_ENV_CHECK(SetStaticBooleanField(env, nativeLLClass, aField, 1));
	aField = JAVA_ENV_CHECK(GetStaticFieldID(env, nativeLLClass, "libraryAvailable", "Z"));
	JAVA_ENV_CHECK(SetStaticBooleanField(env, nativeLLClass, aField, 1));

/* register the native methods */
	JNINativeMethod			BluetoothInputStream[]={
		{"pipePrime", "(I)V", Java_com_intel_bluetooth_BluetoothInputStream_pipePrime}
		};
	JNINativeMethod			BluetoothL2CAPConnection[]={
		{"socket", "(ZZ)I", Java_com_intel_bluetooth_BluetoothL2CAPConnection_socket},
		{"nativeConnect", "(IJIII)V", Java_com_intel_bluetooth_BluetoothL2CAPConnection_nativeConnect},
		{"send", "([B)V", Java_com_intel_bluetooth_BluetoothL2CAPConnection_send},
		{"closeSocket", "(I)V", Java_com_intel_bluetooth_BluetoothL2CAPConnection_closeSocket}
		};
	JNINativeMethod			BluetoothOBEXConnection[]={
		{"nativeConnect", "(IJIII)V", Java_com_intel_bluetooth_BluetoothOBEXConnection_nativeConnect},
		{"send", "([B)V", Java_com_intel_bluetooth_BluetoothOBEXConnection_send}
		};
	JNINativeMethod			BluetoothRFCOMMConnection[]={
		{"nativeConnect", "(IJIII)V", Java_com_intel_bluetooth_BluetoothRFCOMMConnection_nativeConnect},
		{"send", "([B)V", Java_com_intel_bluetooth_BluetoothRFCOMMConnection_send}
		};
	JNINativeMethod			DiscoveryAgentImpl[]={
		{"retrieveDevices", "(I)[Ljavax/bluetooth/RemoteDevice;", Java_com_intel_bluetooth_DiscoveryAgentImpl_retrieveDevices},
		{"startInquiry", "(ILjavax/bluetooth/DiscoveryListener;)Z", Java_com_intel_bluetooth_DiscoveryAgentImpl_startInquiry},
		{"cancelInquiry", "(Ljavax/bluetooth/DiscoveryListener;)Z", Java_com_intel_bluetooth_DiscoveryAgentImpl_cancelInquiry},
		{"searchServices", "([I[Ljavax/bluetooth/UUID;Ljavax/bluetooth/RemoteDevice;Ljavax/bluetooth/DiscoveryListener;)I", Java_com_intel_bluetooth_DiscoveryAgentImpl_searchServices},
		{"cancelServiceSearch", "(I)Z", Java_com_intel_bluetooth_DiscoveryAgentImpl_cancelServiceSearch},
		{"selectService", "(Ljavax/bluetooth/UUID;IZ)Ljava/lang/String;", Java_com_intel_bluetooth_DiscoveryAgentImpl_selectService}
		};
		
	JNINativeMethod			nativeMethodsLocalDev[] = {
		{"getLocalAddress", "()J", Java_com_intel_bluetooth_LocalDeviceImpl_getLocalAddress},										//1
		{"getFriendlyName", "()Ljava/lang/String;", Java_com_intel_bluetooth_LocalDeviceImpl_getFriendlyName},
		{"getDeviceClass", "()Ljavax/bluetooth/DeviceClass;", Java_com_intel_bluetooth_LocalDeviceImpl_getDeviceClass},
		{"setDiscoverable", "(I)Z", Java_com_intel_bluetooth_LocalDeviceImpl_setDiscoverable},
		{"getAdjustedSystemProperties", "()Ljava/util/Properties;", Java_com_intel_bluetooth_LocalDeviceImpl_getAdjustedSystemProperties},
		{"getDiscoverable", "()I", Java_com_intel_bluetooth_LocalDeviceImpl_getDiscoverable}
		};
	JNINativeMethod			nativeRemoteDeviceImpl[] = {
		{"getFriendlyName", "(Z)Ljava/lang/String;", Java_com_intel_bluetooth_RemoteDeviceImpl_getFriendlyName}
		};
	JNINativeMethod			nativeMethodsServRecImpl[] = {
		{"getServiceAttributes","([IJI)[B", Java_com_intel_bluetooth_ServiceRecordImpl_getServiceAttributes},
		{"native_populateRecord", "([I)Z", Java_com_intel_bluetooth_ServiceRecordImpl_native_1populateRecord}				//1
		};
			
	jclass		interface ;
	
	interface = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/BluetoothInputStream"));
	err = JAVA_ENV_CHECK(RegisterNatives(env, interface, BluetoothInputStream, 1));
	interface = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/BluetoothL2CAPConnection"));
	err = JAVA_ENV_CHECK(RegisterNatives(env, interface, BluetoothL2CAPConnection, 4));
	interface = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/BluetoothOBEXConnection"));
	err = JAVA_ENV_CHECK(RegisterNatives(env, interface, BluetoothOBEXConnection, 2));
	interface = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/BluetoothRFCOMMConnection"));
	err = JAVA_ENV_CHECK(RegisterNatives(env, interface, BluetoothRFCOMMConnection, 2));
	interface = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/DiscoveryAgentImpl"));
	err = JAVA_ENV_CHECK(RegisterNatives(env, interface, DiscoveryAgentImpl, 6));
	interface = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/LocalDeviceImpl"));
	err = JAVA_ENV_CHECK(RegisterNatives(env, interface, nativeMethodsLocalDev, 6));
	interface = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/RemoteDeviceImpl"));
	err = JAVA_ENV_CHECK(RegisterNatives(env, interface, nativeRemoteDeviceImpl, 1));
	interface = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/ServiceRecordImpl"));
	err = JAVA_ENV_CHECK(RegisterNatives(env, interface, nativeMethodsServRecImpl, 2));

	JNI_OnLoad(jvm, NULL);
	/* invoke the main thread */
	 cls = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/test/RFCOMMTest"));
//	 cls = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/test/StandaloneTest"));
//	 cls = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/test/DummyTest"));
	 stringCls = JAVA_ENV_CHECK(FindClass(env, "java/lang/String"));
	jmethodID mid = JAVA_ENV_CHECK(GetStaticMethodID(env, cls, "main", "([Ljava/lang/String;)V"));

jobjectArray params = JAVA_ENV_CHECK(NewObjectArray(env, 0,  stringCls, NULL));
	jsize	aSize = JAVA_ENV_CHECK(GetArrayLength(env, params));
	JAVA_ENV_CHECK(CallStaticVoidMethod(env, cls, mid, params));

	/* just in case main returns right away sleep the thread longer than the debug session */
	sleep(10000000);
	(*jvm)->DestroyJavaVM(jvm);
	
	return 0;
	}