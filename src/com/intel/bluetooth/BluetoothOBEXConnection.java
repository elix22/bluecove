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

package com.intel.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.bluetooth.BluetoothConnectionException;

public class BluetoothOBEXConnection extends BluetoothRFCOMMConnection {
	
	protected BluetoothOBEXConnection(String address, int channel, boolean authenticate,
			boolean encrypt, int requestedRecieveMTU, int requestedTransmitMTU) throws IOException {
		super(address, channel, authenticate, encrypt, requestedRecieveMTU, requestedTransmitMTU);
		
	}
	
	private native void nativeConnect(int socket, long address, int channel,
			int requestedRecieveMTU, int requestedTransmitMTU) throws IOException;

	public DataInputStream openDataInputStream() throws IOException {
		throw new UnsupportedOperationException();
	}


	public DataOutputStream openDataOutputStream() throws IOException {
		throw new UnsupportedOperationException();
	}
	
	public native void send(byte[] data) throws java.io.IOException;

}