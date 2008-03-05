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
			url = new URL("http://localhost:"+HTTPServer.port);
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

	public static boolean setLocalDeviceDiscoverable(int mode,
			long address) {
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
