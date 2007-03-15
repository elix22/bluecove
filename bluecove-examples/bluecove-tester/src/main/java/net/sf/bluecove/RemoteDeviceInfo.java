/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
package net.sf.bluecove;

import java.util.Hashtable;

import javax.bluetooth.RemoteDevice;

/**
 * @author vlads
 *
 */
public class RemoteDeviceInfo {
	
	public static Hashtable devices = new Hashtable();
	
	public RemoteDevice remoteDevice;
	
	public int discoveredCount;
	
	public long discoveredFirstTime;
	
	public long discoveredLastTime;
	
	public static synchronized void deviceFound(RemoteDevice remoteDevice) {
		String addr = remoteDevice.getBluetoothAddress().toUpperCase();
		RemoteDeviceInfo devInfo = (RemoteDeviceInfo)devices.get(addr);
		if (devInfo == null) {
			devInfo = new RemoteDeviceInfo();
			devInfo.discoveredFirstTime = System.currentTimeMillis();
			devices.put(addr, devInfo);
		}
		devInfo.remoteDevice = remoteDevice;
		devInfo.discoveredCount ++;
		devInfo.discoveredLastTime = System.currentTimeMillis(); 
	}
}
