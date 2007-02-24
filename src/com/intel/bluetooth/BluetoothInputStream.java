/*
 Copyright 2004 Intel Corporation

 This file is part of Blue Cove.

 Blue Cove is free software; you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation; either version 2.1 of the License, or
 (at your option) any later version.

 Blue Cove is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with Blue Cove; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.intel.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.bluetooth.LocalDevice;

class BluetoothInputStream extends InputStream {
	private BluetoothConnection conn;
	
	protected 	PipedInputStream		pInput;
	protected 	PipedOutputStream		pOutput;
	private		boolean					flushEnabled;
	private		PipeFlusher				flushThread;

	public BluetoothInputStream(BluetoothConnection conn) {
		this.conn = conn;

		try {
			pInput = new PipedInputStream();
			pOutput = new PipedOutputStream(pInput);
			/* Some implementations will add data into the buffer as soon as a 
			 * conncetion is enabled. This thread flushes it so the OS doesn't 
			 * stall in a callback do to the piped buffer filling up.
			 */
			flushThread = null;
			close();
		} catch (Exception exp) {
			// should never happen
			exp.printStackTrace();
		}
	}
	private synchronized void setFlushThread(PipeFlusher aThread) {
		flushThread = aThread;
	}
	protected synchronized void open() {
		
		flushEnabled = false;
		flushThread.interrupt();
		flushThread = null;
		
	}
	private class PipeFlusher extends Thread {
		public void run() {
			this.setDaemon(true);
			this.setName("Bluecove Buffer Flusher");
			while(flushEnabled) {
				try {
					pInput.read();
				} catch (IOException exp) {
					
				}
			}
			setFlushThread(null);
		}
	}
	public int available() throws IOException {
		return pInput.available();
	}

	public void close() throws IOException {
		if(flushThread == null) {
			flushThread = new PipeFlusher();
			flushEnabled = true;
			flushThread.start();
		}
	}
	public int read() throws IOException {
		pipePrime(1);
		return pInput.read();
	}
	public long skip(long n) throws IOException {
		return pInput.skip(n);
	}

	public int read(byte[] b, int off, int len) throws IOException {
		pipePrime(len);
		return pInput.read( b, off, len);
	}

	/**
	 * For OS's that need to be polled for data this initiates the polling
	 * for {@code len} bytes.
	 * 
	 * @param len 	the number of bytes desired
	 */
	private native void  pipePrime(int len);
}