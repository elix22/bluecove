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
import java.io.InputStream;
import java.io.InterruptedIOException;

/**
 * @author vlads
 * 
 */
public class ConnectedInputStream extends InputStream {

	/**
	 * The circular buffer which receives data.
	 */
	private byte buffer[];

	private boolean closed = false;

	private boolean receiverClosed = false;

	/**
	 * The index of the position in the circular buffer at which the byte of
	 * data will be stored.
	 */
	private int write = 0;

	/**
	 * The index of the position in the circular buffer from which the next byte
	 * of data will be read.
	 */
	private int read = 0;

	private int available = 0;

	public ConnectedInputStream(int size) {
		buffer = new byte[size];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.InputStream#read()
	 */
	public synchronized int read() throws IOException {
		while (available == 0) {
			if (closed) {
				throw new IOException("Stream closed");
			}
			if (receiverClosed) {
				// EOF
				return -1;
			}
			// Let the receive run
			notifyAll();
			try {
				wait(1000);
			} catch (InterruptedException e) {
				throw new InterruptedIOException();
			}
		}
		int r = buffer[read++] & 0xFF;
		if (read >= buffer.length) {
			read = 0;
		}
		available--;
		return r;
	}

	public synchronized int available() throws IOException {
		return available;
	}

	synchronized void receive(int b) throws IOException {
		if (available == buffer.length) {
			waitFreeBuffer();
		}
		buffer[write++] = (byte) (b & 0xFF);
		if (write >= buffer.length) {
			write = 0;
		}
		available++;
	}

	private void waitFreeBuffer() throws IOException {
		while (available == buffer.length) {
			if (receiverClosed || closed) {
				throw new IOException("Receiver closed");
			}
			// Let the read run
			notifyAll();
			try {
				wait(1000);
			} catch (InterruptedException e) {
				throw new InterruptedIOException();
			}
		}
	}

	synchronized void receiverClose() throws IOException {
		receiverClosed = true;
		notifyAll();
	}

	public synchronized void close() throws IOException {
		closed = true;
		notifyAll();
	}
}
