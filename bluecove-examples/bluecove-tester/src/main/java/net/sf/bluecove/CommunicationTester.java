/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
package net.sf.bluecove;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.StreamConnection;

import junit.framework.Assert;

public class CommunicationTester implements Consts {

	public static final UUID uuid = new UUID(Consts.RESPONDER_UUID, Consts.useShortUUID); 

	public static boolean dataOutputStreamFlush = true;
	
	public static int clientConnectionOpenRetry = 3;
	
	private static final String stringData = "TestString2007";
	
	private static final String stringUTFData = "\u0413\u043E\u043B\u0443\u0431\u043E\u0439\u0417\u0443\u0431";
	
	private static final int byteCount = 12; //1024;
	
	private static final byte[] byteAray = new byte[] {1 , 7, -40, 80, 90, -1, 126, 100, 87, -10, 127, 31, -127, 0, -77};  
	
	private static final byte streamAvailableByteCount = 126;
	
	private static final int byteArayLargeSize = 0x2010; // More then 8K
	
	private static final byte aKnowndPositiveByte = 21;
	
	private static final byte aKnowndNegativeByte = -33;
	
	static void sendString(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeUTF(stringData);
		if (dataOutputStreamFlush) {
			dos.flush();
		}
	}
	
	static void readString(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		String got = dis.readUTF();
		Assert.assertEquals("ReadString", stringData, got);
	}

	static void sendUTFString(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeUTF(stringUTFData);
		if (dataOutputStreamFlush) {
			dos.flush();
		}
	}
	
	static void readUTFString(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		String got = dis.readUTF();
		Assert.assertEquals("ReadString", stringUTFData, got);
	}
	
	static void sendByte(OutputStream os) throws IOException {
		os.write(aKnowndPositiveByte);
		os.write(aKnowndNegativeByte);
		
		// Test int conversions
		int bp = aKnowndPositiveByte;
		os.write(bp);
		int bn = aKnowndNegativeByte;
		os.write(bn);
		
		for(int i = 1; i < byteCount; i++) {
			os.write((byte)i);
		}
		for(int i = 0; i < byteAray.length; i++) {
			os.write(byteAray[i]);
		}
		// The byte to be written is the eight low-order bits of the argument b.
		os.write(0xABC);
		os.write(aKnowndPositiveByte);
	}
	
	static void readByte(InputStream is) throws IOException {
		Assert.assertEquals("positiveByte", aKnowndPositiveByte, (byte)is.read());
		Assert.assertEquals("negativeByte", aKnowndNegativeByte, (byte)is.read());
		Assert.assertEquals("positiveByte written(int)", aKnowndPositiveByte, (byte)is.read());
		Assert.assertEquals("negativeByte written(int)", aKnowndNegativeByte, (byte)is.read());
		for(int i = 1; i < byteCount; i++) {
			byte got = (byte)is.read();
			Assert.assertEquals("t1, byte [" + i + "]", (byte)i, got);
		}
		for(int i = 0; i < byteAray.length; i++) {
			byte got = (byte)is.read();
			Assert.assertEquals("t2, byte [" + i + "]", byteAray[i], got);
		}
		int abc = is.read();
		Assert.assertEquals("written(0xABC)", 0xBC, abc);
		Assert.assertEquals("positiveByte", aKnowndPositiveByte, (byte)is.read());
	}

	static void sendByteAray(OutputStream os) throws IOException {
		os.write(byteAray);
	}

	static void readByteAray(InputStream is) throws IOException {
		byte[] byteArayGot = new byte[byteAray.length];
		int got = is.read(byteArayGot);
		Assert.assertEquals("byteAray.len", byteAray.length, got);
		for(int i = 0; i < byteAray.length; i++) {
			Assert.assertEquals("byte", byteAray[i], byteArayGot[i]);
		}
	}

	static void sendDataStream(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeUTF(stringData);
		dos.writeInt(1025);
		dos.writeLong(567890025);
		dos.writeBoolean(true);
		dos.writeBoolean(false);
		dos.writeChar('O');
		dos.writeShort(541);
//		CLDC_1_0 dos.writeFloat((float)3.14159); 
//		CLDC_1_0 dos.writeDouble(Math.E); 
		dos.writeByte(aKnowndPositiveByte);
		dos.writeByte(aKnowndNegativeByte);
		if (dataOutputStreamFlush) {
			dos.flush();
		}
	}
	
	static void readDataStream(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		String got = dis.readUTF();
		Assert.assertEquals("ReadString", stringData, got);
		Assert.assertEquals("ReadInt", 1025, dis.readInt());
		Assert.assertEquals("ReadLong", 567890025, dis.readLong());
		Assert.assertEquals("ReadBoolean", true, dis.readBoolean());
		Assert.assertEquals("ReadBoolean2", false, dis.readBoolean());
		Assert.assertEquals("ReadChar", 'O', dis.readChar());
		Assert.assertEquals("readShort", 541, dis.readShort());
//		CLDC_1_0 Assert.assertEquals("readFloat", (float)3.14159, dis.readFloat(), (float)0.0000001);
//		CLDC_1_0 Assert.assertEquals("readDouble", Math.E, dis.readDouble(), 0.0000000000000001);
		Assert.assertEquals("positiveByte", aKnowndPositiveByte, dis.readByte());
		Assert.assertEquals("negativeByte", aKnowndNegativeByte, dis.readByte());
	}
	
	private static void sendStreamAvailable(InputStream is, OutputStream os) throws IOException {
		for(int i = 1; i < streamAvailableByteCount; i++) {
			os.write(i);
			if (i % 10 == 0) {
				os.flush();
			}
		}
		// Long test need conformation
		os.flush();
		byte got = (byte)is.read();
		Assert.assertEquals("conformation byte", streamAvailableByteCount, got);
	}

	private static void readStreamAvailable(InputStream is, OutputStream os) throws IOException {
		int available = 0; 
		for(int i = 1; i < streamAvailableByteCount; i++) {
			boolean hasData = (available > 0);
			int tryCount = 0;
			while(!hasData) {
				// This blocks on Nokia(Srv) on second call connected to Widcomm(Client)
				available = is.available();
				if (available > 0) {
					hasData = true;
				} else if (available < 0) {
					Assert.fail("negative available");
				}
				tryCount ++;
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					Assert.fail("Test Interrupted");
				}
				if (tryCount > 70) {
					Assert.fail("Test Available took too long, got " + i + " bytes");
				}
			}
			
			byte got = (byte)is.read();
			Assert.assertEquals("byte[" + i + "]", i, got);
			available --;
		}
		os.write(streamAvailableByteCount);
		os.flush();
	}
	
	private static void sendEOF(InputStream is, OutputStream os, TestStatus testStatus) throws IOException  {
		os.write(aKnowndPositiveByte);
		os.flush();
		// Let the server read the message
		try {
			Thread.sleep(700);
		} catch (InterruptedException e) {
			Assert.fail("Test Interrupted");
		}
		os.close();
		is.close();
		testStatus.streamClosed = true;
	}

	private static void readEOF(InputStream is, OutputStream os, TestStatus testStatus) throws IOException {
		Assert.assertEquals("byte", aKnowndPositiveByte, is.read());
		Assert.assertEquals("EOF expected", -1, is.read());
		testStatus.streamClosed = true;
		testStatus.isSuccess = true;
	}

	private static void sendArayEOF(InputStream is, OutputStream os, TestStatus testStatus) throws IOException {
		os.write(aKnowndPositiveByte);
		os.write(aKnowndNegativeByte);
		os.flush();
		// Let the server read the message
		try {
			Thread.sleep(700);
		} catch (InterruptedException e) {
			Assert.fail("Test Interrupted");
		}
		os.close();
		is.close();
		testStatus.streamClosed = true;
	}

	private static void readArayEOF(InputStream is, OutputStream os, TestStatus testStatus) throws IOException {
		byte[] byteArayGot = new byte[3];
		int got = is.read(byteArayGot);
		if (got == 1) {
			got += is.read(byteArayGot, 1, 2);
		}
		Assert.assertEquals("byteAray.len", 2, got);
		Assert.assertEquals("byte1", aKnowndPositiveByte, byteArayGot[0]);
		Assert.assertEquals("byte2", aKnowndNegativeByte, byteArayGot[1]);
		int got2 = is.read(byteArayGot);
		Assert.assertEquals("EOF expected", -1, got2);
		testStatus.streamClosed = true;
		testStatus.isSuccess = true;
	}

	private static void sendClosedConnection(InputStream is, OutputStream os, TestStatus testStatus) throws IOException {
		os.write(aKnowndPositiveByte);
		os.write(aKnowndNegativeByte);
		os.flush();
		// Let the server read the message
		try {
			Thread.sleep(700);
		} catch (InterruptedException e) {
			Assert.fail("Test Interrupted");
		}
		os.close();
		is.close();
		testStatus.streamClosed = true;
		
		try {
			os.write(byteAray);
			os.flush();
			Assert.fail("Can write to closed OutputStream");
		} catch (IOException ok) {
			testStatus.isSuccess = true;
		}
	}

	private static void readClosedConnection(InputStream is, OutputStream os, TestStatus testStatus) throws IOException {
		Assert.assertEquals("byte1", aKnowndPositiveByte, (byte)is.read());
		Assert.assertEquals("byte2", aKnowndNegativeByte, (byte)is.read());
		testStatus.streamClosed = true;
		try {
			is.read();
		} catch (IOException helloAvetana) {
		}
		try {
			os.write(byteAray);
			os.flush();
			Assert.fail("Can write to closed BT Connection");
		} catch (IOException ok) {
			testStatus.isSuccess = true;
		}
	}
	
	private static void serverRemoteDevice(StreamConnection conn, InputStream is, OutputStream os, TestStatus testStatus) throws IOException {
		RemoteDevice device = RemoteDevice.getRemoteDevice(conn);
		Logger.debug("is connected to BTAddress " + device.getBluetoothAddress());
		DataInputStream dis = new DataInputStream(is);
		String gotBluetoothAddress = dis.readUTF();
		Assert.assertEquals("PairBTAddress", gotBluetoothAddress.toUpperCase(), device.getBluetoothAddress().toUpperCase());
	}
	
	private static void clientRemoteDevice(StreamConnection conn, InputStream is, OutputStream os, TestStatus testStatus) throws IOException {
		RemoteDevice device = RemoteDevice.getRemoteDevice(conn);
		Logger.debug("is connected toBTAddress " + device.getBluetoothAddress());
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeUTF(LocalDevice.getLocalDevice().getBluetoothAddress());
		if (dataOutputStreamFlush) {
			dos.flush();
		}
		Assert.assertEquals("PairBTAddress", testStatus.pairBTAddress.toUpperCase(), device.getBluetoothAddress().toUpperCase());
	}
	
	static void sendByteArayLarge(OutputStream os) throws IOException {
		byte[] byteArayLarge = new byte[byteArayLargeSize];
		for(int i = 0; i < byteArayLargeSize; i++) {
			byteArayLarge[i] = (byte)(i & 0xF);
		}
		os.write(byteArayLarge);
	}

	static void readByteArayLarge(InputStream is) throws IOException {
		byte[] byteArayGot = new byte[byteArayLargeSize];
		int got = 0;
		while (got < byteArayLargeSize) {
			int read = is.read(byteArayGot, got, byteArayLargeSize - got);
			if (read == -1) {
				break;
			}
			got += read; 
		}
		
		Assert.assertEquals("byteArayLarge.len", byteArayLargeSize, got);
		for(int i = 0; i < byteArayLargeSize; i++) {
			Assert.assertEquals("byte", (i & 0xF), byteArayGot[i]);
		}
	}
	
	public static void runTest(int testType, boolean server, StreamConnection conn, InputStream is, OutputStream os, TestStatus testStatus) throws IOException {
		switch (testType) {
		case TEST_STRING:
			testStatus.setName("STRING");
			if (server) {
				CommunicationTester.readString(is);
			} else {
				CommunicationTester.sendString(os);
			}
			break;
		case TEST_STRING_BACK:
			testStatus.setName("STRING_BACK");
			if (!server) {
				CommunicationTester.readString(is);
			} else {
				CommunicationTester.sendString(os);
			}
			break;
		case TEST_BYTE:
			testStatus.setName("BYTE");
			if (server) {
				CommunicationTester.readByte(is);
			} else {
				CommunicationTester.sendByte(os);
			}
			break;
		case TEST_BYTE_BACK:
			testStatus.setName("BYTE_BACK");
			if (!server) {
				CommunicationTester.readByte(is);
			} else {
				CommunicationTester.sendByte(os);
			}
			break;
		case TEST_STRING_UTF:
			testStatus.setName("STRING_UTF");
			if (server) {
				CommunicationTester.readUTFString(is);
			} else {
				CommunicationTester.sendUTFString(os);
			}
		case TEST_STRING_UTF_BACK:
			testStatus.setName("STRING_UTF_BACK");
			if (!server) {
				CommunicationTester.readUTFString(is);
			} else {
				CommunicationTester.sendUTFString(os);
			}
		case TEST_BYTE_ARRAY:
			testStatus.setName("BYTE_ARRAY");
			if (server) {
				CommunicationTester.readByteAray(is);
			} else {
				CommunicationTester.sendByteAray(os);
			}
			break;
		case TEST_BYTE_ARRAY_BACK:
			testStatus.setName("BYTE_ARRAY_BACK");
			if (!server) {
				CommunicationTester.readByteAray(is);
			} else {
				CommunicationTester.sendByteAray(os);
			}
			break;
		case TEST_DataStream:
			testStatus.setName("DataStream");
			if (server) {
				CommunicationTester.readDataStream(is);
			} else {
				CommunicationTester.sendDataStream(os);
			}
			break;
		case TEST_DataStream_BACK:
			testStatus.setName("DataStream_BACK");
			if (!server) {
				CommunicationTester.readDataStream(is);
			} else {
				CommunicationTester.sendDataStream(os);
			}
			break;
		case TEST_StreamAvailable:
			testStatus.setName("StreamAvailable");
			if (server) {
				CommunicationTester.readStreamAvailable(is, os);
			} else {
				CommunicationTester.sendStreamAvailable(is, os);
			}
			break;
		case TEST_StreamAvailable_BACK:
			testStatus.setName("StreamAvailable_BACK");
			if (!server) {
				CommunicationTester.readStreamAvailable(is, os);
			} else {
				CommunicationTester.sendStreamAvailable(is, os);
			}
			break;
		case TEST_EOF_READ:
			testStatus.setName("EOF_READ");
			if (server) {
				CommunicationTester.readEOF(is, os, testStatus);
			} else {
				CommunicationTester.sendEOF(is, os, testStatus);
			}
			break;
		case TEST_EOF_READ_BACK:
			testStatus.setName("EOF_READ_BACK");
			if (!server) {
				CommunicationTester.readEOF(is, os, testStatus);
			} else {
				CommunicationTester.sendEOF(is, os, testStatus);
			}
			break;
		case TEST_EOF_READ_ARRAY:
			testStatus.setName("EOF_READ_ARRAY");
			if (server) {
				CommunicationTester.readArayEOF(is, os, testStatus);
			} else {
				CommunicationTester.sendArayEOF(is, os, testStatus);
			}
			break;
		case TEST_EOF_READ_ARRAY_BACK:
			testStatus.setName("EOF_READ_ARRAY_BACK");
			if (!server) {
				CommunicationTester.readArayEOF(is, os, testStatus);
			} else {
				CommunicationTester.sendArayEOF(is, os, testStatus);
			}
			break;
		case TEST_CONNECTION_INFO:
			testStatus.setName("TEST_CONNECTION_INFO");
			if (server) {
				CommunicationTester.serverRemoteDevice(conn, is, os, testStatus);
			} else {
				CommunicationTester.clientRemoteDevice(conn, is, os, testStatus);
			}
			break;
		case TEST_CLOSED_CONNECTION:
			testStatus.setName("CLOSED_CONNECTION");
			if (server) {
				CommunicationTester.readClosedConnection(is, os, testStatus);
			} else {
				CommunicationTester.sendClosedConnection(is, os, testStatus);
			}
			break;
		case TEST_CLOSED_CONNECTION_BACK:
			testStatus.setName("CLOSED_CONNECTION_BACK");
			if (!server) {
				CommunicationTester.readClosedConnection(is, os, testStatus);
			} else {
				CommunicationTester.sendClosedConnection(is, os, testStatus);
			}
			break;
		case TEST_LARGE_BYTE_ARRAY:
			testStatus.setName("LARGE_BYTE_ARRAY");
			if (server) {
				CommunicationTester.readByteArayLarge(is);
			} else {
				CommunicationTester.sendByteArayLarge(os);
			}
			break;
		case TEST_LARGE_BYTE_ARRAY_BACK:
			testStatus.setName("LARGE_BYTE_ARRAY_BACK");
			if (!server) {
				CommunicationTester.readByteArayLarge(is);
			} else {
				CommunicationTester.sendByteArayLarge(os);
			}
			break;			
		case TEST_SERVER_TERMINATE:
			return;
		default:
			Assert.fail("Invalid test#" + testType);	
		}
	}
}
