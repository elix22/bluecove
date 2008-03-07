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
import java.util.HashMap;

import javax.bluetooth.DiscoveryAgent;

public class DeviceManagerServiceImpl implements DeviceManagerService {

	public static final int MAJOR_COMPUTER = 0x0100;

	private static HashMap devices = new HashMap();

	public DeviceManagerServiceImpl() {

	}

	public DeviceDescriptor createNewDevice() {
		synchronized (devices) {
			long address = getNextAvailableBTAddress();
			DeviceDescriptor descriptor = new DeviceDescriptor(address, "Device" + address, MAJOR_COMPUTER);
			devices.put(new Long(address), descriptor);
			return descriptor;
		}
	}

	public DeviceDescriptor[] getDiscoveredDevices(long address) {
		HashMap temp = (HashMap) devices.clone();
		temp.remove(new Long(address));
		return (DeviceDescriptor[]) temp.values().toArray(new DeviceDescriptor[] {});
	}

	public int getLocalDeviceDiscoverable(long address) {
		return DiscoveryAgent.GIAC;
	}

	public String getRemoteDeviceFriendlyName(long address) {
		return ((DeviceDescriptor) devices.get(new Long(address))).getName();
	}

	public void releaseDevice(long address) {
		devices.remove(new Long(address));
	}

	public boolean setLocalDeviceDiscoverable(int mode, long address) {
		return true;
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
