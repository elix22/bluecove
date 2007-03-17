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
	
	private long totalDiscovery;

	public static long totalDeviceInquiryCount;
	
	public static long totalDeviceInquiryDuration;
	
	public long serviceSearchCount;
	
	private long totalServiceSearchDuration;

	public static long allServiceSearchCount;
	
	public static long allServiceSearchDuration;
	
	public int serviceDiscoveredCount;
	
	public long serviceDiscoveredFirstTime;
	
	public long serviceDiscoveredLastTime;
	
	private long totalServiceDiscovery;
	
	public static synchronized void clear() {
		devices = new Hashtable();
	}
	
	public static synchronized RemoteDeviceInfo getDevice(RemoteDevice remoteDevice) {
		String addr = remoteDevice.getBluetoothAddress().toUpperCase();
		RemoteDeviceInfo devInfo = (RemoteDeviceInfo)devices.get(addr);
		if (devInfo == null) {
			devInfo = new RemoteDeviceInfo();
			devices.put(addr, devInfo);
		}
		return devInfo;
	}

	public static synchronized void deviceFound(RemoteDevice remoteDevice) {
		RemoteDeviceInfo devInfo = getDevice(remoteDevice);
		long now = System.currentTimeMillis();
		if (devInfo.discoveredCount == 0) {
			devInfo.discoveredFirstTime = now;
		} else {
			devInfo.totalDiscovery += (now - devInfo.discoveredLastTime);
		}
		devInfo.remoteDevice = remoteDevice;
		devInfo.discoveredCount ++;
		devInfo.discoveredLastTime = now; 
	}
	
	public static synchronized void deviceServiceFound(RemoteDevice remoteDevice) {
		RemoteDeviceInfo devInfo = getDevice(remoteDevice);
		long now = System.currentTimeMillis();
		if (devInfo.serviceDiscoveredCount == 0) {
			devInfo.serviceDiscoveredFirstTime = now;
		} else {
			devInfo.totalServiceDiscovery += (now - devInfo.serviceDiscoveredLastTime);
		}
		devInfo.remoteDevice = remoteDevice;
		devInfo.serviceDiscoveredCount ++;
		devInfo.serviceDiscoveredLastTime = now; 
	}
	
	public static synchronized void searchServices(RemoteDevice remoteDevice, boolean found, long servicesSearch) {
		RemoteDeviceInfo devInfo = getDevice(remoteDevice);
		devInfo.serviceSearchCount ++;
		devInfo.totalServiceSearchDuration += servicesSearch;
		allServiceSearchCount ++;
		allServiceSearchDuration += servicesSearch;
	}

	public static void discoveryInquiryFinished(long discoveryInquiry) {
		totalDeviceInquiryCount ++;
		totalDeviceInquiryDuration += discoveryInquiry;
	}
	
	public static long allAvgDeviceInquiryDurationSec() {
		if (totalDeviceInquiryCount == 0) {
			return 0;
		}
		return (totalDeviceInquiryDuration/(1000 * totalDeviceInquiryCount));
	}
	
	public static long allAvgServiceSearchDurationSec() {
		if (allServiceSearchCount == 0) {
			return 0;
		}
		return (allServiceSearchDuration/(1000 * allServiceSearchCount));
	}
	
	public long avgDiscoveryFrequencySec() {
		if (discoveredCount == 0) {
			return 0;
		}
		return (totalDiscovery/(1000 * discoveredCount));
	}

	public long avgServiceDiscoveryFrequencySec() {
		if (serviceDiscoveredCount == 0) {
			return 0;
		}
		return (totalServiceDiscovery/(1000 * serviceDiscoveredCount));
	}
	
	public long avgServiceSearchDurationSec() {
		if (serviceSearchCount == 0) {
			return 0;
		}
		return (totalServiceSearchDuration/(1000 * serviceSearchCount));
	}
	
	public long serviceSearchSuccess() {
		if ((serviceSearchCount) == 0) {
			return 0;
		}
		return (100 * serviceDiscoveredCount)/(serviceSearchCount);
	}
	
}
