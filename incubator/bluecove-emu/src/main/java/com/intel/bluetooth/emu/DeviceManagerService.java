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

import com.pyx4j.rpcoverhttp.common.RoHService;

public interface DeviceManagerService extends RoHService {

	public EmulatorConfiguration getEmulatorConfiguration();

	public DeviceDescriptor createNewDevice(String deviceID, String deviceAddress);

	public void releaseDevice(long address);

	public int getLocalDeviceDiscoverable(long address);

	public boolean setLocalDeviceDiscoverable(long address, int mode);

	public DeviceDescriptor[] getDiscoveredDevices(long address);

	public String getRemoteDeviceFriendlyName(long address);

}
