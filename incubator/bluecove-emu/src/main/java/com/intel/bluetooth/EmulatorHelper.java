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

import java.util.HashMap;
import java.util.Map;

import javax.bluetooth.BluetoothStateException;

import com.intel.bluetooth.emu.DeviceDescriptor;
import com.intel.bluetooth.emu.DeviceManagerService;
import com.intel.bluetooth.rmi.Client;

class EmulatorHelper {

	private static Map<EmulatorLocalDevice, EmulatorCommandReceiver> receivers = new HashMap<EmulatorLocalDevice, EmulatorCommandReceiver>();

	static EmulatorLocalDevice createNewLocalDevice() throws BluetoothStateException {
		String host = BlueCoveImpl.getConfigProperty("bluecove.emu.rmiRegistryHost");
		String port = BlueCoveImpl.getConfigProperty("bluecove.emu.rmiRegistryPort");
		DeviceDescriptor deviceDescriptor;
		DeviceManagerService service;
		try {
			service = (DeviceManagerService) Client.getService(DeviceManagerService.class, host, port);
			deviceDescriptor = service.createNewDevice(BlueCoveImpl.getConfigProperty("bluecove.deviceID"),
					BlueCoveImpl.getConfigProperty("bluecove.deviceAddress"));
		} catch (Exception e) {
			throw (BluetoothStateException) UtilsJavaSE.initCause(new BluetoothStateException(e.getMessage()), e);
		}
		EmulatorLocalDevice device = new EmulatorLocalDevice(service, deviceDescriptor);
		EmulatorCommandReceiver receiver = new EmulatorCommandReceiver(device);
		receivers.put(device, receiver);
		return device;
	}

	static void releaseDevice(EmulatorLocalDevice device) {
		EmulatorCommandReceiver receiver = receivers.remove(device);
		if (receiver != null) {
			receiver.shutdownReceiver();
		}
		device.getDeviceManagerService().releaseDevice(device.getAddress());
		device.destroy();
	}

}
