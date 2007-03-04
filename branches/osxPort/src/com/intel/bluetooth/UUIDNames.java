package com.intel.bluetooth;

import java.util.Hashtable;

import javax.bluetooth.UUID;

public class UUIDNames {
	private static Hashtable<UUID, String>		lookups;
	
	public static String getUUIDDescrip(UUID anID) {
		return lookups.get(anID) + ", (" + anID.toString() + ')';
	}
	static {
		lookups = new Hashtable<UUID, String>(100);
		lookups.put(new UUID("0001", true), "Service Discovery Protocol (SDP)");
		lookups.put(new UUID("0002", true), "User Datagram Protocol (UDP)");
		lookups.put(new UUID("0003", true), "Radio Frequency Communication Protocol (RFCOMM)");
		lookups.put(new UUID("0004", true), "Transmission Control Protocol (TCP)");
		lookups.put(new UUID("0005", true), "Service Discovery Protocol (TCSBIN)");
		lookups.put(new UUID("0006", true), "Service Discovery Protocol (TCSAT)");
		lookups.put(new UUID("0008", true), "Object Exchange Protocol (OBEX)");
		lookups.put(new UUID("0009", true), "Service Discovery Protocol (IP)");
		lookups.put(new UUID("000A", true), "Service Discovery Protocol (FTP)");
		lookups.put(new UUID("000C", true), "Service Discovery Protocol (HTTP)");
		lookups.put(new UUID("000E", true), "Service Discovery Protocol (WSP)");
		lookups.put(new UUID("000F", true), "Service Discovery Protocol (BNEP)");
		lookups.put(new UUID("0010", true), "Service Discovery Protocol (UPNP)");
		lookups.put(new UUID("0011", true), "Service Discovery Protocol (HIDP)");
		lookups.put(new UUID("0012", true), "Hardcopy Control Channel Protocol");
		lookups.put(new UUID("0014", true), "Hardcopy Data Channel Protocol");
		lookups.put(new UUID("0016", true), "Hardcopy Notification Protocol");
		lookups.put(new UUID("0017", true), "Protocol (VCTP)");
		lookups.put(new UUID("0019", true), "Protocol (VDTP)");
		lookups.put(new UUID("001B", true), "Protocol (CMPT)");
		lookups.put(new UUID("001D", true), "Protocol (UDI C Plane)");
		lookups.put(new UUID("0100", true), "Protocol (L2CAP)");
		
		// service classes
		lookups.put(new UUID("1000", true), "ServiceDiscoveryServer Service Class");
		lookups.put(new UUID("1001", true), "BrowseGroupDescriptor Service Class");
		lookups.put(new UUID("1002", true), "PublicBrowseGroup Service Class");
		lookups.put(new UUID("1101", true), "SerialPort Service Class");
		lookups.put(new UUID("1102", true), "LANAccessUsingPPP Service Class");
		lookups.put(new UUID("1103", true), "DialupNetworking Service Class");
		lookups.put(new UUID("1104", true), "IrMCSync Service Class");
		lookups.put(new UUID("1105", true), "OBEXObjectPush Service Class");
		lookups.put(new UUID("1106", true), "OBEXFileTransfer Service Class");
		lookups.put(new UUID("1107", true), "IrMCSyncCommand Service Class");
		lookups.put(new UUID("1108", true), "Headset Service Class");
		lookups.put(new UUID("1109", true), "CordlessTelephony Service Class");
		lookups.put(new UUID("110A", true), "AudioSource Service Class");
		lookups.put(new UUID("110B", true), "AudioSink Service Class");
		lookups.put(new UUID("110C", true), "AVRemoteControlTarget Service Class");
		lookups.put(new UUID("110D", true), "AdvancedAudioDistribution Service Class");
		lookups.put(new UUID("110E", true), "AVRemoteControl Service Class");
		lookups.put(new UUID("110F", true), "sVideoConferencing Service Class");
		lookups.put(new UUID("1110", true), "sIntercomService Class");
		lookups.put(new UUID("1111", true), "FAX Service Class");
		lookups.put(new UUID("1112", true), "HeadsetAudioGateway Service Class");
		lookups.put(new UUID("1113", true), "WAP Service Class");
		lookups.put(new UUID("1114", true), "WAP Client Service Class");
		lookups.put(new UUID("1115", true), "PANU Service Class");
		lookups.put(new UUID("1116", true), "NAP Service Class");
		lookups.put(new UUID("1117", true), "GN Service Class");
		lookups.put(new UUID("1118", true), "Direct Printing Service Class");
		lookups.put(new UUID("1119", true), "Reference Printing Service Class");
		lookups.put(new UUID("111A", true), "Imaging Service Class");
		lookups.put(new UUID("111B", true), "Imaging Responder Service Class");
		lookups.put(new UUID("111C", true), "Imaging Automatic Archive Service Class");
		lookups.put(new UUID("111D", true), "Imaging Referenced Objects Service Class");
		lookups.put(new UUID("111E", true), "Handsfree Service Class");
		lookups.put(new UUID("111F", true), "Handsfree Audio Service Class");
		lookups.put(new UUID("1120", true), "Direct Printing Reference Objects Service Class");
		lookups.put(new UUID("1121", true), "Reflected UI Service Class");
		lookups.put(new UUID("1122", true), "Basic Printing Service Class");
		lookups.put(new UUID("1123", true), "Printing Status Service Class");
		lookups.put(new UUID("1124", true), "Human Interface Device Service Class");
		lookups.put(new UUID("1125", true), "Hardcopy Cable Replacement Service Class");
		lookups.put(new UUID("1126", true), "Hardcopy Cable Replacement Print Service Class");
		lookups.put(new UUID("1127", true), "Hardcopy Cable Replacement Scan Service Class");
		lookups.put(new UUID("1128", true), "Common ISDN Access Service Class");
		lookups.put(new UUID("1129", true), "Video Conferencing Gateway Service Class");
		lookups.put(new UUID("112A", true), "UDI MT Service Class");
		lookups.put(new UUID("112B", true), "UDI TA Service Class");
		lookups.put(new UUID("1200", true), "PnP Information Service Class");
		lookups.put(new UUID("1201", true), "Generic Networking Service Class");
		lookups.put(new UUID("1202", true), "Generic File Transfer Service Class");
		lookups.put(new UUID("1203", true), "Generic Audio Service Class");
		lookups.put(new UUID("1204", true), "Generic Telephony Service Class");
	}
}
