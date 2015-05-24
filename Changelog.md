[SVN Commit log message](http://code.google.com/p/bluecove/source/list)

**2.1.1 to be released 2009-XX-XX**

> 2009-03-08
  * bluecove-bluez: Service registration over D-Bus implemented on BlueZ v4 and BlueZ v3

> 2009-03-08
  * bluecove-bluez: Device Inquiry and Search Service work on BlueZ v4 and BlueZ v3

> 2009-03-05
  * bluecove-bluez: fixed Inquiry, Search Service, Authentication, LocalDevice discoverable mode on BlueZ V3
  * bluecove-bluez: Adding API for Unix domain sockets

> 2009-02-13
  * Implementation of RemoteDeviceHelper.readRSSI(RemoteDevice) on Mac OS X

> 2009-02-10
  * Implementation of DiscoveryAgent.retrieveDevices on Mac OS X
  * Native ReceiveBuffer 64K changed to 256K (OS X/WIDCOMM) to avoid 'Receive buffer overflown' during GC

> 2009-01-16
  * bluecove.native.path  accept path separated by system-dependent path-separator (: or ;)
  * On Linux look for native libraries in path /usr/lib/bluecove/${version}/ or /usr/lib64/bluecove/${version}/ if library not found in resources

**2.1.0 released 2008-12-25**

> 2008-11-19
  * Drop dual stack support on Windows XP in favor of Broadcom BTW Stack 5.5 and 6.1. See http://code.google.com/p/bluecove/wiki/stacks for more information
  * bluecove-gpl binary build compatibility with BlueZ v3 and BlueZ v4
  * Tested for JSR-82 1.1.1 compatibility.
  * OBEX Authenticator for PUT, GET, and SET PATH operations

> 2008-12-03
  * Fixed thread.interrupt() will break read on MS stack

> 2008-11-14
  * Implementation of DiscoveryAgent.retrieveDevices on MS stack, Sponsored by Zargis Medical Corp

> 2008-10-24
  * Authenticate and remove authentication/Non JSR-82 on MS Stack, Sponsored by Zargis Medical Corp

> 2008-10-16
  * Change license to Apache License, Version 2.0
  * Function to remove authentication/Non JSR-82 on WIDCOMM

**2.0.3 released 2008-08-11**

> 2008-06-01 - 2008-08-11
  * Fixed 64K buffer limit on WIDCOMM Stack
  * Fixed ERROR\_IO\_INCOMPLETE on BlueSoleil Stack
  * Fixed services search on WIDCOMM to use 'and' for all UUID in query

> 2008-05-01 - 2008-05-25
  * BlueCoveImpl.shutdown() API function
  * Supports IBM J9 (MIDP and PPRO 1.0) on Linux, Sun CDC on Windows XP
  * OBEX Operation Get supports InputStream

> 2008-04-17
  * bluecove-emu: JSR 82 Emulator

> 2008-04-08
  * Support Multiple Adapters and Bluetooth Stacks in same JVM

> 2008-03-06
  * Fixed OBEX bug reciving Time Headers (Nokia)
  * Proper behavior for concurrent DiscoveryAgent.searchServices, "bluetooth.sd.trans.max"

> 2008-02-27
  * Configurable "bluecove.inquiry.duration" for MS stack and OSX

> 2008-01-30 Vlad Skarzhevskyy
  * Fixed major start problem on Mac OS X
  * Fixed SDP SEQ8, SEQ16, UUID 16 and 36 bit
  * Allow to load x64 libraries on Windows and Linux

> 2008-01-19 Vlad Skarzhevskyy
  * bluecove-gpl: RFCOMM and L2CAP client and services on Linux BlueZ

> 2008-01-12 Mina Shokry
  * bluecove-gpl: Service discovery on Linux

# 2.0.2 released 2008-01-07 #

> 2007-12-09
  * WIDCOMM RFCOMM Service accept multiple connections
  * Implemented ServiceRecord.setDeviceServiceClasses() on XP MS Stack

> 2007-12-05
  * L2CAP, RFCOMM and OBEX server on Mac OS X

> 2007-11-28
  * RFCOMM and L2CAP client on Mac OS X

> 2007-11-19
  * WIDCOMM use BTW-6\_1\_0\_1501-SDK, Fixed Service Attributes BOOLEAN, U\_INT\_8, INT\_8, INT\_16 and UINT\_16

> 2007-11-16
  * Fixed Service Attributes STRING to be UTF8

> 2007-11-05
  * Started OS X development base on Eric Wagner code and with consultation by Bea Lam

> 2007-10-24
  * OBEX read timeout. Throws InterruptedIOException when Connector.open(,,true) is used

> 2007-10-04
  * fixed initialization exceptions, e.g. BluetoothStateException when device is not ready or no stack found
  * No need for WTK to run the build.

> 2007-09-09
  * connection and stream close() functions will work according to specification.

> 2007-09-04
  * for J9 -Dmicroedition.connection.pkgs=com.intel.bluetooth is optional

> 2007-08-28
  * log4j integration, Bluecove log redirected to log4j when one is available in classpath

# 2.0.1 released 2007-08-28 #

> 2007-08-24
  * Full OBEX over RFCOMM and TCP
  * Bluetooth\_1-1\_006\_TCK on WIDCOMM, Pass 513, Fail 19. (some security features not implemented)
  * Review javadocs

> 2007-07-22
  * L2CAP implementation only on WIDCOMM stack
  * Bluetooth\_1-1\_005\_TCK on WIDCOMM, Pass 322, Fail 3. (security not tested)

> 2007-07-15
  * Use TCK JSR 82 for tests, fixed major incompatibility problems

# 2.0.0 released 2007-07-05 #

> 2007-06-29
  * OBEX PUT over rfcomm and tcp  (btgoep and tcpobex)
  * Merged J9 MIDP compatibility enhancements suggested by Kobus Grobler
  * bluecove\_ce.dll WIDCOMM Stack on WinCE

> 2007-06-20 version 2.0.0-b2
  * Moved large Winsock discovery buffers to heap instead of stack to improve stability
  * Fixed WIDCOMM Write Flow to pass all tests

> 2007-06-15 version 2.0.0-b1
  * BlueSoleil Stack working Server implementation
  * BlueSoleil Stack client using overlapped I/O
  * intelbth.dll build by VC2005 Configuration "Win32 Release" for Microsoft and BlueSoleil Bluetooth Stack
  * bluecove.dll build by VC6 Configuration "Win32 Release"  for WIDCOMM Bluetooth Stacks
  * FIFO ReceiveBuffer for WIDCOMM Stack

> 2007-06-03

  * WIDCOMM Stack RFCOMM Server implementation

> 2007-05-30

  * WIDCOMM Stack RFCOMM Client implementation stable

> 2007-05-18

  * Initial **BlueSoleil** Stack RFCOMM Client implementation

> 2007-05-15
  * `InputStream.read()` return -1 on gracefully closed Connection

  * Interface `com.intel.bluetooth.BluetoothStack` to support Stacks other than Microsoft winsock

  * Initial **WIDCOMM** Stack RFCOMM Client implementation

  * `LocalDevice.getProperty()` additional properties: `bluecove, bluecove.stack, bluecove.radio.manufacturer, bluecove.radio.version`

  * System property "bluecove.stack" to force Stack, values `widcomm, bluesoleil or winsock`

# 1.2.3 Released  2007-05-11 #

  * acceptAndOpen will update` ServiceRecord` in SDDB

  * Implemented `LocalDevice.updateRecord`

  * `LocalDevice.getProperty("bluecove");`  will return bluecove version

  * implement `set/getDiscoverable` and `getDeviceClass()` function of `LocalDevice`

  * Support IBM J9 JVM

  * Compiled dll for Windows Mobile (WinCE)

  * Binary jar will run on Java 1.1

  * `InputStream.available()` implemented

# 1.2.2 #

  * Fixed `LocalDevice.getLocalDevice().getBluetoothAddress();`

  * dll build for Vista and XP

  * Removed use of unimplemented exception that prevented multiple apps from running.

  * Implement openOutputStream  and openInputStream in Connector

  * Fixes for native Code to avoid JVM Errors


# 1.2.1 #

  * dll-in-the-jar

  * Merged WTK compatibility enhancements

  * Merged avetanaOBEX compatibility fixes by jrincayc

  * Windows CE support.
> > Remco Poortinga has contributed updated support for Windows CE. That has
> > been merged and now I merged some fixes by him.

# 1.2.0 #


> 2006-03-01 Paul T�tterman
  * javax.bluetooth.LocalDevice.getFriendlyName(): Now returns a real name
  * Migrated to Visual C++ 2005 Express Edition
  * Migrated to Subversion, CVS exists only for archival now
  * javax.bluetooth.RemoteDevice.getAddress(): Now returns _remote_ address
  * javax.bluetooth.RemoteDevice.getFriendlyName(): Now returns a real name
  * Reformatted changelog close to GNU standards


# Beta 1.1 #

> 2005-05-15 Denis Labaye
  * Added the method setDeviceServiceClasses(int service\_telephony) in
> > javax.bluetooth.ServiceRecord for compatibility with Benhui example.

# Beginning #


> 2004 James Scott, Eben Upton and Christophe Diot at Intel Research Cambridge

