package net.sf.bluecove;

import java.io.IOException;
import java.io.InputStream;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

public class TestResponderServer implements Consts {
	
	public static final UUID uuid = new UUID(Consts.RESPONDER_UUID, false);

	public TestResponderServer() throws BluetoothStateException {
		
		//System.setProperty("bluecove.debug", "true");
		//System.setProperty("bluecove.native.path", ".");
		
		
		LocalDevice localDevice = LocalDevice.getLocalDevice();
		Logger.info("address:" + localDevice.getBluetoothAddress());
		Logger.info("name:" + localDevice.getFriendlyName());
 	    
		run();
	}
	
	public void run() {
		try {
			StreamConnectionNotifier server = (StreamConnectionNotifier) Connector
					.open("btspp://localhost:"
							+ uuid
							+ ";name="
							+ Consts.RESPONDER_SERVERNAME
							+ ";authorize=false;authenticate=false;encrypt=false");

			Logger.info("Server started");
			
			connctionLoop: while (true) {
				
				StreamConnection conn = server.acceptAndOpen();

				Logger.info("Received connection");

				InputStream is = null;
				int testType = 0;
				try {
					is = conn.openInputStream();

					testType = is.read();

					switch (testType) {
					case TEST_STRING:
						CommunicationTester.readString(is);
						break;
					case TEST_TERMINATE:
						break connctionLoop;
					}
				} catch (Throwable e) {
					Logger.error("Test " + testType + " error", e);
				}

				is.close();
				conn.close();
			}

			server.close();
		} catch (IOException e) {
			Logger.error("Server start error", e);
		}
	}

	public static void main(String[] args) {
		try {
			new TestResponderServer();
			//System.exit(0);
		} catch (Throwable e) {
			System.out.println("start error " + e);
			e.printStackTrace(System.out);
		}
	}
}
