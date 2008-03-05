package com.intel.bluetooth.emu;

import com.pyx4j.rpcoverhttp.common.RoHService;

public interface DeviceManagerService extends RoHService {

	public void releaseDevice(long address);

	public int getLocalDeviceDiscoverable(long address);

	public boolean setLocalDeviceDiscoverable(int mode,
			long address);

	public DeviceDescriptor[] getDiscoveredDevices(long address);

	public String getRemoteDeviceFriendlyName(long address);

	public DeviceDescriptor createNewDevice();
		
}
