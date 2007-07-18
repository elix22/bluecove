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

import java.io.IOException;

import junit.framework.Assert;

/**
 * @author vlads
 *
 */
public class CommunicationTesterL2CAP extends CommunicationData {

	private static byte[] startPrefix(int testType, byte[] data) {
		byte[] dataToSend = new byte[data.length + 2];
		dataToSend[0] = Consts.SEND_TEST_START;
		System.arraycopy(data, 0, dataToSend, 2, data.length);
		return dataToSend;
	}
	
	public static void runTest(int testType, boolean server, ConnectionHolderL2CAP c, byte[] initialData, TestStatus testStatus) throws IOException {
		switch (testType) {
		case 1:
			testStatus.setName("l2-byteAray");
			if (!server) {
				c.channel.send(startPrefix(testType, byteAray));
			} else {
				Assert.assertEquals("byteAray.len", byteAray.length, initialData.length);
				for(int i = 0; i < byteAray.length; i++) {
					Assert.assertEquals("byte[" + i + "]", byteAray[i], initialData[i]);
				}
			}
		}

	}
}
