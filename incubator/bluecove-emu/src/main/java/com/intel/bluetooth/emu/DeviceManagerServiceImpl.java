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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.bluetooth.DiscoveryAgent;

public class DeviceManagerServiceImpl implements DeviceManagerService {

	public static final int MAJOR_COMPUTER = 0x0100;

	private static EmulatorConfiguration configuration = new EmulatorConfiguration();

	private static Hashtable devices = new Hashtable();

	public DeviceManagerServiceImpl() {

	}

	public EmulatorConfiguration getEmulatorConfiguration() {
		return configuration;
	}

	public DeviceDescriptor createNewDevice(String deviceID, String deviceAddress) {
		synchronized (devices) {
			long address = getNextAvailableBTAddress();
			DeviceDescriptor descriptor = new DeviceDescriptor(address, "Device" + address, MAJOR_COMPUTER);
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

	long getNextAvailableBTAddress() {
		Object[] addresses = devices.keySet().toArray();
		Arrays.sort(addresses);
		if (addresses.length == 0) {
			return 1;
		}
		for (int i = 0; i < addresses.length; i++) {
			if (((Long) addresses[i]).longValue() != i + 1) {
				return (long) i + 1;
			}
		}
		return ((Long) addresses[addresses.length - 1]).longValue() + 1;
	}
}
