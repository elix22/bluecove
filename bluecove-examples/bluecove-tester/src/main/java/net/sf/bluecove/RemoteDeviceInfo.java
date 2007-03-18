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
	
	public String name;
	
	public RemoteDevice remoteDevice;

	public int discoveredCount;
	
	public long discoveredFirstTime;
	
	public long discoveredLastTime;
	
	private TimeStatistic deviceDiscovery = new TimeStatistic();

	public static TimeStatistic deviceInquiryDuration = new TimeStatistic(); 
	
	private TimeStatistic serviceSearch = new TimeStatistic();

	public static TimeStatistic allServiceSearch = new TimeStatistic(); 
	
	public long serviceDiscoveredFirstTime;
	
	public long serviceDiscoveredLastTime;
	
	public TimeStatistic serviceDiscovered = new TimeStatistic();
	
	public byte[] variableData;
	
	public long variableDataCheckLastTime;
	
	public boolean variableDataUpdated = false;
	
	public static synchronized void clear() {
		devices = new Hashtable();
		allServiceSearch.clear();
		deviceInquiryDuration.clear();
	}
	
	public static synchronized RemoteDeviceInfo getDevice(RemoteDevice remoteDevice) {
		String addr = remoteDevice.getBluetoothAddress().toUpperCase();
		RemoteDeviceInfo devInfo = (RemoteDeviceInfo)devices.get(addr);
		if (devInfo == null) {
			devInfo = new RemoteDeviceInfo();
			devInfo.name = TestResponderClient.niceDeviceName(addr);
			devices.put(addr, devInfo);
		}
		return devInfo;
	}

	public static synchronized void deviceFound(RemoteDevice remoteDevice) {
		RemoteDeviceInfo devInfo = getDevice(remoteDevice);
		long now = System.currentTimeMillis();
		if (devInfo.discoveredCount == 0) {
			devInfo.discoveredFirstTime = now;
			devInfo.deviceDiscovery.add(0);
		} else {
			devInfo.deviceDiscovery.add(now - devInfo.discoveredLastTime);
		}
		devInfo.remoteDevice = remoteDevice;
		devInfo.discoveredCount ++;
		devInfo.discoveredLastTime = now; 
	}
	
	public static synchronized void deviceServiceFound(RemoteDevice remoteDevice, byte[] variableData) {
		RemoteDeviceInfo devInfo = getDevice(remoteDevice);
		long now = System.currentTimeMillis();
		if (devInfo.serviceDiscovered.count == 0) {
			devInfo.serviceDiscoveredFirstTime = now;
			devInfo.serviceDiscovered.add(0);
		} else {
			devInfo.serviceDiscovered.add(now - devInfo.serviceDiscoveredLastTime);
		}
		devInfo.remoteDevice = remoteDevice;
		devInfo.serviceDiscoveredLastTime = now;
		if (variableData != null) {
			long frequencyMSec = now - devInfo.variableDataCheckLastTime;
			if ((devInfo.variableData != null) && (frequencyMSec > 1000 * (20 + Configuration.serverMAXTimeSec))) {
				devInfo.variableDataCheckLastTime = now;
				boolean er = false; 
				if (variableData[0] == devInfo.variableData[0]) {
					Logger.warn("not updated [0]  " + variableData[0]);
					TestResponderClient.failure.addFailure("not updated [0]  " + variableData[1] + " on " + devInfo.name);
					er = true;
				}
				if (variableData[1] == devInfo.variableData[1]) {
					Logger.warn("not updated count 1  " + variableData[1]);
					TestResponderClient.failure.addFailure("not updated [1]  " + variableData[1] + " on " + devInfo.name);
					er = true;
				}
				
				if (!er) {
					devInfo.variableDataUpdated = true;
					Logger.warn("Var info OK" + variableData[0] + " " + variableData[1]);
				}
			}
			devInfo.variableData = variableData;
		}
	}
	
	public static synchronized void searchServices(RemoteDevice remoteDevice, boolean found, long servicesSearch) {
		RemoteDeviceInfo devInfo = getDevice(remoteDevice);
		devInfo.serviceSearch.add(servicesSearch);
		allServiceSearch.add(servicesSearch);
	}

	public static void discoveryInquiryFinished(long discoveryInquiry) {
		deviceInquiryDuration.add(discoveryInquiry);
	}
	
	public static long allAvgDeviceInquiryDurationSec() {
		return deviceInquiryDuration.avgSec();
	}
	
	public static long allAvgServiceSearchDurationSec() {
		return allServiceSearch.avgSec();
	}
	
	public long avgDiscoveryFrequencySec() {
		return deviceDiscovery.avgSec();
	}

	public long avgServiceDiscoveryFrequencySec() {
		return serviceDiscovered.avgSec();
	}
	
	public long avgServiceSearchDurationSec() {
		return serviceSearch.durationMaxSec();
	}
	
	public long serviceSearchSuccessPrc() {
		if ((serviceSearch.count) == 0) {
			return 0;
		}
		return (100 * serviceDiscovered.count)/(serviceSearch.count);
	}
	
}
