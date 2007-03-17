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

public interface Consts {

	public static final boolean useShortUUID = false;
	
	public static final String RESPONDER_UUID = "B1011111111111111111111111110001";
	
	//public static final boolean useShortUUID = true;
	
	//public static final String RESPONDER_UUID = "1212";

	public static final String RESPONDER_SERVERNAME = "bluecovesrv";

	public static final int TEST_SERVICE_ATTRIBUTE_INT_ID = 0x0A0;
	
	public static final int TEST_SERVICE_ATTRIBUTE_INT_VALUE = 77;
	
	public static final int TEST_SERVICE_ATTRIBUTE_STR_ID = 0x0A1;
    
	public static final String TEST_SERVICE_ATTRIBUTE_STR_VALUE = "SomeData";
	
    public static final int TEST_SERVICE_ATTRIBUTE_URL_ID = 0x0A2;
    
    public static final String TEST_SERVICE_ATTRIBUTE_URL_VALUE = "http:/bluecove.sourceforge.net:80/someUrl?q=10&bluecove=123&ServiceDiscovery=Test";
	
	public static final int clientReconnectSleep = 5100;
	
	public static final int serverSendSleep = 1000;
	
	public static final int serverTimeOutMin = 2;
	
    public static final int DEVICE_COMPUTER = 0x0100;

    public static final int DEVICE_PHONE = 0x0200;
    
	public static final int TEST_REPLY_OK = 77;
	
	public static final int TEST_TERMINATE = 99;
	
	public static final int TEST_START = 1;
	
	public static final int TEST_STRING = 1;
	
	public static final int TEST_STRING_BACK = 2;
	
	public static final int TEST_BYTE = 3;
	
	public static final int TEST_BYTE_BACK = 4;
	
	public static final int TEST_STRING_UTF = 5;
	
	public static final int TEST_STRING_UTF_BACK = 6;
	
	public static final int TEST_BYTE_ARRAY = 7;
	
	public static final int TEST_BYTE_ARRAY_BACK = 8;
	
	public static final int TEST_DataStream = 9;
	
	public static final int TEST_DataStream_BACK = 10;
	
	public static final int TEST_LAST = 10;
}
