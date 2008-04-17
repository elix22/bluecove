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
 *  @version $Id$
 */
package net.sf.bluecove;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import junit.framework.TestCase;

import com.intel.bluetooth.EmulatorTestsHelper;

/**
 * @author vlads
 * 
 */
public class ExampleTest extends TestCase {

	private static final UUID uuid = new UUID(0x2108);

	private Thread serverThread;

	protected void setUp() throws Exception {
		super.setUp();
		EmulatorTestsHelper.startInProcessServer();
		serverThread = EmulatorTestsHelper.runNewEmulatorStack(new EchoServerRunnable());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if (serverThread != null) {
			serverThread.interrupt();
		}
		EmulatorTestsHelper.stopInProcessServer();
	}

	private class EchoServerRunnable implements Runnable {
		public void run() {
			try {
				String url = "btspp://localhost:" + uuid.toString() + ";name=TServer";
				StreamConnectionNotifier service = (StreamConnectionNotifier) Connector.open(url);

				StreamConnection con = (StreamConnection) service.acceptAndOpen();

				System.out.println("Server received connection");

				DataOutputStream dos = con.openDataOutputStream();
				DataInputStream dis = con.openDataInputStream();

				String greeting = "I echo";

				dos.writeUTF(greeting);
				dos.flush();

				String received = dis.readUTF();
				System.out.print("received:");
				System.out.println(received);

				dos.writeUTF(received);
				dos.flush();

				dos.close();
				dis.close();

				con.close();
			} catch (Throwable e) {
				System.err.print(e.toString());
			}
		}
	}

	public void testConnection() {

	}
}
