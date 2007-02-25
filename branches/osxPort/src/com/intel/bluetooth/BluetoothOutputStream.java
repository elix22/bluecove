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
import java.io.OutputStream;

class BluetoothOutputStream extends OutputStream {
	private BluetoothRFCOMMConnection conn;
	private	boolean closed;

	public BluetoothOutputStream(BluetoothRFCOMMConnection conn) {
		this.conn = conn;
		closed = true;
	}

	public void write(int b) throws IOException {
		if(closed) throw new IOException("Stream closed");
		if (conn == null)
			throw new IOException();
		byte		aByte[] = new byte[1];
		aByte[0] = (byte)(b & 0x000000FFL);
		conn.send(aByte);
		
	}
	public void write(byte[] b) throws IOException {
		if(closed) throw new IOException("Stream closed");
		if(b==null) throw new NullPointerException();
		conn.send(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		if(closed) throw new IOException("Stream closed");
		if(b==null) throw new NullPointerException();
		if (off < 0 || len < 0 || off + len > b.length) {
			throw new IndexOutOfBoundsException();
		}

		if (conn == null) throw new IOException();
		/* see if we can avoid a copy */
		if(off==0 && len==b.length) {
			conn.send(b);
		} else {
			byte[]			data = new byte[len];
			System.arraycopy(b, off, data, 0, len);
			conn.send(data);
		}
	}
	public void open() {
		closed = false;
	}
	public void close() throws IOException {
		// leave all the work to the connection
		// we may be opened again.
		closed = true;
	}
}