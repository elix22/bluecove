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

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;

import com.intel.bluetooth.BluetoothConsts.DeviceClassConsts;
import com.intel.bluetooth.emu.DeviceDescriptor;
import com.intel.bluetooth.emu.DeviceManagerService;
import com.intel.bluetooth.emu.EmulatorConfiguration;
import com.intel.bluetooth.emu.EmulatorUtils;

/**
 * @author vlads
 * 
 */
class EmulatorLocalDevice {

	private DeviceManagerService service;

	private DeviceDescriptor deviceDescriptor;

	private int bluetooth_sd_attr_retrievable_max = 0;

	private int bluetooth_l2cap_receiveMTU_max = 0;

	private EmulatorConfiguration configuration;

	private Map/* <String,String> */propertiesMap;

	private Vector channels = new Vector();

	private Vector pcms = new Vector();

	private long connectionCount = 0;

	private Map connections = new Hashtable();

	public EmulatorLocalDevice(DeviceManagerService service, DeviceDescriptor deviceDescriptor) {
		this.service = service;
		this.deviceDescriptor = deviceDescriptor;

		propertiesMap = new Hashtable();
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
		bluetooth_sd_attr_retrievable_max = Integer.valueOf(
				configuration.getProperty("bluetooth.sd.attr.retrievable.max")).intValue();
		bluetooth_l2cap_receiveMTU_max = Integer.valueOf(configuration.getProperty("bluetooth.l2cap.receiveMTU.max"))
				.intValue();

		String[] property = { "bluetooth.master.switch", "bluetooth.sd.attr.retrievable.max",
				"bluetooth.connected.devices.max", "bluetooth.l2cap.receiveMTU.max", "bluetooth.sd.trans.max",
				"bluetooth.connected.inquiry.scan", "bluetooth.connected.page.scan", "bluetooth.connected.inquiry",
				"bluetooth.connected.page" };
		for (int i = 0; i < property.length; i++) {
			propertiesMap.put(property[i], configuration.getProperty(property[i]));
		}
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

	public void setLocalDeviceServiceClasses(int classOfDevice) {
		int c = deviceDescriptor.getDeviceClass();
		c &= DeviceClassConsts.MAJOR_MASK | DeviceClassConsts.MINOR_MASK;
		c |= classOfDevice;
		deviceDescriptor.setDeviceClass(c);
		service.setLocalDeviceServiceClasses(deviceDescriptor.getAddress(), c);
	}

	public boolean isLocalDevicePowerOn() {
		return true;
	}

	public String getLocalDeviceProperty(String property) {
		return (String) propertiesMap.get(property);
	}

	public int getBluetooth_sd_attr_retrievable_max() {
		return bluetooth_sd_attr_retrievable_max;
	}

	public int getBluetooth_l2cap_receiveMTU_max() {
		return this.bluetooth_l2cap_receiveMTU_max;
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

	EmulatorConnection getConnection(long handle) throws IOException {
		Object c = connections.get(new Long(handle));
		if (c == null) {
			throw new IOException("Invalid connection handle " + handle);
		}
		return (EmulatorConnection) c;
	}

	void removeConnection(EmulatorConnection c) {
		connections.remove(new Long(c.getHandle()));
		if (c instanceof EmulatorRFCOMMService) {
			channels.remove(new Long(((EmulatorRFCOMMService) c).getChannel()));
		} else if (c instanceof EmulatorL2CAPService) {
			pcms.remove(new Long(((EmulatorL2CAPService) c).getPcm()));
		}
	}

	private long nextConnectionId() {
		long id;
		synchronized (connections) {
			connectionCount++;
			id = connectionCount;
		}
		return id;
	}

	EmulatorRFCOMMService createRFCOMMService() {
		EmulatorRFCOMMService s;
		synchronized (connections) {
			long handle = nextConnectionId();
			int channel = (int) EmulatorUtils.getNextAvailable(channels, 1, 1);
			s = new EmulatorRFCOMMService(this, handle, channel);
			connections.put(new Long(handle), s);
			channels.addElement(new Long(channel));
		}
		return s;
	}

	EmulatorRFCOMMClient createRFCOMMClient() {
		EmulatorRFCOMMClient c;
		synchronized (connections) {
			long handle = nextConnectionId();
			c = new EmulatorRFCOMMClient(this, handle);
			connections.put(new Long(handle), c);
		}
		return c;
	}

	EmulatorL2CAPService createL2CAPService(int bluecove_ext_psm) {
		EmulatorL2CAPService s;
		synchronized (connections) {
			long handle = nextConnectionId();
			int pcm = (int) EmulatorUtils.getNextAvailable(pcms, 0x1001, 2);
			s = new EmulatorL2CAPService(this, handle, pcm);
			connections.put(new Long(handle), s);
			pcms.addElement(new Long(pcm));
		}
		return s;
	}

	EmulatorL2CAPClient createL2CAPClient() {
		EmulatorL2CAPClient c;
		synchronized (connections) {
			long handle = nextConnectionId();
			c = new EmulatorL2CAPClient(this, handle);
			connections.put(new Long(handle), c);
		}
		return c;
	}

}
