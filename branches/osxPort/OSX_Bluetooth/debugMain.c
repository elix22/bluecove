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
	 
	options[1].optionString = "-Djava.class.path=/Users/work/Documents/workspace/BlueCoveOSX/resources/javaOnlyBluecove.jar";
	options[2].optionString = "-verbose:jni";
	
	err = JNI_GetDefaultJavaVMInitArgs(&vm_args);
		vm_args.version = JNI_VERSION_1_2;
	vm_args.options = options;
	
	/* set this to 3 if you want to see the linker actions logged to the console */
	vm_args.nOptions = 2;
	/* load and initialize the JVM */
	err = JNI_CreateJavaVM(&jvm, (void**) &env, &vm_args);

/* register the native methods */
	JNINativeMethod			nativeMethodsBTPeer[]={
		{"doInquiry", "(ILjavax/bluetooth/DiscoveryListener;)I", Java_com_intel_bluetooth_BluetoothPeer_doInquiry},			//1
		{"cancelInquiry", "(Ljavax/bluetooth/DiscoveryListener;)Z",Java_com_intel_bluetooth_BluetoothPeer_cancelInquiry },	//2
		{"getServiceHandles", "([Ljavax/bluetooth/UUID;J)[I", Java_com_intel_bluetooth_BluetoothPeer_getServiceHandles},	//3
		{"getServiceAttributes", "([IJI)[B", Java_com_intel_bluetooth_BluetoothPeer_getServiceAttributes},					//4
		{"registerService", "([B)I",Java_com_intel_bluetooth_BluetoothPeer_registerService},								//5
		{"unregisterService", "(I)V", Java_com_intel_bluetooth_BluetoothPeer_unregisterService},							//6
		{"socket", "(ZZ)I", Java_com_intel_bluetooth_BluetoothPeer_socket},													//7
		{"getsockaddress", "(I)J", Java_com_intel_bluetooth_BluetoothPeer_getsockaddress},									//8
		{"getsockchannel", "(I)I", Java_com_intel_bluetooth_BluetoothPeer_getsockchannel},									//9
		{"connect", "(IJI)V",Java_com_intel_bluetooth_BluetoothPeer_connect },												//10
		{"listen", "(I)V", Java_com_intel_bluetooth_BluetoothPeer_listen},													//11
		{"accept", "(I)I", Java_com_intel_bluetooth_BluetoothPeer_accept},													//12
		{"recv", "(I)I", Java_com_intel_bluetooth_BluetoothPeer_recv__I},													//13
		{"recv", "(I[BII)I", Java_com_intel_bluetooth_BluetoothPeer_recv__I_3BII},											//14
		{"send", "(II)V", Java_com_intel_bluetooth_BluetoothPeer_send__II},													//15
		{"send", "(I[BII)V", Java_com_intel_bluetooth_BluetoothPeer_send__I_3BII},											//16
		{"close", "(I)V", Java_com_intel_bluetooth_BluetoothPeer_close},													//17
		{"getpeername", "(J)Ljava/lang/String;", Java_com_intel_bluetooth_BluetoothPeer_getpeername},						//18
		{"getpeeraddress", "(I)J", Java_com_intel_bluetooth_BluetoothPeer_getpeeraddress},									//19
		{"getradioname", "(J)Ljava/lang/String;", Java_com_intel_bluetooth_BluetoothPeer_getradioname},						//20
		{"asyncStopSearchServices", "(I)Z", Java_com_intel_bluetooth_BluetoothPeer_asyncStopSearchServices},				//21
		{"getServiceHandles", "([Ljavax/bluetooth/UUID;J)[I", Java_com_intel_bluetooth_BluetoothPeer_getServiceHandles}		//22
		};
	JNINativeMethod			nativeMethodsServRecImpl[] = {
		{"native_populateRecord", "([I)Z", Java_com_intel_bluetooth_ServiceRecordImpl_native_1populateRecord}				//1
		};
		
		
	jclass		interface = (*env)->FindClass(env, "com/intel/bluetooth/BluetoothPeer");
	err = (*env)->RegisterNatives(env, interface, nativeMethodsBTPeer, 22);
	interface = (*env)->FindClass(env, "com/intel/bluetooth/ServiceRecordImpl");
	err = (*env)->RegisterNatives(env, interface, nativeMethodsServRecImpl, 1);

	/* trick the Native Library load class into thinking its already loaded the library
		If you don't do this it'll try to load a library and it may succeed in loading an
		old version from somewhere. This ensures only the current code is being debugged */
	jclass		nativeLLClass = (*env)->FindClass(env, "com/intel/bluetooth/NativeLibLoader");
	jobject exp = (*env)->ExceptionOccurred(env);
	if(exp) {
		(*env)->ExceptionDescribe(env);
		return;
	}

	jfieldID	aField = (*env)->GetStaticFieldID(env, nativeLLClass, "triedToLoadAlredy", "Z");
	(*env)->SetStaticBooleanField(env, nativeLLClass, aField, 1);
	aField = (*env)->GetStaticFieldID(env, nativeLLClass, "libraryAvailable", "Z");
	(*env)->SetStaticBooleanField(env, nativeLLClass, aField, 1);

	JNI_OnLoad(jvm, NULL);
	/* invoke the main thread */
	 cls = (*env)->FindClass(env, "com/intel/bluetooth/test/ClientTest");
//	 cls = (*env)->FindClass(env, "com/intel/bluetooth/test/StandaloneTest");
//	 cls = (*env)->FindClass(env, "com/intel/bluetooth/test/DummyTest");
	 stringCls = (*env)->FindClass(env, "java/lang/String");
	jmethodID mid = (*env)->GetStaticMethodID(env, cls, "main", "([Ljava/lang/String;)V");

jobjectArray params = (*env)->NewObjectArray(env, 0,  stringCls, NULL);
	jsize	aSize = (*env)->GetArrayLength(env, params);
	(*env)->CallStaticVoidMethod(env, cls, mid, params);
	/* just in case main returns right away sleep the thread longer than the debug session */
	sleep(10000000);
	(*jvm)->DestroyJavaVM(jvm);
	
	return 0;
	}