package com.intel.bluetooth.emu;

import junit.framework.TestCase;

public class DeviceManagerTest extends TestCase {

	DeviceManagerServiceImpl deviceManager;
	
	public void setUp() throws Exception {
		deviceManager = new DeviceManagerServiceImpl();
	}
	
	public void tearDown() throws Exception {
	}
	
	public void testCreateNewDevice() throws Exception {
		DeviceDescriptor descriptor = deviceManager.createNewDevice();
		System.out.println(descriptor);
		descriptor = deviceManager.createNewDevice();
		System.out.println(descriptor);
		descriptor = deviceManager.createNewDevice();
		System.out.println(descriptor);
		descriptor = deviceManager.createNewDevice();
		System.out.println(descriptor);
		deviceManager.releaseDevice(1);
		descriptor = deviceManager.createNewDevice();
		System.out.println(descriptor);
		descriptor = deviceManager.createNewDevice();
		System.out.println(descriptor);
		
	}

}
