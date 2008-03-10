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

	private static long connectionCount = 0;

	private long connectionId = 0;

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
		if (connectionId <= 0) {
			throw new InterruptedIOException();
		}
		return connectionId;
	}

	long connect(Device clientDevice, boolean authenticate, boolean encrypt) throws IOException {
		try {
			PipedInputStream cis = new PipedInputStream();
			PipedOutputStream sos = new PipedOutputStream(cis);

			PipedInputStream sis = new PipedInputStream();
			PipedOutputStream cos = new PipedOutputStream(sis);

			ConnectionBuffer cb = new ConnectionBufferRFCOMM(serverDevice.getDescriptor().getAddress(), cis, cos);
			ConnectionBuffer sb = new ConnectionBufferRFCOMM(clientDevice.getDescriptor().getAddress(), sis, sos);

			long id;
			synchronized (ServiceListener.class) {
				connectionCount++;
				id = connectionCount;
			}
			clientDevice.addConnectionBuffer(id, cb);
			serverDevice.addConnectionBuffer(id, sb);

			connectionId = id;
			return id;
		} finally {
			synchronized (lock) {
				lock.notify();
			}
		}
	}

	void close() {
		connectionId = -1;
		synchronized (lock) {
			lock.notify();
		}
	}
}
