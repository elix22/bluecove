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
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * @author vlads
 * 
 */
class ServiceListener {

	private static final String RFCOMM_PREFIX = "rfcomm-";

	private static final String L2CAP_PREFIX = "l2cap-";

	private String portID;

	private Object lock = new Object();

	private Device serverDevice;

	static String rfPrefix(int channel) {
		return RFCOMM_PREFIX + channel;
	}

	static String l2Prefix(int channel) {
		return L2CAP_PREFIX + channel;
	}

	ServiceListener(String portID) {
		this.portID = portID;
	}

	String getPortID() {
		return this.portID;
	}

	long accept(Device serverDevice, boolean authenticate, boolean encrypt) throws IOException {
		this.serverDevice = serverDevice;
		synchronized (lock) {
			try {
				lock.wait();
			} catch (InterruptedException e) {
				throw new InterruptedIOException();
			}
		}
		throw new IOException("TODO");
		// return 0;
	}

	long connect(Device clientDevice, boolean authenticate, boolean encrypt) throws IOException {
		PipedInputStream cis = new PipedInputStream();
		PipedOutputStream sos = new PipedOutputStream(cis);

		PipedInputStream sis = new PipedInputStream();
		OutputStream cos = new PipedOutputStream(sis);

		ConnectionBufferRFCOMM c = new ConnectionBufferRFCOMM(serverDevice.getDescriptor().getAddress(), cis, cos);
		ConnectionBufferRFCOMM s = new ConnectionBufferRFCOMM(clientDevice.getDescriptor().getAddress(), sis, sos);

		synchronized (lock) {
			lock.notify();
		}
		throw new IOException("TODO");
	}

	void close() {
		synchronized (lock) {
			lock.notify();
		}
	}

	static ConnectionBufferRFCOMM getConnectionBufferRFCOMM(Device localDevice, long connectionId) throws IOException {
		return null;
	}
}
