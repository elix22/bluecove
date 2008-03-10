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

import javax.bluetooth.ServiceRegistrationException;

import com.pyx4j.rpcoverhttp.common.RoHService;

public interface DeviceManagerService extends RoHService {

	public EmulatorConfiguration getEmulatorConfiguration();

	public DeviceDescriptor createNewDevice(String deviceID, String deviceAddress);

	public void releaseDevice(long address);

	public int getLocalDeviceDiscoverable(long address);

	public boolean setLocalDeviceDiscoverable(long address, int mode);

	public DeviceDescriptor[] getDiscoveredDevices(long address);

	public String getRemoteDeviceFriendlyName(long address);

	public void updateServiceRecord(long address, long handle, ServicesDescriptor sdpData)
			throws ServiceRegistrationException;

	public void removeServiceRecord(long address, long handle);

	public long[] searchServices(long address, String[] uuidSet);

	public byte[] getServicesRecordBinary(long address, long handle) throws IOException;

	public long rfAccept(long address, int channel, boolean authenticate, boolean encrypt) throws IOException;

	public long rfConnect(long address, int channel, boolean authenticate, boolean encrypt) throws IOException;

	public void rfCloseService(long address, int channel);

	public void rfCloseConnection(long address, long connectionId);

	public long l2Accept(long address, int channel, boolean authenticate, boolean encrypt) throws IOException;

	public long l2Connect(long address, int channel, boolean authenticate, boolean encrypt) throws IOException;

	public void l2CloseService(long address, int channel);

	public void rfWrite(long address, long connectionId, byte[] b) throws IOException;

	public int rfAvailable(long address, long connectionId) throws IOException;

	public byte[] rfRead(long address, long connectionId, int len) throws IOException;
}
