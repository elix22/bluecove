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

import com.intel.bluetooth.emu.DeviceDescriptor;
import com.intel.bluetooth.emu.DeviceManagerService;
import com.pyx4j.rpcoverhttp.client.ServiceProxy;
import com.pyx4j.rpcoverhttp.server.HTTPServer;

public class Helper {

	private static URL url;

	static {
		try {
			url = new URL("http://localhost:" + HTTPServer.port);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void releaseDevice(long address) {
		DeviceManagerService service = (DeviceManagerService) ServiceProxy.getService(DeviceManagerService.class, url);
		service.releaseDevice(address);
	}

	public static int getLocalDeviceDiscoverable(long address) {
		DeviceManagerService service = (DeviceManagerService) ServiceProxy.getService(DeviceManagerService.class, url);
		return service.getLocalDeviceDiscoverable(address);
	}

	public static boolean setLocalDeviceDiscoverable(int mode, long address) {
		DeviceManagerService service = (DeviceManagerService) ServiceProxy.getService(DeviceManagerService.class, url);
		return service.setLocalDeviceDiscoverable(mode, address);
	}

	public static DeviceDescriptor[] getDiscoveredDevices(long address) {
		DeviceManagerService service = (DeviceManagerService) ServiceProxy.getService(DeviceManagerService.class, url);
		return service.getDiscoveredDevices(address);
	}

	public static String getRemoteDeviceFriendlyName(long address) {
		DeviceManagerService service = (DeviceManagerService) ServiceProxy.getService(DeviceManagerService.class, url);
		return service.getRemoteDeviceFriendlyName(address);
	}

	public static DeviceDescriptor createNewDevice() {
		DeviceManagerService service = (DeviceManagerService) ServiceProxy.getService(DeviceManagerService.class, url);
		return service.createNewDevice();
	}

}
