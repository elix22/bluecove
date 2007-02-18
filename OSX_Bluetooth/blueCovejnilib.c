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

static 		currInq					*s_inquiryList;
static		JavaVM					*s_vm;		
static		CFRunLoopRef			s_runLoop;
static		CFRunLoopSourceRef		s_inquiryStartSource, s_inquiryStopSource;
static		jobject					s_systemProperties;

void printMessage(const char* msg, int level) {
/* prints out a message when the library debug level is set higher than the called level */
#if DEBUG
	if(level < DEBUG) {
		fprintf(stderr, "BlueCove OS X native lib: %s\n",msg);
		fflush(stderr);
		
	}
#endif
}

#define				NUM_WAITS		1
const static char* s_errorBase = "Mac OS X Bluetooth Error: ";
static char		s_errorBuffer[36];
/**
 * Called by the VM when this library is loaded. We use it to set up the CFRunLoop and sources */
 
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
		pthread_t				aThread;
		pthread_mutex_t			initializeMutex;
		pthread_cond_t			initializeCond;		
		
		s_inquiryList = NULL;
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

	
/**
	An issue with the OS X BT implementation is all the calls need to come from the same thread. Since the java source 
	calls could be coming from many thread sources we consolidate them into a CFRunLoop thread as CFRunLoopSources
	
	 this first function doesn't do much except quiet complaints about memory leaks, unless the underlying implementation
		starts to actually do garbage collection before the function returns. Which is always possible. */
		
static void* cocoaWrapper(void* v_pthreadCond) {
	ThreadCleanups			theWrapper;
	
	theWrapper.function = runLoopThread;
	theWrapper.data = v_pthreadCond;
	
	OSXThreadWrapper(&theWrapper);

	return NULL;
	}

static void* runLoopThread(void* v_threadValues) {
	jint					jErr;
	JNIEnv					*env;
	doInquiryRec			inquiryRec;
	cancelInquiryRec		cancelRec;
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
		sprintf(s_errorBuffer, "infoPtr=%p", aContext.info);
		printMessage(s_errorBuffer, DEBUG_INFO_LEVEL);

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
		printMessage("Adding Bluetooth Properties to Java System Properties", DEBUG_WARN_LEVEL);
		aMethod = (*env)->GetStaticMethodID(env, systemClass, "setProperties",
									"(Ljava/util/Properties;)V");
		(*env)->CallStaticVoidMethod(env, systemClass, aMethod, s_systemProperties);
		
		exp = (*env)->ExceptionOccurred(env);
		
		if(exp) {
			printMessage("Couldn't set system properties, security manager blocked it.", DEBUG_WARN_LEVEL);
		}
		(*env)->ExceptionClear(env);

		
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

/* returns non zero if there was a fatal error and the run loop shouldn't be initiated*/
static int generateProperties(JNIEnv	*env) {

		/* create a property list (s_systemProperties), append it to the System list if possible */
		jstring			key, prop, oldProp;
		jstring			jTrue, jFalse;
		jmethodID		setPropMethod;
		jclass			propertiesClass;
		
	
		jTrue = (*env)->NewStringUTF(env, "true");
		jFalse = (*env)->NewStringUTF(env, "false");
		
		propertiesClass = (*env)->FindClass(env, "java/util/Properties");
		setPropMethod = (*env)->GetMethodID(env, propertiesClass, "setProperty", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;");
		
		key = (*env)->NewStringUTF(env, BLUECOVE_SYSTEM_PROP_NATIVE_LIBRARY_VERSION);
		prop = (*env)->NewStringUTF(env, NATIVE_VERSION);
		oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, prop);

		key = (*env)->NewStringUTF(env, BLUECOVE_SYSTEM_PROP_NATIVE_LIBRARY_DESCRIP);
		prop = (*env)->NewStringUTF(env, NATIVE_DESCRIP);
		oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, prop);
		
		{
			NumVersion					btVersion;
			BluetoothHCIVersionInfo		hciVersion;
			IOReturn					verErr;
			char						swVers[133];
			
			verErr = IOBluetoothGetVersion( &btVersion, &hciVersion );
			sprintf(swVers, "Software version %1d%1d.%1d.%1d rev %d", btVersion.majorRev >> 4,
				btVersion.majorRev & 0x0F, btVersion.minorAndBugRev >> 4, btVersion.minorAndBugRev & 0x0F,
				btVersion.nonRelRev);
			if(!verErr) {
				// also got the local hardware versioninfo
				char			hciVers[100];
				sprintf(hciVers, ", Hardware Manufacturer: %d, LMP Version: %d.%d, HCI Version: %d.%d",
									hciVersion.manufacturerName, hciVersion.lmpVersion, hciVersion.lmpSubVersion,
									hciVersion.hciVersion, hciVersion.hciRevision);
				strcat(swVers, hciVers);
			}

			key = (*env)->NewStringUTF(env, BLUECOVE_SYSTEM_PROP_NATIVE_OSBT_LIBRARY_VERSION);
			prop = (*env)->NewStringUTF(env, swVers);
			oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, prop);
		}
	
		{
			key = (*env)->NewStringUTF(env, BLUECOVE_SYSTEM_PROP_HARDWARE_PRESENT);
			if( IOBluetoothLocalDeviceAvailable() ) {
				oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, jTrue);
			} else {
				oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, jFalse);
				/* no hardware avaliable so bow out this thread  */
				/* don't bother gathering more properties since the hardware isn't available */
				return 1;
			}
		}
		{
			BluetoothDeviceName			aName;
			IOBluetoothLocalDeviceReadName(	aName,
												NULL, NULL, NULL);		
			key = (*env)->NewStringUTF(env, BLUECOVE_SYSTEM_PROP_LOCAL_NAME);
			prop = (*env)->NewStringUTF(env, (char*)aName);
			oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, prop);
			
			/* lock the thread for each call back that needs to complete */
		//	pthread_mutex_lock(recursiveLock);
		}
		{
			BluetoothDeviceAddress			localAddress;
			
			if(! IOBluetoothLocalDeviceReadAddress(	&localAddress, NULL, NULL, NULL)) {
				CFStringRef		aString = IOBluetoothCFStringFromDeviceAddress( &localAddress );
				CFRange			range;
				UniChar			*charBuf;
				
				range.location = 0;
				range.length = CFStringGetLength(aString);
				
				charBuf = malloc(sizeof(UniChar) * range.length);
				CFStringGetCharacters(aString, range, charBuf);
				
				prop = (*env)->NewString(env, (jchar *)charBuf, (jsize)range.length);
				
				
				key = (*env)->NewStringUTF(env, BLUECOVE_SYSTEM_PROP_LOCAL_ADDRESS);
				oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, prop);
				free(charBuf);
			}
		}
		
		key = (*env)->NewStringUTF(env, JSR82_SYSTEM_ENV_API_VERSION);
		prop = (*env)->NewStringUTF(env, "1.0");
		oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, prop);
		
		{
			BluetoothHCISupportedFeatures		features;
			
			IOBluetoothLocalDeviceReadSupportedFeatures(&features, NULL, NULL, NULL);
			
			key = (*env)->NewStringUTF(env, JSR82_SYSTEM_ENV_MASTER_SWITCH);
			prop = ( kBluetoothFeatureSwitchRoles & features.data[7]) ?
					jTrue : jFalse;
					
			oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, prop);
		
			key = (*env)->NewStringUTF(env, JSR82_SYSTEM_ENV_MAX_CON_DEV);
			if(kBluetoothFeatureParkMode & features.data[6]) {
				/* park mode is enabled so lets say 255, this should be fixed */
				prop = (*env)->NewStringUTF(env, "255");
			} else {
				/* park mode not enabled so lets say 7, should be fixed */
				prop = (*env)->NewStringUTF(env, "7");
			}
			oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, prop);
		
		}
		/* The API says put 0 in the request for an infinite number of posibilites,
			I'm calling ~2^16 in this case */
		key = (*env)->NewStringUTF(env, JSR82_SYSTEM_ENV_MAX_SRV_ATTR);
		prop = (*env)->NewStringUTF(env, "65000");
		oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, prop);

	
		{
			char		conv[50];
			
			sprintf(conv, "%d", kBluetoothL2CAPMTUMaximum);
			key = (*env)->NewStringUTF(env, JSR82_SYSTEM_ENV_MAX_RECV_MTU);
			prop = (*env)->NewStringUTF(env, conv);
			oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, prop);
		}
		/* with this implementation this is just limited by the length of the linked list
			which is effectively just a limit on memory */
		key = (*env)->NewStringUTF(env, JSR82_SYSTEM_ENV_MAX_CONCURRENT_SRV_DISC);
		prop = (*env)->NewStringUTF(env, "65000");
		oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, prop);

		key = (*env)->NewStringUTF(env, JSR82_SYSTEM_ENV_INQR_SCAN_DUR_CONN);
		oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, jTrue);

		key = (*env)->NewStringUTF(env, JSR82_SYSTEM_ENV_PAGE_SCAN_DUR_CONN);
		oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, jTrue);

		key = (*env)->NewStringUTF(env, JSR82_SYSTEM_ENV_INQR_DUR_CONN);
		oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, jTrue);
	
		key = (*env)->NewStringUTF(env, JSR82_SYSTEM_ENV_PAGE_DUR_CONN);
		oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, jTrue);

		key = (*env)->NewStringUTF(env, BLUECOVE_SYSTEM_PROP_NATIVE_ASYNC_ENABLED);
		oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, jTrue);
	
		
		
	return 0;
}


void JNI_OnUnload(JavaVM *vm, void *reserved){
	/* should cleanup even be attempted? */
	printMessage("Unloading " NATIVE_DESCRIP "\n", DEBUG_INFO_LEVEL);
}



void throwException(JNIEnv *env, const char *name, const char *msg)
{
	 jclass cls = (*env)->FindClass(env, name);
     /* if cls is NULL, an exception has already been thrown */
     if (cls != NULL) {
         (*env)->ThrowNew(env, cls, msg);
	 } else {
		 (*env)->FatalError(env, "illegal Exception name");	
	 }
     /* free the local ref */
    (*env)->DeleteLocalRef(env, cls);
}


void throwIOException(JNIEnv *env, const char *msg) 
{
	throwException(env, "java/io/IOException", msg);
}

/*
 * Class:     com_intel_bluetooth_BluetoothPeer
 * Method:    doInquiry
 * Signature: (ILjavax/bluetooth/DiscoveryListener;)I
 */
JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_doInquiry
  (JNIEnv *env, jobject peer, jint accessCode, jobject listener){
		
		CFRunLoopSourceContext		aContext={0};
		doInquiryRec				*record;
		printMessage("Java_com_intel_bluetooth_BluetoothPeer_doInquiry called", DEBUG_INFO_LEVEL);
		
		CFRunLoopSourceGetContext(s_inquiryStartSource, &aContext);

		/* set the data for the work function */
		record = (doInquiryRec*) aContext.info;
		record->peer = (*env)->NewGlobalRef(env, peer);
		record->accessCode = accessCode;
		record->listener = (*env)->NewGlobalRef(env, listener);

		CFRunLoopSourceSignal(s_inquiryStartSource);
		CFRunLoopWakeUp (s_runLoop);
		
		return -1;
	
}

static void performInquiry(void *info) {

		/* now in the run loop thread */
		doInquiryRec				*record = (doInquiryRec*)info;
		JNIEnv						*env;
  
		IOReturn						error;
		currInq							*inquiryItem, *iter;
		jint							jError;
		
		printMessage("performInquiry: called", DEBUG_INFO_LEVEL);
		sprintf(s_errorBuffer, "infoPtr=%p", record);
		printMessage(s_errorBuffer, DEBUG_INFO_LEVEL);
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


static void inquiryDeviceFound(void *v_listener, IOBluetoothDeviceInquiryRef inquiryRef, IOBluetoothDeviceRef deviceRef)
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
	bigEndianAddress = devAddress->data[1];
	bigEndianAddress <<= 8;
	bigEndianAddress = devAddress->data[2];
	bigEndianAddress <<= 8;
	bigEndianAddress = devAddress->data[3];
	bigEndianAddress <<= 8;
	bigEndianAddress = devAddress->data[4];
	bigEndianAddress <<= 8;
	bigEndianAddress = devAddress->data[5];
	bigEndianAddress <<= 8;
	
	nativeEndianAddress = CFSwapInt64BigToHost(bigEndianAddress);
	
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
static void inquiryStarted(void * v_listener, IOBluetoothDeviceInquiryRef		inquiryRef	){
							
	currInq			*listener = (currInq*)v_listener;						
	
	printMessage("inquiryStarted: entered", DEBUG_LEVEL_INFO);
	if(listener->inquiryStarted) {
		printMessage("inquiryStarted: Warning, this listener has more than one inquiry running!", DEBUG_WARN_LEVEL);
	}
	
	listener->inquiryStarted ++;
	printMessage("inquiryStarted: exiting", DEBUG_LEVEL_INFO);
	
}
static void inquiryComplete(void *						v_listener,
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

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothPeer_cancelInquiry
  (JNIEnv *env, jobject peer, jobject listener){
	
	cancelInquiryRec			*record;
	pthread_mutex_t				aMutex;
	CFRunLoopSourceContext		aContext = {0};
	
	printMessage("Java_com_intel_bluetooth_BluetoothPeer_cancelInquiry: called", DEBUG_INFO_LEVEL);
	
	pthread_cond_init(&record->waiter, NULL);
	pthread_mutex_init(&aMutex, NULL);
	pthread_mutex_lock(&aMutex);

	CFRunLoopSourceGetContext(s_inquiryStopSource, &aContext);

		/* set the data for the work function */
	record = (cancelInquiryRec*) aContext.info;
	record->peer = peer;
	record->listener = listener; /* no need for a global ref since we're done with this when we return */
	
	CFRunLoopSourceSignal(s_inquiryStopSource);
	CFRunLoopWakeUp (s_runLoop);
	
	// wait until the work is done
	pthread_cond_wait(&record->waiter, &aMutex);
	
	// cleanup
	pthread_cond_destroy(&record->waiter);
	pthread_mutex_destroy(&aMutex);
	
	printMessage("Java_com_intel_bluetooth_BluetoothPeer_cancelInquiry: exiting", DEBUG_INFO_LEVEL);

	
	return record->success;
  }
  
  static void	cancelInquiry(void *v_cancel){
	
	cancelInquiryRec			aRec = (cancelInquiryRec*)v_cancel;
	currInq						*anInquiryListItem = s_inquiryList;
	JNIEnv						*env;
	jint						jErr;
	
	printMessage("cancelInquiry: called", DEBUG_INFO_LEVEL);
	
	jErr = (*s_vm)->GetEnv(s_vm, (void**) &env, JNI_VERSION_1_2);
	
	
	// extract the record for this listener
	
	while(anInquiry!=NULL && !( (*env)->IsSameObject(env, anInquiryListItem->aListener, aRec->listener)) ) {
		anInquiryListItem = anInquiryListItem->next;
	}
	
	if(anInquiry) {
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



JNIEXPORT jintArray JNICALL Java_com_intel_bluetooth_BluetoothPeer_getServiceHandles
  (JNIEnv *env, jobject peer, jobjectArray uuidSet, jlong address){
  printMessage("Java_com_intel_bluetooth_BluetoothPeer_getServiceHandles: called", DEBUG_INFO_LEVEL);
	return (*env)->NewIntArray(env, 10);
  
  
  }


JNIEXPORT jbyteArray JNICALL Java_com_intel_bluetooth_BluetoothPeer_getServiceAttributes
  (JNIEnv *env, jobject peer, jintArray attrIDs, jlong address, jint handle){
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_getServiceAttributes called", DEBUG_INFO_LEVEL);

  return (*env)->NewByteArray(env, 10);
  
  }


JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_registerService
  (JNIEnv *env, jobject peer, jbyteArray record){
  printMessage("Java_com_intel_bluetooth_BluetoothPeer_registerService called", DEBUG_INFO_LEVEL);
  return 0;
  }


JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_unregisterService
  (JNIEnv *env, jobject peer, jint handle){
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_unregisterService called", DEBUG_INFO_LEVEL);
}


JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_socket
  (JNIEnv *env, jobject peer, jboolean authenticate, jboolean encrypt){
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_socket called", DEBUG_INFO_LEVEL);

  return 0;
  }


JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothPeer_getsockaddress
  (JNIEnv *env, jobject peer, jint socket){
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_getsockaddress called", DEBUG_INFO_LEVEL);

  return 0LL;
  }


JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_getsockchannel
  (JNIEnv *env, jobject peer, jint socket){
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_getsockchannel called", DEBUG_INFO_LEVEL);

  
  return 0;
  }


JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_connect
  (JNIEnv *env, jobject peer, jint socket, jlong address, jint channel){
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_connect called", DEBUG_INFO_LEVEL);
}


JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_listen
  (JNIEnv *env, jobject peer, jint socket){
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_listen called", DEBUG_INFO_LEVEL);
}


JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_accept
  (JNIEnv *env, jobject peer, jint socket){
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_accept called", DEBUG_INFO_LEVEL);

  return 0;
  }


JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_recv__I
  (JNIEnv *env, jobject peer, jint socket){
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_recv__I called", DEBUG_INFO_LEVEL);

  return 0;
  }


JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_recv__I_3BII
  (JNIEnv *env, jobject peer, jint socket, jbyteArray b, jint off, jint len){
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_recv__I_3BII called", DEBUG_INFO_LEVEL);

  return 0;
  }


JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_send__II
  (JNIEnv *env, jobject peer, jint socket, jint b){
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_send__II called", DEBUG_INFO_LEVEL);
}


JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_send__I_3BII
  (JNIEnv *env, jobject peer, jint socket, jbyteArray b, jint off, jint len){
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_send__I_3BII called", DEBUG_INFO_LEVEL);

  }


JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_close
  (JNIEnv *env, jobject peer, jint socket){
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_close called", DEBUG_INFO_LEVEL);

  
  }


JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothPeer_getpeername
  (JNIEnv *env, jobject peer, jlong address){
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_getpeername called", DEBUG_INFO_LEVEL);

  		return (*env)->NewStringUTF(env, "fixme");
 }


JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothPeer_getpeeraddress
  (JNIEnv *env, jobject peer, jint socket) {
  
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_getpeeraddress called", DEBUG_INFO_LEVEL);

	return 0LL;
	}


JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothPeer_getradioname
  (JNIEnv *env, jobject peer, jlong address){
    printMessage("Java_com_intel_bluetooth_BluetoothPeer_getradioname called", DEBUG_INFO_LEVEL);

		return (*env)->NewStringUTF(env, "fixme");
  
  }
