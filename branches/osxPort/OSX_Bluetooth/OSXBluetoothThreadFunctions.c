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

/* library globals */
currInq					*s_inquiryList;
currServiceInq			*s_serviceInqList;
JavaVM					*s_vm;		
CFRunLoopRef			s_runLoop;
CFRunLoopSourceRef		s_inquiryStartSource, s_inquiryStopSource;
CFRunLoopSourceRef		s_searchServicesStart, s_populateServiceAttrs;
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
	/* TODO add mutexes for these to be safe */
	doInquiryRec			inquiryRec;
	cancelInquiryRec		cancelRec;
	searchServicesRec		searchSrvStartRec;
	populateAttributesRec	_populateAttributesRec;
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
		aContext.info = &inquiryRec;

		aContext.perform = performInquiry;
		s_inquiryStartSource = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &aContext);
		CFRunLoopAddSource(s_runLoop, s_inquiryStartSource, kCFRunLoopDefaultMode);
		printMessage("Registed inquiry Start Source", DEBUG_INFO_LEVEL);
		
		/* create/install the inquiry stop source */
		aContext.info = &cancelRec;
		aContext.perform = cancelInquiry;
		s_inquiryStopSource = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &aContext);
		CFRunLoopAddSource(s_runLoop, s_inquiryStopSource, kCFRunLoopDefaultMode);
		printMessage("Registered inquiry Stop Source", DEBUG_INFO_LEVEL);
		
		/* create/istall the search services start source */
		aContext.info = &searchSrvStartRec;
		aContext.perform = asyncSearchServices;
		s_searchServicesStart = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &aContext);
		CFRunLoopAddSource(s_runLoop, s_searchServicesStart, kCFRunLoopDefaultMode);
		printMessage("Registered Service Search Start Source", DEBUG_INFO_LEVEL);
		
		
		/* create/istall the service attribute populater here  */
		aContext.info = &_populateAttributesRec;
		aContext.perform = getServiceAttributes;
		s_populateServiceAttrs = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &aContext);
		CFRunLoopAddSource(s_runLoop, s_populateServiceAttrs, kCFRunLoopDefaultMode);
		printMessage("Registered Populate Service Attributes Start Source", DEBUG_INFO_LEVEL);
	}

	{
		jclass			systemClass;
		jmethodID		aMethod;
		jobject			exp;

		systemClass = (*env)->FindClass(env, "java/lang/System");
		aMethod = (*env)->GetStaticMethodID(env, systemClass, "getProperties", "()Ljava/util/Properties;");
		s_systemProperties = (*env)->CallStaticObjectMethod(env, systemClass, aMethod);
		s_systemProperties = (*env)->NewGlobalRef(env, s_systemProperties);
		
		
		propGenErr = generateProperties(env);
		
			
		/* Try to set the collected properties into the system properties */
		printMessage("Adding Bluetooth Properties to Java System Properties", DEBUG_INFO_LEVEL);
		aMethod = (*env)->GetStaticMethodID(env, systemClass, "setProperties",
									"(Ljava/util/Properties;)V");
		(*env)->CallStaticVoidMethod(env, systemClass, aMethod, s_systemProperties);
		
		exp = (*env)->ExceptionOccurred(env);
		
		if(exp) {
			printMessage("Couldn't set system properties, security manager blocked it.", DEBUG_WARN_LEVEL);
			(*env)->ExceptionClear(env);
		} else {
			printMessage("Successfully updated system properties.", DEBUG_INFO_LEVEL);
		}

		
	}
	{
		/* TODO make sure that this works even if the BluetoothPeer object hasn't been instatiated */
		jclass			peer, serviceImpl;
		jfieldID		nativeIsAsyncField, nativeLibParsesSDP;
		
		printMessage("Attempting to set static class variables specific to native lib functionality", DEBUG_INFO_LEVEL);

		
		serviceImpl = (*env)->FindClass(env, "com/intel/bluetooth/ServiceRecordImpl");
		nativeLibParsesSDP = (*env)->GetStaticFieldID(env, serviceImpl, "nativeLibParsesSDP", "Z");
		(*env)->SetStaticBooleanField(env, serviceImpl, nativeLibParsesSDP, 1);
				
		peer = (*env)->FindClass(env, "com/intel/bluetooth/BluetoothPeer");
		nativeIsAsyncField = (*env)->GetStaticFieldID(env, peer, "nativeIsAsync", "Z");
		(*env)->SetStaticBooleanField(env, peer, nativeIsAsyncField, 1);
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



void performInquiry(void *info) {

		/* now in the run loop thread */
		doInquiryRec				*record = (doInquiryRec*)info;
		JNIEnv						*env;
  
		IOReturn						error;
		currInq							*inquiryItem, *iter;
		jint							jError;
		
		printMessage("performInquiry: called", DEBUG_INFO_LEVEL);
		jError = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);
		if(jError != JNI_OK) {
			printMessage("performInquiry: unable to get java environment!", DEBUG_ERROR_LEVEL);
		}
		
		/* Alloc an inquiry item for the list for the current inquiry */
		inquiryItem = malloc(sizeof(currInq));
		if(inquiryItem == 0L) {
			/* can't do much, even constructing an excpetion is too much at this point */
			printMessage("performInquiry: Ran out of memory!", DEBUG_ERROR_LEVEL);
			goto performInquiry_cleanup;
		}
		
		
		/* create the list item */
		inquiryItem->aListener = (*env)->NewGlobalRef(env, record->listener);
		inquiryItem->refCount = 1;
		inquiryItem->next = 0L;
		inquiryItem->inquiryStarted = 0;
		
		/* store the item in the list */
		if(!s_inquiryList) {
			s_inquiryList = inquiryItem;
		} else {
			iter = s_inquiryList;
			while(iter->next != 0L) iter = iter->next;
			iter->next = inquiryItem;
		}
		
		/* create the inquiry */
		inquiryItem->anInquiry = IOBluetoothDeviceInquiryCreateWithCallbackRefCon(inquiryItem);

			
		/* listen for found devices */
		error = IOBluetoothDeviceInquirySetDeviceFoundCallback(inquiryItem->anInquiry, inquiryDeviceFound);
			if(error != kIOReturnSuccess) {
				printMessage("performInquiry: IOBluetoothDeviceInquirySetDeviceFoundCallback Failed", DEBUG_ERROR_LEVEL);
				sprintf(s_errorBuffer, "%s%i", s_errorBase, error);
				printMessage(s_errorBuffer, DEBUG_ERROR_LEVEL);
				inquiryComplete(inquiryItem, inquiryItem->anInquiry, error, TRUE);
				goto performInquiry_cleanup; /* inquiry error */
			}
		error = IOBluetoothDeviceInquirySetStartedCallback(inquiryItem->anInquiry,inquiryStarted );
			if(error != kIOReturnSuccess) {
				printMessage("performInquiry: IOBluetoothDeviceInquirySetStartedCallback Failed", DEBUG_ERROR_LEVEL);
				sprintf(s_errorBuffer, "%s%i", s_errorBase, error);
				printMessage(s_errorBuffer, DEBUG_ERROR_LEVEL);
				inquiryComplete(inquiryItem, inquiryItem->anInquiry, error, TRUE);
				goto performInquiry_cleanup; /* inquiry error */
			}		
		/* set the completeion callback */
		error = IOBluetoothDeviceInquirySetCompleteCallback(inquiryItem->anInquiry, inquiryComplete);
			if(error != kIOReturnSuccess) {
				printMessage("performInquiry: IOBluetoothDeviceInquirySetCompleteCallback Failed", DEBUG_ERROR_LEVEL);
				sprintf(s_errorBuffer, "%s%i", s_errorBase, error);
				printMessage(s_errorBuffer, DEBUG_ERROR_LEVEL);
				inquiryComplete(inquiryItem, inquiryItem->anInquiry, error, TRUE);
				goto performInquiry_cleanup; /* inquiry error */
			}
		if (record->accessCode == kBluetoothLimitedInquiryAccessCodeLAPValue) 	{
			/* only looking for limited discovery items */
				error = IOBluetoothDeviceInquirySetSearchCriteria(inquiryItem->anInquiry,
																kBluetoothServiceClassMajorLimitedDiscoverableMode,
																kBluetoothDeviceClassMajorAny,
																kBluetoothDeviceClassMinorAny	);
				if(error != kIOReturnSuccess) {
					printMessage("performInquiry: IOBluetoothDeviceInquirySetSearchCriteria Failed", DEBUG_ERROR_LEVEL);
					sprintf(s_errorBuffer, "%s%i", s_errorBase, error);
					printMessage(s_errorBuffer, DEBUG_ERROR_LEVEL);
					inquiryComplete(inquiryItem, inquiryItem->anInquiry, error, TRUE);
					goto performInquiry_cleanup; /* inquiry error */
			} else printMessage("performInquiry: IOBluetoothDeviceInquirySetSearchCriteria succeeded", DEBUG_INFO_LEVEL);	
		}
		error = IOBluetoothDeviceInquiryStart(inquiryItem->anInquiry);
			if(error != kIOReturnSuccess) {
				printMessage("performInquiry: IOBluetoothDeviceInquiryStart Failed", DEBUG_ERROR_LEVEL);
				sprintf(s_errorBuffer, "%s%i", s_errorBase, error);
					printMessage(s_errorBuffer, DEBUG_ERROR_LEVEL);
					inquiryComplete(inquiryItem, inquiryItem->anInquiry, error, TRUE);
					goto performInquiry_cleanup; /* inquiry error */
			} else printMessage("performInquiry: IOBluetoothDeviceInquiryStart succeeded", DEBUG_INFO_LEVEL);
	
	
	/* clean up the doInquiry record */
performInquiry_cleanup:
	
	printMessage("performInquiry: Cleanup started", DEBUG_INFO_LEVEL);
	
	(*env)->DeleteGlobalRef(env, record->peer);
	record->peer = NULL;
	
	(*env)->DeleteGlobalRef(env, record->listener);
	record->listener = NULL;

	printMessage("performInquiry: Exiting", DEBUG_INFO_LEVEL);
}


void inquiryDeviceFound(void *v_listener, IOBluetoothDeviceInquiryRef inquiryRef, IOBluetoothDeviceRef deviceRef)
{
	/* need to create a RemoteDevice and DeviceClass objects for the device */
	
	JNIEnv							*env;
	jclass							remoteDevCls, devCls, listenerCls;
	jmethodID						constructor, callback;
	jobject							remoteDev, remoteDeviceClass;
	BluetoothClassOfDevice			devClass;
	CFStringRef						name; /* no release needed! May be null! */
	const BluetoothDeviceAddress	*devAddress;
	UInt64							bigEndianAddress;
	UInt64							nativeEndianAddress;
	jstring							devName;
	CFRange							aRange;
	UniChar							*buffer;
	jint							jErr;
	currInq							*listener = (currInq*)v_listener;
	
	printMessage("inquiryDeviceFound called", DEBUG_INFO_LEVEL);
	if(listener == NULL) printMessage("listener data is NULL!", DEBUG_ERROR_LEVEL);
	if(listener->aListener == NULL) printMessage("listener jobject is NULL!", DEBUG_ERROR_LEVEL);
	
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
	
	bigEndianAddress = devAddress->data[0];
	bigEndianAddress <<= 8;
	bigEndianAddress |= devAddress->data[1];
	bigEndianAddress <<= 8;
	bigEndianAddress |= devAddress->data[2];
	bigEndianAddress <<= 8;
	bigEndianAddress |= devAddress->data[3];
	bigEndianAddress <<= 8;
	bigEndianAddress |= devAddress->data[4];
	bigEndianAddress <<= 8;
	bigEndianAddress |= devAddress->data[5];
	/* TODO check on a big endian machine if the address is still correct */
//	nativeEndianAddress = CFSwapInt64BigToHost(bigEndianAddress);
	
	nativeEndianAddress = bigEndianAddress;
	
	name = IOBluetoothDeviceGetName(deviceRef);
	printMessage("IOBluetoothDeviceGetName returned", DEBUG_INFO_LEVEL);
	

	if(name) {
		aRange.location = 0;
		aRange.length = CFStringGetLength(name);
		buffer = (UniChar*) malloc(sizeof(UniChar) * aRange.length);
		CFStringGetCharacters(name, aRange, buffer);
		devName = (*env)->NewString(env, (jchar*) buffer, (jsize)aRange.length);
		free(buffer);
	} else {
		/* no name in stack so make an empty string */
		devName = (*env)->NewStringUTF(env, "");
	}
	
	printMessage("Device Name constructed", DEBUG_INFO_LEVEL);
	
	remoteDevCls = (*env)->FindClass(env, "javax/bluetooth/RemoteDevice");
	constructor= (*env)->GetMethodID(env, remoteDevCls, "<init>", "(Ljava/lang/String;J)V");
	
	remoteDev = (*env)->NewObject(env, remoteDevCls, constructor, devName, nativeEndianAddress);
	printMessage("Remote Device object constructed", DEBUG_INFO_LEVEL);
	devClass = IOBluetoothDeviceGetClassOfDevice(deviceRef);
	
	devCls = (*env)->FindClass(env, "javax/bluetooth/DeviceClass");
	constructor = (*env)->GetMethodID(env, devCls, "<init>", "(I)V");
	
	remoteDeviceClass = (*env)->NewObject(env, devCls, constructor, devClass);
	printMessage("Device Class Object constructed", DEBUG_INFO_LEVEL);
	listenerCls = (*env)->GetObjectClass(env, listener->aListener);
	printMessage("Listener Class obtained", DEBUG_INFO_LEVEL);
	callback = (*env)->GetMethodID(env, listenerCls, "deviceDiscovered", "(Ljavax/bluetooth/RemoteDevice;Ljavax/bluetooth/DeviceClass;)V");
	printMessage("Calling callback on listener", DEBUG_INFO_LEVEL);
	/* call the java callback */
	(*env)->CallVoidMethod(env, listener->aListener, callback, remoteDev, remoteDeviceClass);
	printMessage("Callback returned", DEBUG_INFO_LEVEL);
}

void inquiryStarted(void * v_listener, IOBluetoothDeviceInquiryRef		inquiryRef	){
							
	currInq			*listener = (currInq*)v_listener;						
	
	printMessage("inquiryStarted: entered", DEBUG_INFO_LEVEL);
	if(listener->inquiryStarted) {
		printMessage("inquiryStarted: Warning, this listener has more than one inquiry running!", DEBUG_WARN_LEVEL);
	}
	
	listener->inquiryStarted ++;
	printMessage("inquiryStarted: exiting", DEBUG_INFO_LEVEL);
	
}

void inquiryComplete(void *						v_listener,
						IOBluetoothDeviceInquiryRef inquiryRef,
						IOReturn					error,
						Boolean						aborted		)
{
	JNIEnv							*env;
	jmethodID						callback;
	jclass							listenerCls;
	jint							discType;
	jint							jErr;
	currInq							*aPtr;
	currInq							*listener = (currInq*)v_listener;
	
	printMessage("inquiryComplete: called", DEBUG_INFO_LEVEL);


	jErr = (*s_vm)->GetEnv(s_vm, (void**) &env, JNI_VERSION_1_2);
	listenerCls = (*env)->GetObjectClass(env, listener->aListener);
	callback = (*env)->GetMethodID(env, listenerCls, "inquiryCompleted", "(I)V");
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

	(*env)->CallVoidMethod(env, listener->aListener, callback, discType);

	printMessage("inquiryComplete: cleaning up", DEBUG_INFO_LEVEL);
	
	/* clean up allocations */
	(*env)->DeleteGlobalRef(env, listener->aListener);
	/* pull listener out of the list */
	if(s_inquiryList == listener) {
		/* most likely */
		s_inquiryList = listener->next;
	} else {
		aPtr = s_inquiryList;
		while (aPtr->next != listener) aPtr = aPtr->next;
		aPtr->next = listener->next;
	}
	free(listener);
	printMessage("inquiryComplete: complete", DEBUG_INFO_LEVEL);

}

void	cancelInquiry(void *v_cancel){
	
	cancelInquiryRec			*aRec = (cancelInquiryRec*)v_cancel;
	currInq						*anInquiryListItem = s_inquiryList;
	JNIEnv						*env;
	jint						jErr;
	
	printMessage("cancelInquiry: called", DEBUG_INFO_LEVEL);
	
	jErr = (*s_vm)->GetEnv(s_vm, (void**) &env, JNI_VERSION_1_2);
	
	
	// extract the record for this listener
	
	while(anInquiryListItem!=NULL && !( (*env)->IsSameObject(env, anInquiryListItem->aListener, aRec->listener)) ) {
		anInquiryListItem = anInquiryListItem->next;
	}
	
	if(anInquiryListItem) {
		IOReturn					err;
	
		err =	IOBluetoothDeviceInquiryStop(anInquiryListItem->anInquiry);
		if(!err && (anInquiryListItem->inquiryStarted)) {
			// active inquiry was stopped
			aRec->success = 1;
		}
	} else {
		printMessage("cancelInquiry: called with a nonexistant inquiry!", DEBUG_WARN_LEVEL);
	}
	// let the java caller proceed
	pthread_cond_signal(&aRec->waiter);
	
	// TODO check if the OS calls the inquiry complete function
	printMessage("cancelInquiry: exiting", DEBUG_INFO_LEVEL);
  }


void  asyncSearchServices(void* in) {
	/* now in the run loop thread */
	searchServicesRec			*record = (searchServicesRec*)in;
	JNIEnv						*env;
	IOReturn					err;
	jint						jErr;
	currServiceInq				*osRecord = record->theInq;
	BluetoothDeviceAddress		btAddress;
	CFStringRef					localString;
	const jchar					*chars;
	
	jErr = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);
	
	chars = (*env)->GetStringChars(env, record->deviceAddress, NULL);
	localString = CFStringCreateWithCharacters (kCFAllocatorDefault, chars, (*env)->GetStringLength(env, record->deviceAddress));
	(*env)->ReleaseStringChars(env, record->deviceAddress, chars);

	err = IOBluetoothCFStringToDeviceAddress( localString, &btAddress );
	osRecord->aDevice = IOBluetoothDeviceCreateWithAddress( &btAddress);
	err = IOBluetoothDevicePerformSDPQuery(osRecord->aDevice, bluetoothSDPQueryCallback, in);
	
}
void getServiceAttributes(void *in) {
	
	jint							j, numAttr;
	jint							*attributes;
	populateAttributesRec			*params = (populateAttributesRec*)in;
	JNIEnv							*env;
	jint							jErr;
	jclass							serviceClass;
	jfieldID						serviceHandleID;
	jint							serviceHandle;
	IOBluetoothSDPServiceRecordRef	serviceRef;
	jmethodID						setAttribute;
	
	jErr = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);

	
	serviceClass = (*env)->GetObjectClass(env, params->serviceRecord);
	serviceHandleID = (*env)->GetFieldID(env, serviceClass, "handle", "I");
	serviceHandle = (*env)->GetIntField(env, params->serviceRecord, serviceHandleID);
	/* YUCK! */
	serviceRef = (IOBluetoothSDPServiceRecordRef)serviceHandle;
	
	
	numAttr = (*env)->GetArrayLength(env, params->attrSet);
	attributes = (*env)->GetIntArrayElements(env, params->attrSet, NULL);
	setAttribute = (*env)->GetMethodID(env, serviceClass, "setAttributeValue", "(ILjavax/bluetooth/DataElement;)Z");
	
	for(j=0; j<numAttr;j++) {
		IOBluetoothSDPDataElementRef	dataElement;
		jobject							jDataElement;
		jboolean							jResult;
		
		dataElement = IOBluetoothSDPServiceRecordGetAttributeDataElement(serviceRef, attributes[j]);
		jDataElement = getjDataElement(env, dataElement);
		jResult = (*env)->CallBooleanMethod(env, params->serviceRecord, setAttribute, attributes[j], jDataElement);
	}
}


  
void bluetoothSDPQueryCallback( void * v_serviceRec, IOBluetoothDeviceRef deviceRef, IOReturn status ){
	searchServicesRec	*record = (searchServicesRec*)v_serviceRec;
	JNIEnv				*env;
	jint				jErr, respCode;
	jsize				i, len;
	jclass				listenerClass, aClass;
	IOReturn			err;
	jobject				*serviceArray = NULL;
	
	
	jErr = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);
	listenerClass = (*env)->GetObjectClass(env, record->listener);
	if(status != 0) {
		respCode = 3;
	} else {
		jobject				aUUID;
		jsize				foundServices;
		
		aClass = (*env)->FindClass(env, "javax/bluetooth/UUID");
		len = (*env)->GetArrayLength(env, record->uuidSet);
		serviceArray = (jobject*)malloc(sizeof(jobject) * len);
		foundServices=0;
		for(i=0;i<len;i++) {
			jbyteArray						uuidValue;
			jbyte							*UUIDBytes;
			IOBluetoothSDPUUIDRef			uuidRef;
			IOBluetoothSDPServiceRecordRef	osServiceRecord;
			UInt8							len;
			jfieldID						uuidField;
			
		// get a UUID
			aUUID = (*env)->GetObjectArrayElement(env, record->uuidSet, i);
			uuidField = (*env)->GetFieldID(env, aClass, "uuidValue", "[B");
			uuidValue = (jbyteArray)(*env)->GetObjectField(env, aUUID, uuidField);
			len = (*env)->GetArrayLength(env, uuidValue);
			UUIDBytes = (*env)->GetByteArrayElements(env, uuidValue, NULL);
			uuidRef =  IOBluetoothSDPUUIDCreateWithBytes(UUIDBytes, len);
			(*env)->ReleaseByteArrayElements(env, uuidValue, UUIDBytes, JNI_ABORT);
			osServiceRecord =  IOBluetoothDeviceGetServiceRecordForUUID(record->theInq->aDevice, uuidRef);
			if(osServiceRecord) {
				BluetoothSDPServiceRecordHandle		aHandle;
				err = IOBluetoothSDPServiceRecordGetServiceRecordHandle(osServiceRecord, &aHandle);
				if(!err) {
					/* create the service record ServiceRecordImpl(RemoteDevice device, int handle)*/
					jclass				serviceRecImpl;
					jobject				aServiceRecord;
					jmethodID			constructor;
					
					serviceRecImpl = (*env)->FindClass(env, "com/intel/bluetooth/ServiceRecordImpl");
					constructor = (*env)->GetMethodID(env, serviceRecImpl, "<init>", "(Ljavax/bluetooth/RemoteDevice;I)V");
					
					aServiceRecord = (*env)->NewObject(env, serviceRecImpl, constructor, record->device, (jint)aHandle);
					serviceArray[foundServices] = aServiceRecord;
					foundServices++;
					
					/* call for the defaults */
					populateAttributesRec				someParams;
					jint			anArray[5] = {0x0000, 0x0001, 0x0002, 0x0003, 0x0004};
					jintArray		javaArray;
					
					javaArray =(*env)->NewIntArray(env, 5);
					(*env)->SetIntArrayRegion(env, javaArray, 0, 5, anArray); 

					someParams.serviceRecord = aServiceRecord;
					someParams.attrSet = javaArray;
					someParams.waiterValid = 0;
					
					getServiceAttributes(&someParams);
					
				} else {
					printMessage("No Service found for provided UUID!", DEBUG_WARN_LEVEL);
				}
			}
		}
		if(foundServices>0) {
			/* create the java object array of services now */
			jobjectArray	theServiceArray;
			jclass			serviceClass;
			jmethodID		listenerCallback;
			
			serviceClass = (*env)->FindClass(env, "javax/bluetooth/ServiceRecord");
			theServiceArray = (*env)->NewObjectArray(env, foundServices, serviceClass, NULL);
			for(i=0;i<foundServices;i++ ) {
				(*env)->SetObjectArrayElement(env, theServiceArray, i, serviceArray[i]);
			}
			/* notify the listener */
			listenerClass = (*env)->GetObjectClass(env, record->listener);
			listenerCallback = (*env)->GetMethodID(env, listenerClass, "servicesDiscovered", "(I[Ljavax/bluetooth/ServiceRecord;)V");
			
			(*env)->CallVoidMethod(env, record->listener, listenerCallback, record->theInq->index, theServiceArray);
			respCode = 1; /*SERVICE_SEARCH_COMPLETED*/
		} else {
			respCode = 4; /*SERVICE_SEARCH_NO_RECORDS*/
		}
		if(serviceArray) free(serviceArray);
	}
				
	{
	/* notify listener of completetion */
	jclass				listenerClass;
	jmethodID			listenerCompletionCallback;
	
	listenerClass = (*env)->GetObjectClass(env, record->listener);
	listenerCompletionCallback = (*env)->GetMethodID(env, listenerClass, "serviceSearchCompleted", "(II)V");
	(*env)->CallVoidMethod(env, record->listener, listenerCompletionCallback, (jint)record->theInq->index, (jint)respCode);

	}
	

	

}
 