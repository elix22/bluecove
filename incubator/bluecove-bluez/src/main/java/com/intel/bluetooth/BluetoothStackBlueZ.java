/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2008 Mark Swanson
 *  Copyright (C) 2008 Vlad Skarzhevskyy
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *
 * @version $Id$
 */
package com.intel.bluetooth;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

import org.bluez.Adapter;
import org.bluez.Manager;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;

import cx.ath.matthew.unix.UnixSocket;

/**
 * Property "bluecove.deviceID" or "bluecove.deviceAddress" can be used to
 * select Local Bluetooth device.
 *
 */
class BluetoothStackBlueZ implements BluetoothStack, DeviceInquiryRunnable, SearchServicesRunnable {

	// Our reusable DBUS connection.
	DBusConnection conn = null;

	// Our reusable default host adapter.
	Adapter defaultAdapter = null;

	// The current Manager.
	Manager manager = null;

	static final int BLUECOVE_DBUS_VERSION = 203;

	private int deviceID;

	private int deviceDescriptor;

	private long localDeviceBTAddress;

	private long sdpSesion;

	private int registeredServicesCount = 0;

	private Map<String, String> propertiesMap;

	private DiscoveryListener discoveryListener;

	// Prevent the device from been discovered twice
	private Vector<RemoteDevice> discoveredDevices;

	private boolean deviceInquiryCanceled = false;

	// Used mainly in Unit Tests
	static {
		NativeLibLoader.isAvailable("unix-java", UnixSocket.class);
		NativeLibLoader.isAvailable(BlueCoveImpl.NATIVE_LIB_BLUEZ, BluetoothStackBlueZ.class);
	}

	BluetoothStackBlueZ() {
	}


	public String getStackID() {
		return BlueCoveImpl.STACK_BLUEZ;
	}

	//public native int getLibraryVersionNative();

	public int getLibraryVersion() throws BluetoothStateException {
		return BLUECOVE_DBUS_VERSION;
	}

	public int detectBluetoothStack() {
		return BlueCoveImpl.BLUECOVE_STACK_DETECT_BLUEZ;
	}

	//private native int nativeGetDeviceID(int id, long findLocalDeviceBTAddress) throws BluetoothStateException;
	private int nativeGetDeviceID(int id, long findLocalDeviceBTAddress) throws BluetoothStateException {
		String localDeviceBTAddress = RemoteDeviceHelper.getBluetoothAddress(findLocalDeviceBTAddress);
			String[] adapters = manager.ListAdapters();
			for (String adapterAddress: adapters) {
				DebugLog.debug(" adapterAddress " + adapterAddress + "matching:" + localDeviceBTAddress);
				if (!adapterAddress.equalsIgnoreCase(localDeviceBTAddress))
					continue;
				try {
					Adapter adapter = (Adapter) conn.getRemoteObject("org.bluez",
						adapterAddress, Adapter.class);
					// hereiam - I have no idea why a deviceID is used .
					// the BlueCoveBlueZ_LocalDevice.cc doesn't seem to map to
					// Java in a way I can understand for the methods:
					// hci_open_dev(), hci_read_bd()
					// Please help me grok how dbus uses deviceID - I think
					// atm that this is an API impedance mismatch and that the
					// deviceID concept is non-existent in DBUS. true?
				} catch (DBusException ex) {
					DebugLog.error("Failed to get adapter with address:" + adapterAddress, ex);
					throw new BluetoothStateException("Failed to get adapter with address:" +
						adapterAddress);
				}
			}
		throw new BluetoothStateException("Bluetooth device not found:" + localDeviceBTAddress);
	}

	//private native int nativeOpenDevice(int deviceID) throws BluetoothStateException;
	private int nativeOpenDevice(int deviceID) throws BluetoothStateException {

		throw new BluetoothStateException("Not supported yet.");
	}

	public void initialize() throws BluetoothStateException {
		try {
			conn = DBusConnection.getConnection(DBusConnection.SYSTEM);
		} catch (DBusException ex) {
			DebugLog.error("initialize() failed to get the dbus connection.", ex);
			throw new BluetoothStateException(ex.getMessage());
		}
		try {
			manager = (Manager) conn.getRemoteObject("org.bluez",
				"/org/bluez", Manager.class);
		} catch (DBusException ex) {
			DebugLog.error("initialize() failed to get the dbus manager.", ex);
			throw new BluetoothStateException(ex.getMessage());
		}
		DebugLog.debug("InterfaceVersion " + manager.InterfaceVersion());

		String defaultAdapterName = manager.DefaultAdapter();
		DebugLog.debug("DefaultAdapter name:" + defaultAdapterName);

		try {
			defaultAdapter = (Adapter) conn.getRemoteObject("org.bluez",
				defaultAdapterName, Adapter.class);
		} catch (DBusException ex) {
			DebugLog.error("initialize() failed to get the dbus adapter.", ex);
			throw new BluetoothStateException(ex.getMessage());
		}
		DebugLog.debug("DefaultAdapter address " + defaultAdapter.GetAddress());
		int findID = -1;
		long findLocalDeviceBTAddress = -1;
		String deviceIDStr = BlueCoveImpl.getConfigProperty("bluecove.deviceID");
		if (deviceIDStr != null) {
			findID = Integer.parseInt(deviceIDStr);
		}
		String deviceAddressStr = BlueCoveImpl.getConfigProperty("bluecove.deviceAddress");
		if (deviceAddressStr != null) {
			findLocalDeviceBTAddress = Long.parseLong(deviceAddressStr, 16);
		}
		deviceID = nativeGetDeviceID(findID, findLocalDeviceBTAddress);
		DebugLog.debug("localDeviceID", deviceID);
		deviceDescriptor = nativeOpenDevice(deviceID);
		localDeviceBTAddress = getLocalDeviceBluetoothAddressImpl(deviceDescriptor);
		propertiesMap = new TreeMap<String, String>();
		propertiesMap.put("bluetooth.api.version", "1.1");
	}

	//private native void nativeCloseDevice(int deviceDescriptor);
	private void nativeCloseDevice(int deviceDescriptor) {
		//conn.getConnection(arg0)
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void destroy() {
		if (sdpSesion != 0) {
			try {
				long s = sdpSesion;
				sdpSesion = 0;
				closeSDPSessionImpl(s, true);
			} catch (ServiceRegistrationException ignore) {
			}
		}
		nativeCloseDevice(deviceDescriptor);
	}

	//public native void enableNativeDebug(Class nativeDebugCallback, boolean on);
	public void enableNativeDebug(Class nativeDebugCallback, boolean on) {

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothStack#isCurrentThreadInterruptedCallback()
	 */
	public boolean isCurrentThreadInterruptedCallback() {
		return Thread.interrupted();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothStack#getFeatureSet()
	 */
	public int getFeatureSet() {
		return FEATURE_SERVICE_ATTRIBUTES | FEATURE_L2CAP;
	}

	// --- LocalDevice

	//private native long getLocalDeviceBluetoothAddressImpl(int deviceDescriptor) throws BluetoothStateException;
	private long getLocalDeviceBluetoothAddressImpl(int deviceDescriptor) throws BluetoothStateException {

		throw new BluetoothStateException("Not supported yet.");
	}

	public String getLocalDeviceBluetoothAddress() throws BluetoothStateException {
		return RemoteDeviceHelper.getBluetoothAddress(getLocalDeviceBluetoothAddressImpl(deviceDescriptor));
	}

	//private native int nativeGetDeviceClass(int deviceDescriptor);
	private int nativeGetDeviceClass(int deviceDescriptor) {

		throw new UnsupportedOperationException("Not supported yet.");
	}

	public DeviceClass getLocalDeviceClass() {
		int record = nativeGetDeviceClass(deviceDescriptor);
		if (record == 0xff000000) {
			// could not be determined
			return null;
		}
		return new DeviceClass(record);
	}

	//private native String nativeGetDeviceName(int deviceDescriptor);
	private String nativeGetDeviceName(int deviceDescriptor) {

		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getLocalDeviceName() {
		return nativeGetDeviceName(deviceDescriptor);
	}

	public boolean isLocalDevicePowerOn() {
		// Have no idea how turn on and off device on BlueZ, as well to how to
		// detect this condition.
		return true;
	}

	public String getLocalDeviceProperty(String property) {
		return (String) propertiesMap.get(property);
	}

	//private native int nativeGetLocalDeviceDiscoverable(int deviceDescriptor);
	private int nativeGetLocalDeviceDiscoverable(int deviceDescriptor) {

		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getLocalDeviceDiscoverable() {
		return nativeGetLocalDeviceDiscoverable(deviceDescriptor);
	}

	//private native int nativeSetLocalDeviceDiscoverable(int deviceDescriptor, int mode);
	private int nativeSetLocalDeviceDiscoverable(int deviceDescriptor, int mode) {

		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		int error = nativeSetLocalDeviceDiscoverable(deviceDescriptor, mode);
		if (error != 0) {
			throw new BluetoothStateException("Unable to change discovery mode. It may be because you aren't root");
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothStack#setLocalDeviceServiceClasses(int)
	 */
	public void setLocalDeviceServiceClasses(int classOfDevice) {
		throw new NotSupportedRuntimeException(getStackID());
	}

	// --- Device Inquiry

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		if (discoveryListener != null) {
			throw new BluetoothStateException("Another inquiry already running");
		}
		discoveryListener = listener;
		discoveredDevices = new Vector();
		deviceInquiryCanceled = false;
		return DeviceInquiryThread.startInquiry(this, accessCode, listener);
	}

	//private native int runDeviceInquiryImpl(DeviceInquiryThread startedNotify, int deviceID, int deviceDescriptor,
			//int accessCode, int inquiryLength, int maxResponses, DiscoveryListener listener)
			//throws BluetoothStateException;
	private int runDeviceInquiryImpl(DeviceInquiryThread startedNotify, int deviceID, int deviceDescriptor,
			int accessCode, int inquiryLength, int maxResponses, DiscoveryListener listener)
			throws BluetoothStateException {

		throw new BluetoothStateException("Not supported yet.");
	}

	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener)
			throws BluetoothStateException {
		try {
			int discType = runDeviceInquiryImpl(startedNotify, deviceID, deviceDescriptor, accessCode, 8, 20, listener);
			if (deviceInquiryCanceled) {
				return DiscoveryListener.INQUIRY_TERMINATED;
			}
			return discType;
		} finally {
			discoveryListener = null;
			discoveredDevices = null;
		}
	}

	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass,
			String deviceName, boolean paired) {
		RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(this, deviceAddr, deviceName, paired);
		if (deviceInquiryCanceled || (discoveryListener == null) || (discoveredDevices == null)
				|| (discoveredDevices.contains(remoteDevice))) {
			return;
		}
		discoveredDevices.addElement(remoteDevice);
		DeviceClass cod = new DeviceClass(deviceClass);
		DebugLog.debug("deviceDiscoveredCallback address", remoteDevice.getBluetoothAddress());
		DebugLog.debug("deviceDiscoveredCallback deviceClass", cod);
		listener.deviceDiscovered(remoteDevice, cod);
	}

	//private native boolean deviceInquiryCancelImpl(int deviceDescriptor);
	private boolean deviceInquiryCancelImpl(int deviceDescriptor) {

		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean cancelInquiry(DiscoveryListener listener) {
		if (discoveryListener != null && discoveryListener == listener) {
			deviceInquiryCanceled = true;
			return deviceInquiryCancelImpl(deviceDescriptor);
		}
		return false;
	}

	//private native String getRemoteDeviceFriendlyNameImpl(int deviceDescriptor, long remoteAddress) throws IOException;
	private String getRemoteDeviceFriendlyNameImpl(int deviceDescriptor, long remoteAddress) throws IOException {

		throw new IOException("Not supported yet.");
	}

	public String getRemoteDeviceFriendlyName(long address) throws IOException {
		return getRemoteDeviceFriendlyNameImpl(deviceDescriptor, address);
	}

	// --- Service search

	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener)
			throws BluetoothStateException {
		return SearchServicesThread.startSearchServices(this, attrSet, uuidSet, device, listener);
	}

	//private native int runSearchServicesImpl(SearchServicesThread sst, long localDeviceBTAddress, byte[][] uuidValues,
			//long remoteDeviceAddress) throws SearchServicesException;
	private int runSearchServicesImpl(SearchServicesThread sst, long localDeviceBTAddress, byte[][] uuidValues,
			long remoteDeviceAddress) throws SearchServicesException {

		throw new SearchServicesException("Not supported yet.");
	}

	public int runSearchServices(SearchServicesThread sst, int[] attrSet, UUID[] uuidSet, RemoteDevice device,
			DiscoveryListener listener) throws BluetoothStateException {
		sst.searchServicesStartedCallback();
		try {
			byte[][] uuidValues = new byte[uuidSet.length][];
			for (int i = 0; i < uuidSet.length; i++) {
				uuidValues[i] = Utils.UUIDToByteArray(uuidSet[i]);
			}
			int respCode = runSearchServicesImpl(sst, this.localDeviceBTAddress, uuidValues, RemoteDeviceHelper
					.getAddress(device));
			if ((respCode != DiscoveryListener.SERVICE_SEARCH_ERROR) && (sst.isTerminated())) {
				return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
			} else if (respCode == DiscoveryListener.SERVICE_SEARCH_COMPLETED) {
				Vector records = sst.getServicesRecords();
				if (records.size() != 0) {
					DebugLog.debug("SearchServices finished", sst.getTransID());
					ServiceRecord[] servRecordArray = (ServiceRecord[]) Utils.vector2toArray(records,
							new ServiceRecord[records.size()]);
					listener.servicesDiscovered(sst.getTransID(), servRecordArray);
				}
				if (records.size() != 0) {
					return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
				} else {
					return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
				}
			} else {
				return respCode;
			}
		} catch (SearchServicesDeviceNotReachableException e) {
			return DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE;
		} catch (SearchServicesTerminatedException e) {
			return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
		} catch (SearchServicesException e) {
			return DiscoveryListener.SERVICE_SEARCH_ERROR;
		}
	}

	public boolean serviceDiscoveredCallback(SearchServicesThread sst, long sdpSession, long handle) {
		if (sst.isTerminated()) {
			return true;
		}
		ServiceRecordImpl servRecord = new ServiceRecordImpl(this, sst.getDevice(), handle);
		int[] attrIDs = sst.getAttrSet();
		long remoteDeviceAddress = RemoteDeviceHelper.getAddress(sst.getDevice());
		populateServiceRecordAttributeValuesImpl(this.localDeviceBTAddress, remoteDeviceAddress, sdpSession, handle,
				attrIDs, servRecord);
		sst.addServicesRecords(servRecord);
		return false;
	}

	public boolean cancelServiceSearch(int transID) {
		SearchServicesThread sst = SearchServicesThread.getServiceSearchThread(transID);
		if (sst != null) {
			return sst.setTerminated();
		} else {
			return false;
		}
	}

	//private native boolean populateServiceRecordAttributeValuesImpl(long localDeviceBTAddress,
			//long remoteDeviceAddress, long sdpSession, long handle, int[] attrIDs, ServiceRecordImpl serviceRecord);
	private boolean populateServiceRecordAttributeValuesImpl(long localDeviceBTAddress,
		long remoteDeviceAddress, long sdpSession, long handle, int[] attrIDs, ServiceRecordImpl serviceRecord) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs)
			throws IOException {
		long remoteDeviceAddress = RemoteDeviceHelper.getAddress(serviceRecord.getHostDevice());
		return populateServiceRecordAttributeValuesImpl(this.localDeviceBTAddress, remoteDeviceAddress, 0,
				serviceRecord.getHandle(), attrIDs, serviceRecord);
	}

	// --- SDP Server

	//private native long openSDPSessionImpl() throws ServiceRegistrationException;
	private long openSDPSessionImpl() throws ServiceRegistrationException {
		throw new ServiceRegistrationException("Not supported yet.");
	}

	private synchronized long getSDPSession() throws ServiceRegistrationException {
		if (this.sdpSesion == 0) {
			sdpSesion = openSDPSessionImpl();
			DebugLog.debug("created SDPSession", sdpSesion);
		}
		return sdpSesion;
	}

	//private native void closeSDPSessionImpl(long sdpSesion, boolean quietly) throws ServiceRegistrationException;
	private void closeSDPSessionImpl(long sdpSesion, boolean quietly) throws ServiceRegistrationException {

		throw new ServiceRegistrationException("Not supported yet.");
	}

	//private native long registerSDPServiceImpl(long sdpSesion, long localDeviceBTAddress, byte[] record)
		//throws ServiceRegistrationException;
	private long registerSDPServiceImpl(long sdpSesion, long localDeviceBTAddress, byte[] record)
		throws ServiceRegistrationException {

		throw new ServiceRegistrationException("Not supported yet.");
	}

	//private native void updateSDPServiceImpl(long sdpSesion, long localDeviceBTAddress, long handle, byte[] record)
		//throws ServiceRegistrationException;
	private void updateSDPServiceImpl(long sdpSesion, long localDeviceBTAddress, long handle, byte[] record)
		throws ServiceRegistrationException {

		throw new ServiceRegistrationException("Not supported yet.");
	}

	//private native void unregisterSDPServiceImpl(long sdpSesion, long localDeviceBTAddress, long handle, byte[] record)
		//throws ServiceRegistrationException;
	private void unregisterSDPServiceImpl(long sdpSesion, long localDeviceBTAddress, long handle, byte[] record)
		throws ServiceRegistrationException {

		throw new ServiceRegistrationException("Not supported yet.");
	}

	private byte[] getSDPBinary(ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
		byte[] blob;
		try {
			blob = serviceRecord.toByteArray();
		} catch (IOException e) {
			throw new ServiceRegistrationException(e.toString());
		}
		return blob;
	}

	private synchronized void registerSDPRecord(ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
		long handle = registerSDPServiceImpl(getSDPSession(), this.localDeviceBTAddress, getSDPBinary(serviceRecord));
		serviceRecord.setHandle(handle);
		serviceRecord.populateAttributeValue(BluetoothConsts.ServiceRecordHandle, new DataElement(DataElement.U_INT_4,
				handle));
		registeredServicesCount++;
	}

	private void updateSDPRecord(ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
		updateSDPServiceImpl(getSDPSession(), this.localDeviceBTAddress, serviceRecord.getHandle(),
				getSDPBinary(serviceRecord));
	}

	private synchronized void unregisterSDPRecord(ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
		try {
			unregisterSDPServiceImpl(getSDPSession(), this.localDeviceBTAddress, serviceRecord.getHandle(),
					getSDPBinary(serviceRecord));
		} finally {
			registeredServicesCount--;
			if (registeredServicesCount <= 0) {
				registeredServicesCount = 0;
				DebugLog.debug("closeSDPSession", sdpSesion);
				long s = sdpSesion;
				sdpSesion = 0;
				closeSDPSessionImpl(s, false);
			}
		}
	}

	// --- Client RFCOMM connections

	//private native long connectionRfOpenClientConnectionImpl(long localDeviceBTAddress, long address, int channel,
			//boolean authenticate, boolean encrypt, int timeout) throws IOException;
	private long connectionRfOpenClientConnectionImpl(long localDeviceBTAddress, long address, int channel,
		boolean authenticate, boolean encrypt, int timeout) throws IOException {
		throw new IOException("Not supported yet.");
	}

	public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException {
		return connectionRfOpenClientConnectionImpl(localDeviceBTAddress, params.address, params.channel,
				params.authenticate, params.encrypt, params.timeout);
	}

	//public native void connectionRfCloseClientConnection(long handle) throws IOException;
	public void connectionRfCloseClientConnection(long handle) throws IOException {
		throw new IOException("Not supported yet.");
	}

	//public native int rfGetSecurityOptImpl(long handle) throws IOException;
	public int rfGetSecurityOptImpl(long handle) throws IOException {
		throw new IOException("Not supported yet.");
	}

	public int rfGetSecurityOpt(long handle, int expected) throws IOException {
		return rfGetSecurityOptImpl(handle);
	}

	//private native long rfServerOpenImpl(long localDeviceBTAddress, boolean authorize, boolean authenticate,
			//boolean encrypt, boolean master, boolean timeouts, int backlog) throws IOException;
	private long rfServerOpenImpl(long localDeviceBTAddress, boolean authorize, boolean authenticate,
		boolean encrypt, boolean master, boolean timeouts, int backlog) throws IOException {
		throw new IOException("Not supported yet.");
	}

	//private native int rfServerGetChannelIDImpl(long handle) throws IOException;
	private int rfServerGetChannelIDImpl(long handle) throws IOException {
		throw new IOException("Not supported yet.");
	}

	public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord)
			throws IOException {
		final int listen_backlog = 1;
		long socket = rfServerOpenImpl(this.localDeviceBTAddress, params.authorize, params.authenticate,
				params.encrypt, params.master, params.timeouts, listen_backlog);
		boolean success = false;
		try {
			int channel = rfServerGetChannelIDImpl(socket);
			serviceRecord.populateRFCOMMAttributes(0, channel, params.uuid, params.name, params.obex);
			registerSDPRecord(serviceRecord);
			success = true;
			return socket;
		} finally {
			if (!success) {
				rfServerCloseImpl(socket, true);
			}
		}
	}

	//private native void rfServerCloseImpl(long handle, boolean quietly) throws IOException;
	private void rfServerCloseImpl(long handle, boolean quietly) throws IOException {
		throw new IOException("Not supported yet.");
	}

	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		try {
			unregisterSDPRecord(serviceRecord);
		} finally {
			rfServerCloseImpl(handle, false);
		}
	}

	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		updateSDPRecord(serviceRecord);
	}

	//public native long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException;
	public long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException {
		throw new IOException("Not supported yet.");
	}

	public void connectionRfCloseServerConnection(long clientHandle) throws IOException {
		connectionRfCloseClientConnection(clientHandle);
	}

	// --- Shared Client and Server RFCOMM connections

	//public native int connectionRfRead(long handle) throws IOException;
	public int connectionRfRead(long handle) throws IOException {

		throw new IOException("Not supported yet.");
	}

	//public native int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;
	public int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException {

		throw new IOException("Not supported yet.");
	}

	//public native int connectionRfReadAvailable(long handle) throws IOException;
	public int connectionRfReadAvailable(long handle) throws IOException {

		throw new IOException("Not supported yet.");
	}

	//public native void connectionRfWrite(long handle, int b) throws IOException;
	public void connectionRfWrite(long handle, int b) throws IOException {

		throw new IOException("Not supported yet.");
	}

	//public native void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException;
	public void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException {

		throw new IOException("Not supported yet.");
	}

	//public native void connectionRfFlush(long handle) throws IOException;
	public void connectionRfFlush(long handle) throws IOException {

		throw new IOException("Not supported yet.");
	}

	//public native long getConnectionRfRemoteAddress(long handle) throws IOException;
	public long getConnectionRfRemoteAddress(long handle) throws IOException {

		throw new IOException("Not supported yet.");
	}

	// --- Client and Server L2CAP connections

	//private native long l2OpenClientConnectionImpl(long localDeviceBTAddress, long address, int channel,
			//boolean authenticate, boolean encrypt, int receiveMTU, int transmitMTU, int timeout) throws IOException;
	private long l2OpenClientConnectionImpl(long localDeviceBTAddress, long address, int channel,
		boolean authenticate, boolean encrypt, int receiveMTU, int transmitMTU, int timeout) throws IOException {

		throw new IOException("Not supported yet.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothStack#l2OpenClientConnection(com.intel.bluetooth.BluetoothConnectionParams,
	 *      int, int)
	 */
	public long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU)
			throws IOException {
		return l2OpenClientConnectionImpl(this.localDeviceBTAddress, params.address, params.channel,
				params.authenticate, params.encrypt, receiveMTU, transmitMTU, params.timeout);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseClientConnection(long)
	 */
	//public native void l2CloseClientConnection(long handle) throws IOException;
	public void l2CloseClientConnection(long handle) throws IOException {

		throw new IOException("Not supported yet.");
	}

	//private native long l2ServerOpenImpl(long localDeviceBTAddress, boolean authorize, boolean authenticate,
			//boolean encrypt, boolean master, boolean timeouts, int backlog, int receiveMTU, int transmitMTU)
			//throws IOException;
	private long l2ServerOpenImpl(long localDeviceBTAddress, boolean authorize, boolean authenticate,
			boolean encrypt, boolean master, boolean timeouts, int backlog, int receiveMTU, int transmitMTU)
			throws IOException {

		throw new IOException("Not supported yet.");
	}

	//public native int l2ServerGetPSMImpl(long handle) throws IOException;
	public int l2ServerGetPSMImpl(long handle) throws IOException {

		throw new IOException("Not supported yet.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerOpen(com.intel.bluetooth.BluetoothConnectionNotifierParams,
	 *      int, int, com.intel.bluetooth.ServiceRecordImpl)
	 */
	public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU,
			ServiceRecordImpl serviceRecord) throws IOException {
		final int listen_backlog = 1;
		long socket = l2ServerOpenImpl(this.localDeviceBTAddress, params.authorize, params.authenticate,
				params.encrypt, params.master, params.timeouts, listen_backlog, receiveMTU, transmitMTU);
		boolean success = false;
		try {
			int channel = l2ServerGetPSMImpl(socket);
			serviceRecord.populateL2CAPAttributes(0, channel, params.uuid, params.name);
			registerSDPRecord(serviceRecord);
			success = true;
			return socket;
		} finally {
			if (!success) {
				l2ServerCloseImpl(socket, true);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerUpdateServiceRecord(long,
	 *      com.intel.bluetooth.ServiceRecordImpl, boolean)
	 */
	public void l2ServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		updateSDPRecord(serviceRecord);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerAcceptAndOpenServerConnection(long)
	 */
	//public native long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException;
	public long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException {

		throw new IOException("Not supported yet.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseServerConnection(long)
	 */
	public void l2CloseServerConnection(long handle) throws IOException {
		l2CloseClientConnection(handle);
	}

	//private native void l2ServerCloseImpl(long handle, boolean quietly) throws IOException;
	private void l2ServerCloseImpl(long handle, boolean quietly) throws IOException {

		throw new IOException("Not supported yet.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerClose(long,
	 *      com.intel.bluetooth.ServiceRecordImpl)
	 */
	public void l2ServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		try {
			unregisterSDPRecord(serviceRecord);
		} finally {
			l2ServerCloseImpl(handle, false);
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothStack#l2Ready(long)
	 */
	//public native boolean l2Ready(long handle) throws IOException;
	public boolean l2Ready(long handle) throws IOException {

		throw new IOException("Not supported yet.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothStack#l2receive(long, byte[])
	 */
	//public native int l2Receive(long handle, byte[] inBuf) throws IOException;
	public int l2Receive(long handle, byte[] inBuf) throws IOException {

		throw new IOException("Not supported yet.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothStack#l2send(long, byte[])
	 */
	//public native void l2Send(long handle, byte[] data) throws IOException;
	public void l2Send(long handle, byte[] data) throws IOException {

		throw new IOException("Not supported yet.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothStack#l2GetReceiveMTU(long)
	 */
	//public native int l2GetReceiveMTU(long handle) throws IOException;
	public int l2GetReceiveMTU(long handle) throws IOException {

		throw new IOException("Not supported yet.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothStack#l2GetTransmitMTU(long)
	 */
	//public native int l2GetTransmitMTU(long handle) throws IOException;
	public int l2GetTransmitMTU(long handle) throws IOException {

		throw new IOException("Not supported yet.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothStack#l2RemoteAddress(long)
	 */
	//public native long l2RemoteAddress(long handle) throws IOException;
	public long l2RemoteAddress(long handle) throws IOException {

		throw new IOException("Not supported yet.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothStack#l2GetSecurityOpt(long, int)
	 */
	//public native int l2GetSecurityOpt(long handle, int expected) throws IOException;
	public int l2GetSecurityOpt(long handle, int expected) throws IOException {

		throw new IOException("Not supported yet.");
	}

}