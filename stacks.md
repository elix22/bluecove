# BlueCove supported stacks #

> On Windows WIDCOMM and BlueSoleil stacks are added in BlueCove version 2.0.0.

> Support for Mac OS X added in BlueCove version 2.0.2.

> Support for Linux BlueZ using historic libbluetooth.so API added in BlueCove version 2.0.3 as additional GPL licensed module.

> Support for Linux BlueZ using modern D-Bus API added in BlueCove version 2.1.1 as additional AL-2.0 module.

> To determine which Bluetooth stack is installed on your Windows XP [see this page](http://www.bluecove.org/bluecove-examples/bluetooth-stack.html)

> [Windows Mobile](http://code.google.com/p/bluecove/wiki/WindowsMobile)

> If automatic Bluetooth Stack detection is not enough Java System property "bluecove.stack" can be used to force desired Stack Initialization.
> Values "widcomm", "bluesoleil" or "winsock". By default winsock is selected if available.

> Another property "bluecove.stack.first" is used optimize stack detection.
> If -Dbluecove.stack.first=widcomm then widcomm (bluecove.dll) stack is loaded first and if not available then BlueCove will switch to winsock.
> By default intelbth.dll is loaded first.

> If multiple stacks are detected they are selected in following order: "winsock", "widcomm", "bluesoleil".
> Since BlueCove v2.0.1 "bluecove.stack.first" will alter the order of stack selection.

> If System property is not an option (e.g. when running in Webstart) create text file "bluecove.stack" or "bluecove.stack.first" containing stack name and add this file to BlueCove or Application jar. (Since v2.0.1)

> Use `LocalDevice.getProperty("bluecove.stack")` to find out what stack is used.

> On Windows XP (Not on Vista) WIDCOMM (before 5.5) and Microsoft can run at the same time and so different instances of BlueCove (since 2.1.0 you need special-build) can use this stacks in parallel.
> If you are adventures users on Vista you can use BlueCove (2.0.x) dll build with BTW-6\_1\_0\_1501-SDK http://snapshot.bluecove.org/special-build/ to have access to WIDCOMM stack

> Since BlueCove 2.1 WIDCOMM and Microsoft on the same computer are not supported, in favor of Broadcom BTW Stack 5.5 and 6.1 support out of the box.
> You may still use bluecove.dll build using BTW-5\_1\_0\_3101-SDK http://snapshot.bluecove.org/special-build/ to have dual stack support on Windows XP.

> Also there is hack I used for testing to have dual stack: unplug Microsoft USB, Make first connection using Broadcom to intialize BTW Stack without MS, plug Microsoft USB = You have two stacks running.

## Broadcom (WIDCOMM) ##

![http://bluecove.googlecode.com/svn/trunk/src/site/resources/images/broadcom_logo.png](http://bluecove.googlecode.com/svn/trunk/src/site/resources/images/broadcom_logo.png)

> This stack is most stable stack from one supported by BlueCove
> Supported RFCOMM and L2CAP (since BlueCove v2.0.1)

### Requirements ###

  * BTW Stack software version 1.4.2.10 SP5 or above on Windows32 (before Vista)
  * BTW Stack 5.5 and 6.1 supported since BlueCove 2.1
  * For AMD64 systems you need to build dll yourself
  * Broadcom wbtapi.dll required. It is installed with WIDCOMM drivers.
  * On Windows Mobile 2003 WIDCOMM BTW-CE stack versions 1.4.1.60 or newer
  * On Windows Mobile 5.0 any working Broadcom's WIDCOMM BTW-CE

### JSR-82 Limitations ###

  * `LocalDevice.setDiscoverable(...)` not supported.
  * `ServiceRecord.setDeviceServiceClasses(..)` not supported.
  * `ServiceRecord` attributes of `DataElement` types URL are discovered as STRING
  * If remote device changed its class DiscoveryListener.deviceDiscovered may report older value when -Dbluecove.inquiry.report\_asap=true
  * Service search requests are executes sequentially. "bluetooth.sd.trans.max=1"
  * The discovery database is cumulative. It contains the results of all previous discoveries of the application or any other applications that are running or that have run. So ServiceSearch may return service even when it is down.
  * L2CAP unable to send empty L2CAP packet
  * WIDCOMM Bag: Do not call service search while device discovery running! The inquiryCompleted will not be called and DeviceInquiryThread will stay in native code until cancelInquiry is called.

  * For BlueCove v2.0.x: `ServiceRecord` attributes of `DataElement` types U\_INT\_8, INT\_8, INT\_16 and UINT\_16 are not properly discovered (fixed in BTW-6\_1\_0\_1501-SDK and stack v 4, get dlls here http://snapshot.bluecove.org/special-build/)
  * For BlueCove v2.0.x: On Vista WIDCOMM stack detected as Microsoft with all the benefits of one. If you need L2CAP support on Vista and Broadcom v6 use [custom bluecove.dll build](http://snapshot.bluecove.org/special-build/)

## Winsock (Microsoft) ##

![http://bluecove.googlecode.com/svn/trunk/src/site/resources/images/win.png](http://bluecove.googlecode.com/svn/trunk/src/site/resources/images/win.png)

### Requirements ###

  * Microsoft Bluetooth stack (currently this means Windows XP SP2 or newer and Windows Mobile 2003 or newer)
  * Broadcom stack v6 on Windows Vista
  * A Bluetooth [device supported](http://support.microsoft.com/default.aspx?kbid=841803) by the Microsoft Bluetooth stack.
  * [List of Bluetooth USB dongles working with Microsoft Bluetooth stack](http://bluecove.wiki.sourceforge.net/ms-usb-dongles)
  * [Windows Mobile Devices and installation instructions](http://code.google.com/p/bluecove/wiki/WindowsMobile)

### Limitations ###

  * Microsoft Bluetooth stack only support RFCOMM connections
  * L2CAP not supported
  * `DiscoveryListener.deviceDiscovered()` would be called for devices that a _paired_ with your Microsoft BT stack regardless if device ON or OFF
  * `LocalDevice.setDiscoverable(NOT_DISCOVERABLE)` will not change the state if discoverable is enabled system wide in "Bluetooth Devices" "Options"
  * ServiceRecord.setDeviceServiceClasses not supported on Windows Mobile

## BlueSoleil (IVT Corporation) ##

![http://bluecove.googlecode.com/svn/trunk/src/site/resources/images/bluesoleil_logo.png](http://bluecove.googlecode.com/svn/trunk/src/site/resources/images/bluesoleil_logo.png)

> We don't recommend the use of this stack for the begginers in JSR-82. There are too many limitations that may be very confusing.
> Also our experience show that you may need to frequently reboot your Windows.

### Requirements ###

  * BlueSoleil version 1.6.0, 2.3 or 3.2.2.8. BlueSoleil version 5.0.5 not supported.

### JSR-82 Limitations ###

> SDK provided by IVT is very limited. We just implemented what we can on top of it.

  * BlueCove on BlueSoleil Bluetooth stack only support RFCOMM and OBEX connections
  * Only RFCOMM services can be created. No OBEX services
  * L2CAP not supported
  * Service attributes are not supported in Service search of when creating Server, `LocalDevice.getProperty("bluetooth.sd.attr.retrievable.max") == "0"`
  * `DiscoveryAgent.searchServices()` can only find 128bits-GUID service with specific UUID. You can't list all service on remote device.
  * `DiscoveryAgent.searchServices(.. UUID[] uuidSet, ..)`  only ONE UUID is used during search, the last one in array.
  * You need to run `DiscoveryAgent.searchServices()` with UUID of your service before you can make connection to it using `Connector.open(url);`
  * Client `Connector.open()` use GUID of the discovered service. Only one would be selected
  * Server can't close incoming connection. It will wait until connection is closed remotely. (Our implementation should be changed to restart Service(Server) each time connection is closed to fix this)
  * `searchServices` can't distinguish the service type (RFCOMM or OBEX). To solve this following UUIDs are always discoved as obex (btgoep://) connections
    * IR\_MC\_SYNC (0x1104), OBEX\_OBJECT\_PUSH (0x1105), OBEX\_FILE\_TRANSFER (0x1106), IR\_MC\_SYNC\_COMMAND (0x1107), IMG\_RESPONDER (0x111B)
  * ServiceRecord.setDeviceServiceClasses not supported
  * authenticate=true and encrypt=true mode not supported when opening connections

**N.B.** In our test lab when using fake ES-388 Bluetooth USB Adapter we can only make from 180 to 250 max connections in sequence. After this BlueSoleil.exe needs to restart or Windows needs to be rebooted.
This does not happen when we used dongle bought from [bluesoleil.com](http://bluesoleil.com)


## OS X ##

![http://bluecove.googlecode.com/svn/trunk/src/site/resources/images/mac_universal.png](http://bluecove.googlecode.com/svn/trunk/src/site/resources/images/mac_universal.png)

### Requirements ###

  * PowerPC- or Intel-based Mac OS X 10.4 (Bluetooth v1.2) or late

### JSR-82 Limitations ###

  * `LocalDevice.setDiscoverable(...)` not supported.
  * Dynamic assignment of PSM for L2CAP service does not work. 1001 is assigned all the time. Use btl2cap://localhost:uuid;name=test;bluecovepsm=1003
  * `ServiceRecord` attributes of `DataElement` types U\_INT\_8, INT\_8 are not properly publshed by service on OS X 10.4
  * `ServiceRecord` attributes of `DataElement` type BOOL published as U\_INT\_1
  * `ServiceRecord` attributes of `DataElement` type URL published as STRING
  * ServiceRecord.setDeviceServiceClasses(..) will work on Mac OS X 10.5 Leo and later.
  * encrypt=true mode not supported when opening connections
  * L2CAP unable to send empty L2CAP packet

## Linux BlueZ (D-Bus BlueZ API) ##

![http://bluecove.googlecode.com/svn/trunk/src/site/resources/images/linux.png](http://bluecove.googlecode.com/svn/trunk/src/site/resources/images/linux.png)

> Support for BlueZ is added in BlueCove version 2.1.1 as additional module `bluecove-bluez`.
> This module JNI binaries are NOT linked with `libbluetooth.so`. We use only socket APIs to connect to D-Bus (AF\_UNIX) and Bluetooth (AF\_BLUETOOTH) sockets.

### Requirements ###

  * BlueCove library of the same mojor version
  * BlueZ version 3.x (3.10 or later) or version 4.x installed on your system
  * On 64-bit Linux platform 64-bit java should be used

### JSR-82 Limitations ###

  * `ServiceRecord.setDeviceServiceClasses(..)` not supported.

### BlueCove API Limitations ###

  * In BlueZ 3.x Service registration with multiple adapters installed would be visible on all Adapters (This is because BlueZ interface org.bluez.Database is not bound to adapter)

## Linux BlueZ (historic BlueZ API) ##

![http://bluecove.googlecode.com/svn/trunk/src/site/resources/images/linux.png](http://bluecove.googlecode.com/svn/trunk/src/site/resources/images/linux.png)

**N.B.** Support for BlueZ is added in BlueCove version 2.0.3 as additional [GNU General Public License](http://www.gnu.org/licenses/gpl.html) module `bluecove-gpl`.

### Requirements ###

  * BlueCove library of the same mojor version
  * Package bluez-libs 3.7 or later installed on your system
  * We linked JNI library with `libbluetooth.so` (Not libbluetooth.so.3 or libbluetooth.so.2) to be able to use same build with Bluez Version 3 and Version 4
    * You need package/rpm that creates a link libbluetooth.so to already installed libbluetooth.so.3 or libbluetooth.so.2
    * `libbluetooth-dev` on Ubuntu
    * `bluez-libs-devel` on Fedora
    * `bluez-devel` on openSUSE
  * To change Discoverable mode of the device you should be root
  * On 64-bit Linux platform 64-bit java should be used

### JSR-82 Limitations ###

  * `LocalDevice.setDiscoverable(...)` supported only when running with superuser privileges.
  * `ServiceRecord.setDeviceServiceClasses(..)` not supported.
  * authenticate and encrypt not implemented.