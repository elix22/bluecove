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

/**
 * Called by the VM when this library is loaded. We use it to set up the CFRunLoop and sources
 */
 
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
		pthread_t				aThread;
		pthread_mutex_t			initializeMutex;
		pthread_cond_t			initializeCond;		
		
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
  	
  		getPreknownDevicesRec			record;
  		threadPassType					typeMask;
  		
  		printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_retrieveDevices entered", DEBUG_INFO_LEVEL);
  	
 		record.option = option;
 		record.result = NULL;
 
  		if((option != 0) && (option != 1)) {
			throwException(env, "java/lang/IllegalArgumentException", "Option not valid");
  		} else {
  			
  			typeMask.dataReq.getPreknownDevicesPtr = &record;
  			
  			doSynchronousTask(s_GetPreknownDevices, &typeMask);
  		}
  			
  		printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_retrieveDevices exited", DEBUG_INFO_LEVEL);
  	
  		return record.result;
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
		threadPassType				*typeMaskPtr;
		todoListRoot				*todoListPtr;
		int							error=0;
		printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_startInquiry called", DEBUG_INFO_LEVEL);
		
		error = pthread_mutex_trylock(&s_inquiryInProgress);
		if(error == EBUSY) {
			/* stack is busy with an inquiry */
			throwException(env, "javax/bluetooth/BluetoothStateException", "Inquiry already in progress");
			
		} else {
			if(getPendingInquiryRef(listener)) { /* verify that the listener isn't aready being used */
				throwException(env, "java/lang/IllegalArgumentException", "DiscoveryListener is already being utilized in an inquiry!");
			
			} else {
				CFRunLoopSourceGetContext(s_inquiryStartSource, &aContext);
				todoListPtr = (todoListRoot*)aContext.info;
			
				/* set the data for the work function */
				record = (doInquiryRec*)malloc(sizeof(doInquiryRec));
				typeMaskPtr = (threadPassType*)malloc(sizeof(threadPassType));
			
				record->accessCode = accessCode;
				record->listener = JAVA_ENV_CHECK(NewGlobalRef(env, listener));

				typeMaskPtr->validCondition = FALSE;
				typeMaskPtr->dataReq.doInquiryPtr = record;
				addToDoItem(todoListPtr, typeMaskPtr);
			
				CFRunLoopSourceSignal(s_inquiryStartSource);
				CFRunLoopWakeUp (s_runLoop);
			}
			pthread_mutex_unlock(&s_inquiryInProgress);
		}
		printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_startInquiry exiting", DEBUG_INFO_LEVEL);
		return 1;
	
}


JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_DiscoveryAgentImpl_cancelInquiry
  (JNIEnv *env, jobject peer, jobject listener){
	
	cancelInquiryRec			record;
	threadPassType				typeMask;
	
	printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_cancelInquiry: called", DEBUG_INFO_LEVEL);
	
	typeMask.dataReq.cancelInquiryPtr = &record;
	record.listener = listener; /* no need for a global ref since we're done with this when we return */
	doSynchronousTask(s_inquiryStopSource, &typeMask);

	printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_cancelInquiry: exiting", DEBUG_INFO_LEVEL);

	return record.success;
  }
  
JNIEXPORT jint JNICALL Java_com_intel_bluetooth_DiscoveryAgentImpl_searchServices
  (JNIEnv *env, jobject peer, jintArray attrSet, jobjectArray uuidSet, jobject device, jobject listener){
	
	CFRunLoopSourceContext		aContext = {0};
	searchServicesRec			*record;
	threadPassType				*typeMaskPtr;
	todoListRoot				*todoListPtr;
	
	jstring						deviceAddress;
	jmethodID					getAddress;
	jclass						deviceClass;
	jint						myRef;
	
	printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_searchServices: called", DEBUG_INFO_LEVEL);
	/* TODO check for illeagal arguments */
	pthread_mutex_lock(&s_inquiryInProgress);

	/* get the address of the remote device */
	deviceClass = JAVA_ENV_CHECK(GetObjectClass(env, device));
	getAddress = JAVA_ENV_CHECK(GetMethodID(env, deviceClass, "getBluetoothAddress", "()Ljava/lang/String;"));
	deviceAddress = JAVA_ENV_CHECK(CallObjectMethod(env, device, getAddress));
	
	
	CFRunLoopSourceGetContext(s_searchServicesStart, &aContext);
	todoListPtr = (todoListRoot*)aContext.info;
	
	record = (searchServicesRec*)malloc(sizeof(searchServicesRec));
	typeMaskPtr = (threadPassType*)malloc(sizeof(threadPassType));
	
	if(attrSet) {
		record->attrSet = JAVA_ENV_CHECK(NewGlobalRef(env, attrSet));
	} else { 
		record->attrSet = NULL;
	}
	record->deviceAddress = JAVA_ENV_CHECK(NewGlobalRef(env, deviceAddress));
	record->listener = JAVA_ENV_CHECK(NewGlobalRef(env, listener));
	record->device = JAVA_ENV_CHECK(NewGlobalRef(env, device));
	record->stopped = FALSE;
	if(uuidSet) {
		record->uuidSet = JAVA_ENV_CHECK(NewGlobalRef(env, uuidSet));
	} else 
		record->uuidSet = NULL;
	
	myRef = addServiceSearch(record);
	record->refNum = myRef;
	
	typeMaskPtr->validCondition = FALSE;
	typeMaskPtr->dataReq.searchSrvPtr = record;
	addToDoItem(todoListPtr, typeMaskPtr);
	
	CFRunLoopSourceSignal(s_searchServicesStart);
	CFRunLoopWakeUp(s_runLoop);

	pthread_mutex_unlock(&s_inquiryInProgress);

	printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_searchServices exiting", DEBUG_INFO_LEVEL);

	return myRef;
  
}
JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_ServiceRecordImpl_native_1populateRecord
  (JNIEnv *env, jobject peer, jintArray attrIDs){
 		populateAttributesRec		record;
 		threadPassType				typeMask;
		
		printMessage("Java_com_intel_bluetooth_ServiceRecordImpl_native_1populateRecord called", DEBUG_INFO_LEVEL);
		
		typeMask.dataReq.populateAttrPtr = &record;
		record.serviceRecord = peer;
		record.attrSet = attrIDs;
	
		doSynchronousTask(s_populateServiceAttrs, &typeMask);

 		printMessage("Java_com_intel_bluetooth_ServiceRecordImpl_native_1populateRecord exiting", DEBUG_INFO_LEVEL);
 
		return record.result;
  }
  
JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_DiscoveryAgentImpl_cancelServiceSearch
  (JNIEnv *env, jobject peer, jint transID){
  	searchServicesRec*		aRec;
  	jboolean				result;
  	printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_cancelServiceSearch called", DEBUG_INFO_LEVEL);
  	/* we have no way of actually canceling the radio call but we can swallow the results */
  	aRec = getServiceSearchRec(transID);
  	if(aRec) {
  		result = TRUE;
  		aRec->stopped = TRUE;
  	} else result = FALSE;

	printMessage("Java_com_intel_bluetooth_DiscoveryAgentImpl_cancelServiceSearch exiting", DEBUG_INFO_LEVEL);
	
	return result;
  
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
	threadPassType				typeMask;
	int							error;
	
    printMessage("Java_com_intel_bluetooth_BluetoothRFCOMMConnection_nativeConnect called", DEBUG_INFO_LEVEL);

	error = pthread_mutex_trylock(&s_inquiryInProgress);
	if(error == EBUSY) {
		/* stack is busy with an inquiry */
		throwException(env, "java/io/IOException", "Can't make connections during Discovery Inquiry!");
	} else {

		typeMask.dataReq.connectPtr = &connectionRequest;
	
		connectionRequest.peer = peer;
		connectionRequest.socket = socket;
		connectionRequest.address = address;
		connectionRequest.channel = channel;
		connectionRequest.errorException = NULL;
	
		pthread_mutex_unlock(&s_inquiryInProgress);
		doSynchronousTask(s_NewRFCOMMConnectionRequest, &typeMask);
	
		if(connectionRequest.errorException) {
			/* there was a problem */
			(*env)->Throw(env, connectionRequest.errorException);
			(*env)->DeleteGlobalRef(env, connectionRequest.errorException);
		}
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
    
   	jclass				remoteDevCls;
   	jmethodID			getAddrMethod;
   	getRemoteNameRec		getNameRec;
   	threadPassType		typeMask;
 	
 	printMessage("Java_com_intel_bluetooth_RemoteDeviceImpl_getFriendlyName entered", DEBUG_INFO_LEVEL);
  	
 	remoteDevCls = JAVA_ENV_CHECK(GetObjectClass(env, peer));
 	getAddrMethod = JAVA_ENV_CHECK(GetMethodID(env, remoteDevCls, "getBluetoothAddress", "()Ljava/lang/String;"));
 	getNameRec.address = JAVA_ENV_CHECK(CallObjectMethod(env, peer, getAddrMethod));
 	getNameRec.alwaysAsk = alwaysAsk;
 	getNameRec.result = NULL;
 	getNameRec.errorException = NULL;
 	
 	typeMask.dataReq.getRemoteNamePtr = &getNameRec;
 	doSynchronousTask(s_RemoteDeviceGetFriendlyName, &typeMask);
	if(getNameRec.errorException) {
		(*env)->Throw(env, getNameRec.errorException);
		(*env)->DeleteGlobalRef(env, getNameRec.errorException);
	}
 	
 	printMessage("Java_com_intel_bluetooth_RemoteDeviceImpl_getFriendlyName exiting", DEBUG_INFO_LEVEL);

  	return getNameRec.result;
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
	/* TODO need to fix to actually check if the local address is available, if the hard
	 * ware isn't turned on when the library initializes then we'll not have a local address
	 * set */
	 
	printMessage("Java_com_intel_bluetooth_LocalDeviceImpl_getLocalAddress entered", DEBUG_INFO_LEVEL);
	/* I assume this always just for the local device */
	propertyName = JAVA_ENV_CHECK(NewStringUTF(env, BLUECOVE_SYSTEM_PROP_LOCAL_ADDRESS));
	propList = JAVA_ENV_CHECK(GetObjectClass(env, s_systemProperties));
	getProperty =  JAVA_ENV_CHECK(GetMethodID(env, propList, "getProperty", "(Ljava/lang/String;)Ljava/lang/String;"));
	localAddress = JAVA_ENV_CHECK(CallObjectMethod(env, s_systemProperties, getProperty, propertyName));
	fprintf(stderr, "localAddress %p\n", localAddress);
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
	threadPassType					typeMask;
	jstring							result;
	
    printMessage("Java_com_intel_bluetooth_LocalDeviceImpl_getFriendlyName entered", DEBUG_INFO_LEVEL);
    
	typeMask.dataReq.localNamePtr = &nameRequest;
	
	nameRequest.aName = NULL;
	
	doSynchronousTask(s_LocalDeviceNameRequest, &typeMask);

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
	threadPassType					typeMask;
	jobject							result;
	
    printMessage("Java_com_intel_bluetooth_LocalDeviceImpl_getDeviceClass entered", DEBUG_INFO_LEVEL);
    
	typeMask.dataReq.localDevClassPtr = &devClsRequest;	
	devClsRequest.devClass = NULL;
	
	doSynchronousTask(s_LocalDeviceClassRequest, &typeMask);

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
	threadPassType					typeMask;
	jboolean						result = JNI_FALSE;
	
    printMessage("Java_com_intel_bluetooth_LocalDeviceImpl_setDiscoverable entered", DEBUG_INFO_LEVEL);
    
	typeMask.dataReq.setDiscoveryModePtr = &aRec;
	
	aRec.errorException = NULL;
	aRec.mode = mode;
	
	doSynchronousTask(s_LocalDeviceSetDiscoveryMode, &typeMask);
	
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
	threadPassType					typeMask;
	
    printMessage("Java_com_intel_bluetooth_LocalDeviceImpl_getDiscoverable entered", DEBUG_INFO_LEVEL);
    
	typeMask.dataReq.getDiscoveryModePtr = &aRec;
	
	aRec.mode = 0;
	
	doSynchronousTask(s_LocalDeviceGetDiscoveryMode, &typeMask);

    printMessage("Java_com_intel_bluetooth_LocalDeviceImpl_getDiscoverable exiting", DEBUG_INFO_LEVEL);
	
	return aRec.mode;

}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothL2CAPConnection_nativeConnect
								(JNIEnv *env, jobject peer, jint socket, jlong address, jint channel,
								 jint rMTU, jint tMTU){
    printMessage("Java_com_intel_bluetooth_BluetoothL2CAPConnection_nativeConnect entering", DEBUG_INFO_LEVEL);
	throwException(env, "com/intel/bluetooth/NotImplementedError", "L2CAP not yet implemented on Mac OS X");
    printMessage("Java_com_intel_bluetooth_BluetoothL2CAPConnection_nativeConnect exiting", DEBUG_INFO_LEVEL);
  
  }
JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothL2CAPConnection_send
								(JNIEnv *env, jobject jConn, jbyteArray jData){
    printMessage("Java_com_intel_bluetooth_BluetoothL2CAPConnection_send entering", DEBUG_INFO_LEVEL);
	throwException(env, "com/intel/bluetooth/NotImplementedError", "L2CAP not yet implemented on Mac OS X");
    printMessage("Java_com_intel_bluetooth_BluetoothL2CAPConnection_send exiting", DEBUG_INFO_LEVEL);
								
								}
								

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothOBEXConnection_nativeConnect
(JNIEnv *env, jobject peer, jint socket, jlong address, jint channel,
								 jint rMTU, jint tMTU){
    printMessage("Java_com_intel_bluetooth_BluetoothOBEXConnection_nativeConnect entering", DEBUG_INFO_LEVEL);
	throwException(env, "com/intel/bluetooth/NotImplementedError", "OBEX not yet implemented on Mac OS X");
    printMessage("Java_com_intel_bluetooth_BluetoothOBEXConnection_nativeConnect exiting", DEBUG_INFO_LEVEL);
  
  }
  
  
JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothOBEXConnection_send
								(JNIEnv *env, jobject jConn, jbyteArray jData){
    printMessage("Java_com_intel_bluetooth_BluetoothOBEXConnection_send entering", DEBUG_INFO_LEVEL);
 	throwException(env, "com/intel/bluetooth/NotImplementedError", "OBEX not yet implemented on Mac OS X");
    printMessage("Java_com_intel_bluetooth_BluetoothOBEXConnection_send exiting", DEBUG_INFO_LEVEL);
								
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothRFCOMMConnection_send
								(JNIEnv *env, jobject jConn, jbyteArray jData){
	
	sendRFCOMMDataRec				*aRec;
	CFRunLoopSourceContext			aContext={0};
	todoListRoot					*aToDoList;
	threadPassType					*typeMaskPtr;
	jsize							dataLen;
	jclass							connectionClass;
	jfieldID						aField;
	
    printMessage("Java_com_intel_bluetooth_BluetoothRFCOMMConnection_send entering", DEBUG_INFO_LEVEL);
    aRec = (sendRFCOMMDataRec*) malloc(sizeof(sendRFCOMMDataRec));
	
	typeMaskPtr = (threadPassType*)malloc(sizeof(threadPassType));
	typeMaskPtr->dataReq.sendRFCOMMDataPtr = aRec;
	
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
	
	addToDoItem(aToDoList, typeMaskPtr);
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
