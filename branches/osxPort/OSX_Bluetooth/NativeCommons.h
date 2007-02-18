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


/* Correct the following three defines in a platform specific header */
#ifndef NATIVE_NAME
	#define NATIVE_NAME "Unknown"
#endif

#ifndef	NATIVE_VERSION
	#define	NATIVE_VERSION "Unknown"
#endif

#ifndef NATIVE_DESCRIP
	#define NATIVE_DESCRIP "Unknown"
#endif

/*	string for the version number of the native library */
#define BLUECOVE_SYSTEM_PROP_NATIVE_LIBRARY_VERSION "javax.bluetooth.bluecove.nativeVersion"

/*	string for the description of the native library */
#define BLUECOVE_SYSTEM_PROP_NATIVE_LIBRARY_DESCRIP "javax.bluetooth.bluecove.nativeDescrip"

/*	string for the description of the OS library versions */
#define BLUECOVE_SYSTEM_PROP_NATIVE_OSBT_LIBRARY_VERSION "javax.bluetooth.bluecove.os.bluetoothLibVersion"

/*	Hardware Present is either true or false if the local hardware is present and usable */
#define BLUECOVE_SYSTEM_PROP_HARDWARE_PRESENT "javax.bluetooth.bluecove.hardwarePresent" 

/*	If available defines the friendly name of the local device */
#define BLUECOVE_SYSTEM_PROP_LOCAL_NAME "javax.bluetooth.bluecove.localFriendlyName"

/* Local address of device */
#define BLUECOVE_SYSTEM_PROP_LOCAL_ADDRESS "javax.bluetooth.bluecove.localAddress"

/* Native Library Supports native async calls */
#define BLUECOVE_SYSTEM_PROP_NATIVE_ASYNC_ENABLED "javax.bluetooth.bluecove.asyncEnabled"


/*	The JSR82 properties will be set as System properties with the javax prefix */

/*	The version of the Java API for Bluetooth wireless technology that is supported. For 
	this version it will be set to "1.0".*/
#define JSR82_SYSTEM_ENV_API_VERSION "javax.bluetooth.api.version"

/*	Is master/slave switch allowed? Valid values are either "true" or "false". */
#define JSR82_SYSTEM_ENV_MASTER_SWITCH "javax.bluetooth.master.switch"

/*	Maximum number of service attributes to be retrieved per service record. The string 
	will be in Base 10 digits. */
#define JSR82_SYSTEM_ENV_MAX_SRV_ATTR "javax.bluetooth.sd.attr.retrievable.max"

/*	The maximum number of connected devices supported. This number may be greater 
	than 7 if the implementation handles parked connections. The string will be in 
	Base 10 digits. */
#define JSR82_SYSTEM_ENV_MAX_CON_DEV "javax.bluetooth.connected.devices.max"

/*	The maximum ReceiveMTU size in bytes supported in L2CAP. The string will be in 
	Base 10 digits, e.g. "32". */
#define JSR82_SYSTEM_ENV_MAX_RECV_MTU "javax.bluetooth.l2cap.receiveMTU.max"

/*	Maximum number of concurrent service discovery transactions. The string will be in 
	Base 10 digits. */
#define JSR82_SYSTEM_ENV_MAX_CONCURRENT_SRV_DISC "javax.bluetooth.sd.trans.max"

/*	Is Inquiry scanning allowed during connection? Valid values are either "true" 
	or "false". */
#define JSR82_SYSTEM_ENV_INQR_SCAN_DUR_CONN "javax.bluetooth.connected.inquiry.scan"

/*	Is Page scanning allowed during connection? Valid values are either "true" 
	or "false". */
#define JSR82_SYSTEM_ENV_PAGE_SCAN_DUR_CONN "javax.bluetooth.connected.page.scan"

/*	Is Inquiry allowed during a connection? Valid values are either "true" or
	"false". */
#define JSR82_SYSTEM_ENV_INQR_DUR_CONN "javax.bluetooth.connected.inquiry"

/*	Is paging allowed during a connection? In other words, can a connection be established
	to one device if it is already connected to another device. Valid values are either
	"true" or "false". */
#define JSR82_SYSTEM_ENV_PAGE_DUR_CONN "javax.bluetooth.connected.page"

