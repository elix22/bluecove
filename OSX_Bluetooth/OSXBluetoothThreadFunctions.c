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

/** 
 * This file contains the functions that are called in the single OSX CFRunLoop thread 
 */
 
#include "blueCovejnilib.h"
#include <stdlib.h>

/* library globals */
macSocket				*s_openSocketList;
JavaVM					*s_vm;		
CFRunLoopRef			s_runLoop;
CFRunLoopSourceRef		s_inquiryStartSource;
CFRunLoopSourceRef		s_inquiryStopSource;
CFRunLoopSourceRef		s_searchServicesStart;
CFRunLoopSourceRef		s_populateServiceAttrs;
CFRunLoopSourceRef		s_NewRFCOMMConnectionRequest;
CFRunLoopSourceRef		s_SendRFCOMMData;
CFRunLoopSourceRef		s_LocalDeviceNameRequest;
CFRunLoopSourceRef		s_LocalDeviceClassRequest;
CFRunLoopSourceRef		s_LocalDeviceSetDiscoveryMode;
CFRunLoopSourceRef		s_LocalDeviceGetDiscoveryMode;
CFRunLoopSourceRef		s_RemoteDeviceGetFriendlyName;
CFRunLoopSourceRef		s_GetPreknownDevices;
jobject					s_systemProperties;


/**
	An issue with the OS X BT implementation is all the calls need to come from the same thread. Since the java source 
	calls could be coming from many thread sources we consolidate them into a CFRunLoop thread as CFRunLoopSources
	
	 this first function doesn't do much except quiet complaints about memory leaks, unless the underlying implementation
		starts to actually do garbage collection before the function returns. Which is always possible. */
		
void* cocoaWrapper(void* v_pthreadCond) {
	ThreadCleanups			theWrapper;
	
	theWrapper.function = runLoopThread;
	theWrapper.data = v_pthreadCond;
	
	OSXThreadWrapper(&theWrapper);

	return NULL;
	}

void* runLoopThread(void* v_threadValues) {
	jint					jErr;
	JNIEnv					*env;
	todoListRoot			pendingConnections;
	todoListRoot			rfCOMMDataQueue;
	todoListRoot			pendingLocalDeviceRequests;
	todoListRoot			pendingLocalDeviceClassRequests;
	todoListRoot			getDiscoveryModeList;
	todoListRoot			setDiscoveryModeList;
	todoListRoot			populateAttributesList;
	todoListRoot			getRemoteDevFriendlyNameList;
	todoListRoot			getPreknownDeviceList;
	todoListRoot			startInquiryList;
	todoListRoot			cancelInquiryList;
	todoListRoot			searchSrvStartList;
	int						propGenErr;
	

	{
		JavaVMAttachArgs		args;
		args.version = JNI_VERSION_1_2;
		args.name = "OS X Bluetooth CFRunLoop";
		args.group = NULL;
	
		jErr= (*s_vm)->AttachCurrentThreadAsDaemon(s_vm, (void**)&env, &args);
		
		if(jErr) {
			printMessage("Error encountered creating the Runloop thread\n", DEBUG_ERROR_LEVEL);
		} else {
			printMessage("Successfully created the Runloop thread\n", DEBUG_INFO_LEVEL);
		}
	}
	
	{
		/* create event sources, i.e. requests from the java VM */
		CFRunLoopSourceContext		aContext = {0};
		
		s_runLoop = CFRunLoopGetCurrent();
				
		/* create/install the inquiry start source */
		initializeToDoList(&startInquiryList);
		aContext.info = &startInquiryList;
		aContext.perform = performInquiry;
		s_inquiryStartSource = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &aContext);
		CFRunLoopAddSource(s_runLoop, s_inquiryStartSource, kCFRunLoopDefaultMode);
		printMessage("Registed inquiry Start Source", DEBUG_INFO_LEVEL);
		
		/* create/install the inquiry stop source */
		initializeToDoList(&cancelInquiryList);
		aContext.info = &cancelInquiryList;
		aContext.perform = cancelInquiry;
		s_inquiryStopSource = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &aContext);
		CFRunLoopAddSource(s_runLoop, s_inquiryStopSource, kCFRunLoopDefaultMode);
		printMessage("Registered inquiry Stop Source", DEBUG_INFO_LEVEL);
		
		/* create/istall the search services start source */
		initializeToDoList(&searchSrvStartList);
		aContext.info = &searchSrvStartList;
		aContext.perform = asyncSearchServices;
		s_searchServicesStart = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &aContext);
		CFRunLoopAddSource(s_runLoop, s_searchServicesStart, kCFRunLoopDefaultMode);
		printMessage("Registered Service Search Start Source", DEBUG_INFO_LEVEL);
		
		
		/* create/istall the service attribute populater here  */
		initializeToDoList(&populateAttributesList);
		aContext.info = &populateAttributesList;
		aContext.perform = getServiceAttributes;
		s_populateServiceAttrs = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &aContext);
		CFRunLoopAddSource(s_runLoop, s_populateServiceAttrs, kCFRunLoopDefaultMode);
		printMessage("Registered Populate Service Attributes Start Source", DEBUG_INFO_LEVEL);
			
		/* create/install the new RFCOMM connection source here */
		initializeToDoList(&pendingConnections);
		aContext.info = &pendingConnections;
		aContext.perform = RFCOMMConnect;
		s_NewRFCOMMConnectionRequest = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &aContext);
		CFRunLoopAddSource(s_runLoop, s_NewRFCOMMConnectionRequest, kCFRunLoopDefaultMode);
		printMessage("Registered RFCOMM new connection Start Source", DEBUG_INFO_LEVEL);
		
		/* create/install the send RFCOMM data here */
		initializeToDoList(&rfCOMMDataQueue);
		aContext.info = &rfCOMMDataQueue;
		aContext.perform = rfcommSendData;
		s_SendRFCOMMData = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &aContext);
		CFRunLoopAddSource(s_runLoop, s_SendRFCOMMData, kCFRunLoopDefaultMode);
		printMessage("Registered send RFCOMM data Start Source", DEBUG_INFO_LEVEL);

		/* create/install the local name request source here */
		initializeToDoList(&pendingLocalDeviceRequests);
		aContext.info = &pendingLocalDeviceRequests;
		aContext.perform = getLocalDeviceName;
		s_LocalDeviceNameRequest = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &aContext);
		CFRunLoopAddSource(s_runLoop, s_LocalDeviceNameRequest, kCFRunLoopDefaultMode);
		printMessage("Registered local name request Start Source", DEBUG_INFO_LEVEL);
		
		/* create/install the local device class request source here */
		initializeToDoList(&pendingLocalDeviceClassRequests);
		aContext.info = &pendingLocalDeviceClassRequests;
		aContext.perform = getLocalDeviceClass;
		s_LocalDeviceClassRequest = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &aContext);
		CFRunLoopAddSource(s_runLoop, s_LocalDeviceClassRequest, kCFRunLoopDefaultMode);
		printMessage("Registered local device class request Start Source", DEBUG_INFO_LEVEL);
		
		/* create/install the get discover mode here */
		initializeToDoList(&getDiscoveryModeList);
		aContext.info = &getDiscoveryModeList;
		aContext.perform = getLocalDiscoveryMode;
		s_LocalDeviceGetDiscoveryMode = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &aContext);
		CFRunLoopAddSource(s_runLoop, s_LocalDeviceGetDiscoveryMode, kCFRunLoopDefaultMode);
		printMessage("Registered get discovery mode Start Source", DEBUG_INFO_LEVEL);

		/* create/install the set discovery mode here */
		initializeToDoList(&setDiscoveryModeList);
		aContext.info = &setDiscoveryModeList;
		aContext.perform = setLocalDiscoveryMode;
		s_LocalDeviceSetDiscoveryMode = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &aContext);
		CFRunLoopAddSource(s_runLoop, s_LocalDeviceSetDiscoveryMode, kCFRunLoopDefaultMode);
		printMessage("Registered set discovery mode Start Source", DEBUG_INFO_LEVEL);

		/* create/install the set discovery mode here */
		initializeToDoList(&getRemoteDevFriendlyNameList);
		aContext.info = &getRemoteDevFriendlyNameList;
		aContext.perform = getRemoteDeviceFriendlyName;
		s_RemoteDeviceGetFriendlyName = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &aContext);
		CFRunLoopAddSource(s_runLoop, s_RemoteDeviceGetFriendlyName, kCFRunLoopDefaultMode);
		printMessage("Registered get remote device name Source", DEBUG_INFO_LEVEL);

		/* create/install the set discovery mode here */
		initializeToDoList(&getPreknownDeviceList);
		aContext.info = &getPreknownDeviceList;
		aContext.perform = getPreknownDevices;
		s_GetPreknownDevices = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &aContext);
		CFRunLoopAddSource(s_runLoop, s_GetPreknownDevices, kCFRunLoopDefaultMode);
		printMessage("Registered get preknown devices Source", DEBUG_INFO_LEVEL);
		
	}

	{
		jclass			systemClass;
		jmethodID		aMethod;
		jobject			exception;

		systemClass = JAVA_ENV_CHECK(FindClass(env, "java/lang/System"));
		aMethod = JAVA_ENV_CHECK(GetStaticMethodID(env, systemClass, "getProperties", "()Ljava/util/Properties;"));
		s_systemProperties = JAVA_ENV_CHECK(CallStaticObjectMethod(env, systemClass, aMethod));
		s_systemProperties = JAVA_ENV_CHECK(NewGlobalRef(env, s_systemProperties));
		
		
		propGenErr = generateProperties(env);
		
			
		/* Try to set the collected properties into the system properties */
		printMessage("Adding Bluetooth Properties to Java System Properties", DEBUG_INFO_LEVEL);
		aMethod = JAVA_ENV_CHECK(GetStaticMethodID(env, systemClass, "setProperties",
									"(Ljava/util/Properties;)V"));
		JAVA_ENV_CHECK(CallStaticVoidMethod(env, systemClass, aMethod, s_systemProperties));
		
		exception = (*env)->ExceptionOccurred(env);
		
		if(exception) {
			printMessage("Couldn't set system properties, security manager blocked it.", DEBUG_WARN_LEVEL);
			(*env)->ExceptionClear(env);
		} else {
			printMessage("Successfully updated system properties.", DEBUG_INFO_LEVEL);
		}
				
		JAVA_ENV_CHECK(DeleteLocalRef(env, systemClass));
		if(exception) JAVA_ENV_CHECK(DeleteLocalRef(env, exception));

		
	}
	{
		/* TODO make sure that this works even if the BluetoothPeer object hasn't been instatiated */
		jclass			serviceImpl, inputStream;
		jfieldID		nativeLibParsesSDP, deadlockPreventor;
		
		printMessage("Attempting to set static class variables specific to native lib functionality", DEBUG_INFO_LEVEL);

		
		serviceImpl = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/ServiceRecordImpl"));
		nativeLibParsesSDP = JAVA_ENV_CHECK(GetStaticFieldID(env, serviceImpl, "nativeLibParsesSDP", "Z"));
		JAVA_ENV_CHECK(SetStaticBooleanField(env, serviceImpl, nativeLibParsesSDP, 1));
				
		inputStream = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/BluetoothInputStream"));
		deadlockPreventor = JAVA_ENV_CHECK(GetStaticFieldID(env, inputStream, "disableDeadlockPreventor", "Z"));
		JAVA_ENV_CHECK(SetStaticBooleanField(env, inputStream, deadlockPreventor, 0));
		
		JAVA_ENV_CHECK(DeleteLocalRef(env, serviceImpl));
		JAVA_ENV_CHECK(DeleteLocalRef(env, inputStream));
	}
	
	printMessage("Init complete, releasing the library load thread", DEBUG_INFO_LEVEL);
	
	pthread_cond_signal((pthread_cond_t*)v_threadValues);

	if(propGenErr) {
		printMessage("No OS X Bluetooth hardware available! CFRunLoop not started", DEBUG_ERROR_LEVEL);
		return NULL;
	}

	/* run the loop */
	printMessage("Starting the CFRunLoop", DEBUG_INFO_LEVEL);
	CFRunLoopRun();	
	/* should only reach this point when getting unloaded */

	printMessage("CF RunLoop exiting!\n", DEBUG_INFO_LEVEL);
	return NULL;
}



void performInquiry(void *voidPtr) {

		/* now in the run loop thread */
	todoListRoot		*aRoot;
	threadPassType		*aTypePtr;
	jint				jError;
	JNIEnv				*env;
 	
		
	printMessage("performInquiry: called", DEBUG_INFO_LEVEL);
	jError = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);
	if(jError != JNI_OK) {
		printMessage("performInquiry: unable to get java environment!", DEBUG_ERROR_LEVEL);
	}
		
	aRoot = (todoListRoot*)voidPtr;
	aTypePtr = getNextToDoItem(aRoot);
	while(aTypePtr) {
		doInquiryRec			*record = aTypePtr->dataReq.doInquiryPtr;
		IOReturn				error;
		IOBluetoothDeviceInquiryRef		aRef;
		
		aRef = IOBluetoothDeviceInquiryCreateWithCallbackRefCon(record);
		addInquiry(record->listener, aRef);
		
		/* listen for found devices */
		error = IOBluetoothDeviceInquirySetDeviceFoundCallback(aRef, inquiryDeviceFound);
			if(error != kIOReturnSuccess) {
				printMessage("performInquiry: IOBluetoothDeviceInquirySetDeviceFoundCallback Failed", DEBUG_ERROR_LEVEL);
				sprintf(s_errorBuffer, "%s%i", s_errorBase, error);
				printMessage(s_errorBuffer, DEBUG_ERROR_LEVEL);
				inquiryComplete(record->listener, aRef, error, TRUE);
				return;
			}
		/* set the completeion callback */
		error = IOBluetoothDeviceInquirySetCompleteCallback(aRef, inquiryComplete);
			if(error != kIOReturnSuccess) {
				printMessage("performInquiry: IOBluetoothDeviceInquirySetCompleteCallback Failed", DEBUG_ERROR_LEVEL);
				sprintf(s_errorBuffer, "%s%i", s_errorBase, error);
				printMessage(s_errorBuffer, DEBUG_ERROR_LEVEL);
				inquiryComplete(record->listener, aRef, error, TRUE);
				return;
			}
		
		error = IOBluetoothDeviceInquiryStart(aRef);
			if(error != kIOReturnSuccess) {
				printMessage("performInquiry: IOBluetoothDeviceInquiryStart Failed", DEBUG_ERROR_LEVEL);
				sprintf(s_errorBuffer, "%s%i", s_errorBase, error);
					printMessage(s_errorBuffer, DEBUG_ERROR_LEVEL);
					inquiryComplete(record->listener, aRef, error, TRUE);
					return;
			} else printMessage("performInquiry: IOBluetoothDeviceInquiryStart succeeded", DEBUG_INFO_LEVEL);
	
		
		aTypePtr = getNextToDoItem(aRoot);
	}
	
}


void inquiryDeviceFound(void *v_listener, IOBluetoothDeviceInquiryRef inquiryRef, IOBluetoothDeviceRef deviceRef)
{
	/* need to create a RemoteDevice and DeviceClass objects for the device */
	
	JNIEnv							*env;
	jclass							remoteDevCls, devCls, listenerCls;
	jmethodID						constructor, callback;
	jobject							remoteDev, remoteDeviceClass;
	BluetoothClassOfDevice			devClass;
	const BluetoothDeviceAddress	*devAddress;
	char							devAddressString[13];
	jstring							devAddressJString;
	jint							jErr;
	doInquiryRec					*record = (doInquiryRec*)v_listener;
		
	printMessage("inquiryDeviceFound called", DEBUG_INFO_LEVEL);
		
	/* Step 0: check if we're only looking for limited items */
	if(record->accessCode == kBluetoothLimitedInquiryAccessCodeLAPValue) {
		BluetoothServiceClassMajor	majorClass;
		
		majorClass = IOBluetoothDeviceGetServiceClassMajor(deviceRef);
		if( ! (majorClass & kBluetoothServiceClassMajorLimitedDiscoverableMode)) {
			printMessage("GAIC device found, not reporting it since LIAC was requested.", DEBUG_WARN_LEVEL);
			return;
		}
	}
	
	jErr = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);
	if(jErr) {
		sprintf(s_errorBuffer, "%s%ld", s_errorBase, jErr);
		printMessage(s_errorBuffer, DEBUG_ERROR_LEVEL);
	}

	/* Step 1: extract the address and name */
	devAddress = IOBluetoothDeviceGetAddress(deviceRef);
	if(!devAddress) {
		printMessage("IOBluetoothDeviceGetAddress returned null!", DEBUG_ERROR_LEVEL);
	}
	sprintf(devAddressString, "%02x%02x%02x%02x%02x%02x",devAddress->data[0],
					devAddress->data[1], devAddress->data[2], devAddress->data[3],
					devAddress->data[4], devAddress->data[5]); 
	devAddressJString = JAVA_ENV_CHECK(NewStringUTF(env, devAddressString));
	
	remoteDevCls = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/RemoteDeviceImpl"));
	constructor= JAVA_ENV_CHECK(GetMethodID(env, remoteDevCls, "<init>", "(Ljava/lang/String;)V"));
	
	remoteDev = JAVA_ENV_CHECK(NewObject(env, remoteDevCls, constructor, devAddressJString));
	printMessage("Remote Device object constructed", DEBUG_INFO_LEVEL);
	devClass = IOBluetoothDeviceGetClassOfDevice(deviceRef);
	
	devCls = JAVA_ENV_CHECK(FindClass(env, "javax/bluetooth/DeviceClass"));
	constructor = JAVA_ENV_CHECK(GetMethodID(env, devCls, "<init>", "(I)V"));
	
	remoteDeviceClass = JAVA_ENV_CHECK(NewObject(env, devCls, constructor, devClass));
	printMessage("Device Class Object constructed", DEBUG_INFO_LEVEL);
	listenerCls = JAVA_ENV_CHECK(GetObjectClass(env, record->listener));
	printMessage("Listener Class obtained", DEBUG_INFO_LEVEL);
	callback = JAVA_ENV_CHECK(GetMethodID(env, listenerCls, "deviceDiscovered", "(Ljavax/bluetooth/RemoteDevice;Ljavax/bluetooth/DeviceClass;)V"));
	printMessage("Calling callback on listener", DEBUG_INFO_LEVEL);
	/* call the java callback */
	JAVA_ENV_CHECK(CallVoidMethod(env, record->listener, callback, remoteDev, remoteDeviceClass));
	printMessage("Callback returned", DEBUG_INFO_LEVEL);
}


void inquiryComplete(void * 						userRefCon,
					IOBluetoothDeviceInquiryRef	inquiryRef,
					IOReturn					error,
					Boolean						aborted )
{
	JNIEnv					*env;
	jmethodID				callback;
	jclass					listenerCls;
	jint					discType;
	jint					jErr;
	doInquiryRec			*record  = (doInquiryRec*)userRefCon;
	
	printMessage("inquiryComplete: called", DEBUG_INFO_LEVEL);


	jErr = (*s_vm)->GetEnv(s_vm, (void**) &env, JNI_VERSION_1_2);
	listenerCls = JAVA_ENV_CHECK(GetObjectClass(env, record->listener));
	callback = JAVA_ENV_CHECK(GetMethodID(env, listenerCls, "inquiryCompleted", "(I)V"));
	printMessage("inquiryComplete: got callback", DEBUG_INFO_LEVEL);

	if(aborted) {
		discType = 5;
	} else {
		if(error) {
			discType = 7;
		} else {
			discType = 0;
		}
	}
	// remove from the lookup dictionary
	removeInquiry(record->listener);
	JAVA_ENV_CHECK(CallVoidMethod(env, record->listener, callback, discType));

	printMessage("inquiryComplete: cleaning up", DEBUG_INFO_LEVEL);
	
	/* clean up allocations */
	JAVA_ENV_CHECK(DeleteGlobalRef(env, record->listener));
	free(record);
	IOBluetoothDeviceInquiryDelete(inquiryRef);
	printMessage("inquiryComplete: complete", DEBUG_INFO_LEVEL);

}

void	cancelInquiry(void *v_cancel){
	todoListRoot				*aRoot;
	threadPassType				*aTypePtr;
	JNIEnv						*env;
	jint						jErr;
	IOBluetoothDeviceInquiryRef	aRef;
	
	printMessage("cancelInquiry: called", DEBUG_INFO_LEVEL);
	
	jErr = (*s_vm)->GetEnv(s_vm, (void**) &env, JNI_VERSION_1_2);
	aRoot = (todoListRoot*)v_cancel;
	aTypePtr = getNextToDoItem(aRoot);
	while(aTypePtr) {
		cancelInquiryRec			*aRec = aTypePtr->dataReq.cancelInquiryPtr;
		
		aRec->success = 0;
		aRef = getPendingInquiryRef(aRec->listener);
	
		if(aRef) {
			IOReturn					err;
		
			// TODO see if this calles the stop callback
			err =	IOBluetoothDeviceInquiryStop(aRef);
			if(!err ) {
				// active inquiry was stopped without error
				aRec->success = 1;
			} 
		} else {
			printMessage("cancelInquiry: called with a nonexistant inquiry!", DEBUG_WARN_LEVEL);
		}
		// let the java caller proceed
		if(aTypePtr->validCondition)
			pthread_cond_signal(& (aTypePtr->callComplete));
		
		aTypePtr = getNextToDoItem(aRoot);
	}
	
	// TODO check if the OS calls the inquiry complete function
	printMessage("cancelInquiry: exiting", DEBUG_INFO_LEVEL);
  }


void  asyncSearchServices(void* voidPtr) {
	/* now in the run loop thread */
	
	todoListRoot				*aRoot;
	threadPassType				*aTypePtr;
	
	JNIEnv						*env;
	jint						jErr;
		
	printMessage("asyncSearchServices: called", DEBUG_INFO_LEVEL);
	jErr = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);
	aRoot = (todoListRoot*)voidPtr;
	aTypePtr = getNextToDoItem(aRoot);
	while(aTypePtr) {
		searchServicesRec			*record = aTypePtr->dataReq.searchSrvPtr;
		IOReturn					err;
		BluetoothDeviceAddress		btAddress;
		CFStringRef					localString;
		const jchar					*chars;
		IOBluetoothDeviceRef		aRef;
		
		chars = JAVA_ENV_CHECK(GetStringChars(env, record->deviceAddress, NULL));
		localString = CFStringCreateWithCharacters (kCFAllocatorDefault, chars, (*env)->GetStringLength(env, record->deviceAddress));
		JAVA_ENV_CHECK(ReleaseStringChars(env, record->deviceAddress, chars));

		err = IOBluetoothCFStringToDeviceAddress( localString, &btAddress );
		aRef = IOBluetoothDeviceCreateWithAddress( &btAddress);
		err = IOBluetoothDevicePerformSDPQuery(aRef, bluetoothSDPQueryCallback, record);
		/* clean up memory alloc*/
		IOBluetoothObjectRelease(aRef);
		free(aTypePtr);
		aTypePtr = getNextToDoItem(aRoot);
	}
	
	printMessage("asyncSearchServices: exiting", DEBUG_INFO_LEVEL);
}
void getServiceAttributes(void *voidPtr) {
	
	todoListRoot					*aRoot;
	threadPassType					*aTypePtr;
	jint							j, numAttr;
	jint							*attributes;
	JNIEnv							*env;
	jint							jErr;
	jclass							serviceClass;
	jfieldID						serviceHandleID;
	jint							serviceHandle;
	IOBluetoothSDPServiceRecordRef	serviceRef;
	jmethodID						setAttribute;
	jobject							hashTable;
	jclass							jHashtableClass;
	jfieldID						servRecordHashField;
	jmethodID						intConstructor;
	jclass							intClass;
	
	
	jErr = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);

	aRoot = (todoListRoot*)voidPtr;
	aTypePtr = getNextToDoItem(aRoot);
	
	while(aTypePtr){
		populateAttributesRec			*params = aTypePtr->dataReq.populateAttrPtr;
		
		serviceClass = JAVA_ENV_CHECK(GetObjectClass(env, params->serviceRecord));
		serviceHandleID = JAVA_ENV_CHECK(GetFieldID(env, serviceClass, "handle", "I"));
		serviceHandle = JAVA_ENV_CHECK(GetIntField(env, params->serviceRecord, serviceHandleID));
		/* YUCK! */
		serviceRef = (IOBluetoothSDPServiceRecordRef)serviceHandle;
	
	
		numAttr = JAVA_ENV_CHECK(GetArrayLength(env, params->attrSet));
		attributes = JAVA_ENV_CHECK(GetIntArrayElements(env, params->attrSet, NULL));
		jHashtableClass = JAVA_ENV_CHECK(FindClass(env, "java/util/Hashtable"));
		servRecordHashField = JAVA_ENV_CHECK(GetFieldID(env, serviceClass, "attributes", "Ljava/util/Hashtable;"));
		hashTable = JAVA_ENV_CHECK(GetObjectField(env, params->serviceRecord, servRecordHashField));
		setAttribute = JAVA_ENV_CHECK(GetMethodID(env, jHashtableClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"));
		intClass = JAVA_ENV_CHECK(FindClass(env, "java/lang/Integer"));
		intConstructor = JAVA_ENV_CHECK(GetMethodID(env, intClass, "<init>", "(I)V"));
	
		for(j=0; j<numAttr;j++) {
			IOBluetoothSDPDataElementRef	dataElement;
			jobject							jDataElement;
			jobject							jResult;
			jobject							attributeID;
		
			attributeID = JAVA_ENV_CHECK(NewObject(env, intClass, intConstructor, attributes[j]));
			dataElement = IOBluetoothSDPServiceRecordGetAttributeDataElement(serviceRef, attributes[j]);
			jDataElement = getjDataElement(env, dataElement);
			jResult = JAVA_ENV_CHECK(CallObjectMethod(env, hashTable, setAttribute, attributeID, jDataElement));
		}
		aTypePtr = getNextToDoItem(aRoot);
	}
}

  
void bluetoothSDPQueryCallback( void * v_serviceRec, IOBluetoothDeviceRef deviceRef, IOReturn status ){
	searchServicesRec	*record = (searchServicesRec*)v_serviceRec;
	JNIEnv				*env;
	jint				jErr, respCode;
	jsize				i, len;
	jclass				listenerClass, aClass;
	jobject				*serviceArray = NULL;
	
	
	jErr = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);
	listenerClass = JAVA_ENV_CHECK(GetObjectClass(env, record->listener));
	if(status != 0) {
		respCode = 3;
	} else {
		jobject				aUUID;
		jsize				foundServices;
		
		aClass = JAVA_ENV_CHECK(FindClass(env, "javax/bluetooth/UUID"));
		len = JAVA_ENV_CHECK(GetArrayLength(env, record->uuidSet));
		serviceArray = (jobject*)malloc(sizeof(jobject) * len);
		foundServices=0;
		for(i=0;i<len;i++) {
			jbyteArray						uuidValue;
			jbyte							*UUIDBytes;
			IOBluetoothSDPUUIDRef			uuidRef;
			IOBluetoothSDPServiceRecordRef	osServiceRecord;
			UInt8							arrayLen;
			jfieldID						uuidField;
			
		// get a UUID
			aUUID = JAVA_ENV_CHECK(GetObjectArrayElement(env, record->uuidSet, i));
			uuidField = JAVA_ENV_CHECK(GetFieldID(env, aClass, "uuidValue", "[B"));
			uuidValue = (jbyteArray)JAVA_ENV_CHECK(GetObjectField(env, aUUID, uuidField));
			arrayLen = JAVA_ENV_CHECK(GetArrayLength(env, uuidValue));
			UUIDBytes = JAVA_ENV_CHECK(GetByteArrayElements(env, uuidValue, NULL));
			uuidRef =  IOBluetoothSDPUUIDCreateWithBytes(UUIDBytes, arrayLen);
			JAVA_ENV_CHECK(ReleaseByteArrayElements(env, uuidValue, UUIDBytes, JNI_ABORT));
			osServiceRecord =  IOBluetoothDeviceGetServiceRecordForUUID(deviceRef, uuidRef);
			if(osServiceRecord) {
				
				/* create the service record ServiceRecordImpl(RemoteDevice device, int handle)*/
				jclass					serviceRecImpl;
				jobject					aServiceRecord;
				jmethodID				constructor;
				populateAttributesRec	someParams, userParams;
				jint					anArray[5] = {0x0000, 0x0001, 0x0002, 0x0003, 0x0004};
				jintArray				javaArray;
				threadPassType			aType, userTypes;
				todoListRoot			tempRoot;
					
				serviceRecImpl = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/ServiceRecordImpl"));
				constructor = JAVA_ENV_CHECK(GetMethodID(env, serviceRecImpl, "<init>", "(Ljavax/bluetooth/RemoteDevice;I)V"));
					
				aServiceRecord = JAVA_ENV_CHECK(NewObject(env, serviceRecImpl, constructor, record->device, (jint)osServiceRecord));
				serviceArray[foundServices] = aServiceRecord;
				foundServices++;
					
				/* call for the defaults */
					
				javaArray =JAVA_ENV_CHECK(NewIntArray(env, 5));
				JAVA_ENV_CHECK(SetIntArrayRegion(env, javaArray, 0, 5, anArray)); 

				initializeToDoList(&tempRoot);
				
				aType.dataReq.populateAttrPtr = &someParams;
				
				someParams.serviceRecord = aServiceRecord;
				someParams.attrSet = javaArray;
				aType.validCondition = FALSE;
				addToDoItem(&tempRoot, &aType);
				
				/* get the extra attributes if any set by the user */
				if(record->attrSet) {
					userParams.attrSet = record->attrSet;
					userParams.serviceRecord = aServiceRecord;
					userTypes.dataReq.populateAttrPtr = &userParams;
					userTypes.validCondition = FALSE;
					addToDoItem(&tempRoot, &userTypes);
					}
				getServiceAttributes(&tempRoot);

			}
		}
		if(foundServices>0) {
			/* create the java object array of services now */
			jobjectArray	theServiceArray;
			jclass			serviceClass;
			jmethodID		listenerCallback;
			
			serviceClass = JAVA_ENV_CHECK(FindClass(env, "javax/bluetooth/ServiceRecord"));
			theServiceArray = JAVA_ENV_CHECK(NewObjectArray(env, foundServices, serviceClass, NULL));
			for(i=0;i<foundServices;i++ ) {
				JAVA_ENV_CHECK(SetObjectArrayElement(env, theServiceArray, i, serviceArray[i]));
			}
			/* notify the listener */
				/* but first check if we haven't been cancelled */
			if(!record->stopped) {
				listenerClass = JAVA_ENV_CHECK(GetObjectClass(env, record->listener));
				listenerCallback = JAVA_ENV_CHECK(GetMethodID(env, listenerClass, "servicesDiscovered", "(I[Ljavax/bluetooth/ServiceRecord;)V"));
			
				JAVA_ENV_CHECK(CallVoidMethod(env, record->listener, listenerCallback, record->refNum, theServiceArray));
				respCode = 1; /*SERVICE_SEARCH_COMPLETED*/
			} else respCode = 2;
		} else {
			respCode = 4; /*SERVICE_SEARCH_NO_RECORDS*/
		}
		if(serviceArray) free(serviceArray);
		serviceArray = NULL;
	}
				
	{
		/* notify listener of completetion */
		jmethodID			listenerCompletionCallback;
	
		/* first remove this search from the active list */
		removeServiceSearchRec(record->refNum);
		listenerCompletionCallback = JAVA_ENV_CHECK(GetMethodID(env, listenerClass, "serviceSearchCompleted", "(II)V"));
		JAVA_ENV_CHECK(CallVoidMethod(env, record->listener, listenerCompletionCallback, record->refNum, (jint)respCode));
		/* clean up global references */
		if(record->attrSet) JAVA_ENV_CHECK(DeleteGlobalRef(env, record->attrSet));
		if(record->uuidSet) JAVA_ENV_CHECK(DeleteGlobalRef(env, record->uuidSet));
		JAVA_ENV_CHECK(DeleteGlobalRef(env, record->deviceAddress));
		JAVA_ENV_CHECK(DeleteGlobalRef(env, record->listener));
		JAVA_ENV_CHECK(DeleteGlobalRef(env, record->device));
		free(record);
		
	}
	

	

}

void RFCOMMConnect(void* voidPtr) {

	todoListRoot		*pendingConnections;
	threadPassType		*aTypePtr;
	jint				jErr;
	JNIEnv				*env;


	printMessage("Entering RFCOMMConnect", DEBUG_INFO_LEVEL);

	jErr = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);

	pendingConnections = (todoListRoot*)voidPtr;
	aTypePtr = getNextToDoItem(pendingConnections);
	while(aTypePtr ) {
		IOReturn			err;
		connectRec			*currentRequest;
		macSocket			*aSocket;
		
								
		currentRequest = aTypePtr->dataReq.connectPtr;
		aSocket = getMacSocket(currentRequest->socket);
		if(aSocket != NULL) {
			BluetoothDeviceAddress	anAddress;
			IOBluetoothDeviceRef	devRef;
			jfieldID				inBufferFld, pipeField, mtuField;
			jobject					inStream;
			jobject					pipedStream;
			jclass					connCls, inStreamCls, pipedStreamCls;
			jint					mtu;
			
			connCls = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/BluetoothRFCOMMConnection"));
			inStreamCls = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/BluetoothInputStream"));
			
			inBufferFld = JAVA_ENV_CHECK(GetFieldID(env, connCls, "in", "Lcom/intel/bluetooth/BluetoothInputStream;"));
			inStream = JAVA_ENV_CHECK(GetObjectField(env, currentRequest->peer, inBufferFld));
			pipedStreamCls = JAVA_ENV_CHECK(FindClass(env, "java/io/PipedOutputStream"));
			pipeField = JAVA_ENV_CHECK(GetFieldID(env, inStreamCls, "pOutput", "Ljava/io/PipedOutputStream;"));
			pipedStream = JAVA_ENV_CHECK(GetObjectField(env, inStream, pipeField));
			
			aSocket->listenerPeer = JAVA_ENV_CHECK(NewGlobalRef(env, pipedStream));
			longToAddress(currentRequest->address, &anAddress);

			devRef = IOBluetoothDeviceCreateWithAddress(&anAddress);

			err = IOBluetoothDeviceOpenRFCOMMChannelSync(devRef, &(aSocket->ref.rfcommRef), currentRequest->channel,
							rfcommEventListener, aSocket);
			mtu = IOBluetoothRFCOMMChannelGetMTU( aSocket->ref.rfcommRef );
			mtuField = JAVA_ENV_CHECK(GetFieldID(env, connCls, "transmitMTU", "I"));
			JAVA_ENV_CHECK(SetIntField(env, currentRequest->peer, mtuField, mtu));
			mtuField = JAVA_ENV_CHECK(GetFieldID(env, connCls, "receiveMTU", "I"));
			JAVA_ENV_CHECK(SetIntField(env, currentRequest->peer, mtuField, mtu));

			if(err != kIOReturnSuccess) {
				jclass				ioExpCls;
				jmethodID			ioExpConst;
				jstring				message;
				
				sprintf(s_errorBuffer, "OS X IO Error: 0x%08X", err);
				message = JAVA_ENV_CHECK(NewStringUTF(env, s_errorBuffer));
				ioExpCls = JAVA_ENV_CHECK(FindClass(env, "java/io/IOException"));
				ioExpConst = JAVA_ENV_CHECK(GetMethodID(env, ioExpCls, "<init>", "(Ljava/lang/String;)V"));
				currentRequest->errorException = JAVA_ENV_CHECK(NewObject(env, ioExpCls, ioExpConst, message));
			}
				
			IOBluetoothObjectRelease(devRef);
			if(aTypePtr->validCondition)
				pthread_cond_signal(& (aTypePtr->callComplete));
		}
		
		aTypePtr = getNextToDoItem(pendingConnections);
	}

	printMessage("Exiting RFCOMMConnect", DEBUG_INFO_LEVEL);
}


void rfcommEventListener (IOBluetoothRFCOMMChannelRef rfcommChannel, void *refCon, IOBluetoothRFCOMMChannelEvent *event)
    {
		jint				length;
		printMessage("rfcommEventListener Entered", DEBUG_INFO_LEVEL);
        switch (event->eventType)
        {
                case kIOBluetoothRFCOMMNewDataEvent:
					length = event->u.newData.dataSize;
					if( length>0) {
						jint				jErr;
						JNIEnv				*env;
						jbyteArray			theBytes;
						jmethodID			writeMethod;
						jclass				peerCls;
						macSocket			*aSocket;
						
						aSocket = (macSocket*)refCon;
						jErr = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);
						peerCls = JAVA_ENV_CHECK(GetObjectClass(env, aSocket->listenerPeer));
						writeMethod = JAVA_ENV_CHECK(GetMethodID(env, peerCls, "write", "([BII)V"));
						theBytes = JAVA_ENV_CHECK(NewByteArray(env, length));
						JAVA_ENV_CHECK(SetByteArrayRegion(env, theBytes, 0, length, event->u.newData.dataPtr));
						(*env)->CallVoidMethod(env, aSocket->listenerPeer, writeMethod, theBytes, 0, length);
						if((*env)->ExceptionOccurred(env)) {
							printMessage("Trouble filling the Bluetooth Input Stream. The stream probably was in the middle of opening.",
										DEBUG_WARN_LEVEL);
							(*env)->ExceptionDescribe(env);
							(*env)->ExceptionClear(env);
						}
					}
					printMessage("RFCOMM Channel Data Received", DEBUG_INFO_LEVEL);

					
                break;
                
                case kIOBluetoothRFCOMMFlowControlChangedEvent:
				/*
                    // event->u.flowStatus       is the status of flow control (see IOBluetoothRFCOMMFlowControlStatus for current restrictions)
          */
					printMessage("RFCOMM Channel Flow Control Changed", DEBUG_INFO_LEVEL);
				      break;
                
                case kIOBluetoothRFCOMMChannelTerminatedEvent:
				/*
                     event->u.terminatedChannel is the channel that was terminated.
					*/
					printMessage("RFCOMM Channel Terminated", DEBUG_INFO_LEVEL);
                break;
				case kIOBluetoothRFCOMMChannelEventTypeOpenComplete:
					printMessage("RFCOMM Channel Open Complete", DEBUG_INFO_LEVEL);
				break;
				case kIOBluetoothRFCOMMChannelEventTypeControlSignalsChanged:
					printMessage("RFCOMM Channel Control Signals Changed", DEBUG_INFO_LEVEL);
				break;
				case kIOBluetoothRFCOMMChannelEventTypeWriteComplete:
					printMessage("RFCOMM Channel Write Complete", DEBUG_INFO_LEVEL);
				break;
				case kIOBluetoothRFCOMMChannelEventTypeQueueSpaceAvailable:
					printMessage("RFCOMM Channel Queue Space Available", DEBUG_INFO_LEVEL);
				break;
        }
		printMessage("rfcommEventListener Exiting", DEBUG_INFO_LEVEL);
    }

void getLocalDeviceName(void	*voidPtr) {
	todoListRoot		*pendingNameReqs;
	threadPassType		*aTypePtr;
	jint				jErr;
	JNIEnv				*env;


	printMessage("Entering getLocalDeviceName", DEBUG_INFO_LEVEL);

	jErr = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);

	pendingNameReqs = (todoListRoot*)voidPtr;
	aTypePtr = getNextToDoItem(pendingNameReqs);
	while(aTypePtr ) {
		IOReturn			err;
		localNameRec		*currentRequest = aTypePtr->dataReq.localNamePtr;
		BluetoothDeviceName	theUTFName;
		
		err = IOBluetoothLocalDeviceReadName(theUTFName, NULL, NULL, NULL);
		if(err == kIOReturnSuccess) {
			jstring			jName;
			jName = JAVA_ENV_CHECK(NewStringUTF(env, (char*)theUTFName));
			currentRequest->aName = JAVA_ENV_CHECK(NewGlobalRef(env, jName));
			JAVA_ENV_CHECK(DeleteLocalRef(env, jName));
		} /* else the name remains null */
		
		if(aTypePtr->validCondition)
			pthread_cond_signal(& (aTypePtr->callComplete));
		
		aTypePtr = getNextToDoItem(pendingNameReqs);
	}
	printMessage("Exiting getLocalDeviceName", DEBUG_INFO_LEVEL);
	
}
void getLocalDeviceClass(void *voidPtr) {
	todoListRoot		*pendingDevClsReqs;
	threadPassType		*aTypePtr;
	jint				jErr;
	JNIEnv				*env;


	printMessage("Entering getLocalDeviceClass", DEBUG_INFO_LEVEL);

	jErr = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);

	pendingDevClsReqs = (todoListRoot*)voidPtr;
	aTypePtr = getNextToDoItem(pendingDevClsReqs);
	while(aTypePtr) {
		IOReturn			err;
		localDeviceClassRec	*currentRequest = aTypePtr->dataReq.localDevClassPtr;
		BluetoothClassOfDevice	theDevClass;
		
		err = IOBluetoothLocalDeviceReadClassOfDevice(&theDevClass, NULL, NULL, NULL);
		if(err == kIOReturnSuccess) {
			jclass				jDevClass;
			jmethodID			jDevClassConstructor;
			jobject				jLocalDevClass;
			
			jDevClass = JAVA_ENV_CHECK(FindClass(env, "javax/bluetooth/DeviceClass"));
			jDevClassConstructor = JAVA_ENV_CHECK(GetMethodID(env, jDevClass, "<init>", "(I)V"));
			jLocalDevClass = JAVA_ENV_CHECK(NewObject(env, jDevClass, jDevClassConstructor, theDevClass));
		
			currentRequest->devClass = JAVA_ENV_CHECK(NewGlobalRef(env, jLocalDevClass));
			JAVA_ENV_CHECK(DeleteLocalRef(env, jLocalDevClass));
		} /* else the object remains null */

		if(aTypePtr->validCondition)
			pthread_cond_signal(& (aTypePtr->callComplete));
		aTypePtr = getNextToDoItem(pendingDevClsReqs);
	}
	printMessage("Exiting getLocalDeviceClass", DEBUG_INFO_LEVEL);

}

IOReturn getCurrentModeInternal(int		*mode) {
		BluetoothClassOfDevice	theDevClass;
		IOReturn				err;
		Boolean					discoveryOn;
		
		err = IOBluetoothLocalDeviceGetDiscoverable(&discoveryOn);
		if(err != kIOReturnSuccess) return err;
		err = IOBluetoothLocalDeviceReadClassOfDevice(&theDevClass, NULL, NULL, NULL);
		if(err != kIOReturnSuccess) return err;
		if(discoveryOn) {
			if(theDevClass & 0x00002000) {
				/* we're in limited discovery */
				*mode = 0x9E8B00;
			} else {
				/* we're in general discovery */
				*mode = 0x9E8B33;
			}
		} else {
			*mode = 0;
		}
		return err;
}

void getLocalDiscoveryMode(void *voidPtr){
	todoListRoot		*aRoot;
	threadPassType		*aTypePtr;
	jint				jErr;
	JNIEnv				*env;


	printMessage("Entering getLocalDiscoveryMode", DEBUG_INFO_LEVEL);

	jErr = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);

	aRoot = (todoListRoot*)voidPtr;
	aTypePtr = getNextToDoItem(aRoot);
	while(aTypePtr) {
		IOReturn				err;
		getDiscoveryModeRec		*currentRequest = aTypePtr->dataReq.getDiscoveryModePtr;
		int						mode=0;
		
		err = getCurrentModeInternal(&mode);
		currentRequest->mode = mode;

		if(aTypePtr->validCondition)
			pthread_cond_signal(& (aTypePtr->callComplete));
		aTypePtr = getNextToDoItem(aRoot);
	}
	printMessage("Exiting getLocalDiscoveryMode", DEBUG_INFO_LEVEL);
}


void setLocalDiscoveryMode(void *voidPtr){
	todoListRoot		*aRoot;
	threadPassType		*aTypePtr;
	jint				jErr;
	JNIEnv				*env;


	printMessage("Entering getLocalDiscoveryMode", DEBUG_INFO_LEVEL);

	jErr = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);

	aRoot = (todoListRoot*)voidPtr;
	aTypePtr = getNextToDoItem(aRoot);
	while(aTypePtr) {
		IOReturn				err;
		setDiscoveryModeRec		*currentRequest = aTypePtr->dataReq.setDiscoveryModePtr;
		int						mode=0;
		
		err = getCurrentModeInternal(&mode);
		/* I don't see a way to change the discovery mode 
			so we return true if the mode to set was the same as the existing
			false if it was different
			probably should pursue IOKit interfaces underneath the bluetooth api
			  */
		
		currentRequest->mode = mode;

		if(aTypePtr->validCondition)
			pthread_cond_signal(& (aTypePtr->callComplete));
		
		aTypePtr = getNextToDoItem(aRoot);
	}
	printMessage("Exiting getLocalDiscoveryMode", DEBUG_INFO_LEVEL);


}
void rfcommSendData(void *voidPtr) {

	todoListRoot		*aRoot;
	threadPassType		*aTypePtr;
	jint				jErr;
	JNIEnv				*env;


	printMessage("Entering rfcommSendData", DEBUG_INFO_LEVEL);

	jErr = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);

	aRoot = (todoListRoot*)voidPtr;
	aTypePtr = getNextToDoItem(aRoot);
	while(aTypePtr) {
		IOReturn				err;
		sendRFCOMMDataRec		*currentRequest = aTypePtr->dataReq.sendRFCOMMDataPtr;
		macSocket				*aSocket;
		UInt32					sent;

		aSocket = getMacSocket(currentRequest->socket);
		err = IOBluetoothRFCOMMChannelWriteSimple(aSocket->ref.rfcommRef, currentRequest->bytes, currentRequest->buflength,
				TRUE, &sent);
		sprintf(s_errorBuffer, "Wrote %ld to socket %d", sent, currentRequest->socket);
		printMessage(s_errorBuffer, DEBUG_INFO_LEVEL);
		
		/* clean up the memory */
		free( currentRequest->bytes);
		free(currentRequest);
		aTypePtr = getNextToDoItem(aRoot);
	}
	printMessage("Exiting rfcommSendData", DEBUG_INFO_LEVEL);
}
void getRemoteDeviceFriendlyName(void *voidPtr) {
		
	todoListRoot		*aRoot;
	threadPassType		*aTypePtr;
	jint				jErr;
	JNIEnv				*env;


	printMessage("Entering getRemoteDeviceFriendlyName", DEBUG_INFO_LEVEL);

	jErr = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);

	aRoot = (todoListRoot*)voidPtr;
	aTypePtr = getNextToDoItem(aRoot);
	while(aTypePtr ) {
		IOReturn					err;
		getRemoteNameRec			*currentRequest = aTypePtr->dataReq.getRemoteNamePtr;
		BluetoothDeviceAddress		btAddress;
		IOBluetoothDeviceRef		aDevRef;
		CFStringRef					localString;
		const jchar					*chars;
		int							length;
		BluetoothDeviceName			aName;
		
		chars = JAVA_ENV_CHECK(GetStringChars(env, currentRequest->address, NULL));
		length = JAVA_ENV_CHECK(GetStringLength(env, currentRequest->address));
		localString = CFStringCreateWithCharacters (kCFAllocatorDefault, chars, length);
		JAVA_ENV_CHECK(ReleaseStringChars(env, currentRequest->address, chars));
		CFRelease(localString);
		
		err = IOBluetoothCFStringToDeviceAddress( localString, &btAddress );
		aDevRef = IOBluetoothDeviceCreateWithAddress( &btAddress);
		localString = IOBluetoothDeviceGetName(aDevRef);
		if(localString != NULL) {
			CFRange			range;
			UniChar			*charBuf;
				
			range.location = 0;
			range.length = CFStringGetLength(localString);
				
			charBuf = malloc(sizeof(UniChar) * range.length);
			CFStringGetCharacters(localString, range, charBuf);
				
			currentRequest->result = JAVA_ENV_CHECK(NewString(env, (jchar *)charBuf, (jsize)range.length));
			free(charBuf);
		} 

		if(currentRequest->alwaysAsk || (localString==NULL)){
			// we contact the remote device
			err = IOBluetoothDeviceRemoteNameRequest(aDevRef, NULL, NULL, aName);
			if(err) {
				/* TODO add some details */
				jclass				ioExp;
				jmethodID			defaultCnstr;
				
				ioExp = JAVA_ENV_CHECK(FindClass(env, "java/io/IOException"));
				defaultCnstr = JAVA_ENV_CHECK(GetMethodID(env, ioExp, "<init>", "()V"));
				currentRequest->errorException = JAVA_ENV_CHECK(NewObject(env, ioExp, defaultCnstr));
			} else {
				currentRequest->result = JAVA_ENV_CHECK(NewStringUTF(env, (char*)aName));
			}
		} 
	
		if(aTypePtr->validCondition)
			pthread_cond_signal(& (aTypePtr->callComplete));
		
		aTypePtr = getNextToDoItem(aRoot);
	}
	printMessage("Exiting getRemoteDeviceFriendlyName", DEBUG_INFO_LEVEL);
}

void getPreknownDevices(void *voidPtr) {		
	todoListRoot		*aRoot;
	threadPassType		*aTypePtr;
	jint				jErr;
	JNIEnv				*env;
	jmethodID			cnstr;
	jclass				remDevCls;
	
	printMessage("Entering getPreknownDevices", DEBUG_INFO_LEVEL);

	jErr = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);
	remDevCls = JAVA_ENV_CHECK(FindClass(env, "com/intel/bluetooth/RemoteDeviceImpl"));
	cnstr = JAVA_ENV_CHECK(GetMethodID(env, remDevCls, "<init>", "(Ljava/lang/String;)V"));
	
	aRoot = (todoListRoot*)voidPtr;
	aTypePtr = getNextToDoItem(aRoot);
	while(aTypePtr ) {
		getPreknownDevicesRec		*currentRequest = aTypePtr->dataReq.getPreknownDevicesPtr;
		CFArrayRef					deviceList;
		CFIndex						i, count;
		jstring						anAddress;
		BluetoothDeviceAddress		*btAddress;
		IOBluetoothDeviceRef		aDevRef;
		char						utfAddress[13];
		jobject						jRemDev;

		if(currentRequest->option == 0) {
			/* looking for cached devices */
			deviceList = IOBluetoothRecentDevices(0);
		} else {
			deviceList = IOBluetoothFavoriteDevices();
		}
		count = CFArrayGetCount(deviceList);
		if(count) {
			currentRequest->result = JAVA_ENV_CHECK(NewObjectArray(env, count, remDevCls, NULL));
			
			for(i=0;i<count;i++) {
				aDevRef = (IOBluetoothDeviceRef)CFArrayGetValueAtIndex(deviceList, i);
				btAddress = (BluetoothDeviceAddress*)IOBluetoothDeviceGetAddress(aDevRef);
				sprintf(utfAddress, "%02x%02x%02x%02x%02x%02x", btAddress->data[0], btAddress->data[1],
					btAddress->data[2], btAddress->data[3], btAddress->data[4], btAddress->data[5]);
				anAddress = JAVA_ENV_CHECK(NewStringUTF(env, utfAddress));
				jRemDev = JAVA_ENV_CHECK(NewObject(env, remDevCls, cnstr, anAddress));
				JAVA_ENV_CHECK(SetObjectArrayElement(env,  currentRequest->result, i, jRemDev));
			}
		}
		
		
		CFRelease(deviceList);
	
		if(aTypePtr->validCondition)
			pthread_cond_signal(& (aTypePtr->callComplete));
		
		aTypePtr = getNextToDoItem(aRoot);
	}
	printMessage("Exiting getPreknownDevices", DEBUG_INFO_LEVEL);
}
