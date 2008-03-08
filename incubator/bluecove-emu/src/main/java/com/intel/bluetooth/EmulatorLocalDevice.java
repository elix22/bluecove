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

import java.util.Hashtable;
import java.util.Map;

import javax.bluetooth.BluetoothStateException;

import com.intel.bluetooth.emu.DeviceDescriptor;
import com.intel.bluetooth.emu.DeviceManagerService;
import com.intel.bluetooth.emu.EmulatorConfiguration;

/**
 * @author vlads
 * 
 */
public class EmulatorLocalDevice {

	private DeviceManagerService service;

	private DeviceDescriptor deviceDescriptor;

	private final static int ATTR_RETRIEVABLE_MAX = 256;

	private EmulatorConfiguration configuration;

	private Map/* <String,String> */propertiesMap;

	public EmulatorLocalDevice(DeviceManagerService service, DeviceDescriptor deviceDescriptor) {
		this.service = service;
		this.deviceDescriptor = deviceDescriptor;

		propertiesMap = new Hashtable();
		final String TRUE = "true";
		final String FALSE = "false";
		propertiesMap.put("bluetooth.connected.devices.max", "7");
		propertiesMap.put("bluetooth.sd.trans.max", "7");
		propertiesMap.put("bluetooth.connected.inquiry.scan", TRUE);
		propertiesMap.put("bluetooth.connected.page.scan", TRUE);
		propertiesMap.put("bluetooth.connected.inquiry", TRUE);
		propertiesMap.put("bluetooth.connected.page", TRUE);
		propertiesMap.put("bluetooth.sd.attr.retrievable.max", String.valueOf(ATTR_RETRIEVABLE_MAX));
		propertiesMap.put("bluetooth.master.switch", FALSE);
		propertiesMap.put("bluetooth.l2cap.receiveMTU.max", "65535");

		propertiesMap.put("bluecove.radio.version", BlueCoveImpl.version);
		propertiesMap.put("bluecove.radio.manufacturer", "pyx4j.com");
		propertiesMap.put("bluecove.stack.version", BlueCoveImpl.version);

		updateConfiguration();
	}

	void destroy() {
		service = null;
		deviceDescriptor = null;
	}

	public DeviceManagerService getDeviceManagerService() {
		return service;
	}

	public void updateConfiguration() {
		configuration = service.getEmulatorConfiguration();
	}

	public long getAddress() {
		return deviceDescriptor.getAddress();
	}

	public String getName() {
		return deviceDescriptor.getName();
	}

	public int getDeviceClass() {
		return deviceDescriptor.getDeviceClass();
	}

	public boolean isLocalDevicePowerOn() {
		return true;
	}

	public String getLocalDeviceProperty(String property) {
		return (String) propertiesMap.get(property);
	}

	public int getLocalDeviceDiscoverable() {
		return service.getLocalDeviceDiscoverable(getAddress());
	}

	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		return service.setLocalDeviceDiscoverable(getAddress(), mode);
	}

	public EmulatorConfiguration getConfiguration() {
		return configuration;
	}

}
