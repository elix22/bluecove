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
import java.util.NoSuchElementException;
import java.util.Vector;

import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.RemoteDevice;

public class BluetoothL2CAPConnection implements L2CAPConnection {
	
	int socket;
	long binaryAddress;
	String	textAddress;
	BluetoothOutputStream out;
	private boolean closing;
	private boolean closed;
	protected RemoteDevice		endpoint;
	
	protected int			transmitMTU;
	protected int			receiveMTU;
	/**
	 * A {@link java/util/Vector} of byte arrays wrapped in arrayWrappers
	 * representing one packet each
	 */
	protected Vector		packetQueue;

	private class arrayWrapper {
		public			byte[]		buffer;
	}
	/** Create an L2CAP packet connection. Note that streams aren't available
	 * for this type of connection.
	 * @param address the remote device's Bluetooth address
	 * @param channel the channel to connect to on the remote device
	 * @param authenticate if authentication should be attempted
	 * @param encrypt if encryption should be attempted
	 * @param requestedRecieveMTU the requested recieve MTU. 
	 * @param requestedTransmitMTU the requested transmit MTU
	 * @throws IOException 
	 */
	protected BluetoothL2CAPConnection(String address, int channel, boolean authenticate,
			boolean encrypt, int requestedRecieveMTU, int requestedTransmitMTU) 
				throws IOException {
		endpoint = null;
		textAddress = address;
		binaryAddress = Long.parseLong(address, 16);
		socket = socket(authenticate, encrypt);
		closed = true;
		closing = false;
		connect(socket, binaryAddress, channel, requestedRecieveMTU, requestedTransmitMTU);
		closed = false;
	}

	/** Create a socket to refer to an OS's implementation of a connection. 
	 * 
	 * @param authenticate if authentication should be attempted on connection
	 * @param encrypt if encryption should be attempted on connection
	 * @return the socket number
	 * @throws IOException
	 */
	protected native int socket(boolean authenticate, boolean encrypt)
			throws IOException;
	
	protected void connect(int socket, long address, int channel,int requestedRecieveMTU,
			int requestedTransmitMTU)	throws IOException {
		packetQueue = new Vector();
		nativeConnect(socket, address, channel, requestedRecieveMTU, requestedTransmitMTU);
	}
	
	private native void nativeConnect(int socket, long address, int channel,
			int requestedRecieveMTU, int requestedTransmitMTU) throws IOException;
	
	/* see super class for docs. the native library should set the 
	 * MTU's in the connect method */
	
	public int getTransmitMTU() throws java.io.IOException {
		if(closed) throw new IOException("Connection is closed");
		return transmitMTU;
	}
	/* see super class for docs. the native library should set the 
	 * MTU's in the connect method */
	public int getReceiveMTU() throws java.io.IOException {
		if(closed) throw new IOException("Connection is closed");
		return receiveMTU;
	}
	
	public boolean ready() throws java.io.IOException {

		return (packetQueue.size() > 0);

	}
	
	/* should a timeout be put in? */
	public synchronized int receive(byte[] inBuf) throws java.io.IOException {
		if(closed || closing) throw new IOException();
		if(inBuf == null) throw new NullPointerException();
		arrayWrapper	anObj = null;
		while(anObj == null) {
			try {
				anObj = (arrayWrapper)packetQueue.firstElement();
				packetQueue.remove(0);
			} catch (NoSuchElementException exp) {
				// no element found sow we wait
				try {
					wait();
				} catch (InterruptedException exp2) {
					
				}
			}
		}
		// find who has the bigger buffer
		byte[]		aPacket = anObj.buffer;
		int			copySize;
		
		if(aPacket.length < inBuf.length) {
			copySize = aPacket.length;
		} else copySize = inBuf.length;
		
		System.arraycopy(aPacket, 0, inBuf, 0, copySize);
		return copySize;
	}
	public native void send(byte[] data) throws java.io.IOException;
	
	
	public String getRemoteTextAddress() throws IOException {
		if(closing) throw new IOException();
		return textAddress;
	}
	public RemoteDevice getRemoteDevice() throws IOException{
		if(closing) throw new IOException();
		if(endpoint==null) endpoint = new RemoteDeviceImpl(textAddress);
		return endpoint;
	}
	
	public void close() throws IOException {
		closing = true;
		closeSocket(socket);
	}
	private native void closeSocket(int aSocket);
	
	public void finalize() throws Throwable {
		try {
			if(!closing) close();
		} catch (IOException exp) {
			System.err.println("IOException attempting to close abanded Bluetooth Exception");
			exp.printStackTrace();
		}
	}
}