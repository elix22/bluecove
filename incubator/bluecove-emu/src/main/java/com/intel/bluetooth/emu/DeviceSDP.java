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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.intel.bluetooth.RemoteDeviceHelper;

/**
 * @author vlads
 * 
 */
class DeviceSDP {

	private long address;

	private static Hashtable services = new Hashtable();

	DeviceSDP(long address) {
		this.address = address;
	}

	synchronized void updateServiceRecord(long handle, ServicesDescriptor sdpData) {
		Long key = new Long(handle);
		boolean update = (services.get(key) != null);
		services.put(key, sdpData);

		String[] serviceUuidSet = sdpData.getUuidSet();
		for (int i = 0; i < serviceUuidSet.length; i++) {
			System.out.println((update ? "Update" : "Create") + " Srv on "
					+ RemoteDeviceHelper.getBluetoothAddress(address) + " " + handle + " " + i + " "
					+ serviceUuidSet[i]);
		}
	}

	synchronized void removeServiceRecord(long handle) {
		services.remove(new Long(handle));
	}

	ServicesDescriptor getServicesDescriptor(long handle) {
		return (ServicesDescriptor) services.get(new Long(handle));
	}

	synchronized long[] searchServices(String[] uuidSet) {
		Vector handles = new Vector();

		for (Enumeration iterator = services.keys(); iterator.hasMoreElements();) {
			Long key = (Long) iterator.nextElement();
			ServicesDescriptor service = (ServicesDescriptor) services.get(key);
			String[] serviceUuidSet = service.getUuidSet();
			// No duplicate values in any set!
			int match = 0;
			for (int i = 0; i < serviceUuidSet.length; i++) {
				for (int k = 0; k < uuidSet.length; k++) {
					if (uuidSet[k].equals(serviceUuidSet[i])) {
						match++;
						break;
					}
				}
			}
			if (match == uuidSet.length) {
				handles.addElement(key);
			}
		}

		long[] h = new long[handles.size()];
		int i = 0;
		for (Enumeration e = handles.elements(); e.hasMoreElements();) {
			h[i++] = ((Long) e.nextElement()).intValue();
		}
		return h;
	}
}
