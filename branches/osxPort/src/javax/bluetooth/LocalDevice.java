/*
 Copyright 2004 Intel Corporation

 This file is part of Blue Cove.

 Blue Cove is free software; you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation; either version 2.1 of the License, or
 (at your option) any later version.

 Blue Cove is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with Blue Cove; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package javax.bluetooth;

import javax.microedition.io.Connection;

import com.intel.bluetooth.BluetoothPeer;
/**
 * The LocalDevice class defines the basic functions of the Bluetooth manager. 
 * The Bluetooth manager provides the lowest level of interface possible into 
 * the Bluetooth stack. It provides access to and control of the local Bluetooth
 * device.
 * <p>
 * This class produces a singleton object.
 *
 */
public class LocalDevice {
	
	private static LocalDevice localDevice;

	private BluetoothPeer bluetoothPeer;

	private DiscoveryAgent discoveryAgent;

	private String address;

	private long bluetoothAddress;

	private LocalDevice() {
		bluetoothPeer = new BluetoothPeer();

		discoveryAgent = new DiscoveryAgent();

		bluetoothAddress = getLocalAddress();
		
		address = Long.toHexString(bluetoothAddress);

		address = "000000000000".substring(address.length()) + address;
	}

	private native long getLocalAddress();
	
	public BluetoothPeer getBluetoothPeer() {
		return bluetoothPeer;
	}

	/**
	 * Retrieves the {@code LocalDevice} object for the local Bluetooth device. Multiple
	 * calls to this method will return the same object. This method will never
	 * return {@code null}. 
	 * 
	 * @return an object that represents the local Bluetooth device 
	 * @throws BluetoothStateException  if the Bluetooth system could
	 * not be initialized
	 */

	public static LocalDevice getLocalDevice() throws BluetoothStateException {
		if (localDevice == null)
			localDevice = new LocalDevice();

		return localDevice;
	}

	/**
	 * Returns the discovery agent for this device. Multiple calls to this
	 * method will return the same object. This method will never return {@code null}.
	 * 
	 * @return the {@link DiscoveryAgent} for the local device
	 */

	public DiscoveryAgent getDiscoveryAgent() {
		return discoveryAgent;
	}

	/**
	 * Retrieves the name of the local device. The Bluetooth specification calls
	 * this name the "Bluetooth device name" or the "user-friendly name".
	 * 
	 * @return the name of the local device; {@code null} if the name could not be
	 * retrieved
	 */

	public String getFriendlyName() {
		return bluetoothPeer.getradioname(bluetoothAddress);
	}

	/**
	 * Retrieves the {@link DeviceClass} object that represents the service classes,
	 * major device class, and minor device class of the local device. This
	 * method will return {@code null} if the service classes, major device class, or
	 * minor device class could not be determined. 
	 * 
	 * @return the service classes, major device class, and minor device class of the 
	 * local device, or {@code null} if the service classes, major device class or 
	 * minor device class could not be determined
	 */

	public DeviceClass getDeviceClass() {
		return null;
	}

	/**
	 * Sets the discoverable mode of the device. The mode may be any number in
	 * the range 0x9E8B00 to 0x9E8B3F as defined by the Bluetooth Assigned
	 * Numbers Document. When this specification was defined, only GIAC
	 * ({@link DiscoveryAgent#GIAC}) and LIAC ({@link DiscoveryAgent#LIAC}) were defined, but
	 * Bluetooth profiles may add additional access codes in the future. To
	 * determine what values may be used, check the Bluetooth Assigned Numbers
	 * document at <a href="http://www.bluetooth.org/assigned-numbers/baseband.htm">
	 * http://www.bluetooth.org/assigned-numbers/baseband.htm</a>. If
	 * {@link DiscoveryAgent#GIAC} or {@link DiscoveryAgent#LIAC} are provided, then this method
	 * will attempt to put the device into general or limited discoverable mode,
	 * respectively. To take a device out of discoverable mode, provide the
	 * {@link DiscoveryAgent#NOT_DISCOVERABLE} flag. The BCC decides if the request will
	 * be granted. In addition to the BCC, the Bluetooth system could effect the
	 * discoverability of a device. According to the Bluetooth Specification, a
	 * device should only be limited discoverable (DiscoveryAgent.LIAC) for 1
	 * minute. This is handled by the implementation of the API. After the
	 * minute is up, the device will revert back to the previous discoverable
	 * mode.
	 * 
	 * @param mode the mode the device should be in; valid modes are
	 * {@link DiscoveryAgent#GIAC}, {@link DiscoveryAgent#LIAC}, 
	 * {@link DiscoveryAgent#NOT_DISCOVERABLE}
	 * and any value in the range 0x9E8B00 to 0x9E8B3F 
	 * @return {@code true} if the
	 * request succeeded, otherwise {@code false} if the request failed because the BCC
	 * denied the request; {@code false} if the Bluetooth system does not support the
	 * access mode specified in mode 
	 * @throws java.lang.IllegalArgumentException  if the
	 * mode is not {@link DiscoveryAgent#GIAC}, {@link DiscoveryAgent#LIAC}, 
	 * {@link DiscoveryAgent#NOT_DISCOVERABLE}, or in the range 0x9E8B00 to 0x9E8B3F
	 * @throws BluetoothStateException  if the Bluetooth system is in a state that does
	 * not allow the discoverable mode to be changed 
	 * @see DiscoveryAgent#GIAC
	 * @see DiscoveryAgent#LIAC
	 * @see DiscoveryAgent#NOT_DISCOVERABLE
	 */

	public boolean setDiscoverable(int mode) throws BluetoothStateException {
		return false;
	}

	/**
	 * Retrieves Bluetooth system properties. The following properties must be
	 * supported, but additional values are allowed: 
	 * <table><tr><th>Property Name</th><th>Description</th></tr>
	 * <tr><td>bluetooth.api.version The version of the Java API for Bluetooth wireless
	 * technology that is supported. For this version it will be set to "1.0".</td></tr>
	 * <tr><td>bluetooth.master.switch</td><td>Is master/slave switch allowed? Valid values are
	 * either "true" or "false". </td></tr>
	 * <tr><td>bluetooth.sd.attr.retrievable.max</td><td>Maximum
	 * number of service attributes to be retrieved per service record. The
	 * string will be in Base 10 digits. </td></tr>
	 * <tr><td>bluetooth.connected.devices.max</td><td>The
	 * maximum number of connected devices supported. This number may be greater
	 * than 7 if the implementation handles parked connections. The string will
	 * be in Base 10 digits. </td></tr>
	 * <tr><td>bluetooth.l2cap.receiveMTU.max</td><td>The maximum
	 * ReceiveMTU size in bytes supported in L2CAP. The string will be in Base
	 * 10 digits, e.g. "32". </td></tr>
	 * <tr><td>bluetooth.sd.trans.max</td><td>Maximum number of concurrent
	 * service discovery transactions. The string will be in Base 10 digits.
	 * bluetooth.connected.inquiry.scan Is Inquiry scanning allowed during
	 * connection? Valid values are either "true" or "false".</td></tr>
	 * <tr><td>bluetooth.connected.page.scan</td><td>Is Page scanning allowed during connection?
	 * Valid values are either "true" or "false". </td></tr>
	 * <tr><td>bluetooth.connected.inquiry</td><td>Is
	 * Inquiry allowed during a connection? Valid values are either "true" or
	 * "false". </td></tr>
	 * <tr><td>bluetooth.connected.page</td><td>Is paging allowed during a connection?
	 * In other words, can a connection be established to one device if it is
	 * already connected to another device. Valid values are either "true" or
	 * "false".</td></tr></table>
	 * 
	 * @param property the property to retrieve as defined in this class.
	 * @return the value of the property specified; {@code null} if the property is not
	 * defined
	 */

	public static String getProperty(String property) {
		String		fullPropName = "javax." + property;
		String		propValue;
		
		propValue = System.getProperty(fullPropName);
		
		if(propValue == null) {
			// the native library may not have been able to change the system
			// properties due to a security manager rejecting it so we check
			// its copy as a backup
			try {
				return getLocalDevice().bluetoothPeer.getAdjustedSystemProperties().getProperty(fullPropName);
			} catch (Exception exp) {
				return null;
			}
		} else return propValue;
	}

	/**
	 * Retrieves the local device's discoverable mode. The return value will be
	 * {@link DiscoveryAgent#GIAC}, {@link DiscoveryAgent#LIAC},
	 * {@link DiscoveryAgent#NOT_DISCOVERABLE}, or a value in the range 0x9E8B00 to
	 * 0x9E8B3F. 
	 * 
	 * @return the discoverable mode the device is presently in 
	 * @see DiscoveryAgent#GIAC
	 * @see DiscoveryAgent#LIAC
	 * @see DiscoveryAgent#NOT_DISCOVERABLE
	 */

	public int getDiscoverable() {
		return DiscoveryAgent.NOT_DISCOVERABLE;
	}

	/**
	 * Retrieves the Bluetooth address of the local device. The Bluetooth
	 * address will never be {@code null}. The Bluetooth address will be 12 characters
	 * long. Valid characters are 0-9 and A-F. 
	 * 
	 * @return the Bluetooth address of the local device
	 */

	public String getBluetoothAddress() {
		return address;
	}

	/**
	 * Gets the service record corresponding to a btspp, btl2cap, or btgoep
	 * notifier. In the case of a run-before-connect service, the service record
	 * returned by {@link #getRecord(Connection)} was created by the same call to
	 * {@link  javax.microedition.io.Connector#open(String, int, boolean)}
	 * that created the notifier. If a connect-anytime server application does
	 * not already have a service record in the SDDB, either because a service
	 * record for this service was never added to the SDDB or because the
	 * service record was added and then removed, then the {@link ServiceRecord}
	 * returned by {@link #getRecord(Connection)} was created by the same call to
	 * {@link  javax.microedition.io.Connector#open(String, int, boolean)} 
	 * that created the notifier.
	 * 
	 * In the case of a connect-anytime service, there may be a service record
	 * in the SDDB corresponding to this service prior to application startup.
	 * In this case, the {@link #getRecord(Connection)} method must return a 
	 * {@link ServiceRecord} whose
	 * contents match those of the corresponding service record in the SDDB. If
	 * a connect-anytime server application made changes previously to its
	 * service record in the SDDB (for example, during a previous execution of
	 * the server), and that service record is still in the SDDB, then those
	 * changes must be reflected in the {@link ServiceRecord} returned by 
	 * {@link #getRecord(Connection)}.
	 * 
	 * Two invocations of this method with the same notifier argument return
	 * objects that describe the same service attributes, but the return values
	 * may be different object references.
	 * 
	 * @param notifier a connection that waits for clients to connect to
	 * a Bluetooth service 
	 * @return the ServiceRecord associated with notifier
	 * @throws java.lang.IllegalArgumentException if notifier is closed, or if notifier
	 * does not implement one of the following interfaces:
	 * {@link javax.microedition.io.StreamConnectionNotifier},
	 * {@link L2CAPConnectionNotifier}, or {@link javax.obex.SessionNotifier}.
	 * This exception is also thrown if notifier is not a Bluetooth notifier,
	 * e.g., a {@link javax.microedition.io.StreamConnectionNotifier} created with a scheme other than btspp.
	 * @throws java.lang.NullPointerException if notifier is {@code null}
	 */
/*
	public ServiceRecord getRecord(Connection notifier) {
		if (notifier == null)
			throw new NullPointerException();

		if (!(notifier instanceof BluetoothStreamConnectionNotifier))
			throw new IllegalArgumentException();

		return ((BluetoothStreamConnectionNotifier) notifier)
				.getServiceRecord();
	}
*/
	/**
	 * Updates the service record in the local SDDB that corresponds to the
	 * {@link ServiceRecord} parameter. Updating is possible only if {@code srvRecord} was
	 * obtained using the {@link #getRecord(Connection)} method. The service record in the SDDB is
	 * modified to have the same service attributes with the same contents as
	 * srvRecord. If srvRecord was obtained from the SDDB of a remote device
	 * using the service search methods, updating is not possible and this
	 * method will throw an {@link java.lang.IllegalArgumentException}.
	 * 
	 * If the {@code srvRecord} parameter is a {@code btspp} service record, then before the
	 * SDDB is changed the following checks are performed. If any of these
	 * checks fail, then an {@link java.lang.IllegalArgumentException} is thrown.
	 * <ul>
	 * <li>ServiceClassIDList and ProtocolDescriptorList, the mandatory service
	 * attributes for a {@code btspp} service record, must be present in srvRecord.</li>
	 * <li>L2CAP and RFCOMM must be in the ProtocolDescriptorList.</li>
	 * <li> srvRecord must
	 * not have changed the RFCOMM server channel number from the channel number
	 * that is currently in the SDDB version of this service record.</li></ul> 
	 * If the
	 * srvRecord parameter is a btl2cap service record, then before the SDDB is
	 * changed the following checks are performed. If any of these checks fail,
	 * then an IllegalArgumentException is thrown.
	 * 
	 * <ul>
	 * <li>ServiceClassIDList and ProtocolDescriptorList, the mandatory service
	 * attributes for a btl2cap service record, must be present in srvRecord.</li>
	 * <li>L2CAP must be in the ProtocolDescriptorList.</li>
	 * <li>srvRecord must not have
	 * changed the PSM value from the PSM value that is currently in the SDDB
	 * version of this service record.</li></ul>
	 * 
	 * If the srvRecord parameter is a {@code btgoep}
	 * service record, then before the SDDB is changed the following checks are
	 * performed. If any of these checks fail, then an {@link java.lang.IllegalArgumentException}
	 * is thrown.
	 * 
	 * <ul><li>ServiceClassIDList and ProtocolDescriptorList, the mandatory service
	 * attributes for a {@code btgoep} service record, must be present in srvRecord.</li>
	 * <li>L2CAP, RFCOMM and OBEX must all be in the ProtocolDescriptorList.</li>
	 * <li>srvRecord must not have changed the RFCOMM server channel number from the
	 * channel number that is currently in the SDDB version of this service
	 * record.</li></ul>
	 * {@code updateRecord()} is not required to ensure that {@code srvRecord} is a
	 * completely valid service record. It is the responsibility of the
	 * application to ensure that {@code srvRecord} follows all of the applicable
	 * syntactic and semantic rules for service record correctness.
	 * 
	 * If there is currently no SDDB version of the {@code srvRecord} service record,
	 * then this method will do nothing.
	 * 
	 * @param srvRecord  the new contents to use for the service record in
	 * the SDDB 
	 * @throws java.lang.NullPointerException  if {@code srvRecord} is {@code null}
	 * @throws java.lang.IllegalArgumentException if the structure of the srvRecord is missing
	 * any mandatory service attributes, or if an attempt has been made to
	 * change any of the values described as fixed. 
	 * @throws ServiceRegistrationException 
	 * if the local SDDB could not be updated successfully due to insufficient
	 * disk space, database locks, etc.
	 */
	
	 public void updateRecord(ServiceRecord srvRecord) throws
	  ServiceRegistrationException { }
	 
}