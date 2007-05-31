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
import java.util.Hashtable;
import java.util.Vector;

import javax.bluetooth.DataElement;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

public abstract class BluetoothTypesInfo {

	public static final String NULL = "{null}";
	
	public static class UUIDConsts {

		private static String SHORT_BASE = "00001000800000805F9B34FB";

		private static Hashtable uuidNames = new Hashtable();

		private static void addName(String uuid, String name) {
			uuidNames.put(uuid.toUpperCase(), name);
		}
		
		private static void addName(int uuid, String name) {
			addName(new UUID(uuid).toString(), name);
		}

		static {
			addName(0x0001, "SDP");
			addName(0x0002, "UDP");
			addName(0x0003, "RFCOMM");
			addName(0x0004, "TCP");
			addName(0x0008, "OBEX");
			addName(0x000C, "HTTP");
			addName(0x000F, "BNEP");
			addName(0x0100, "L2CAP");

			addName(0x1000, "SDP_SERVER");
			addName(0x1001, "BROWSE_GROUP_DESCRIPTOR");
			addName(0x1002, "PUBLICBROWSE_GROUP");
			addName(0x1101, "SERIAL_PORT");
			addName(0x1102, "LAN_ACCESS_PPP");
			addName(0x1103, "DIALUP_NETWORKING");
			addName(0x1104, "IR_MC_SYNC");
			addName(0x1105, "OBEX_OBJECT_PUSH");
			addName(0x1106, "OBEX_FILE_TRANSFER");
			addName(0x1107, "IR_MC_SYNC_COMMAND");
			addName(0x1108, "HEADSET");
			addName(0x1109, "CORDLESS_TELEPHONY");
			addName(0x110A, "AUDIO_SOURCE");
			addName(0x110B, "AUDIO_SINK");
			addName(0x110C, "AV_REMOTE_CTL_TARGET");
			addName(0x110D, "ADVANCED_AUDIO_DISTRIB");
			addName(0x110E, "AV_REMOTE_CTL");
			addName(0x110F, "VIDEO_CONFERENCING");
			addName(0x1110, "INTERCOM");
			addName(0x1111, "FAX");
			addName(0x1112, "HEADSET_AUDIO_GATEWAY");
			addName(0x1113, "WAP");
			addName(0x1114, "WAP_CLIENT");
			addName(0x1115, "PAN_USER");
			addName(0x1116, "NETWORK_ACCESS_POINT");
			addName(0x1117, "GROUP_NETWORK");
			addName(0x1118, "DIRECT_PRINTING");
			addName(0x1119, "REFERENCE_PRINTING");
			addName(0x111A, "IMG");
			addName(0x111B, "IMG_RESPONDER");
			addName(0x111C, "IMG_AUTO_ARCHIVE");
			addName(0x111D, "IMG_REFERENCE_OBJECTS");
			addName(0x111E, "HANDSFREE");
			addName(0x111F, "HANDSFREE_AUDIO_GATEWAY");
			addName(0x1120, "DIRECT_PRINT");
			addName(0x1121, "REFLECTED_UI");
			addName(0x1122, "BASIC_PRINTING");
			addName(0x1123, "PRINTING_STATUS");
			addName(0x1124, "HI_DEVICE");
			addName(0x1125, "HARD_COPY_CABLE_REPLACE");
			addName(0x1126, "HCR_PRINT");
			addName(0x1127, "HCR_SCAN");
			addName(0x1128, "COMMON_ISDN_ACCESS");
			addName(0x1129, "VIDEO_CONF_GW");
			addName(0x112A, "UDI_MT");
			addName(0x112B, "UDI_TA");
			addName(0x112C, "AUDIO_VIDEO");
			addName(0x112D, "SIM_ACCESS");
			addName(0x1200, "PNP_INFO");
			addName(0x1201, "GENERIC_NETWORKING");
			addName(0x1202, "GENERIC_FILE_TRANSFER");
			addName(0x1203, "GENERIC_AUDIO");
			addName(0x1204, "GENERIC_TELEPHONY");
			addName(0x1205, "UPNP_SERVICE");
			addName(0x1206, "UPNP_IP_SERVICE");
			addName(0x1300, "ESDP_UPNP_IP_PAN");
			addName(0x1301, "ESDP_UPNP_IP_LAP");
			addName(0x1302, "ESDP_UPNP_L2CAP");
			
			addName("B10C0FE1111111111111111111110001", "BlueCoveT");
			
		}

		public static String getName(UUID uuid) {
			if (uuid == null) {
				return null;
			}
			String str = uuid.toString().toUpperCase();
			String name = (String)uuidNames.get(str);
			if (name != null) {
				return name;
			}
			int shortIdx = str.indexOf(SHORT_BASE);
			if ((shortIdx != -1) && (shortIdx + SHORT_BASE.length() == str.length())) {
				// This is short 16-bit UUID
				return toHexString(Integer.parseInt(str.substring(0, shortIdx), 16));
			}
			return null;
		}
	}

	public static String toString(ServiceRecord sr) {
		if (sr == null) {
			return NULL;
		}
		int[] ids = sr.getAttributeIDs();
		if (ids == null) {
			return "attributes " + NULL;
		}
		if (ids.length == 0) {
			return "not attributes";
		}
		Vector sorted = new Vector();
		for (int i = 0; i < ids.length; i++) {
			sorted.addElement(new Integer(ids[i]));
		}
		ConnectionUtils.sort(sorted);
		StringBuffer buf = new StringBuffer();
		for (Enumeration en = sorted.elements(); en.hasMoreElements();) {
			int id = ((Integer)en.nextElement()).intValue();
			buf.append(toHexString(id));
			buf.append(" ");
			buf.append(toStringServiceSttributeID(id));
			buf.append(":  ");

			DataElement d = sr.getAttributeValue(id);
			buf.append(toString(d));
			buf.append("\n");
		}
		return buf.toString();
	}

	public static String toStringServiceSttributeID(int id) {
		switch (id) {
		case 0x0000:
			return "ServiceRecordHandle";
		case 0x0001:
			return "ServiceClassIDList";
		case 0x0002:
			return "ServiceRecordState";
		case 0x0003:
			return "ServiceID";
		case 0x0004:
			return "ProtocolDescriptorList";
		case 0x0005:
			return "BrowseGroupList";
		case 0x0006:
			return "LanguageBasedAttributeIDList";
		case 0x0007:
			return "ServiceInfoTimeToLive";
		case 0x0008:
			return "ServiceAvailability";
		case 0x0009:
			return "BluetoothProfileDescriptorList";
		case 0x000A:
			return "DocumentationURL";
		case 0x000B:
			return "ClientExecutableURL";
		case 0x000C:
			return "IconURL";
		case 0x000D:
			return "AdditionalProtocol";
		case 0x0100:
			return "ServiceName";
		case 0x0101:
			return "ServiceDescription";
		case 0x0102:
			return "ProviderName";
		case 0x0200:
			return "GroupID";
		case 0x0201:
			return "ServiceDatabaseState";
		case 0x0300:
			return "ServiceVersion";
		case 0x0301:
			return "ExternalNetwork";
		case 0x0302:
			return "RemoteAudioVolumeControl";
		case 0x0303:
			return "SupportedFormatList";
		case 0x0304:
			return "FaxClass2Support";
		case 0x0305:
			return "AudioFeedbackSupport";
		case 0x0306:
			return "NetworkAddress";
		case 0x0307:
			return "WAPGateway";
		case 0x0308:
			return "HomePageURL";
		case 0x0309:
			return "WAPStackType";
		case 0x030A:
			return "SecurityDescription";
		case 0x030B:
			return "NetAccessType";
		case 0x030C:
			return "MaxNetAccessrate";
		case 0x030D:
			return "IPv4Subnet";
		case 0x030E:
			return "IPv6Subnet";
		case 0x0310:
			return "SupportedCapabalities";
		case 0x0311:
			return "SupportedFeatures";
		case 0x0312:
			return "SupportedFunctions";
		case 0x0313:
			return "TotalImagingDataCapacity";
		default:
			return "";
		}
	}

	public static String toStringDataElementType(int type) {
		switch (type) {
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
		default: return "Unknown" + type;
		}
	}
	
	public static String toHexString(long l) {
		return "0x" + Integer.toHexString((int)l);
	}
	
	public static String toHexString(int i) {
		String s = Integer.toHexString(i);
		s = s.toUpperCase();
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

	public static String toString(DataElement d) {
		return toString(d, "");
	}

	public static String toString(DataElement d, String ident) {
		if (d == null) {
			return NULL;
		}
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
			buf.append(" ").append(toHexString(d.getLong()));
			break;
		case DataElement.BOOL:
			buf.append(" ").append(d.getBoolean());
			break;
		case DataElement.URL:
		case DataElement.STRING:
			buf.append(" ").append(d.getValue());
			break;
		case DataElement.UUID:
			buf.append(" ").append(toString((UUID)d.getValue()));
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
				buf.append(toString((DataElement)e.nextElement(), ident + "  ")).append("\n");
			}
			buf.append(ident).append("}");
			break;
		}
		return buf.toString();
	}

	public static String toString(UUID uuid) {
		if (uuid == null) {
			return NULL;
		}
		StringBuffer buf = new StringBuffer();
		buf.append(uuid.toString());
		String name = UUIDConsts.getName(uuid);
		if (name != null) {
			buf.append(" (").append(name).append(")");
		}
		return buf.toString();
	}
}
