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
	private BluetoothRFCOMMConnection conn;
	
	protected 	LargePipedInputStream	pInput;
	protected 	PipedOutputStream		pOutput;
	private		boolean					flushEnabled;
	private		PipeFlusher				flushThread;
	static private	boolean				disableDeadlockPreventor;
	
	static {
		disableDeadlockPreventor = true;
	}
	public static void disableDeadlockPreventor() {
		disableDeadlockPreventor = true;
	}

	public BluetoothInputStream(BluetoothRFCOMMConnection conn) {
		this.conn = conn;

		try {
			pInput = new LargePipedInputStream();
			pOutput = new PipedOutputStream(pInput);
			/* Some implementations will add data into the buffer as soon as a 
			 * conncetion is enabled. This thread flushes it so the OS doesn't 
			 * stall in a callback do to the piped buffer filling up.
			 */
			if(!disableDeadlockPreventor) new DeadlockPrevention().start();
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
		public PipeFlusher() {
			super();
			setDaemon(true);
			setName("Bluecove Buffer Flusher");
		}
		public void run() {
			while(flushEnabled) {
				try {
					pInput.read();
				} catch (IOException exp) {
					
				}
			}
			setFlushThread(null);
		}
	}
	/**
	 * 
	 */
	private class LargePipedInputStream extends PipedInputStream {
		
		public LargePipedInputStream() {
			super();
		}
		public LargePipedInputStream(int bufferSize) {
			super();
			buffer = new byte[bufferSize];
		}
		public LargePipedInputStream(PipedOutputStream src) throws IOException {
			super(src);
	    }
		public LargePipedInputStream(PipedOutputStream src, int bufferSize) throws IOException {
			super(src);
			buffer = new byte[bufferSize];
	    }
		public int getBufferSize() {
			return (buffer.length);
		}
		
	}
	/**
	 * Prevents deadlock in the Bluetooth stack by dropping data after the
	 * buffer fills. On OS X there is the potential to deadlock the BT 
	 * stack while it tries to write to the piped buffer here. If no one 
	 * is reading the buffer and it fills up the BT stack won't accept 
	 * any more commands.
	 *
	 */
	private class DeadlockPrevention extends Thread {
		private static final int		maxSleepTime = 20000;
		public DeadlockPrevention() {
			super();
			setDaemon(true);
			setName("Input Buffer Deadlock Prevention");
		}
		public void run() {
			int		sleepTime = maxSleepTime;
			while(!disableDeadlockPreventor) {
				try {
					sleep(sleepTime);
				} catch (InterruptedException inttr) {
					
				}
				int		bytesInBuffer;
				try {
					bytesInBuffer = available();
				} catch(IOException exp) {
					bytesInBuffer =0;
				}
				if(bytesInBuffer == pInput.getBufferSize()) {
					// buffer is full lets flush it
					int			len = pInput.getBufferSize();
					byte[]		bogusBuffer = new byte[len];
					try {
						len = pInput.read(bogusBuffer, 0, len);
						System.err.println("Warning: "+ String.valueOf(len) + " bytes lost do to overflowing input buffer");
						sleepTime = 100;
					} catch (IOException exp) {
						
					}
				} else {
					/* get the percentage fullness of buffer */
					float percent = 100.0f*bytesInBuffer / pInput.getBufferSize();
					if(percent > 80f) System.err.println("Warning: Input buffer is " + String.valueOf(percent)+ " full,");
					sleepTime = (int)((100 - percent) * maxSleepTime);
				}
				
			}
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