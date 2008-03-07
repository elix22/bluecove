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

import com.intel.bluetooth.emu.DeviceDescriptor;

class BluetoothEmulator implements BluetoothStack, DeviceInquiryRunnable, SearchServicesRunnable {

	static final int NATIVE_LIBRARY_VERSION = BlueCoveImpl.nativeLibraryVersionExpected;

	private DeviceDescriptor deviceDescriptor;

	private DiscoveryListener discoveryListener;

	private boolean deviceInquiryCanceled = false;

	BluetoothEmulator() {
	}

	// --- Library initialization

	// DONE
	public String getStackID() {
		return BlueCoveImpl.STACK_EMULATOR;
	}

	// DONE
	public int getLibraryVersion() throws BluetoothStateException {
		return NATIVE_LIBRARY_VERSION;
	}

	// DONE
	public int detectBluetoothStack() {
		return BlueCoveImpl.BLUECOVE_STACK_DETECT_EMULATOR;
	}

	public void initialize() throws BluetoothStateException {
		deviceDescriptor = Helper.createNewDevice();
	}

	// DONE
	public void destroy() {
		Helper.releaseDevice(deviceDescriptor.getAddress());
	}

	// DONE
	public void enableNativeDebug(Class nativeDebugCallback, boolean on) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#isCurrentThreadInterruptedCallback()
	 */
	// DONE
	public boolean isCurrentThreadInterruptedCallback() {
		return Thread.interrupted();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#getFeatureSet()
	 */
	// DONE
	public int getFeatureSet() {
		return FEATURE_SERVICE_ATTRIBUTES | FEATURE_L2CAP;
	}

	// --- LocalDevice

	// DONE
	public String getLocalDeviceBluetoothAddress() throws BluetoothStateException {
		return RemoteDeviceHelper.getBluetoothAddress(deviceDescriptor.getAddress());
	}

	// DONE
	public DeviceClass getLocalDeviceClass() {
		return new DeviceClass(deviceDescriptor.getDeviceClass());
	}

	// DONE
	public String getLocalDeviceName() {
		return deviceDescriptor.getName();
	}

	// DONE
	public boolean isLocalDevicePowerOn() {
		return true;
	}

	// TODO - return property name for now
	public String getLocalDeviceProperty(String property) {
		return property;
	}

	// DONE
	public int getLocalDeviceDiscoverable() {
		return Helper.getLocalDeviceDiscoverable(deviceDescriptor.getAddress());
	}

	// DONE
	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		return Helper.setLocalDeviceDiscoverable(mode, deviceDescriptor.getAddress());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#setLocalDeviceServiceClasses(int)
	 */
	// DONE
	public void setLocalDeviceServiceClasses(int classOfDevice) {
		throw new NotSupportedRuntimeException(getStackID());
	}

	// --- Device Inquiry

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		if (discoveryListener != null) {
			throw new BluetoothStateException("Another inquiry already running");
		}
		discoveryListener = listener;
		deviceInquiryCanceled = false;
		return DeviceInquiryThread.startInquiry(this, accessCode, listener);
	}

	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener)
			throws BluetoothStateException {
		startedNotify.deviceInquiryStartedCallback();
		try {
			DeviceDescriptor[] devices = Helper.getDiscoveredDevices(deviceDescriptor.getAddress());
			for (int i = 0; i < devices.length; i++) {
				RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(this, devices[i].getAddress(),
						devices[i].getName(), false);
				if (deviceInquiryCanceled || (discoveryListener == null)) {
					return DiscoveryListener.INQUIRY_TERMINATED;
				}
				DeviceClass cod = new DeviceClass(devices[i].getDeviceClass());
				DebugLog.debug("deviceDiscoveredCallback address", remoteDevice.getBluetoothAddress());
				DebugLog.debug("deviceDiscoveredCallback deviceClass", cod);
				listener.deviceDiscovered(remoteDevice, cod);
			}

			if (deviceInquiryCanceled) {
				return DiscoveryListener.INQUIRY_TERMINATED;
			}
			return DiscoveryListener.INQUIRY_COMPLETED;
		} finally {
			discoveryListener = null;
		}
	}

	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass,
			String deviceName, boolean paired) {
	}

	public boolean cancelInquiry(DiscoveryListener listener) {
		if (discoveryListener != null && discoveryListener == listener) {
			deviceInquiryCanceled = true;
			return true;
		}
		return false;
	}

	public String getRemoteDeviceFriendlyName(long address) throws IOException {
		return Helper.getRemoteDeviceFriendlyName(address);
	}

	// --- Service search

	// public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice
	// device, DiscoveryListener listener)
	// throws BluetoothStateException {
	// return SearchServicesThread.startSearchServices(this, attrSet, uuidSet,
	// device, listener);
	// }
	//
	// public int runSearchServices(SearchServicesThread sst, int[] attrSet,
	// UUID[] uuidSet, RemoteDevice device,
	// DiscoveryListener listener) throws BluetoothStateException {
	// sst.searchServicesStartedCallback();
	// try {
	// byte[][] uuidValues = new byte[uuidSet.length][];
	// for (int i = 0; i < uuidSet.length; i++) {
	// uuidValues[i] = Utils.UUIDToByteArray(uuidSet[i]);
	// }
	// int respCode = runSearchServicesImpl(sst, uuidValues,
	// RemoteDeviceHelper.getAddress(device));
	// if ((respCode != DiscoveryListener.SERVICE_SEARCH_ERROR) &&
	// (sst.isTerminated())) {
	// return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
	// } else if (respCode == DiscoveryListener.SERVICE_SEARCH_COMPLETED) {
	// Vector records = sst.getServicesRecords();
	// if (records.size() != 0) {
	// DebugLog.debug("SearchServices finished", sst.getTransID());
	// ServiceRecord[] servRecordArray = (ServiceRecord[])
	// Utils.vector2toArray(records,
	// new ServiceRecord[records.size()]);
	// listener.servicesDiscovered(sst.getTransID(), servRecordArray);
	// }
	// if (records.size() != 0) {
	// return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
	// } else {
	// return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
	// }
	// } else {
	// return respCode;
	// }
	// } catch (SearchServicesDeviceNotReachableException e) {
	// return DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE;
	// } catch (SearchServicesTerminatedException e) {
	// return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
	// } catch (SearchServicesException e) {
	// return DiscoveryListener.SERVICE_SEARCH_ERROR;
	// }
	// }
	//
	// public boolean serviceDiscoveredCallback(SearchServicesThread sst, long
	// sdpSession, long handle) {
	// if (sst.isTerminated()) {
	// return true;
	// }
	// ServiceRecordImpl servRecord = new ServiceRecordImpl(this,
	// sst.getDevice(), handle);
	// int[] attrIDs = sst.getAttrSet();
	// long remoteDeviceAddress =
	// RemoteDeviceHelper.getAddress(sst.getDevice());
	// populateServiceRecordAttributeValuesImpl(remoteDeviceAddress, sdpSession,
	// handle, attrIDs, servRecord);
	// sst.addServicesRecords(servRecord);
	// return false;
	// }
	//
	// public boolean cancelServiceSearch(int transID) {
	// SearchServicesThread sst =
	// SearchServicesThread.getServiceSearchThread(transID);
	// if (sst != null) {
	// return sst.setTerminated();
	// } else {
	// return false;
	// }
	// }
	//
	// public boolean populateServicesRecordAttributeValues(ServiceRecordImpl
	// serviceRecord, int[] attrIDs)
	// throws IOException {
	// long remoteDeviceAddress =
	// RemoteDeviceHelper.getAddress(serviceRecord.getHostDevice());
	// return populateServiceRecordAttributeValuesImpl(remoteDeviceAddress, 0,
	// serviceRecord.getHandle(), attrIDs,
	// serviceRecord);
	// }

	// --- Client RFCOMM connections

	// public long connectionRfOpenClientConnection(BluetoothConnectionParams
	// params) throws IOException {
	// return connectionRfOpenClientConnectionImpl(deviceDescriptor,
	// params.address, params.channel,
	// params.authenticate, params.encrypt, params.timeout);
	// }
	//
	// public native void connectionRfCloseClientConnection(long handle) throws
	// IOException;
	//
	// public native int rfGetSecurityOptImpl(long handle) throws IOException;
	//
	// public int rfGetSecurityOpt(long handle, int expected) throws IOException
	// {
	// return rfGetSecurityOptImpl(handle);
	// }
	//
	// public long rfServerOpen(BluetoothConnectionNotifierParams params,
	// ServiceRecordImpl serviceRecord)
	// throws IOException {
	// final int listen_backlog = 1;
	// long socket = rfServerOpenImpl(this.deviceDescriptor, params.authorize,
	// params.authenticate, params.encrypt,
	// params.master, params.timeouts, listen_backlog);
	// boolean success = false;
	// try {
	// int channel = rfServerGetChannelIDImpl(socket);
	// long serviceRecordHandle = socket;
	// serviceRecord.populateRFCOMMAttributes(serviceRecordHandle, channel,
	// params.uuid, params.name, params.obex);
	// serviceRecord.setHandle(registerSDPServiceImpl(this.deviceDescriptor,
	// serviceRecord.toByteArray()));
	// success = true;
	// return socket;
	// } finally {
	// if (!success) {
	// rfServerClose(socket, true);
	// }
	// }
	// }
	//
	// public void rfServerClose(long handle, ServiceRecordImpl serviceRecord)
	// throws IOException {
	// try {
	// unregisterSDPServiceImpl(serviceRecord.getHandle());
	// } finally {
	// rfServerClose(handle, false);
	// }
	// }
	//
	// public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl
	// serviceRecord, boolean acceptAndOpen)
	// throws ServiceRegistrationException {
	// unregisterSDPServiceImpl(serviceRecord.getHandle());
	// byte[] blob;
	// try {
	// blob = serviceRecord.toByteArray();
	// } catch (IOException e) {
	// throw new ServiceRegistrationException(e.toString());
	// }
	// serviceRecord.setHandle(registerSDPServiceImpl(this.deviceDescriptor,
	// blob));
	// }
	//
	// public native long rfServerAcceptAndOpenRfServerConnection(long handle)
	// throws IOException;
	//
	// public void connectionRfCloseServerConnection(long clientHandle) throws
	// IOException {
	// connectionRfCloseClientConnection(clientHandle);
	// }

	// --- Shared Client and Server RFCOMM connections

	public native int connectionRfRead(long handle) throws IOException;

	public native int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;

	public native int connectionRfReadAvailable(long handle) throws IOException;

	public native void connectionRfWrite(long handle, int b) throws IOException;

	public native void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException;

	public native void connectionRfFlush(long handle) throws IOException;

	public native long getConnectionRfRemoteAddress(long handle) throws IOException;

	// --- Client and Server L2CAP connections

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2OpenClientConnection(com.intel.bluetooth.BluetoothConnectionParams,
	 *      int, int)
	 */
	// public long l2OpenClientConnection(BluetoothConnectionParams params, int
	// receiveMTU, int transmitMTU)
	// throws IOException {
	// return l2OpenClientConnectionImpl(deviceDescriptor, params.address,
	// params.channel, params.authenticate,
	// params.encrypt, receiveMTU, transmitMTU, params.timeout);
	// }
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseClientConnection(long)
	 */
	public native void l2CloseClientConnection(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerOpen(com.intel.bluetooth.BluetoothConnectionNotifierParams,
	 *      int, int, com.intel.bluetooth.ServiceRecordImpl)
	 */
	public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU,
			ServiceRecordImpl serviceRecord) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerUpdateServiceRecord(long,
	 *      com.intel.bluetooth.ServiceRecordImpl, boolean)
	 */
	public void l2ServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		// TODO Auto-generated method stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerAcceptAndOpenServerConnection(long)
	 */
	public long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseServerConnection(long)
	 */
	public void l2CloseServerConnection(long handle) throws IOException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerClose(long,
	 *      com.intel.bluetooth.ServiceRecordImpl)
	 */
	public void l2ServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2Ready(long)
	 */
	public native boolean l2Ready(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2receive(long, byte[])
	 */
	public native int l2Receive(long handle, byte[] inBuf) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2send(long, byte[])
	 */
	public native void l2Send(long handle, byte[] data) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2GetReceiveMTU(long)
	 */
	public native int l2GetReceiveMTU(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2GetTransmitMTU(long)
	 */
	public native int l2GetTransmitMTU(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2RemoteAddress(long)
	 */
	public native long l2RemoteAddress(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2GetSecurityOpt(long, int)
	 */
	public native int l2GetSecurityOpt(long handle, int expected) throws IOException;

	// TODO remove after implementing above methods

	public boolean cancelServiceSearch(int transID) {
		// TODO Auto-generated method stub
		return false;
	}

	public void connectionRfCloseClientConnection(long handle) throws IOException {
		// TODO Auto-generated method stub

	}

	public void connectionRfCloseServerConnection(long handle) throws IOException {
		// TODO Auto-generated method stub

	}

	public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs)
			throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	public int rfGetSecurityOpt(long handle, int expected) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		// TODO Auto-generated method stub

	}

	public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		// TODO Auto-generated method stub

	}

	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener)
			throws BluetoothStateException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int runSearchServices(SearchServicesThread sst, int[] attrSet, UUID[] uuidSet, RemoteDevice device,
			DiscoveryListener listener) throws BluetoothStateException {
		// TODO Auto-generated method stub
		return 0;
	}

}
