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

	public void releaseDevice(long localAddress);

	public int getLocalDeviceDiscoverable(long localAddress);

	public boolean setLocalDeviceDiscoverable(long localAddress, int mode);

	public void setLocalDeviceServiceClasses(long localAddress, int classOfDevice);

	public DeviceDescriptor[] getDiscoveredDevices(long localAddress);

	public String getRemoteDeviceFriendlyName(long remoteAddress);

	public void updateServiceRecord(long localAddress, long handle, ServicesDescriptor sdpData)
			throws ServiceRegistrationException;

	public void removeServiceRecord(long localAddress, long handle);

	public long[] searchServices(long remoteAddress, String[] uuidSet);

	public byte[] getServicesRecordBinary(long remoteAddress, long handle) throws IOException;

	public long rfAccept(long localAddress, int channel, boolean authenticate, boolean encrypt) throws IOException;

	public long rfConnect(long localAddress, long remoteAddress, int channel, boolean authenticate, boolean encrypt)
			throws IOException;

	public void rfCloseService(long localAddress, int channel);

	public void closeConnection(long localAddress, long connectionId) throws IOException;

	public long l2Accept(long localAddress, int channel, boolean authenticate, boolean encrypt, int receiveMTU)
			throws IOException;

	public long l2Connect(long localAddress, long remoteAddress, int channel, boolean authenticate, boolean encrypt,
			int receiveMTU) throws IOException;

	public int l2RemoteDeviceReceiveMTU(long localAddress, long connectionId) throws IOException;

	public void l2CloseService(long localAddress, int channel);

	public long getRemoteAddress(long localAddress, long connectionId) throws IOException;

	public void rfWrite(long localAddress, long connectionId, byte[] b) throws IOException;

	public int rfAvailable(long localAddress, long connectionId) throws IOException;

	public byte[] rfRead(long localAddress, long connectionId, int len) throws IOException;

	public boolean l2Ready(long localAddress, long connectionId) throws IOException;

	public byte[] l2Receive(long localAddress, long connectionId, int len) throws IOException;

	public void l2Send(long localAddress, long connectionId, byte[] data) throws IOException;
}
