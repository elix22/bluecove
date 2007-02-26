package com.intel.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import javax.microedition.io.StreamConnection;

/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Eric Wagner
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
 */
public class BluetoothRFCOMMConnection extends BluetoothL2CAPConnection 
			implements StreamConnection {

	protected BluetoothInputStream			in;
	protected BluetoothOutputStream			out;
	
	protected BluetoothRFCOMMConnection(String address, int channel, boolean authenticate,
			boolean encrypt, int requestedRecieveMTU, int requestedTransmitMTU) throws IOException {
		super(address, channel, authenticate, encrypt, requestedRecieveMTU, requestedTransmitMTU);
		
	}

	protected void connect(int socket, long address, int channel,int requestedRecieveMTU,
			int requestedTransmitMTU)	throws IOException {
		in = new BluetoothInputStream(this);
		out = new BluetoothOutputStream(this);
		nativeConnect(socket, address, channel, requestedRecieveMTU, requestedTransmitMTU);
	}
	private native void nativeConnect(int socket, long address, int channel,
			int requestedRecieveMTU, int requestedTransmitMTU) throws IOException;
	
	public InputStream openInputStream() throws IOException {
		in.open();
		return in;
	}
	public DataInputStream openDataInputStream() throws IOException {
		throw new UnsupportedOperationException();
	}
	
	public OutputStream openOutputStream() throws IOException {
		out.open();
		return out;
	}

	public DataOutputStream openDataOutputStream() throws IOException {
		throw new UnsupportedOperationException();
	}
	public int receive(byte[] inBuf) throws java.io.IOException {
		return in.read(inBuf);
	}
	public native void send(byte[] data) throws java.io.IOException;

	
}
