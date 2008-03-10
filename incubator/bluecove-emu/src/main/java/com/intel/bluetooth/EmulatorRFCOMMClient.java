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

/**
 * @author vlads
 * 
 */
class EmulatorRFCOMMClient extends EmulatorConnection {

	private long remoteAddress;

	public EmulatorRFCOMMClient(EmulatorLocalDevice localDevice, long handle) {
		super(localDevice, handle);

	}

	public void connect(long remoteAddress, long connectionHandle) throws IOException {
		this.connectionHandle = connectionHandle;
	}

	public void connect(BluetoothConnectionParams params) throws IOException {
		this.remoteAddress = params.address;
		this.connectionHandle = localDevice.getDeviceManagerService().rfConnect(params.address, params.channel,
				params.authenticate, params.encrypt);
	}

	public int read() throws IOException {
		byte buf[] = new byte[1];
		int len = read(buf, 0, 1);
		if (len == -1) {
			return -1;
		}
		return buf[0];
	}

	public int read(byte[] b, int off, int len) throws IOException {
		byte buf[] = localDevice.getDeviceManagerService().rfRead(remoteAddress, this.connectionHandle, len);
		if (buf == null) {
			return -1;
		}
		System.arraycopy(buf, 0, b, off, len);
		return buf.length;
	}

	public int available() throws IOException {
		return localDevice.getDeviceManagerService().rfAvailable(remoteAddress, this.connectionHandle);
	}

	public void write(int b) throws IOException {
		byte buf[] = new byte[1];
		buf[0] = (byte) (b & 0xFF);
		write(buf, 0, 1);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		byte buf[];
		if ((b.length == len) && (off == 0)) {
			buf = b;
		} else {
			buf = new byte[len];
			System.arraycopy(b, off, buf, 0, len);
		}
		localDevice.getDeviceManagerService().rfWrite(remoteAddress, this.connectionHandle, buf);
	}

	public void flush() throws IOException {
	}

	public long getRemoteAddress() throws IOException {
		return remoteAddress;
	}

	public void close() throws IOException {
		localDevice.getDeviceManagerService().rfCloseConnection(remoteAddress, this.connectionHandle);
	}
}
