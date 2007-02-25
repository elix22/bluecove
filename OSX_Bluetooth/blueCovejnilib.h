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
 
#define BLUETOOTH_VERSION_USE_CURRENT

#include <CoreFoundation/CoreFoundation.h>
#include <IOBluetooth/IOBluetoothUserLib.h>
#include <IOBluetooth/IOBluetoothUtilities.h>
/* slight problem with OBEXBluetooth header now 
   so until we start implementing it I'm leaving it out 
	#include <IOBluetooth/OBEXBluetooth.h>
*/
#include <jni.h>
#include <pthread.h>
#include "NativeCommons.h"
#include "JavaHeaders.h"
#include "ThreadCleanups.h"
#include "Version.h"

#define DEBUG_DEVEL_LEVEL	100
#define DEBUG_INFO_LEVEL	90
#define DEBUG_WARN_LEVEL	50
#define DEBUG_NORM_LEVEL	30
#define DEBUG_ERROR_LEVEL	10

#ifndef DEBUG
	#define DEBUG 100
#endif

#ifdef NATIVE_NAME
	#undef NATIVE_NAME
	#define NATIVE_NAME	"BlueCove OS X Native Library"
#endif
#ifdef NATIVE_VERSION
	#undef NATIVE_VERSION
	#define NATIVE_VERSION	"v0.2." BUILD_VERSION
#endif
#ifdef NATIVE_DESCRIP
	#undef NATIVE_DESCRIP
	#define NATIVE_DESCRIP NATIVE_NAME " " NATIVE_VERSION
#endif

#define EXPORT __attribute__((visibility("default")))
#ifdef JNIEXPORT
	#undef JNIEXPORT
	#define JNIEXPORT EXPORT
#endif

#define DO_NOT_EXPORT __attribute__((visibility("hidden")))
#if DEBUG >= DEBUG_DEVEL_LEVEL
	#define JAVA_ENV_CHECK(x) (*env)->x; if((*env)->ExceptionOccurred(env)) {(*env)->ExceptionDescribe(env); setBreakPoint();}
#else 
	#define JAVA_ENV_CHECK(x) (*env)->x;
#endif

/* create a linked lists of inquiries associating the native inquiry with the listener */
/* this list should never get very long so I'm going to be a bit lazy with it */

typedef struct currInq {
	IOBluetoothDeviceInquiryRef		anInquiry;
	jobject							aListener;
	int								refCount;
	char							inquiryStarted;
	char							isLimited; /* LAIC set */
	struct currInq					*next;
}  currInq;

typedef struct currServiceInq {
	int								index;
	IOBluetoothDeviceRef			aDevice;
	struct currServiceInq			*next;
} currServiceInq;



typedef struct doInquiryRec {
	jobject			peer;
	jint			accessCode;
	jobject			listener;
	
}  doInquiryRec;

typedef struct cancelInquiryRec{
	jobject			peer;
	jobject			listener;
	pthread_cond_t	waiter;
	char			validCondition;
	char			success;
}  cancelInquiryRec;

typedef struct macSocket {
	int							index;
	union {
		IOBluetoothL2CAPChannelRef	l2capRef;
		IOBluetoothRFCOMMChannelRef	rfcommRef;
		/* header problems in the sdk
		OBEXSessionRef				obexRef;
		*/
	}							ref;
	char						type; /* 1=l2capRef; 2=rfcommRef; 3=obexRef; 0=unknown */
	jobject						listenerPeer;
	char						encrypted;
	char						authenticate;
	struct macSocket			*next;
} macSocket;

 

/**
 * ----------------------------------------------
 * structures to pass between java threads and OS X thread
 * ----------------------------------------------
 */ 
typedef struct getServiceHandlesRec {
	jobject			peer;
	jobjectArray	uuidSet;
	jlong			address;
} getServiceHandlesRec;

typedef struct searchServicesRec {
	jobject			peer;
	jintArray		attrSet;
	jobjectArray	uuidSet;
	jstring			deviceAddress;
	jobject			device;
	jobject			listener;
	currServiceInq	*theInq;
}searchServicesRec;

typedef struct populateAttributesRec {
	jobject			serviceRecord;
	jintArray		attrSet;
	pthread_cond_t	waiter;
	jboolean		validCondition;
	jboolean		result;
} populateAttributesRec;

typedef struct connectRec {
	jobject			peer;
	jint			socket;
	jlong			address;
	jint			channel;
	jthrowable		errorException;
	char			validCondition;
	pthread_cond_t	callComplete;
} connectRec;

typedef struct	sendRFCOMMDataRec {
	UInt8			*bytes;
	UInt16			buflength;
	int				socket;
} sendRFCOMMDataRec;

typedef struct localNameRec {
	jstring			aName;
	char			validCondition;
	pthread_cond_t	callComplete;
} localNameRec;

typedef struct localDeviceClassRec {
	jobject			devClass;
	char			validCondition;
	pthread_cond_t	callComplete;
} localDeviceClassRec;

typedef struct setDiscoveryModeRec {
	jint			mode;
	jthrowable		errorException;
	char			validCondition;
	pthread_cond_t	callComplete;
} setDiscoveryModeRec;

typedef struct getDiscoveryModeRec {
	jint			mode;
	char			validCondition;
	pthread_cond_t	callComplete;
} getDiscoveryModeRec;
 /**
 * ----------------------------------------------
 * structures to maintain thread safety
 * ----------------------------------------------
 */ 
 
typedef union threadPassType {
		getServiceHandlesRec		*getSrvHandlePtr;
		searchServicesRec			*searchSrvPtr;
		populateAttributesRec		*populateAttrPtr;
		connectRec					*connectPtr;
		sendRFCOMMDataRec			*sendRFCOMMDataPtr;
		localNameRec				*localNamePtr;
		localDeviceClassRec			*localDevClassPtr;
		setDiscoveryModeRec			*setDiscoveryModePtr;
		getDiscoveryModeRec			*getDiscoveryModePtr;
		void						*voidPtr;
	}						threadPassType;
typedef struct todoListItem {
	struct todoListItem		*next;
	threadPassType			type;
	} todoListItem;
	
typedef struct todoListRoot {
	todoListItem			*listHead;
	pthread_mutex_t			listLock;
} todoListRoot;


/**
 * ----------------------------------------------
 * function prototypes
 * ----------------------------------------------
 */ 
int					inOSXThread(void);
void*				runLoopThread(void* ignore); 
void				performInquiry(void *info);
void				cancelInquiry(void *info);
void				asyncSearchServices(void* in) ;
void				bluetoothSDPQueryCallback( void * userRefCon, IOBluetoothDeviceRef deviceRef, IOReturn status );
void				inquiryDeviceFound(void *listener, IOBluetoothDeviceInquiryRef inquiryRef, IOBluetoothDeviceRef deviceRef);
void				inquiryComplete(void *listener, IOBluetoothDeviceInquiryRef inquiryRef, IOReturn error,	Boolean	aborted	);
int					generateProperties(JNIEnv	*env);
void*				cocoaWrapper(void* v_pthreadCond);
void				inquiryStarted(void * v_listener, IOBluetoothDeviceInquiryRef inquiryRef);
void				getServiceAttributes(void *in);
void				printMessage(const char* msg, int level);
currServiceInq*		newServiceInqRec(void);
currServiceInq*		getServiceInqRec(int index);
void				disposeServiceInqRec(currServiceInq*  toDelete);
macSocket*			getMacSocket(int index);
macSocket*			newMacSocket(void);
void				disposeMacSocket(macSocket*  toDelete);
void				throwException(JNIEnv *env, const char *name, const char *msg);
void				throwIOException(JNIEnv *env, const char *msg);
jobject				getjDataElement(JNIEnv *env, IOBluetoothSDPDataElementRef dataElement);
void				setBreakPoint(void);
void				initializeToDoList(todoListRoot *rootPtr);
void				addToDoItem(todoListRoot *rootPtr, threadPassType aRec);
void				deleteToDoItem(todoListRoot *rootPtr, threadPassType aRec);
threadPassType		getNextToDoItem(todoListRoot *rootPtr);
void				longToAddress(jlong	aLong, BluetoothDeviceAddress	*btDevAddress);
void				RFCOMMConnect(void* voidPtr);
void				rfcommEventListener (IOBluetoothRFCOMMChannelRef rfcommChannel, void *refCon, IOBluetoothRFCOMMChannelEvent *event);
void				rfcommSendData(void *voidPtr);
void				getLocalDeviceName(void	*voidPtr);
void				getLocalDeviceClass(void *voidPtr);
void				getLocalDiscoveryMode(void *voidPtr);
void				setLocalDiscoveryMode(void *voidPtr);
IOReturn			getCurrentModeInternal(int *mode);
/* Library Globals */
extern 	currInq					*s_inquiryList;
extern 	currServiceInq			*s_serviceInqList;
extern 	JavaVM					*s_vm;		
extern 	CFRunLoopRef			s_runLoop;
extern 	CFRunLoopSourceRef		s_inquiryStartSource;
extern	CFRunLoopSourceRef		s_inquiryStopSource;
extern 	CFRunLoopSourceRef		s_searchServicesStart;
extern	CFRunLoopSourceRef		s_populateServiceAttrs;
extern	CFRunLoopSourceRef		s_NewRFCOMMConnectionRequest;
extern	CFRunLoopSourceRef		s_SendRFCOMMData;
extern	CFRunLoopSourceRef		s_LocalDeviceClassRequest;
extern	CFRunLoopSourceRef		s_LocalDeviceNameRequest;
extern	CFRunLoopSourceRef		s_LocalDeviceSetDiscoveryMode;
extern	CFRunLoopSourceRef		s_LocalDeviceGetDiscoveryMode;
extern 	jobject					s_systemProperties;
extern 	const char*				s_errorBase;
extern 	char					s_errorBuffer[];
extern	macSocket*				s_openSocketList;


 
