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
package com.intel.bluetooth.emu;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.ServiceRegistrationException;

import com.intel.bluetooth.RemoteDeviceHelper;

public class DeviceManagerServiceImpl implements DeviceManagerService {

	public static final int MAJOR_COMPUTER = 0x0100;

	private static EmulatorConfiguration configuration = new EmulatorConfiguration();

	private static Hashtable devices = new Hashtable();

	private static Hashtable devicesSDP = new Hashtable();

	public DeviceManagerServiceImpl() {

	}

	public EmulatorConfiguration getEmulatorConfiguration() {
		return configuration;
	}

	public DeviceDescriptor createNewDevice(String deviceID, String deviceAddress) {
		synchronized (devices) {
			long address = getNextAvailableBTAddress();
			DeviceDescriptor descriptor = new DeviceDescriptor(address, configuration.getDeviceNamePrefix()
					+ RemoteDeviceHelper.getBluetoothAddress(address), MAJOR_COMPUTER);
			devices.put(new Long(address), descriptor);
			return descriptor;
		}
	}

	public void releaseDevice(long address) {
		synchronized (devices) {
			devices.remove(new Long(address));
		}
	}

	private DeviceDescriptor getDevice(long address) {
		return ((DeviceDescriptor) devices.get(new Long(address)));
	}

	public DeviceDescriptor[] getDiscoveredDevices(long address) {
		Vector discoveredDevice = new Vector();
		synchronized (devices) {
			for (Enumeration iterator = devices.elements(); iterator.hasMoreElements();) {
				DeviceDescriptor device = (DeviceDescriptor) iterator.nextElement();
				if (device.getAddress() == address) {
					continue;
				}
				if (isDiscoverable(device)) {
					discoveredDevice.addElement(device);
				}
			}
		}
		return (DeviceDescriptor[]) discoveredDevice.toArray(new DeviceDescriptor[discoveredDevice.size()]);
	}

	private boolean isDiscoverable(DeviceDescriptor device) {
		int discoverableMode = device.getDiscoverableMode();
		switch (discoverableMode) {
		case DiscoveryAgent.NOT_DISCOVERABLE:
			return false;
		case DiscoveryAgent.GIAC:
			return true;
		case DiscoveryAgent.LIAC:
			if (device.getLimitedDiscoverableStart() + configuration.getDurationLIAC() * 1000 * 60 < System
					.currentTimeMillis()) {
				device.setDiscoverableMode(DiscoveryAgent.NOT_DISCOVERABLE);
				return false;
			} else {
				return true;
			}
		default:
			return false;
		}
	}

	public int getLocalDeviceDiscoverable(long address) {
		DeviceDescriptor device = getDevice(address);
		// Update mode if it was LIAC
		isDiscoverable(device);
		return device.getDiscoverableMode();
	}

	public boolean setLocalDeviceDiscoverable(long address, int mode) {
		getDevice(address).setDiscoverableMode(mode);
		return true;
	}

	public String getRemoteDeviceFriendlyName(long address) {
		return getDevice(address).getName();
	}

	private long getNextAvailableBTAddress() {
		return EmulatorUtils.getNextAvailable(devices.keySet(), configuration.getFirstDeviceAddress(), 1);
	}

	private DeviceSDP getDeviceSDP(long address, boolean create) {
		synchronized (devicesSDP) {
			DeviceSDP ds = ((DeviceSDP) devicesSDP.get(new Long(address)));
			if (create && (ds == null)) {
				ds = new DeviceSDP(address);
				devicesSDP.put(new Long(address), ds);
			}
			return ds;
		}
	}

	public void updateServiceRecord(long address, long handle, ServicesDescriptor sdpData)
			throws ServiceRegistrationException {
		if (getDevice(address) == null) {
			throw new ServiceRegistrationException("No such device");
		}
		DeviceSDP ds = getDeviceSDP(address, true);
		ds.updateServiceRecord(handle, sdpData);
	}

	public void removeServiceRecord(long address, long handle) {
		DeviceSDP ds = getDeviceSDP(address, false);
		if (ds != null) {
			ds.removeServiceRecord(handle);
		}
	}

	public long[] searchServices(long address, String[] uuidSet) {
		if (getDevice(address) == null) {
			return null;
		}
		DeviceSDP ds = getDeviceSDP(address, false);
		if (ds == null) {
			return new long[0];
		}
		return ds.searchServices(uuidSet);
	}

	public byte[] getServicesRecordBinary(long address, long handle) throws IOException {
		DeviceSDP ds = getDeviceSDP(address, false);
		if (ds == null) {
			throw new IOException("No such device");
		}
		ServicesDescriptor sd = ds.getServicesDescriptor(handle);
		if (sd == null) {
			throw new IOException("No such service");
		}
		return sd.getSdpBinary();
	}

	public long rfAccept(long address, int channel, boolean authenticate, boolean encrypt) throws IOException {
		try {
			Thread.sleep(12220 * 1000);
		} catch (InterruptedException e) {
		}
		throw new IOException("TODO");
	}
}
