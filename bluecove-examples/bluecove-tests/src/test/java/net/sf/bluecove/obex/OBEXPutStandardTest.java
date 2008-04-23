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
package net.sf.bluecove.obex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;
import javax.obex.SessionNotifier;

import net.sf.bluecove.BaseEmulatorTestCase;
import net.sf.bluecove.TestCaseRunnable;

/**
 * @author vlads
 * 
 */
public class OBEXPutStandardTest extends BaseEmulatorTestCase {

	static final String serverUUID = "11111111111111111111111111111123";

	private HeaderSet serverPutHeaders;

	private byte[] serverData;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		serverPutHeaders = null;
		serverData = null;
	}

	private class RequestHandler extends ServerRequestHandler {

		@Override
		public int onPut(Operation op) {
			try {
				serverPutHeaders = op.getReceivedHeaders();
				InputStream is = op.openInputStream();
				ByteArrayOutputStream buf = new ByteArrayOutputStream();
				int data;
				while ((data = is.read()) != -1) {
					buf.write(data);
				}
				serverData = buf.toByteArray();
				op.close();
				return ResponseCodes.OBEX_HTTP_OK;
			} catch (IOException e) {
				e.printStackTrace();
				return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			}
		}
	}

	/*
	 * Used for profiling
	 */
	public static void main(String args[]) throws Exception {
		long start = System.currentTimeMillis();
		OBEXPutStandardTest t = new OBEXPutStandardTest();
		t.setUp();
		long setUp = System.currentTimeMillis();
		System.out.println("setUp   : " + (setUp - start));

		t.testPUTOperation();
		// t.testPUTOperationBigData();

		long exec = System.currentTimeMillis();
		System.out.println("exec    : " + (exec - setUp));
		t.tearDown();
		long tearDown = System.currentTimeMillis();
		System.out.println("tearDown: " + (tearDown - exec));
		System.out.println("total   : " + (System.currentTimeMillis() - start));
	}

	@Override
	protected Runnable createTestServer() {
		return new TestCaseRunnable() {
			public void execute() throws Exception {
				SessionNotifier serverConnection = (SessionNotifier) Connector.open("btgoep://localhost:" + serverUUID
						+ ";name=ObexTest");
				serverConnection.acceptAndOpen(new RequestHandler());
			}
		};
	}

	public void testPUTOperation() throws IOException {

		ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
		HeaderSet hsConnectReply = clientSession.connect(null);
		assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());

		HeaderSet hsOperation = clientSession.createHeaderSet();
		String name = "Hello.txt";
		hsOperation.setHeader(HeaderSet.NAME, name);

		// Create PUT Operation
		Operation putOperation = clientSession.put(hsOperation);

		// Send some text to server
		byte data[] = "Hello world!".getBytes("iso-8859-1");
		OutputStream os = putOperation.openOutputStream();
		os.write(data);
		os.close();

		putOperation.close();

		clientSession.disconnect(null);

		clientSession.close();

		assertEquals("NAME", name, serverPutHeaders.getHeader(HeaderSet.NAME));
		assertEquals("data", data, serverData);
	}

	public void testPUTOperationBigData() throws IOException {

		ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
		HeaderSet hsConnectReply = clientSession.connect(null);
		assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());

		// Create PUT Operation
		Operation putOperation = clientSession.put(null);

		// Send big Data to server
		int length = 0x4000;
		byte data[] = new byte[length];
		for (int i = 0; i < length; i++) {
			data[i] = (byte) (i & 0xFF);
		}
		OutputStream os = putOperation.openOutputStream();
		os.write(data);
		os.close();

		putOperation.close();

		clientSession.disconnect(null);

		clientSession.close();

		assertEquals("data", data, serverData);
	}

}
