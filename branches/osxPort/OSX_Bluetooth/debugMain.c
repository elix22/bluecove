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
	JNINativeMethod			nativeMethods[]={
		{"doInquiry", "(ILjavax/bluetooth/DiscoveryListener;)I", Java_com_intel_bluetooth_BluetoothPeer_doInquiry},
		{"cancelInquiry", "(Ljavax/bluetooth/DiscoveryListener;)Z",Java_com_intel_bluetooth_BluetoothPeer_cancelInquiry },
		{"getServiceHandles", "([Ljavax/bluetooth/UUID;J)[I", Java_com_intel_bluetooth_BluetoothPeer_getServiceHandles},
		{"getServiceAttributes", "([IJI)[B", Java_com_intel_bluetooth_BluetoothPeer_getServiceAttributes},
		{"registerService", "([B)I",Java_com_intel_bluetooth_BluetoothPeer_registerService},
		{"unregisterService", "(I)V", Java_com_intel_bluetooth_BluetoothPeer_unregisterService},
		{"socket", "(ZZ)I", Java_com_intel_bluetooth_BluetoothPeer_socket},
		{"getsockaddress", "(I)J", Java_com_intel_bluetooth_BluetoothPeer_getsockaddress},
		{"getsockchannel", "(I)I", Java_com_intel_bluetooth_BluetoothPeer_getsockchannel},
		{"connect", "(IJI)V",Java_com_intel_bluetooth_BluetoothPeer_connect },
		{"listen", "(I)V", Java_com_intel_bluetooth_BluetoothPeer_listen},
		{"accept", "(I)I", Java_com_intel_bluetooth_BluetoothPeer_accept},
		{"recv", "(I)I", Java_com_intel_bluetooth_BluetoothPeer_recv__I},
		{"recv", "(I[BII)I", Java_com_intel_bluetooth_BluetoothPeer_recv__I_3BII},
		{"send", "(II)V", Java_com_intel_bluetooth_BluetoothPeer_send__II},
		{"send", "(I[BII)V", Java_com_intel_bluetooth_BluetoothPeer_send__I_3BII},
		{"close", "(I)V", Java_com_intel_bluetooth_BluetoothPeer_close},
		{"getpeername", "(J)Ljava/lang/String;", Java_com_intel_bluetooth_BluetoothPeer_getpeername},
		{"getpeeraddress", "(I)J", Java_com_intel_bluetooth_BluetoothPeer_getpeeraddress},
		{"getradioname", "(J)Ljava/lang/String;", Java_com_intel_bluetooth_BluetoothPeer_getradioname}};
		
		
	jclass		interface = (*env)->FindClass(env, "com/intel/bluetooth/BluetoothPeer");
	err = (*env)->RegisterNatives(env, interface, nativeMethods, 20);

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