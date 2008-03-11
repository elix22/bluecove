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

import java.net.URL;

import javax.bluetooth.BluetoothStateException;

import com.intel.bluetooth.emu.DeviceDescriptor;
import com.intel.bluetooth.emu.DeviceManagerService;
import com.pyx4j.rpcoverhttp.client.ServiceProxy;
import com.pyx4j.rpcoverhttp.common.RoHRuntimeException;
import com.pyx4j.rpcoverhttp.server.HTTPServer;

class EmulatorHelper {

	private static URL url;

	static {
		try {
			url = new URL("http://localhost:" + HTTPServer.port);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static EmulatorLocalDevice createNewLocalDevice() throws BluetoothStateException {
		DeviceManagerService service = (DeviceManagerService) ServiceProxy.getService(DeviceManagerService.class, url);
		DeviceDescriptor deviceDescriptor;
		try {
			deviceDescriptor = service.createNewDevice(BlueCoveImpl.getConfigProperty("bluecove.deviceID"),
					BlueCoveImpl.getConfigProperty("bluecove.deviceAddress"));
		} catch (RoHRuntimeException e) {
			throw (BluetoothStateException) UtilsJavaSE.initCause(new BluetoothStateException(e.getMessage()), e);
		}
		EmulatorLocalDevice device = new EmulatorLocalDevice(service, deviceDescriptor);
		return device;
	}

	static void releaseDevice(EmulatorLocalDevice device) {
		device.getDeviceManagerService().releaseDevice(device.getAddress());
		device.destroy();
	}

	static String getRemoteDeviceFriendlyName(EmulatorLocalDevice localDevice, long address) {
		return localDevice.getDeviceManagerService().getRemoteDeviceFriendlyName(address);
	}

}
