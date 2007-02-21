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
	current->l2capRef = NULL;
	current->rfcommRef = NULL;
	
	nextInq ++;
	
	return current;
}

void disposeMacSocket(macSocket*  toDelete) {
	/* first find the prior item */
	macSocket			*current = s_openSocketList;
	
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

currServiceInq*		getServiceInqRec(int index) {
	/* searchs the current linked list the current inquiry */
	currServiceInq			*current = s_serviceInqList;
	
	while( (current != NULL) && (current->index != index)) {
		current = current->next;
	}
	
	return current;
}


currServiceInq*		newServiceInqRec(void) {
	static					signed int		nextInq = 1;

	currServiceInq			*current = s_serviceInqList;
	
	while(getServiceInqRec(nextInq) != NULL) nextInq++;
	if(nextInq < 0) {
		nextInq = 1;
		while(getServiceInqRec(nextInq) != NULL) nextInq++;
	}
	/* should never happen */
	if(nextInq < 0) 
		printMessage("Ran out of available new service inquiry indexes, you have more than 2 billion current inquiries :-O",
					DEBUG_ERROR_LEVEL);
			
	if(s_serviceInqList != NULL ) {
		while(current->next != NULL) current = current->next;
		current->next = (currServiceInq*)malloc(sizeof(currServiceInq));
		current = current->next;
	} else {
		s_serviceInqList = (currServiceInq*)malloc(sizeof(currServiceInq));
		current = s_serviceInqList;
	}
	current->next = NULL;
	current->index = nextInq;
	
	nextInq ++;
	
	return current;
}




void disposeServiceInqRec(currServiceInq*  toDelete) {
	/* first find the prior item */
	currServiceInq			*current = s_serviceInqList;
	
	if(s_serviceInqList== NULL) {
		printMessage("disposeServiceInqRec called with an unknown currServiceInq!", DEBUG_WARN_LEVEL);
		return;
	}
	if(toDelete == s_serviceInqList) {
		s_serviceInqList = toDelete->next;
		free(toDelete);
		return;
	}
	while((current->next !=NULL) && (current->next != toDelete)) current = current->next;
	
	if(current->next == NULL) {
		printMessage("disposeServiceInqRec called with an unknown currServiceInq!", DEBUG_WARN_LEVEL);
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
				jDataElement = JAVA_ENV_CHECK((*env)->NewObject(env, dataElementClass, constructor, isUnsigned ? 0x0C : 0x14, aJByteArray));
			} else {
				CFNumberRef		aNumber;
				jint			typeValue;
				jlong			aBigInt;
				
				constructor = (*env)->GetMethodID(env, dataElementClass, "<init>", "(IJ)V");
				typeValue = 0;
				aBigInt = 0;
				aNumber = IOBluetoothSDPDataElementGetNumberValue(dataElement);
				CFNumberGetValue (aNumber, kCFNumberLongLongType, &aBigInt);
				switch(typeSize) {
					case 0:
						typeValue = (isUnsigned ? 0x08 : 0x10 );
						break;
					case 1:
						typeValue = (isUnsigned ? 0x09 : 0x11 );
						break;
					case 2:
						typeValue	= (isUnsigned ? 0x0A : 0x12 );
						break;
					case 3:
						typeValue = (isUnsigned ? 0x0B : 0x13 );
						break;
					}
				jDataElement = JAVA_ENV_CHECK((*env)->NewObject(env, dataElementClass, constructor, typeValue, aBigInt));
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
				
				constructor = JAVA_ENV_CHECK((*env)->GetMethodID(env, dataElementClass, "<init>", "(ILjava/lang/Object;)V"));
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
				jStringUUID = JAVA_ENV_CHECK((*env)->NewString(env, (jchar*)charBuf, (jsize)range.length));
				free(charBuf);
				jUUIDClass = JAVA_ENV_CHECK((*env)->FindClass(env, "javax/bluetooth/UUID"));
				jUUIDConstructor = JAVA_ENV_CHECK((*env)->GetMethodID(env, jUUIDClass, "<init>", "(Ljava/lang/String;Z)V"));
				jUUID = JAVA_ENV_CHECK((*env)->NewObject(env, jUUIDClass, jUUIDConstructor, jStringUUID, (range.length == 8) ? 0:1));
					
				jDataElement = JAVA_ENV_CHECK((*env)->NewObject(env, dataElementClass, constructor, 0x18, jUUID));
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
					
				constructor = JAVA_ENV_CHECK((*env)->GetMethodID(env, dataElementClass, "<init>", "(ILjava/lang/Object;)V"));
				aString = IOBluetoothSDPDataElementGetStringValue(dataElement);
				range.location = 0;
				range.length = CFStringGetLength(aString);
				charBuf = malloc(sizeof(UniChar)*range.length);
				CFStringGetCharacters(aString, range, charBuf);
				jStringRef = JAVA_ENV_CHECK((*env)->NewString(env, (jchar*)charBuf, (jsize)range.length));
				free(charBuf);
				CFRelease(aString);
				jDataElement = JAVA_ENV_CHECK((*env)->NewObject(env, dataElementClass, constructor, isURL ? 0x40: 0x20, jStringRef));
			}
			break;
		case kBluetoothSDPDataElementTypeBoolean:
			{
				jboolean			aBool;
				CFNumberRef			aNumber;
				
				constructor = JAVA_ENV_CHECK((*env)->GetMethodID(env, dataElementClass, "<init>", "(Z)V"));
				aNumber = IOBluetoothSDPDataElementGetNumberValue(dataElement);
				CFNumberGetValue(aNumber, kCFNumberCharType, &aBool);
				jDataElement = JAVA_ENV_CHECK((*env)->NewObject(env, dataElementClass, constructor, aBool));
			}
			break;
		case kBluetoothSDPDataElementTypeDataElementSequence:
			isSequence = 1;
		case kBluetoothSDPDataElementTypeDataElementAlternative:
			{
				CFArrayRef			anArray;
				CFIndex				m, count;
				jmethodID			addElement;
				
				addElement = JAVA_ENV_CHECK((*env)->GetMethodID(env, dataElementClass, "addElement", "(Ljavax/bluetooth/DataElement;)V"));
				constructor = JAVA_ENV_CHECK((*env)->GetMethodID(env, dataElementClass, "<init>", "(I)V"));
				jDataElement = JAVA_ENV_CHECK((*env)->NewObject(env, dataElementClass, constructor, isSequence ? 0x30 : 0x38));
				anArray = IOBluetoothSDPDataElementGetArrayValue(dataElement);
				count = CFArrayGetCount(anArray);
				for(m=0;m<count;m++) {
					const IOBluetoothSDPDataElementRef	anItem = (IOBluetoothSDPDataElementRef)CFArrayGetValueAtIndex (anArray, m);
					jobject								ajElement;
		
					ajElement = getjDataElement(env, anItem);
					JAVA_ENV_CHECK((*env)->CallVoidMethod(env, jDataElement, addElement, ajElement));
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
void				setBreakPoint(void){

	printMessage("The debugger should break here", DEBUG_DEVEL_LEVEL);


}
