/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
 *  @version $Id: NativeExceptionTest.java 1570 2008-01-16 22:15:56Z skarzhevskyy $
 */
package com.intel.bluetooth;

import java.io.IOException;

import javax.bluetooth.UUID;

/**
 * @author vlads
 * 
 */
public class ServiceRecordTest extends NativeTestCase {

	public void validateServiceRecordConvert(ServiceRecordImpl serviceRecord) throws IOException {
		byte[] inRecordData = serviceRecord.toByteArray();
		DebugLog.debug("inRecordData", inRecordData);
		byte[] nativeRecord = BluetoothStackBlueZNativeTests.testServiceRecordConvert(inRecordData);
		DebugLog.debug("nativeRecord", nativeRecord);
		assertEquals("length", inRecordData.length, nativeRecord.length);
		for (int k = 0; k < inRecordData.length; k++) {
			assertEquals("byteAray[" + k + "]", inRecordData[k], nativeRecord[k]);
		}

	}

	public void testServiceRecordConvert() throws IOException {
		ServiceRecordImpl serviceRecord = new ServiceRecordImpl(null, null, 0);
		serviceRecord.populateL2CAPAttributes(1, 2, new UUID(3), "BBBB");
		validateServiceRecordConvert(serviceRecord);
	}
}
