/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
 *  @version $Id$
 */
package com.intel.bluetooth;

import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

class BluetoothEmulator implements BluetoothStack {

	static final int NATIVE_LIBRARY_VERSION = BlueCoveImpl.nativeLibraryVersionExpected;

	private EmulatorLocalDevice localDevice;

	private EmulatorDeviceInquiry deviceInquiry;

	BluetoothEmulator() {
	}

	// --- Library initialization

	public String getStackID() {
		return BlueCoveImpl.STACK_EMULATOR;
	}

	public int getLibraryVersion() throws BluetoothStateException {
		return NATIVE_LIBRARY_VERSION;
	}

	public int detectBluetoothStack() {
		return BlueCoveImpl.BLUECOVE_STACK_DETECT_EMULATOR;
	}

	public void initialize() throws BluetoothStateException {
		localDevice = EmulatorHelper.createNewLocalDevice();
	}

	public void destroy() {
		EmulatorHelper.releaseDevice(localDevice);
	}

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
		return FEATURE_SET_DEVICE_SERVICE_CLASSES | FEATURE_SERVICE_ATTRIBUTES | FEATURE_L2CAP;
	}

	// --- LocalDevice

	public String getLocalDeviceBluetoothAddress() throws BluetoothStateException {
		return RemoteDeviceHelper.getBluetoothAddress(localDevice.getAddress());
	}

	public DeviceClass getLocalDeviceClass() {
		return new DeviceClass(localDevice.getDeviceClass());
	}

	public String getLocalDeviceName() {
		return localDevice.getName();
	}

	public boolean isLocalDevicePowerOn() {
		return localDevice.isLocalDevicePowerOn();
	}

	public String getLocalDeviceProperty(String property) {
		return localDevice.getLocalDeviceProperty(property);
	}

	public int getLocalDeviceDiscoverable() {
		return localDevice.getLocalDeviceDiscoverable();
	}

	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		return localDevice.setLocalDeviceDiscoverable(mode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#setLocalDeviceServiceClasses(int)
	 */
	public void setLocalDeviceServiceClasses(int classOfDevice) {
		localDevice.setLocalDeviceServiceClasses(classOfDevice);
	}

	// --- Device Inquiry

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		if (deviceInquiry != null) {
			throw new BluetoothStateException("Another inquiry already running");
		}
		deviceInquiry = new EmulatorDeviceInquiry(localDevice, this, listener);
		return DeviceInquiryThread.startInquiry(deviceInquiry, accessCode, listener);
	}

	public boolean cancelInquiry(DiscoveryListener listener) {
		if (deviceInquiry == null) {
			return false;
		}
		if (deviceInquiry.cancelInquiry(listener)) {
			deviceInquiry = null;
			return true;
		} else {
			return false;
		}
	}

	public String getRemoteDeviceFriendlyName(long address) throws IOException {
		return EmulatorHelper.getRemoteDeviceFriendlyName(localDevice, address);
	}

	// --- Service search

	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener)
			throws BluetoothStateException {
		return SearchServicesThread.startSearchServices(this, new EmulatorSearchServices(localDevice, this), attrSet,
				uuidSet, device, listener);
	}

	public boolean cancelServiceSearch(int transID) {
		SearchServicesThread sst = SearchServicesThread.getServiceSearchThread(transID);
		if (sst != null) {
			synchronized (sst) {
				if (!sst.isTerminated()) {
					sst.setTerminated();
					return true;
				}
			}
		}
		return false;
	}

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs)
			throws IOException {
		if (attrIDs.length > localDevice.getBluetooth_sd_attr_retrievable_max()) {
			throw new IllegalArgumentException();
		}
		return EmulatorSearchServices.populateServicesRecordAttributeValues(localDevice, serviceRecord, attrIDs,
				RemoteDeviceHelper.getAddress(serviceRecord.getHostDevice()), serviceRecord.getHandle());
	}

	// --- Client RFCOMM connections

	public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException {
		EmulatorRFCOMMClient c = localDevice.createRFCOMMClient();
		boolean success = false;
		try {
			c.connect(params);
			success = true;
		} finally {
			if (!success) {
				localDevice.removeConnection(c);
			}
		}
		return c.getHandle();
	}

	public void connectionRfCloseClientConnection(long handle) throws IOException {
		EmulatorRFCOMMClient c = ((EmulatorRFCOMMClient) localDevice.getConnection(handle));
		try {
			c.close();
		} finally {
			localDevice.removeConnection(c);
		}
	}

	public int rfGetSecurityOpt(long handle, int expected) throws IOException {
		return expected;
	}

	// --- Server RFCOMM connections

	public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord)
			throws IOException {
		EmulatorRFCOMMService s = localDevice.createRFCOMMService();
		boolean success = false;
		try {
			s.open(params);
			serviceRecord.setHandle(s.getHandle());
			serviceRecord
					.populateRFCOMMAttributes(s.getHandle(), s.getChannel(), params.uuid, params.name, params.obex);
			s.updateServiceRecord(serviceRecord);
			success = true;
		} finally {
			if (!success) {
				localDevice.removeConnection(s);
			}
		}
		return s.getHandle();
	}

	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		EmulatorRFCOMMService s = ((EmulatorRFCOMMService) localDevice.getConnection(handle));
		try {
			s.close(serviceRecord);
		} finally {
			localDevice.removeConnection(s);
		}
	}

	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		EmulatorRFCOMMService s;
		try {
			s = ((EmulatorRFCOMMService) localDevice.getConnection(handle));
		} catch (IOException e) {
			throw new ServiceRegistrationException(e.getMessage());
		}
		s.updateServiceRecord(serviceRecord);
	}

	public long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException {
		EmulatorRFCOMMService s = ((EmulatorRFCOMMService) localDevice.getConnection(handle));
		long connectionHandle = s.accept();
		EmulatorRFCOMMClient c = localDevice.createRFCOMMClient();
		c.connect(connectionHandle);
		return c.getHandle();
	}

	public void connectionRfCloseServerConnection(long handle) throws IOException {
		connectionRfCloseClientConnection(handle);
	}

	// --- Shared Client and Server RFCOMM connections

	public int connectionRfRead(long handle) throws IOException {
		return ((EmulatorRFCOMMClient) localDevice.getConnection(handle)).read();
	}

	public int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException {
		return ((EmulatorRFCOMMClient) localDevice.getConnection(handle)).read(b, off, len);
	}

	public int connectionRfReadAvailable(long handle) throws IOException {
		return ((EmulatorRFCOMMClient) localDevice.getConnection(handle)).available();
	}

	public void connectionRfWrite(long handle, int b) throws IOException {
		((EmulatorRFCOMMClient) localDevice.getConnection(handle)).write(b);
	}

	public void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException {
		((EmulatorRFCOMMClient) localDevice.getConnection(handle)).write(b, off, len);
	}

	public void connectionRfFlush(long handle) throws IOException {
		((EmulatorRFCOMMClient) localDevice.getConnection(handle)).flush();
	}

	public long getConnectionRfRemoteAddress(long handle) throws IOException {
		return ((EmulatorRFCOMMClient) localDevice.getConnection(handle)).getRemoteAddress();
	}

	// --- Client and Server L2CAP connections

	private void validateMTU(int receiveMTU, int transmitMTU) {
		// if (receiveMTU > receiveMTUMAX()) {
		// throw new IllegalArgumentException("invalid ReceiveMTU value " +
		// receiveMTU);
		// }
	}

	private native long l2OpenClientConnectionImpl(long address, int channel, boolean authenticate, boolean encrypt,
			int receiveMTU, int transmitMTU, int timeout) throws IOException;

	public long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU)
			throws IOException {
		validateMTU(receiveMTU, transmitMTU);
		Object lock = RemoteDeviceHelper.createRemoteDevice(this, params.address, null, false);
		synchronized (lock) {
			return l2OpenClientConnectionImpl(params.address, params.channel, params.authenticate, params.encrypt,
					receiveMTU, transmitMTU, params.timeout);
		}
	}

	public native void l2CloseClientConnection(long handle) throws IOException;

	private native long l2ServerOpenImpl(byte[] uuidValue, boolean authenticate, boolean encrypt, String name,
			int receiveMTU, int transmitMTU, int assignPsm) throws IOException;

	public native int l2ServerPSM(long handle) throws IOException;

	public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU,
			ServiceRecordImpl serviceRecord) throws IOException {
		validateMTU(receiveMTU, transmitMTU);
		byte[] uuidValue = Utils.UUIDToByteArray(params.uuid);
		long handle = l2ServerOpenImpl(uuidValue, params.authenticate, params.encrypt, params.name, receiveMTU,
				transmitMTU, params.bluecove_ext_psm);

		int channel = l2ServerPSM(handle);

		int serviceRecordHandle = (int) handle;

		serviceRecord.populateL2CAPAttributes(serviceRecordHandle, channel, params.uuid, params.name);

		return handle;
	}

	public void l2ServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		// sdpServiceUpdateServiceRecord(handle, 'L', serviceRecord);
	}

	public native long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException;

	public void l2CloseServerConnection(long handle) throws IOException {
		l2CloseClientConnection(handle);
	}

	private native void l2ServerCloseImpl(long handle) throws IOException;

	public void l2ServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		l2ServerCloseImpl(handle);
	}

	public int l2GetSecurityOpt(long handle, int expected) throws IOException {
		return expected;
	}

	public native boolean l2Ready(long handle) throws IOException;

	public native int l2Receive(long handle, byte[] inBuf) throws IOException;

	public native void l2Send(long handle, byte[] data) throws IOException;

	public native int l2GetReceiveMTU(long handle) throws IOException;

	public native int l2GetTransmitMTU(long handle) throws IOException;

	public native long l2RemoteAddress(long handle) throws IOException;
}
