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
#include <JavaVM/jni.h>
#include <pthread.h>
#include "NativeCommons.h"
#include "com_intel_bluetooth_BluetoothPeer.h"
#include "ThreadCleanups.h"



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
	#define NATIVE_VERSION	"v0.1.1"
#endif
#ifdef NATIVE_DESCRIP
	#undef NATIVE_DESCRIP
	#define NATIVE_DESCRIP NATIVE_NAME " " NATIVE_VERSION
#endif
/* create a linked lists of inquiries associating the native inquiry with the listener */
/* this list should never get very long so I'm going to be a bit lazy with it */

typedef struct currInq {
	IOBluetoothDeviceInquiryRef		anInquiry;
	jobject							aListener;
	int								refCount;
	char							inquiryStarted;
	struct currInq					*next;
}  currInq;


typedef struct doInquiryRec {
	jobject			peer;
	jint			accessCode;
	jobject			listener;
	
}  doInquiryRec;

typedef struct cancelInquiryRec{
	jobject			peer;
	jobject			listener;
	pthread_cond_t	waiter;
	char			success;
}  cancelInquiryRec;

typedef struct getServiceHandlesRec {
	jobject			peer;
	jobjectArray	uuidSet;
	jlong			address;
} 


static void* runLoopThread(void* ignore); 


static void	performInquiry(void *info);
static void	cancelInquiry(void *info);

static void inquiryDeviceFound(void *listener, IOBluetoothDeviceInquiryRef inquiryRef, IOBluetoothDeviceRef deviceRef);
static void inquiryComplete(void *listener, IOBluetoothDeviceInquiryRef inquiryRef, IOReturn	error,	Boolean	aborted	);
static int generateProperties(JNIEnv	*env);
static void* cocoaWrapper(void* v_pthreadCond);
static void inquiryStarted(void * v_listener, IOBluetoothDeviceInquiryRef		inquiryRef	);