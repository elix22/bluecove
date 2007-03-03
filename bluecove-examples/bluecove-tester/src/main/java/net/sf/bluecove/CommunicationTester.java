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

	public static final UUID uuid = new UUID(Consts.RESPONDER_UUID, false); 

	public static boolean continuous = false;
	
	private static final String stringData = "TestString2007";
	
	private static final int byteCount = 12; //1024;
	
	static void sendString(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeUTF(stringData);
	}
	
	static void readString(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		String got = dis.readUTF();
		Assert.assertEquals("ReadString", stringData, got);
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
		case TEST_TERMINATE:
			return;
		default:
			Assert.fail("Invalid test#" + testType);	
		}
	}
	
}
