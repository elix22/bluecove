package net.sf.bluecove;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CommunicationTester {

	private static final String stringData = "TestString2007"; 
	
	static void sendString(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeUTF(stringData);
	}
	
	static void readString(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		String got = dis.readUTF();
	}
	
}
