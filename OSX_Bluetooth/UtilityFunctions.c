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

const char* s_errorBase = "Mac OS X Bluetooth Error: ";
char		s_errorBuffer[40];

void printMessage(const char* msg, int level) {
/* prints out a message when the library debug level is set higher than the called level */
#if DEBUG
	if(level < DEBUG) {
		fprintf(stderr, "BlueCove OS X native lib: %s\n",msg);
		fflush(stderr);
		
	}
#endif
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

/* returns non zero if there was a fatal error and the run loop shouldn't be initiated*/
int generateProperties(JNIEnv	*env) {

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
				/* also got the local hardware versioninfo */
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
		/*	pthread_mutex_lock(recursiveLock); */
		}
		{
			BluetoothDeviceAddress			localAddress;
			
			if(! IOBluetoothLocalDeviceReadAddress(	&localAddress, NULL, NULL, NULL)) {
				CFStringRef		aString = IOBluetoothCFStringFromDeviceAddress( &localAddress );
				CFRange			range;
				UniChar			*charBuf;
				printMessage("Local Address:", DEBUG_DEVEL_LEVEL);

				range.location = 0;
				range.length = CFStringGetLength(aString);
				
				charBuf = malloc(sizeof(UniChar) * range.length);
				CFStringGetCharacters(aString, range, charBuf);
					
				prop = (*env)->NewString(env, (jchar *)charBuf, (jsize)range.length);
				
				
				key = (*env)->NewStringUTF(env, BLUECOVE_SYSTEM_PROP_LOCAL_ADDRESS);
				oldProp = (*env)->CallObjectMethod(env, s_systemProperties, setPropMethod, key, prop);
				free(charBuf);
			} else {
				printMessage("Unable to get local Address!!", DEBUG_WARN_LEVEL);
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

macSocket*	getMacSocket(int index) {
	macSocket				*current = s_openSocketList;
	
	while( (current != NULL) && (current->index != index)) {
		current = current->next;
	}
	
	return current;
}

macSocket*		newMacSocket(void) {
	static					signed int		nextInq = 1;

	macSocket			*current = s_openSocketList;
	
	while(getMacSocket(nextInq) != NULL) nextInq++;
	if(nextInq < 0) {
		nextInq = 1;
		while(getMacSocket(nextInq) != NULL) nextInq++;
	}
	/* should never happen */
	if(nextInq < 0) 
		printMessage("Ran out of available socket indexes, you have more than 2 billion current inquiries :-O",
					DEBUG_ERROR_LEVEL);
			
	if(s_openSocketList != NULL ) {
		while(current->next != NULL) current = current->next;
		current->next = (macSocket*)malloc(sizeof(macSocket));
		current = current->next;
	} else {
		s_openSocketList = (macSocket*)malloc(sizeof(macSocket));
		current = s_openSocketList;
	}
	current->next = NULL;
	current->index = nextInq;
	current->ref.l2capRef = NULL;
	current->ref.rfcommRef = NULL;
	
	nextInq ++;
	
	return current;
}

void disposeMacSocket(macSocket*  toDelete) {
	/* first find the prior item */
	macSocket			*current = s_openSocketList;
	jint				jErr;
	JNIEnv				*env;

	jErr = (*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);

	/* clean up existing references, since its a union it doesn't matter
		which one is released */
	IOBluetoothObjectRelease(toDelete->ref.l2capRef);
	(*env)->DeleteGlobalRef(env, toDelete->listenerPeer);
	
	if(s_openSocketList== NULL) {
		printMessage("disposeMacSocket called with an unknown macSocket!", DEBUG_WARN_LEVEL);
		return;
	}
	if(toDelete == s_openSocketList) {
		s_openSocketList = toDelete->next;
		free(toDelete);
		return;
	}
	while((current->next !=NULL) && (current->next != toDelete)) current = current->next;
	
	if(current->next == NULL) {
		printMessage("disposeMacSocket called with an unknown macSocket!", DEBUG_WARN_LEVEL);
		return;
	}
	current->next = toDelete->next;
	free(toDelete);
	
}

jobject getjDataElement(JNIEnv *env, IOBluetoothSDPDataElementRef dataElement) {

	jclass									dataElementClass;
	jobject									jDataElement;
	jmethodID								constructor;
	BluetoothSDPDataElementTypeDescriptor	typeDescrip;
	BluetoothSDPDataElementSizeDescriptor	typeSize;
	UInt32									byteSize;
	jboolean								isUnsigned, isURL, isSequence;
		
	if((*env)->ExceptionOccurred(env)) (*env)->ExceptionDescribe(env);
	dataElementClass = (*env)->FindClass(env, "javax/bluetooth/DataElement");
	typeDescrip = IOBluetoothSDPDataElementGetTypeDescriptor(dataElement);
	typeSize = IOBluetoothSDPDataElementGetSizeDescriptor(dataElement);
	byteSize = IOBluetoothSDPDataElementGetSize(dataElement);
	isUnsigned = 0;
	isURL = 0;
	isSequence = 0;
	
	switch(typeDescrip) {
		case kBluetoothSDPDataElementTypeNil:
				constructor = (*env)->GetMethodID(env, dataElementClass, "<init>", "(I)V");
				jDataElement = (*env)->NewObject(env, dataElementClass, constructor, 0);
				break;
		case kBluetoothSDPDataElementTypeUnsignedInt:
			isUnsigned = 1;
		case kBluetoothSDPDataElementTypeSignedInt:
			if(typeSize==4) { /* 16 byte integer */
				CFDataRef			bigData;
				const UInt8			*byteArray;
				jbyteArray			aJByteArray;
				
				constructor = (*env)->GetMethodID(env, dataElementClass, "<init>", "(ILjava/lang/Object;)V");
				bigData = IOBluetoothSDPDataElementGetDataValue(dataElement);
				byteArray = CFDataGetBytePtr(bigData);
				aJByteArray = (*env)->NewByteArray(env, 16);
				(*env)->SetByteArrayRegion(env, aJByteArray, 0, 16, (jbyte*)byteArray);
				jDataElement = JAVA_ENV_CHECK(NewObject(env, dataElementClass, constructor, isUnsigned ? 0x0C : 0x14, aJByteArray));
			} else {
				CFNumberRef		aNumber;
				jint			typeValue;
				jlong			aBigInt = 0LL;
				
				constructor = (*env)->GetMethodID(env, dataElementClass, "<init>", "(IJ)V");
				typeValue = 0;
				aBigInt = 0;
				aNumber = IOBluetoothSDPDataElementGetNumberValue(dataElement);
				CFNumberGetValue (aNumber, kCFNumberLongLongType, &aBigInt);
				switch(typeSize) {
					case 0: /* 1 byte int */
						if(isUnsigned && (aBigInt < 0)) aBigInt += 0x100;
						typeValue = (isUnsigned ? 0x08 : 0x10 );
						break;
					case 1: /* 2 byte int */
						if(isUnsigned && (aBigInt < 0)) aBigInt += 0x10000;
						typeValue = (isUnsigned ? 0x09 : 0x11 );
						break;
					case 2: /* 4 byte int */
						if(isUnsigned && (aBigInt < 0)) aBigInt += 0x100000000;
						typeValue	= (isUnsigned ? 0x0A : 0x12 );
						break;
					case 3: /* 8 byte int */
						typeValue = (isUnsigned ? 0x0B : 0x13 );
						break;
					}
				jDataElement = JAVA_ENV_CHECK(NewObject(env, dataElementClass, constructor, typeValue, aBigInt));
			}
			break; 
		case kBluetoothSDPDataElementTypeUUID: 
			{
				IOBluetoothSDPUUIDRef	aUUIDRef;
				const jbyte				*uuidBytes;
				UInt8					length, k;
				CFMutableStringRef		stringUUID;
				jstring					jStringUUID;
				UniChar					*charBuf;
				CFRange					range;
				jclass					jUUIDClass;
				jmethodID				jUUIDConstructor;
				jobject					jUUID;
				
				constructor = JAVA_ENV_CHECK(GetMethodID(env, dataElementClass, "<init>", "(ILjava/lang/Object;)V"));
				stringUUID = CFStringCreateMutable (NULL, 500);
				aUUIDRef = IOBluetoothSDPDataElementGetUUIDValue(dataElement);
				uuidBytes = IOBluetoothSDPUUIDGetBytes(aUUIDRef);
				length =  IOBluetoothSDPUUIDGetLength(aUUIDRef);
				for(k=0;k<length;k++) {
					CFStringAppendFormat(stringUUID, NULL, CFSTR("%02x"), uuidBytes[k]);
				}
				range.location = 0;
				range.length = CFStringGetLength(stringUUID);
				charBuf = malloc(sizeof(UniChar) *range.length);
				CFStringGetCharacters(stringUUID, range, charBuf);
				jStringUUID = JAVA_ENV_CHECK(NewString(env, (jchar*)charBuf, (jsize)range.length));
				free(charBuf);
				jUUIDClass = JAVA_ENV_CHECK(FindClass(env, "javax/bluetooth/UUID"));
				jUUIDConstructor = JAVA_ENV_CHECK(GetMethodID(env, jUUIDClass, "<init>", "(Ljava/lang/String;Z)V"));
				jUUID = JAVA_ENV_CHECK(NewObject(env, jUUIDClass, jUUIDConstructor, jStringUUID, (range.length == 8) ? 0:1));
					
				jDataElement = JAVA_ENV_CHECK(NewObject(env, dataElementClass, constructor, 0x18, jUUID));
				CFRelease(stringUUID);
			}
			break;
		case kBluetoothSDPDataElementTypeURL:
			isURL = 1;
		case kBluetoothSDPDataElementTypeString:
			{
				CFStringRef				aString;
				jstring					jStringRef;
				UniChar					*charBuf;
				CFRange					range;
					
				constructor = JAVA_ENV_CHECK(GetMethodID(env, dataElementClass, "<init>", "(ILjava/lang/Object;)V"));
				aString = IOBluetoothSDPDataElementGetStringValue(dataElement);
				range.location = 0;
				range.length = CFStringGetLength(aString);
				charBuf = malloc(sizeof(UniChar)*range.length);
				CFStringGetCharacters(aString, range, charBuf);
				jStringRef = JAVA_ENV_CHECK(NewString(env, (jchar*)charBuf, (jsize)range.length));
				free(charBuf);
				CFRelease(aString);
				jDataElement = JAVA_ENV_CHECK(NewObject(env, dataElementClass, constructor, isURL ? 0x40: 0x20, jStringRef));
			}
			break;
		case kBluetoothSDPDataElementTypeBoolean:
			{
				jboolean			aBool;
				CFNumberRef			aNumber;
				
				constructor = JAVA_ENV_CHECK(GetMethodID(env, dataElementClass, "<init>", "(Z)V"));
				aNumber = IOBluetoothSDPDataElementGetNumberValue(dataElement);
				CFNumberGetValue(aNumber, kCFNumberCharType, &aBool);
				jDataElement = JAVA_ENV_CHECK(NewObject(env, dataElementClass, constructor, aBool));
			}
			break;
		case kBluetoothSDPDataElementTypeDataElementSequence:
			isSequence = 1;
		case kBluetoothSDPDataElementTypeDataElementAlternative:
			{
				CFArrayRef			anArray;
				CFIndex				m, count;
				jmethodID			addElement;
				
				addElement = JAVA_ENV_CHECK(GetMethodID(env, dataElementClass, "addElement", "(Ljavax/bluetooth/DataElement;)V"));
				constructor = JAVA_ENV_CHECK(GetMethodID(env, dataElementClass, "<init>", "(I)V"));
				jDataElement = JAVA_ENV_CHECK(NewObject(env, dataElementClass, constructor, isSequence ? 0x30 : 0x38));
				anArray = IOBluetoothSDPDataElementGetArrayValue(dataElement);
				count = CFArrayGetCount(anArray);
				for(m=0;m<count;m++) {
					const IOBluetoothSDPDataElementRef	anItem = (IOBluetoothSDPDataElementRef)CFArrayGetValueAtIndex (anArray, m);
					jobject								ajElement;
		
					ajElement = getjDataElement(env, anItem);
					JAVA_ENV_CHECK(CallVoidMethod(env, jDataElement, addElement, ajElement));
				}
			}
			break;
		default:
			printMessage("getjDataElement: Unknown data element type encounterd!", DEBUG_WARN_LEVEL);
			jDataElement = NULL;
			break;
			
		}
	return jDataElement;
	
}

void longToAddress(jlong	aLong, BluetoothDeviceAddress*	anAddress) {
	
	int		i;
	
	for(i=5;i>=0;i--) {
		anAddress->data[i] = (aLong & 0x00000000000000FF);
		aLong >>= 8;
	}

}

void doSynchronousTask(CFRunLoopSourceRef  theSource, threadPassType  *typeMaskPtr) {
	pthread_mutex_t			callInProgress;
	CFRunLoopSourceContext	aContext={0};
	todoListRoot			*todoListPtr;
	
	CFRunLoopSourceGetContext(theSource, &aContext);
	todoListPtr = (todoListRoot*)aContext.info;
	
	addToDoItem(todoListPtr, typeMaskPtr);
	if(inOSXThread()) {
		typeMaskPtr->validCondition = FALSE;
		aContext.perform(todoListPtr);
	} else {
		pthread_cond_init(& (typeMaskPtr->callComplete), NULL);
		pthread_mutex_init(&callInProgress, NULL);
		pthread_mutex_lock(&callInProgress);
		typeMaskPtr->validCondition = TRUE;
		CFRunLoopSourceSignal(theSource);
		CFRunLoopWakeUp(s_runLoop);
		pthread_cond_wait(& (typeMaskPtr->callComplete), &callInProgress);
		pthread_mutex_unlock(&callInProgress);
		pthread_mutex_destroy(&callInProgress);
		pthread_cond_destroy(&typeMaskPtr->callComplete);
	}
}

/* in order to avoid deadlocks we need to determine if we're in the OS X thread before halting the current thread */
int		inOSXThread(void) {
		CFRunLoopRef				aRunLoopRef = CFRunLoopGetCurrent();
		return CFEqual(aRunLoopRef, s_runLoop);
}
void				setBreakPoint(void){

	printMessage("The debugger should break here", DEBUG_DEVEL_LEVEL);


}

#if 0
#pragma mark -
#pragma mark === Device Inquiry Management Utilites ===
#endif
static Boolean  equalListeners (const void *value1, const void *value2);
static void initializeInquiryUtilities();

static CFMutableDictionaryRef		s_pendingInquiriesDict = NULL;
static pthread_mutex_t				s_safety4pendingInquiries;

static Boolean  equalListeners (const void *value1, const void *value2) {
	JNIEnv				*env;
	(*s_vm)->GetEnv(s_vm, (void**)&env, JNI_VERSION_1_2);
	return JAVA_ENV_CHECK(IsSameObject(env, (jobject)value1, (jobject)value2));
}
	
static void initializeInquiryUtilities() {
	CFDictionaryKeyCallBacks 	keyCallbacks={0};
	keyCallbacks.equal = equalListeners;

	s_pendingInquiriesDict = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, 
						&keyCallbacks, NULL);
	pthread_mutex_init(&s_safety4pendingInquiries, NULL);
}

IOBluetoothDeviceInquiryRef getPendingInquiryRef(jobject	listener) {
	IOBluetoothDeviceInquiryRef		aRef;

	if(!s_pendingInquiriesDict) initializeInquiryUtilities();
	
	pthread_mutex_lock(&s_safety4pendingInquiries);
	aRef = (IOBluetoothDeviceInquiryRef)CFDictionaryGetValue (s_pendingInquiriesDict, listener);
   	pthread_mutex_unlock(&s_safety4pendingInquiries);
   	return aRef;
}
void addInquiry(jobject listener, IOBluetoothDeviceInquiryRef aRef) {
	if(!s_pendingInquiriesDict) initializeInquiryUtilities();
	
	pthread_mutex_lock(&s_safety4pendingInquiries);
	CFDictionaryAddValue (s_pendingInquiriesDict, listener, aRef);
  	pthread_mutex_unlock(&s_safety4pendingInquiries);
}
void removeInquiry(jobject listener) {
	IOBluetoothDeviceInquiryRef		aRef;

	if(!s_pendingInquiriesDict) initializeInquiryUtilities();
	
	pthread_mutex_lock(&s_safety4pendingInquiries);
	aRef = (IOBluetoothDeviceInquiryRef)CFDictionaryGetValue (s_pendingInquiriesDict, listener);
 	CFDictionaryRemoveValue (s_pendingInquiriesDict, listener);
   	pthread_mutex_unlock(&s_safety4pendingInquiries);
}



#if 0
#pragma mark -
#pragma mark === Service Searching Management Utilites ===
#endif

static CFMutableDictionaryRef		s_runningSearchesDict = NULL;
static pthread_mutex_t			s_safety4runningSearches;
static void initializeServiceSearchUtilities();
	
static void initializeServiceSearchUtilities() {
	s_runningSearchesDict = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, 
						NULL, NULL);
	pthread_mutex_init(&s_safety4runningSearches, NULL);
}
int addServiceSearch(searchServicesRec	*aSearch) {
	static		int		nextIndex = 1;
	int			storedAt;
	
	if(!s_runningSearchesDict) initializeServiceSearchUtilities();

	pthread_mutex_lock(&s_safety4runningSearches);
	if(CFDictionaryGetCount(s_pendingInquiriesDict) > 0x8FFFFFFF) {
		printMessage("ERROR: too many concurrent searchs occuring, around 2 billion!", DEBUG_ERROR_LEVEL);
		storedAt = -1;
	} else {
		while(CFDictionaryGetValue(s_runningSearchesDict, (void*)nextIndex)) {
			nextIndex++;
			if(nextIndex<0) nextIndex=1;
		}
		CFDictionaryAddValue (s_runningSearchesDict, (void*)nextIndex, aSearch);
		/* don't simply this or thread hell will decend */
		storedAt = nextIndex;
		nextIndex++;
		if(nextIndex < 0) nextIndex = 1;
	}
	pthread_mutex_unlock(&s_safety4runningSearches);
	
	return storedAt;
}
searchServicesRec*	getServiceSearchRec(int  ref) {
	searchServicesRec*		aVal;
	if(!s_runningSearchesDict) initializeServiceSearchUtilities();
	
	pthread_mutex_lock(&s_safety4runningSearches);
	aVal = (searchServicesRec*)CFDictionaryGetValue(s_runningSearchesDict, (void*)ref);
	pthread_mutex_unlock(&s_safety4runningSearches);
	return aVal;
}
void removeServiceSearchRec(int ref) {
	if(!s_runningSearchesDict) initializeServiceSearchUtilities();
	
	pthread_mutex_lock(&s_safety4runningSearches);
	CFDictionaryRemoveValue(s_runningSearchesDict, (void*)ref);
	pthread_mutex_unlock(&s_safety4runningSearches);
}

