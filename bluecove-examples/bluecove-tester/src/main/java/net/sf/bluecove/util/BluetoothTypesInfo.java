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

package net.sf.bluecove.util;

import java.util.Enumeration;

import javax.bluetooth.DataElement;
import javax.bluetooth.ServiceRecord;

public abstract class BluetoothTypesInfo {

	public static String toString(ServiceRecord sr) {
		StringBuffer buf = new StringBuffer();
		int[] ids = sr.getAttributeIDs();
		for (int i = 0; i < ids.length; i++) {
			buf.append("0x");
			buf.append(Integer.toHexString(i));
			buf.append(":\t");

			DataElement d = sr.getAttributeValue(ids[i]);
			buf.append(toString(d));
			buf.append("\n");
		}
		return buf.toString();
	}

	public static String toStringDataElementType(int type) {
		if (type == DataElement.DATALT) {
			return "DATATL";
		} else if (type == DataElement.DATSEQ) {
			return "DATSEQ";
		} else if (type == DataElement.U_INT_4) {
			return "U_INT_4";
		} else if (type == DataElement.U_INT_1) {
			return "U_INT_1";
		} else if (type == DataElement.U_INT_2) {
			return "U_INT_2";
		} else if (type == DataElement.INT_1) {
			return "INT_1";
		} else if (type == DataElement.INT_2) {
			return "INT_2";
		} else if (type == DataElement.INT_4) {
			return "INT_4";
		} else if (type == DataElement.INT_8) {
			return "INT_8";
		} else if (type == DataElement.UUID) {
			return "UUID";
		} else if (type == DataElement.U_INT_8) {
			return "U_INT_8";
		} else if (type == DataElement.U_INT_16) {
			return "U_INT_16";
		} else if (type == DataElement.INT_16) {
			return "INT_16";
		} else if (type == DataElement.STRING) {
			return "STRING";
		} else if (type == DataElement.URL) {
			return "URL";
		} else if (type == DataElement.BOOL) {
			return "BOOL";
		} else if (type == DataElement.NULL) {
			return "NULL";
		} else {
			return "UNKNOWN_TYPE";
		}
	}

	public static String toString(DataElement d) {
		return toString(d, "");
	}

	public static String toString(DataElement d, String ident) {
		StringBuffer buf = new StringBuffer();

		int valueType = d.getDataType();
		buf.append(ident);
		buf.append(toStringDataElementType(valueType));

		switch (valueType) {
		case DataElement.U_INT_1:
		case DataElement.U_INT_2:
		case DataElement.U_INT_4:
		case DataElement.INT_1:
		case DataElement.INT_2:
		case DataElement.INT_4:
		case DataElement.INT_8:
			buf.append(" 0x").append(Long.toHexString(d.getLong()));
			break;
		case DataElement.BOOL:
			buf.append(" ").append(d.getBoolean());
			break;
		case DataElement.URL:
		case DataElement.STRING:
		case DataElement.UUID:
			buf.append(" ").append(d.getValue());
			break;
		case DataElement.U_INT_8:
		case DataElement.U_INT_16:
		case DataElement.INT_16:
			byte[] b = (byte[]) d.getValue();
			buf.append(" ");
			for (int i = 0; i < b.length; i++) {
				buf.append(Integer.toHexString(b[i] >> 4 & 0xf));
				buf.append(Integer.toHexString(b[i] & 0xf));
			}
			break;
		case DataElement.DATALT:
		case DataElement.DATSEQ:
			buf.append(" {\n");
			for (Enumeration e = (Enumeration) (d.getValue()); e.hasMoreElements();) {
				buf.append(toString((DataElement) e.nextElement(), ident + "\t")).append("\n");
			}
			buf.append(ident).append("}");
			break;
		}
		return buf.toString();
	}
}
