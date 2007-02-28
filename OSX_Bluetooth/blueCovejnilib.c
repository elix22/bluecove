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

#include "blueCovejnilib.h"

/* The following is a list of mutex's that allow only one thread to access each function call 
   at a time */
 



/**
 * Called by the VM when this library is loaded. We use it to set up the CFRunLoop and sources
 */
 
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
		pthread_t				aThread;
		pthread_mutex_t			initializeMutex;
		pthread_cond_t			initializeCond;		
		
		s_inquiryList = NULL;
		s_serviceInqList = NULL;
		s_openSocketList = NULL;
		printMessage("Loading " NATIVE_DESCRIP "\n", DEBUG_INFO_LEVEL);
		
		pthread_cond_init(&initializeCond, NULL);
		pthread_mutex_init(&initializeMutex, NULL);
		s_vm = vm;
		
		pthread_mutex_lock(&initializeMutex);
		/* start the OS X thread up */
		printMessage("JNI_OnLoad: Starting the OS X init and run thread", DEBUG_INFO_LEVEL);
		pthread_create(&aThread, NULL, cocoaWrapper, (void*) &initializeCond);
		/* wait until the OS X thread has initialized before returning */
		printMessage("JNI_OnLoad: Waiting for initialization to complete", DEBUG_INFO_LEVEL);
		
		pthread_cond_wait(&initializeCond, &initializeMutex);
		
		printMessage("JNI_OnLoad: Initialization complete returning from onload", DEBUG_INFO_LEVEL);
		/* clean up*/
		pthread_cond_destroy(&initializeCond);
		pthread_mutex_unlock(&initializeMutex);
		pthread_mutex_destroy(&initializeMutex);

		printMessage("Loaded " NATIVE_DESCRIP, DEBUG_NORM_LEVEL);
		
		return JNI_VERSION_1_2;
}

	
JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved){
	/* should cleanup even be attempted? */
	printMessage("Unloading " NATIVE_DESCRIP "\n", DEBUG_INFO_LEVEL);
}



/*
 * Class:     com_intel_bluetooth_DiscoveryAgentImpl
 * Method:    retrieveDevices
 * Signature: (I)[Ljavax/bluetooth/RemoteDevice;
 */
JNIEXPORT jobjectArray JNICALL Java_com_intel_bluetooth_DiscoveryAgentImpl_retrieveDevices
  (JNIEnv *env, jobject peer, jint option) {
  		printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_retrieveDevices entered", DEBUG_INFO_LEVEL);
  	
		throwException(env, "com/intel/bluetooth/NotImplementedError", NULL);
  	
  		printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_retrieveDevices exited", DEBUG_INFO_LEVEL);
  	
  		return NULL;
  }
/*
 * Class:     com_intel_bluetooth_DiscoveryAgentImpl
 * Method:    startInquiry
 * Signature: (ILjavax/bluetooth/DiscoveryListener;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_DiscoveryAgentImpl_startInquiry
  (JNIEnv *env, jobject peer, jint accessCode, jobject listener){
		
		CFRunLoopSourceContext		aContext={0};
		doInquiryRec				*record;
		
		printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_startInquiry called", DEBUG_INFO_LEVEL);
		
		CFRunLoopSourceGetContext(s_inquiryStartSource, &aContext);

		/* set the data for the work function */
		record = (doInquiryRec*) aContext.info;
		record->peer = (*env)->NewGlobalRef(env, peer);
		record->accessCode = accessCode;
		record->listener = (*env)->NewGlobalRef(env, listener);
		
		if(inOSXThread()) {
			aContext.perform(record);
		} else {
			CFRunLoopSourceSignal(s_inquiryStartSource);
			CFRunLoopWakeUp (s_runLoop);
		}
		printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_startInquiry exiting", DEBUG_INFO_LEVEL);
		return -1;
	
}


JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_DiscoveryAgentImpl_cancelInquiry
  (JNIEnv *env, jobject peer, jobject listener){
	
	cancelInquiryRec			*record;
	pthread_mutex_t				aMutex;
	CFRunLoopSourceContext		aContext = {0};
	
	printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_cancelInquiry: called", DEBUG_INFO_LEVEL);
	

	CFRunLoopSourceGetContext(s_inquiryStopSource, &aContext);

	record = (cancelInquiryRec*) aContext.info;
	record->peer = peer;
	record->listener = listener; /* no need for a global ref since we're done with this when we return */
	record->validCondition = 0;
	if(inOSXThread()) {
			aContext.perform(record);
	} else {
		pthread_cond_init(&record->waiter, NULL);
		pthread_mutex_init(&aMutex, NULL);
		pthread_mutex_lock(&aMutex);
		record->validCondition = 1;

		CFRunLoopSourceSignal(s_inquiryStopSource);
		CFRunLoopWakeUp (s_runLoop);
	
		/* wait until the work is done */
		pthread_cond_wait(&record->waiter, &aMutex);
	
		/* cleanup */
		pthread_cond_destroy(&record->waiter);
		pthread_mutex_destroy(&aMutex);
	}
	printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_cancelInquiry: exiting", DEBUG_INFO_LEVEL);

	
	return record->success;
  }
  
JNIEXPORT jint JNICALL Java_com_intel_bluetooth_DiscoveryAgentImpl_searchServices
  (JNIEnv *env, jobject peer, jintArray attrSet, jobjectArray uuidSet, jobject device, jobject listener){
	
	searchServicesRec				*record;
	CFRunLoopSourceContext		aContext = {0};
	currServiceInq				*mySearchServices;
	jstring						deviceAddress;
	jmethodID					getAddress;
	jclass						deviceClass;
	
	printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_searchServices: called", DEBUG_INFO_LEVEL);
	
	deviceClass = (*env)->GetObjectClass(env, device);
	getAddress = (*env)->GetMethodID(env, deviceClass, "getBluetoothAddress", "()Ljava/lang/String;");
	deviceAddress = (*env)->CallObjectMethod(env, device, getAddress);
	
	
	mySearchServices = newServiceInqRec();
	CFRunLoopSourceGetContext(s_searchServicesStart, &aContext);

	record = (searchServicesRec*) aContext.info;
	record->peer =  (*env)->NewGlobalRef(env, peer);
	// what if attrSet is NULL?
	record->attrSet =  (*env)->NewGlobalRef(env, attrSet);
	record->uuidSet = (*env)->NewGlobalRef(env, uuidSet);
	record->deviceAddress = (*env)->NewGlobalRef(env, deviceAddress);
	record->device = (*env)->NewGlobalRef(env, device);
	record->listener = (*env)->NewGlobalRef(env, listener); /* no need for a global ref since we're done with this when we return */
	record->theInq = mySearchServices;
	if(inOSXThread()) {
			aContext.perform(record);
	} else {
		CFRunLoopSourceSignal(s_searchServicesStart);
		CFRunLoopWakeUp(s_runLoop);
	}
	printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_searchServices exiting", DEBUG_INFO_LEVEL);

	return mySearchServices->index;
  
}
JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_ServiceRecordImpl_native_1populateRecord
  (JNIEnv *env, jobject peer, jintArray attrIDs){
  		CFRunLoopSourceContext		aContext={0};
		populateAttributesRec		*record;
		pthread_mutex_t				aMutex;
		
		printMessage("Java_com_intel_bluetooth_ServiceRecordImpl_native_1populateRecord called", DEBUG_INFO_LEVEL);
			
		CFRunLoopSourceGetContext(s_populateServiceAttrs, &aContext);
		record = (populateAttributesRec*) aContext.info;
		record->serviceRecord = peer;
		record->attrSet = attrIDs;
		record->validCondition = 0;

		if(inOSXThread()) {
			aContext.perform(record);
		} else {
			pthread_cond_init(&record->waiter, NULL);
			record->validCondition = 1;
			pthread_mutex_init(&aMutex, NULL);
			pthread_mutex_lock(&aMutex);
		
			CFRunLoopSourceSignal(s_populateServiceAttrs);
			CFRunLoopWakeUp (s_runLoop);
		
			// wait until the work is done
			pthread_cond_wait(&record->waiter, &aMutex);
		
			// cleanup
			pthread_cond_destroy(&record->waiter);
			pthread_mutex_destroy(&aMutex);
		}
 		printMessage("Java_com_intel_bluetooth_ServiceRecordImpl_native_1populateRecord exiting", DEBUG_INFO_LEVEL);
 
		return record->result;
  }
  
JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_DiscoveryAgentImpl_cancelServiceSearch
  (JNIEnv *env, jobject peer, jint transID){
  	printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_cancelServiceSearch called", DEBUG_INFO_LEVEL);
	printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_cancelServiceSearch exiting", DEBUG_INFO_LEVEL);
	
	return 0;
  
}
/*
 * Class:     com_intel_bluetooth_DiscoveryAgentImpl
 * Method:    selectService
 * Signature: (Ljavax/bluetooth/UUID;IZ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_DiscoveryAgentImpl_selectService
  (JNIEnv *env, jobject agent, jobject uuid, jint security, jboolean master) {
  	printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_selectService called", DEBUG_INFO_LEVEL);
	
	throwException(env, "com/intel/bluetooth/NotImplementedError", NULL);
	
	printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_selectService exiting", DEBUG_INFO_LEVEL);
  	return NULL;
  }




JNIEXPORT jbyteArray JNICALL Java_com_intel_bluetooth_ServiceRecordImpl_getServiceAttributes
  (JNIEnv *env, jobject peer, jintArray attrIDs, jlong address, jint handle){
    
	printMessage("Java_com_intel_bluetooth_BluetoothPeer_getServiceAttributes called", DEBUG_INFO_LEVEL);
	
	throwException(env, "com/intel/bluetooth/NotImplementedError", "getServiceAttributes not implemented on Mac OS X");
	
	printMessage("Java_com_intel_bluetooth_BluetoothPeer_getServiceAttributes exiting", DEBUG_INFO_LEVEL);

	return NULL;
  
  }
  

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_registerService
  (JNIEnv *env, jobject peer, jbyteArray record){
  printMessage("Java_com_intel_bluetooth_BluetoothPeer_registerService called", DEBUG_INFO_LEVEL);
  printMessage("Java_com_intel_bluetooth_BluetoothPeer_registerService exiting", DEBUG_INFO_LEVEL);
  return 0;
  }


JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_unregisterService
  (JNIEnv *env, jobject peer, jint handle){
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_unregisterService called", DEBUG_INFO_LEVEL);
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_unregisterService exiting", DEBUG_INFO_LEVEL);
}


JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothL2CAPConnection_socket
  (JNIEnv *env, jobject peer, jboolean authenticate, jboolean encrypt){
    printMessage("Java_com_intel_bluetooth_BluetoothL2CAPConnection_socket called", DEBUG_INFO_LEVEL);
	
	macSocket*			aSocket;
	
	aSocket = newMacSocket();
	aSocket->encrypted = encrypt;
	aSocket->authenticate = authenticate;
	aSocket->type = 0;
		
    printMessage("Java_com_intel_bluetooth_BluetoothL2CAPConnection_socket exiting", DEBUG_INFO_LEVEL);

	return aSocket->index;
}


JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_getsockchannel
  (JNIEnv *env, jobject peer, jint socket){
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_getsockchannel called", DEBUG_INFO_LEVEL);
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_getsockchannel exiting", DEBUG_INFO_LEVEL);

  
  return 0;
  }


JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothRFCOMMConnection_nativeConnect
  (JNIEnv *env, jobject peer, jint socket, jlong address, jint channel, jint rMTU, jint tMTU){

	connectRec					connectionRequest;
	CFRunLoopSourceContext		aContext={0};
	todoListRoot				*connectionToDoList;
	threadPassType				typeMask;
	pthread_mutex_t				callInProgress;

	
    printMessage("Java_com_intel_bluetooth_BluetoothRFCOMMConnection_nativeConnect called", DEBUG_INFO_LEVEL);

	typeMask.connectPtr = &connectionRequest;
	
	connectionRequest.peer = peer;
	connectionRequest.socket = socket;
	connectionRequest.address = address;
	connectionRequest.channel = channel;
	connectionRequest.errorException = NULL;
	CFRunLoopSourceGetContext(s_NewRFCOMMConnectionRequest, &aContext);
	connectionToDoList = (todoListRoot*)aContext.info;
	connectionRequest.validCondition =0;
	addToDoItem(connectionToDoList, typeMask);

	if(inOSXThread()) {
		aContext.perform(connectionToDoList);
	} else {
	
		pthread_cond_init(&(connectionRequest.callComplete), NULL);
		pthread_mutex_init(&callInProgress, NULL);	
		pthread_mutex_lock(&callInProgress);
		connectionRequest.validCondition = 1;

		CFRunLoopSourceSignal(s_NewRFCOMMConnectionRequest);
		CFRunLoopWakeUp(s_runLoop);
	
		pthread_cond_wait(&(connectionRequest.callComplete), &callInProgress);
		pthread_mutex_destroy(&callInProgress);
		pthread_cond_destroy(&(connectionRequest.callComplete));
	}
	if(connectionRequest.errorException) {
		/* there was a problem */
		(*env)->Throw(env, connectionRequest.errorException);
		(*env)->DeleteGlobalRef(env, connectionRequest.errorException);
	}
	
    printMessage("Java_com_intel_bluetooth_BluetoothRFCOMMConnection_nativeConnect exiting", DEBUG_INFO_LEVEL);
}



JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothL2CAPConnection_closeSocket
  (JNIEnv *env, jobject peer, jint socket){
  
	macSocket				*aSocket;
	
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_close called", DEBUG_INFO_LEVEL);
    
	aSocket = getMacSocket(socket);
	if(aSocket->ref.l2capRef) {

		/* TODO call IOBluetoothL2CAPChannelCloseChannel in the OS X thread */
	}
	if(aSocket->ref.rfcommRef) {
	
		/* TODO call IOBluetoothRFCOMMChannelCloseChannel */
	}
	
	disposeMacSocket(aSocket);
	
	
	printMessage("Java_com_intel_bluetooth_BluetoothPeer_close exiting", DEBUG_INFO_LEVEL);

  
  }


JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_RemoteDeviceImpl_getFriendlyName
  (JNIEnv *env, jobject peer, jboolean alwaysAsk){
    printMessage("Java_com_intel_bluetooth_RemoteDeviceImpl_getFriendlyName entered", DEBUG_INFO_LEVEL);
    
	throwException(env, "com/intel/bluetooth/NotImplementedError", NULL);
		
    printMessage("Java_com_intel_bluetooth_RemoteDeviceImpl_getFriendlyName exiting", DEBUG_INFO_LEVEL);

  	return NULL;
 }



JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothInputStream_pipePrime(JNIEnv *env, jobject peer, jint numBytes){
	/* do nothing on this platform */
  }
  
  
 
JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_LocalDeviceImpl_getLocalAddress(JNIEnv *env, jobject peer) {

	jstring				localAddress, propertyName;
	jclass				propList;
	jmethodID			getProperty;
	const char			*addressString;
	int					addressValue[6];
	int					i;
	UInt64				intAddress;
	
	printMessage("Java_com_intel_bluetooth_LocalDeviceImpl_getLocalAddress entered", DEBUG_INFO_LEVEL);
	/* I assume this always just for the local device */
	propertyName = JAVA_ENV_CHECK(NewStringUTF(env, BLUECOVE_SYSTEM_PROP_LOCAL_ADDRESS));
	propList = JAVA_ENV_CHECK(GetObjectClass(env, s_systemProperties));
	getProperty =  JAVA_ENV_CHECK(GetMethodID(env, propList, "getProperty", "(Ljava/lang/String;)Ljava/lang/String;"));
	localAddress = JAVA_ENV_CHECK(CallObjectMethod(env, s_systemProperties, getProperty, propertyName));
	addressString =  JAVA_ENV_CHECK(GetStringUTFChars(env, localAddress, NULL));
	sscanf(addressString, "%2x-%2x-%2x-%2x-%2x-%2x", &addressValue[0], &addressValue[1], &addressValue[2],
									&addressValue[3], &addressValue[4], &addressValue[5]);
	JAVA_ENV_CHECK(ReleaseStringUTFChars(env, localAddress, addressString));
	intAddress = 0LL;
	for(i=0;i<6;i++) {
		intAddress <<= 8;
		intAddress |= addressValue[i];
	}
		
    printMessage("Java_com_intel_bluetooth_LocalDeviceImpl_getLocalAddress exiting", DEBUG_INFO_LEVEL);

	return intAddress;
}


JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_LocalDeviceImpl_getFriendlyName(JNIEnv *env, jobject localDevice){

	localNameRec					nameRequest;
	CFRunLoopSourceContext			aContext={0};
	todoListRoot					*nameRequestToDoList;
	threadPassType					typeMask;
	pthread_mutex_t					callInProgress;
	jstring							result;
	
    printMessage("Java_com_intel_bluetooth_LocalDeviceImpl_getFriendlyName entered", DEBUG_INFO_LEVEL);
    
	typeMask.localNamePtr = &nameRequest;
	
	nameRequest.aName = NULL;
	
	CFRunLoopSourceGetContext(s_LocalDeviceNameRequest, &aContext);
	nameRequestToDoList = (todoListRoot*)aContext.info;
	addToDoItem(nameRequestToDoList, typeMask);
	if(inOSXThread()) {
		nameRequest.validCondition = 0;
		aContext.perform(nameRequestToDoList);
	} else {

		pthread_cond_init(& (nameRequest.callComplete), NULL);
		pthread_mutex_init(&callInProgress, NULL);
		pthread_mutex_lock(&callInProgress);
		nameRequest.validCondition = 1;
		CFRunLoopSourceSignal(s_LocalDeviceNameRequest);
		CFRunLoopWakeUp(s_runLoop);
	
		pthread_cond_wait(&(nameRequest.callComplete), &callInProgress);
		pthread_mutex_destroy(&callInProgress);
		pthread_cond_destroy(&(nameRequest.callComplete));
	}
	result = NULL;
	if(nameRequest.aName) {
		result = JAVA_ENV_CHECK(NewLocalRef(env, nameRequest.aName));
		JAVA_ENV_CHECK(DeleteGlobalRef(env, nameRequest.aName));
	}
	

	printMessage("Java_com_intel_bluetooth_LocalDeviceImpl_getFriendlyName exiting", DEBUG_INFO_LEVEL);
	
	return result;
}


JNIEXPORT jobject JNICALL Java_com_intel_bluetooth_LocalDeviceImpl_getDeviceClass(JNIEnv *env, jobject localDevice){

	localDeviceClassRec				devClsRequest;
	CFRunLoopSourceContext			aContext={0};
	todoListRoot					*devRequestToDoList;
	threadPassType					typeMask;
	pthread_mutex_t					callInProgress;
	jobject							result;
	
    printMessage("Java_com_intel_bluetooth_LocalDeviceImpl_getDeviceClass entered", DEBUG_INFO_LEVEL);
    
	typeMask.localDevClassPtr = &devClsRequest;
	
	devClsRequest.devClass = NULL;
	
	CFRunLoopSourceGetContext(s_LocalDeviceClassRequest, &aContext);
	devRequestToDoList = (todoListRoot*)aContext.info;
	
	addToDoItem(devRequestToDoList, typeMask);

	if(inOSXThread()) {
		devClsRequest.validCondition = 0;
		aContext.perform(devRequestToDoList);
	} else {
		pthread_cond_init(& (devClsRequest.callComplete), NULL);
		pthread_mutex_init(&callInProgress, NULL);
		pthread_mutex_lock(&callInProgress);
		devClsRequest.validCondition = 1;
		CFRunLoopSourceSignal(s_LocalDeviceClassRequest);
		CFRunLoopWakeUp(s_runLoop);
	
		pthread_cond_wait(&(devClsRequest.callComplete), &callInProgress);
		pthread_mutex_destroy(&callInProgress);
		pthread_cond_destroy(&(devClsRequest.callComplete));
	}
	result = NULL;
	if(devClsRequest.devClass) {
		result = JAVA_ENV_CHECK(NewLocalRef(env, devClsRequest.devClass));
		JAVA_ENV_CHECK(DeleteGlobalRef(env, devClsRequest.devClass));
	}
		

	printMessage("Java_com_intel_bluetooth_LocalDeviceImpl_getDeviceClass exiting", DEBUG_INFO_LEVEL);
	
	return result;    
}


JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_LocalDeviceImpl_setDiscoverable(JNIEnv *env, jobject localDevice, jint mode){
	setDiscoveryModeRec				aRec;
	CFRunLoopSourceContext			aContext={0};
	todoListRoot					*aToDoList;
	threadPassType					typeMask;
	pthread_mutex_t					callInProgress;
	jboolean						result = JNI_FALSE;
	
    printMessage("Java_com_intel_bluetooth_LocalDeviceImpl_setDiscoverable entered", DEBUG_INFO_LEVEL);
    
	typeMask.setDiscoveryModePtr = &aRec;
	
	aRec.errorException = NULL;
	aRec.mode = mode;
	CFRunLoopSourceGetContext(s_LocalDeviceSetDiscoveryMode, &aContext);
	aToDoList = (todoListRoot*)aContext.info;
	
	addToDoItem(aToDoList, typeMask);
	
	if(inOSXThread()) {
		aRec.validCondition = 0;
		aContext.perform(aToDoList);
	} else {
		pthread_cond_init(& (aRec.callComplete), NULL);
		pthread_mutex_init(&callInProgress, NULL);
		pthread_mutex_lock(&callInProgress);
		aRec.validCondition = 1;
		
		CFRunLoopSourceSignal(s_LocalDeviceSetDiscoveryMode);
		CFRunLoopWakeUp(s_runLoop);
	
		pthread_cond_wait(&(aRec.callComplete), &callInProgress);	
		pthread_mutex_destroy(&callInProgress);
		pthread_cond_destroy(&(aRec.callComplete));
	}
	
	if(aRec.errorException) {
		/* there was a problem */
		(*env)->Throw(env, aRec.errorException);
		(*env)->DeleteGlobalRef(env, aRec.errorException);
	} else {
		result = (mode == aRec.mode);
	}
	
	printMessage("Java_com_intel_bluetooth_LocalDeviceImpl_setDiscoverable exiting", DEBUG_INFO_LEVEL);
	
	return result;
}


JNIEXPORT jobject JNICALL Java_com_intel_bluetooth_LocalDeviceImpl_getAdjustedSystemProperties(JNIEnv *env, jclass localDeviceCls){
	return s_systemProperties;
}


JNIEXPORT jint JNICALL Java_com_intel_bluetooth_LocalDeviceImpl_getDiscoverable(JNIEnv *env, jobject localDevice){
	getDiscoveryModeRec				aRec;
	CFRunLoopSourceContext			aContext={0};
	todoListRoot					*aToDoList;
	threadPassType					typeMask;
	pthread_mutex_t					callInProgress;
	
    printMessage("Java_com_intel_bluetooth_LocalDeviceImpl_getDiscoverable entered", DEBUG_INFO_LEVEL);
    
	typeMask.getDiscoveryModePtr = &aRec;
	
	aRec.mode = 0;
	CFRunLoopSourceGetContext(s_LocalDeviceGetDiscoveryMode, &aContext);
	aToDoList = (todoListRoot*)aContext.info;
	
	addToDoItem(aToDoList, typeMask);
	
	if(inOSXThread()) {
		aRec.validCondition = 0;
		aContext.perform(aToDoList);
	} else {
		pthread_cond_init(& (aRec.callComplete), NULL);
		pthread_mutex_init(&callInProgress, NULL);
		pthread_mutex_lock(&callInProgress);
		aRec.validCondition = 1;
	
		CFRunLoopSourceSignal(s_LocalDeviceGetDiscoveryMode);
		CFRunLoopWakeUp(s_runLoop);
	
		pthread_cond_wait(&(aRec.callComplete), &callInProgress);
		pthread_mutex_destroy(&callInProgress);
		pthread_cond_destroy(&(aRec.callComplete));
	}
    printMessage("Java_com_intel_bluetooth_LocalDeviceImpl_getDiscoverable exiting", DEBUG_INFO_LEVEL);
	
	return aRec.mode;

}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothL2CAPConnection_nativeConnect
								(JNIEnv *env, jobject peer, jint socket, jlong address, jint channel,
								 jint rMTU, jint tMTU){
    printMessage("Java_com_intel_bluetooth_BluetoothL2CAPConnection_nativeConnect entering", DEBUG_INFO_LEVEL);
    printMessage("Java_com_intel_bluetooth_BluetoothL2CAPConnection_nativeConnect exiting", DEBUG_INFO_LEVEL);
  
  }
JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothL2CAPConnection_send
								(JNIEnv *env, jobject jConn, jbyteArray jData){
    printMessage("Java_com_intel_bluetooth_BluetoothL2CAPConnection_send entering", DEBUG_INFO_LEVEL);
    printMessage("Java_com_intel_bluetooth_BluetoothL2CAPConnection_send exiting", DEBUG_INFO_LEVEL);
								
								}
								

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothOBEXConnection_nativeConnect
(JNIEnv *env, jobject peer, jint socket, jlong address, jint channel,
								 jint rMTU, jint tMTU){
    printMessage("Java_com_intel_bluetooth_BluetoothOBEXConnection_nativeConnect entering", DEBUG_INFO_LEVEL);
    printMessage("Java_com_intel_bluetooth_BluetoothOBEXConnection_nativeConnect exiting", DEBUG_INFO_LEVEL);
  
  }
  
  
JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothOBEXConnection_send
								(JNIEnv *env, jobject jConn, jbyteArray jData){
    printMessage("Java_com_intel_bluetooth_BluetoothOBEXConnection_send entering", DEBUG_INFO_LEVEL);
    printMessage("Java_com_intel_bluetooth_BluetoothOBEXConnection_send exiting", DEBUG_INFO_LEVEL);
								
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothRFCOMMConnection_send
								(JNIEnv *env, jobject jConn, jbyteArray jData){
	
	sendRFCOMMDataRec				*aRec;
	CFRunLoopSourceContext			aContext={0};
	todoListRoot					*aToDoList;
	threadPassType					typeMask;
	jsize							dataLen;
	jclass							connectionClass;
	jfieldID						aField;
	
    printMessage("Java_com_intel_bluetooth_BluetoothRFCOMMConnection_send entering", DEBUG_INFO_LEVEL);
    aRec = (sendRFCOMMDataRec*) malloc(sizeof(sendRFCOMMDataRec));
	
	typeMask.sendRFCOMMDataPtr = aRec;
	
	/* determine the socket to write to */
	connectionClass = JAVA_ENV_CHECK(GetObjectClass(env, jConn));
	aField = JAVA_ENV_CHECK(GetFieldID(env, connectionClass, "socket", "I"));
	aRec->socket = JAVA_ENV_CHECK(GetIntField(env, jConn, aField));

	/* copy the data into a byte buffer */
	dataLen = JAVA_ENV_CHECK(GetArrayLength(env, jData));
	aRec->bytes = malloc(dataLen);
	
	JAVA_ENV_CHECK(GetByteArrayRegion(env, jData, 0, dataLen, (SInt8*)aRec->bytes));
	aRec->buflength = dataLen;
		
	CFRunLoopSourceGetContext(s_SendRFCOMMData, &aContext);
	aToDoList = (todoListRoot*)aContext.info;
	
	addToDoItem(aToDoList, typeMask);
	if(inOSXThread()) {
		aContext.perform(aToDoList);
	} else {
		CFRunLoopSourceSignal(s_SendRFCOMMData);
		CFRunLoopWakeUp(s_runLoop);
	}	
    printMessage("Java_com_intel_bluetooth_BluetoothRFCOMMConnection_send exiting", DEBUG_INFO_LEVEL);
								
}


JNIEXPORT jintArray JNICALL Java_com_intel_bluetooth_BluetoothPeer_getServiceHandles
  (JNIEnv *env, jobject peer, jobjectArray uuidSet, jlong address){
	
	printMessage("Java_com_intel_bluetooth_BluetoothPeer_getServiceHandles: called", DEBUG_INFO_LEVEL);
	
	throwException(env, "com/intel/bluetooth/NotImplementedError", "getServiceHandles not implemented on Mac OS X");

	printMessage("Java_com_intel_bluetooth_BluetoothPeer_getServiceHandles: exiting", DEBUG_INFO_LEVEL);
	
	return NULL;
  
  
  }
