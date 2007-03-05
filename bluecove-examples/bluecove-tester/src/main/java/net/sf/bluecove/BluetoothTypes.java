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

import javax.bluetooth.DataElement;

public class BluetoothTypes {

	public static String getDataElementType(int dataType) {
		switch (dataType) {
		case DataElement.NULL: return "NULL";
		case DataElement.U_INT_1: return "U_INT_1";
		case DataElement.U_INT_2: return "U_INT_2";
		case DataElement.U_INT_4: return "U_INT_4";
		case DataElement.INT_1: return "INT_1";
		case DataElement.INT_2: return "INT_2";
		case DataElement.INT_4: return "INT_4";
		case DataElement.INT_8: return "INT_8";
		case DataElement.INT_16: return "INT_16";
		case DataElement.URL: return "URL";
		case DataElement.STRING: return "STRING";
		case DataElement.UUID: return "UUID";
		case DataElement.DATSEQ: return "DATSEQ";
		case DataElement.BOOL: return "BOOL";
		case DataElement.DATALT: return "DATALT";
		default: return "Unknown" + dataType;
		}
	}
    
	public static String toHexString(int i) {
		String s = Integer.toHexString(i);
		switch (s.length()) {
		case 1:
			return "0x000" + s;
		case 2:
			return "0x00" + s;
		case 3:
			return "0x0" + s;
		case 4:
			return "0x" + s;
		default:
			return s;
		}
	}
}
