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

package com.intel.bluetooth;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
/**
 * The <code>DiscoveryAgent</code> class provides methods to perform
 * device and service discovery.  A local device must have only one
 * <code>DiscoveryAgent</code> object.  This object must be retrieved
 * by a call to <code>getDiscoveryAgent()</code> on the
 * <code>LocalDevice</code> object.
 *
 * <H3>Device Discovery</H3>
 *
 * There are two ways to discover devices.  First, an application may
 * use <code>startInquiry()</code> to start an inquiry to find devices
 * in proximity to the local device. Discovered devices are returned
 * via the <code>deviceDiscovered()</code> method of the interface
 * <code>DiscoveryListener</code>.  The second way to
 * discover devices is via the <code>retrieveDevices()</code> method.
 * This method will return devices that have been discovered via a
 * previous inquiry or devices that are classified as pre-known.
 * (Pre-known devices are those devices that are defined in the
 * Bluetooth Control Center as devices this device frequently contacts.)
 * The <code>retrieveDevices()</code> method does not perform an
 * inquiry, but provides a quick way to get a list of devices that may
 * be in the area.
 *
 * <H3>Service Discovery</H3>
 * The <code>DiscoveryAgent</code> class also encapsulates the
 * functionality provided by the service discovery application profile.
 * The class provides an interface for an application to search and
 * retrieve attributes for a particular service.  There are two ways to
 * search for services.  To search for a service on a single device,
 * the <code>searchServices()</code> method should be used.  On the
 * other hand, if you don't care which device a service is on, the
 * <code>selectService()</code> method does a service search on a
 * set of remote devices.
 *
 */
public class DiscoveryAgentImpl extends DiscoveryAgent {

	DiscoveryAgentImpl() {
		super(null);
	}

	/**
	 * Returns an array of Bluetooth devices that have either been found by the
	 * local device during previous inquiry requests or been specified as a
	 * pre-known device depending on the argument. The list of previously found
	 * devices is maintained by the implementation of this API. (In other words,
	 * maintenance of the list of previously found devices is an implementation
	 * detail.) A device can be set as a pre-known device in the Bluetooth
	 * Control Center.
	 * 
	 * @param option  {@link #CACHED} if previously found devices
	 * should be returned; {@link #PREKNOWN} if pre-known devices should be returned
	 * @return an array containing the Bluetooth devices that were previously
	 * found if option is {@link #CACHED}; an array of devices that are pre-known devices
	 * if option is {@link #PREKNOWN}; {@code null} if no devices meet the criteria 
	 * @throws java.lang.IllegalArgumentException - if option is not {@link #CACHED} or
	 * {@link #PREKNOWN}
	 */

	public native RemoteDevice[] retrieveDevices(int option);

	/**
	 * Places the device into inquiry mode. The length of the inquiry is
	 * implementation dependent. This method will search for devices with the
	 * specified inquiry access code. Devices that responded to the inquiry are
	 * returned to the application via the method {@link 
	 * DiscoveryListener#deviceDiscovered(RemoteDevice, DeviceClass) 
	 * deviceDiscovered()} of the interface {@link DiscoveryListener}. The 
	 * {@link #cancelInquiry(DiscoveryListener) cancelInquiry()} method is called 
	 * to stop the inquiry. 
	 * 
	 * @param accessCode the type of inquiry to complete
	 * @param listener  the event listener that will receive device discovery events
	 * @return {@code true} if the inquiry was started; {@code false} if the inquiry was not
	 * started because the accessCode is not supported
	 * @throws java.lang.IllegalArgumentException if the access code provided is not
	 * 			{@link  #LIAC}, {@link #GIAC}, or in the range 0x9E8B00 to 0x9E8B3F
	 * @throws java.lang.NullPointerException  if {@code listener} is {@code null}
	 * @throws BluetoothStateException if the Bluetooth device does not allow
	 * 			an inquiry to be started due to other operations that are being performed
	 * 			by the device 
	 * @see #cancelInquiry(DiscoveryListener)
	 * @see #GIAC
	 * @see #LIAC
	 */

	public native boolean startInquiry(int accessCode, DiscoveryListener listener)
			throws BluetoothStateException;
	/**
	 * Removes the device from inquiry mode. An {@link DiscoveryListener#inquiryCompleted(int)} 
	 * call will occur with a type of {@link DiscoveryListener#INQUIRY_TERMINATED} as a 
	 * result of calling this method. After receiving this event, no further 
	 * {@link DiscoveryListener#deviceDiscovered(RemoteDevice, DeviceClass) deviceDiscovered()}
	 * calls will occur as a result of this inquiry.
	 * <p>
	 * This method will only cancel the inquiry if the listener provided is the
	 * listener that started the inquiry.
	 * 
	 * @param listener - the listener that is receiving inquiry events
	 * @return {@code true} if the inquiry was canceled; otherwise {@code false} if 
	 * the inquiry was not canceled or if the inquiry was not started using {@code listener}
	 * @throws java.lang.NullPointerException - if {@code listener} is {@code null}
	 */


	public native boolean cancelInquiry(DiscoveryListener listener);
	
	/**
	 * Searches for services on a remote Bluetooth device that have the
	 * {@code UUIDs} specified in uuidSet. Once a service is found, the attributes
	 * specified in {@code attrSet} and the default attributes are retrieved. The
	 * default attributes are ServiceRecordHandle (0x0000), ServiceClassIDList
	 * (0x0001), ServiceRecordState (0x0002), ServiceID (0x0003), and
	 * ProtocolDescriptorList (0x0004). If {@code attrSet} is {@code null} then 
	 * only the default attributes will be retrieved. {@code attrSet} does not 
	 * have to be sorted in increasing order, but must only contain values in 
	 * the range [0 - (2<sup>16</sup>-1)]. 
	 * 
	 * @param attrSet indicates the attributes whose values will be retrieved on 
	 * 				services which have the {@code UUIDs} specified in {@code uuidSet}
	 * @param uuidSet the set of {@code UUIDs} that are being searched for; all services
	 * returned will contain all the {@code UUIDs} specified here 
	 * @param device the remote Bluetooth device to search for services on 
	 * @param listener the object that will receive events when services are discovered 
	 * @return the transaction ID of the service search; this number will be positive 
	 * @throws BluetoothStateException - if the number of concurrent service search
	 * transactions exceeds the limit specified by the bluetooth.sd.trans.max
	 * property obtained from the class {@link LocalDevice} or the system is unable to
	 * start one due to current conditions 
	 * @throws java.lang.IllegalArgumentException  if {@code attrSet} has an illegal 
	 * service attribute ID or exceeds the property bluetooth.sd.attr.retrievable.max 
	 * defined in the class {@link LocalDevice}; if {@code attrSet} or {@code uuidSet} is of
	 * length 0; if {@code attrSet} or {@code uuidSet} contains duplicates 
	 * @throws java.lang.NullPointerException - if {@code uuidSet}, {@code btDev}, or 
	 * {@code discListener} is {@code null}; if an element in {@code uuidSet} array is 
	 * {@code null} 
	 * @see DiscoveryListener
	 * @see UUID
	 * @see LocalDevice#getProperty(String)
	 */

	public native int searchServices(int[] attrSet, UUID[] uuidSet,
			RemoteDevice device, DiscoveryListener listener)
			throws BluetoothStateException;


	/**
	 * Cancels the service search transaction that has the specified transaction
	 * ID. The ID was assigned to the transaction by the method
	 * {@link #searchServices(int[], UUID[], RemoteDevice, DiscoveryListener) searchServices()}. 
	 * A {@link DiscoveryListener#serviceSearchCompleted(int, int) serviceSearchCompleted()} call 
	 * with a discovery type of {@link DiscoveryListener#SERVICE_SEARCH_TERMINATED} will occur when 
	 * this method is called. After receiving this event, no further 
	 * {@link DiscoveryListener#servicesDiscovered(int, ServiceRecord[]) servicesDiscovered()}
	 * events will occur as a result of this search. 
	 * 
	 * @param transID - the ID of the service search transaction to cancel; returned by 
	 * {@link #searchServices(int[], UUID[], RemoteDevice, DiscoveryListener) searchServices()}
	 * @return {@code true} if the service search transaction is terminated, else {@code false} 
	 * if the {@code transID} does not represent an active service search transaction
	 */

	public native boolean cancelServiceSearch(int transID);

	/**
	 * Attempts to locate a service that contains {@code uuid} in the ServiceClassIDList
	 * of its service record. This method will return a string that may be used
	 * in {@link javax.microedition.io.Connector#open(String) Connector.open()} to 
	 * establish a connection to the service. How the service is selected if there are 
	 * multiple services with uuid and which devices to search is implementation dependent. 
	 * 
	 * @param uuid the {@link UUID} to search for in the ServiceClassIDList 
	 * @param security specifies the security requirements for a connection to this service; 
	 * must be one of {@link ServiceRecord#NOAUTHENTICATE_NOENCRYPT},
	 * {@link ServiceRecord#AUTHENTICATE_NOENCRYPT}, or
	 * {@link ServiceRecord#AUTHENTICATE_ENCRYPT}
	 * @param master determines if this client must be the master of the connection; {@code true}
	 * if the client must be the master; {@code false} if the client can be the master or the slave
	 * @return the connection string used to connect to the service with a {@link UUID} of 
	 * {@code uuid}; or {@code null} if no service could be found with a {@link UUID} of uuid in the
	 * ServiceClassIDList 
	 * @throws BluetoothStateException  if the Bluetooth system cannot start the request 
	 * due to the current state of the Bluetooth system 
	 * @throws java.lang.NullPointerException - if uuid is {@code null} 
	 * @throws java.lang.IllegalArgumentException 
	 * if security is not {@link ServiceRecord#NOAUTHENTICATE_NOENCRYPT},
	 * {@link ServiceRecord#AUTHENTICATE_NOENCRYPT}, or
	 * {@link ServiceRecord#AUTHENTICATE_ENCRYPT}
	 * @see ServiceRecord#NOAUTHENTICATE_NOENCRYPT
	 * @see ServiceRecord#AUTHENTICATE_NOENCRYPT
	 * @see ServiceRecord#AUTHENTICATE_ENCRYPT
	 */
	
 	public native String selectService(UUID uuid, int security, boolean master)
	  throws BluetoothStateException;
 	
 	
 	
	 
}