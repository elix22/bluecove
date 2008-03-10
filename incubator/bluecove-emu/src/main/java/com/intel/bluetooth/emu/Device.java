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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * @author vlads
 * 
 */
class Device {

	private DeviceDescriptor descriptor;

	private DeviceSDP sdp;

	private Vector serviceListeners;

	private Hashtable connections = new Hashtable();

	Device(DeviceDescriptor descriptor) {
		this.descriptor = descriptor;
		this.serviceListeners = new Vector();
	}

	DeviceDescriptor getDescriptor() {
		return this.descriptor;
	}

	synchronized DeviceSDP getDeviceSDP(boolean create) {
		if (create && (sdp == null)) {
			sdp = new DeviceSDP(descriptor.getAddress());
		}
		return sdp;
	}

	public ServiceListener createServiceListener(String portID) {
		ServiceListener sl = new ServiceListener(portID);
		synchronized (serviceListeners) {
			serviceListeners.addElement(sl);
		}
		return sl;
	}

	public ServiceListener removeServiceListener(String portID) {
		ServiceListener sl = null;
		synchronized (serviceListeners) {
			for (Enumeration iterator = serviceListeners.elements(); iterator.hasMoreElements();) {
				ServiceListener s = (ServiceListener) iterator.nextElement();
				if (s.getPortID().equals(portID)) {
					serviceListeners.removeElement(s);
					sl = s;
					break;
				}
			}
		}
		return sl;
	}

	void addConnectionBuffer(long connectionId, ConnectionBuffer c) {
		connections.put(new Long(connectionId), c);
	}

	ConnectionBuffer getConnectionBuffer(long connectionId) {
		return (ConnectionBuffer) connections.get(new Long(connectionId));
	}

	void closeConnection(long connectionId) throws IOException {
		ConnectionBuffer c = getConnectionBuffer(connectionId);
		if (c == null) {
			throw new IOException("No such connection " + connectionId);
		}
		c.close();
	}

	void release() {
		for (Enumeration iterator = serviceListeners.elements(); iterator.hasMoreElements();) {
			ServiceListener s = (ServiceListener) iterator.nextElement();
			s.close();
		}
		for (Enumeration iterator = connections.elements(); iterator.hasMoreElements();) {
			ConnectionBuffer c = (ConnectionBuffer) iterator.nextElement();
			try {
				c.close();
			} catch (IOException e) {
			}
		}
	}
}
