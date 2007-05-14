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

import javax.bluetooth.UUID;

import junit.framework.Assert;

public class CommunicationTester implements Consts {

	public static final UUID uuid = new UUID(Consts.RESPONDER_UUID, Consts.useShortUUID); 

	public static boolean dataOutputStreamFlush = true;
	
	public static int clientConnectionOpenRetry = 3;
	
	private static final String stringData = "TestString2007";
	
	private static final String stringUTFData = "\u0413\u043E\u043B\u0443\u0431\u043E\u0439\u0417\u0443\u0431";
	
	private static final int byteCount = 12; //1024;
	
	private static final byte[] byteAray = new byte[] {1 , 7, -40, 80, 90, 100, 87, -10, 127, -127, 0, -77};  
	
	private static final int streamAvailableByteCount = 147;
	
	private static final int byteArayLargeSize = 0x2010; // More then 8K
	
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
		for(int i = 1; i < byteCount; i++) {
			os.write(i);
		}
	}
	
	static void readByte(InputStream is) throws IOException {
		for(int i = 1; i < byteCount; i++) {
			int got = is.read();
			Assert.assertEquals("byte", 255 & i, got);
		}
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
		int got = is.read();
		Assert.assertEquals("byte", 255 & streamAvailableByteCount, got);
	}

	private static void readStreamAvailable(InputStream is, OutputStream os) throws IOException {
		for(int i = 1; i < streamAvailableByteCount; i++) {
			boolean hasData = false;
			int tryCount = 0;
			do {
				int avl = is.available();
				if (avl > 0) {
					hasData = true;
				} else if (avl < 0) {
					Assert.fail("negative available");
				}
				tryCount ++;
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					Assert.fail("Test Interrupted");
				}
				if (tryCount > 50) {
					Assert.fail("Test Available took too long");
				}
			} while(!hasData);
			
			int got = is.read();
			Assert.assertEquals("byte", 255 & i, got);
		}
		os.write(streamAvailableByteCount);
		os.flush();
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
	
	public static void runTest(int testType, boolean server, InputStream is, OutputStream os) throws IOException {
		switch (testType) {
		case TEST_STRING:
			if (server) {
				CommunicationTester.readString(is);
			} else {
				CommunicationTester.sendString(os);
			}
			break;
		case TEST_STRING_BACK:
			if (!server) {
				CommunicationTester.readString(is);
			} else {
				CommunicationTester.sendString(os);
			}
			break;
		case TEST_BYTE:
			if (server) {
				CommunicationTester.readByte(is);
			} else {
				CommunicationTester.sendByte(os);
			}
			break;
		case TEST_BYTE_BACK:
			if (!server) {
				CommunicationTester.readByte(is);
			} else {
				CommunicationTester.sendByte(os);
			}
			break;
		case TEST_STRING_UTF:
			if (server) {
				CommunicationTester.readUTFString(is);
			} else {
				CommunicationTester.sendUTFString(os);
			}
		case TEST_STRING_UTF_BACK:
			if (!server) {
				CommunicationTester.readUTFString(is);
			} else {
				CommunicationTester.sendUTFString(os);
			}
		case TEST_BYTE_ARRAY:
			if (server) {
				CommunicationTester.readByteAray(is);
			} else {
				CommunicationTester.sendByteAray(os);
			}
			break;
		case TEST_BYTE_ARRAY_BACK:
			if (!server) {
				CommunicationTester.readByteAray(is);
			} else {
				CommunicationTester.sendByteAray(os);
			}
			break;
		case TEST_DataStream:
			if (server) {
				CommunicationTester.readDataStream(is);
			} else {
				CommunicationTester.sendDataStream(os);
			}
			break;
		case TEST_DataStream_BACK:
			if (!server) {
				CommunicationTester.readDataStream(is);
			} else {
				CommunicationTester.sendDataStream(os);
			}
			break;
		case TEST_StreamAvailable:
			if (server) {
				CommunicationTester.readStreamAvailable(is, os);
			} else {
				CommunicationTester.sendStreamAvailable(is, os);
			}
			break;
		case TEST_StreamAvailable_BACK:
			if (!server) {
				CommunicationTester.readStreamAvailable(is, os);
			} else {
				CommunicationTester.sendStreamAvailable(is, os);
			}
			break;
		case TEST_LARGE_BYTE_ARRAY:
			if (server) {
				CommunicationTester.readByteArayLarge(is);
			} else {
				CommunicationTester.sendByteArayLarge(os);
			}
			break;
		case TEST_LARGE_BYTE_ARRAY_BACK:
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
