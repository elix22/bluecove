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
public class OBEXPutConditionsTest extends BaseEmulatorTestCase {

	static final String serverUUID = "11111111111111111111111111111123";

	private HeaderSet serverPutHeaders;

	private int serverDataLength;

	private byte[] serverData;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		serverPutHeaders = null;
		serverDataLength = -1;
		serverData = null;
	}

	private class RequestHandler extends ServerRequestHandler {

		@Override
		public int onPut(Operation op) {
			try {
				serverPutHeaders = op.getReceivedHeaders();
				Long dataLength = (Long) serverPutHeaders.getHeader(HeaderSet.LENGTH);
				if (dataLength == null) {
					return ResponseCodes.OBEX_HTTP_LENGTH_REQUIRED;
				}
				InputStream is = op.openInputStream();
				int len = dataLength.intValue();
				serverData = new byte[len];
				int got = 0;
				// read fully
				while (got < len) {
					int rc = is.read(serverData, got, len - got);
					if (rc < 0) {
						break;
					}
					got += rc;
				}
				serverDataLength = got;
				op.close();
				return ResponseCodes.OBEX_HTTP_OK;
			} catch (IOException e) {
				e.printStackTrace();
				return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			}
		}
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

	public void testPUTOperationCompleate() throws IOException {

		ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
		HeaderSet hsConnectReply = clientSession.connect(null);
		assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());

		HeaderSet hsOperation = clientSession.createHeaderSet();
		byte data[] = "Hello world!".getBytes("iso-8859-1");
		hsOperation.setHeader(HeaderSet.LENGTH, new Long(data.length));

		// Create PUT Operation
		Operation putOperation = clientSession.put(hsOperation);

		OutputStream os = putOperation.openOutputStream();
		os.write(data);
		os.close();

		putOperation.close();

		clientSession.disconnect(null);

		clientSession.close();

		assertEquals("LENGTH", new Long(data.length), serverPutHeaders.getHeader(HeaderSet.LENGTH));
		assertEquals("data.length", data.length, serverDataLength);
		assertEquals("data", data, serverData);
	}

	public void testPUTOperationSendMore() throws IOException {

		ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
		HeaderSet hsConnectReply = clientSession.connect(null);
		assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());

		HeaderSet hsOperation = clientSession.createHeaderSet();
		byte data[] = "Hello world!".getBytes("iso-8859-1");
		hsOperation.setHeader(HeaderSet.LENGTH, new Long(data.length));

		// Create PUT Operation
		Operation putOperation = clientSession.put(hsOperation);

		OutputStream os = putOperation.openOutputStream();
		os.write(data);
		os.write("More".getBytes("iso-8859-1"));
		os.close();

		putOperation.close();

		clientSession.disconnect(null);

		clientSession.close();

		assertEquals("LENGTH", new Long(data.length), serverPutHeaders.getHeader(HeaderSet.LENGTH));
		assertEquals("data.length", data.length, serverDataLength);
		assertEquals("data", data, serverData);
	}

	public void testPUTOperationSendLess() throws IOException {

		ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
		HeaderSet hsConnectReply = clientSession.connect(null);
		assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());

		HeaderSet hsOperation = clientSession.createHeaderSet();
		byte data[] = "Hello world!".getBytes("iso-8859-1");
		int less = 4;
		hsOperation.setHeader(HeaderSet.LENGTH, new Long(data.length + less));

		// Create PUT Operation
		Operation putOperation = clientSession.put(hsOperation);

		OutputStream os = putOperation.openOutputStream();
		os.write(data);
		os.close();

		putOperation.close();

		clientSession.disconnect(null);

		clientSession.close();

		assertEquals("LENGTH", new Long(data.length + less), serverPutHeaders.getHeader(HeaderSet.LENGTH));
		assertEquals("data.length", data.length, serverDataLength);
		assertEquals("data", data.length, data, serverData);
	}

}
