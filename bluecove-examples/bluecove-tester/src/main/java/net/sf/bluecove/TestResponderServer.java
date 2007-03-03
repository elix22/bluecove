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
import java.io.InputStream;
import java.io.OutputStream;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import junit.framework.Assert;

public class TestResponderServer implements CanShutdown {
	
	public static int countSuccess = 0; 
	
	public static int countFailure = 0;
	
	private boolean stoped = false;
	
	private StreamConnectionNotifier server;
	
	private TestTimeOutMonitor monitor;
	
	public TestResponderServer() throws BluetoothStateException {
		
		//System.setProperty("bluecove.debug", "true");
		//System.setProperty("bluecove.native.path", ".");
		
		
		LocalDevice localDevice = LocalDevice.getLocalDevice();
		Logger.info("address:" + localDevice.getBluetoothAddress());
		Logger.info("name:" + localDevice.getFriendlyName());
 	    
		Assert.assertNotNull("BT Address", localDevice.getBluetoothAddress());
		Assert.assertNotNull("BT Name", localDevice.getFriendlyName());

		run();

	}
	
	public void run() {
		if (!CommunicationTester.continuous) {
			monitor = new TestTimeOutMonitor(this, 1);
		}
		try {
			server = (StreamConnectionNotifier) Connector
					.open("btspp://localhost:"
							+ CommunicationTester.uuid
							+ ";name="
							+ Consts.RESPONDER_SERVERNAME
							+ ";authorize=false;authenticate=false;encrypt=false");

			Logger.info("ResponderServer started");
			
			connctionLoop: while (true) {
				
				Logger.info("Accepting connection");
				StreamConnection conn = server.acceptAndOpen();

				Logger.info("Received connection");

				InputStream is = null;
				OutputStream os = null;
				int testType = 0;
				try {
					is = conn.openInputStream();
					os = conn.openOutputStream();
					testType = is.read();

					if (testType == Consts.TEST_TERMINATE) {
						Logger.info("Stop requested");
						break connctionLoop;
					}
					CommunicationTester.runTest(testType, true, is, os);
					os.write(Consts.TEST_REPLY_OK);
					os.write(testType);
					os.flush();
					countSuccess++;
					Logger.debug("Test# " + testType + " ok");
				} catch (Throwable e) {
					countFailure++;
					Logger.error("Test# " + testType + " error", e);
				} finally {
					IOUtils.closeQuietly(os);
					IOUtils.closeQuietly(is);
					IOUtils.closeQuietly(conn);
				}
				Logger.info("*Test Success:" + countSuccess + " Failure:" + countFailure);
			}

			server.close();
			server = null;
		} catch (IOException e) {
			if (!stoped) {
				Logger.error("Server start error", e);
			}
		} finally {
			Logger.info("Server finished");
		}
		if (monitor != null) {
			monitor.finish();
		}
	}

	public void shutdown() {
		Logger.info("shutdownServer");
		stoped = true;
		if (server != null) {
			try {
				server.close();
			} catch (IOException e) {
			}
		}
	}
	
	public static void main(String[] args) {
		try {
			new TestResponderServer();
			if (TestResponderServer.countFailure > 0) {
				System.exit(1);
			} else {
				System.exit(0);
			}
		} catch (Throwable e) {
			System.out.println("start error " + e);
			e.printStackTrace(System.out);
			System.exit(1);
		}
	}

}
