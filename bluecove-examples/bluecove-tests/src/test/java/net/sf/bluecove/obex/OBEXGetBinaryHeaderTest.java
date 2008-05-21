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

/**
 * @author vlads
 * 
 * This tests OBEX Operation get and OutputStream.
 * 
 * Some talk on the subject here:
 * https://opensource.motorola.com/sf/discussion/do/listPosts/projects.jsr82/discussion.google_jsr_82_support.topc1544
 * 
 */
public class OBEXGetBinaryHeaderTest extends OBEXBaseEmulatorTestCase {

	protected static final byte[] simpleHeaderData = "Ask for data!".getBytes();

	private byte[] serverHeaderData;

	private class RequestHandler extends ServerRequestHandler {

		@Override
		public int onGet(Operation op) {
			try {
				serverRequestHandlerInvocations++;
				serverHeaders = op.getReceivedHeaders();

				InputStream isBinHeader = op.openInputStream();
				ByteArrayOutputStream buf = new ByteArrayOutputStream();
				int data;
				while ((data = isBinHeader.read()) != -1) {
					buf.write(data);
				}
				serverHeaderData = buf.toByteArray();

				HeaderSet hs = createHeaderSet();
				hs.setHeader(HeaderSet.LENGTH, new Long(simpleData.length));
				op.sendHeaders(hs);

				OutputStream os = op.openOutputStream();
				os.write(simpleData);
				os.close();

				op.close();
				return ResponseCodes.OBEX_HTTP_ACCEPTED;
			} catch (IOException e) {
				e.printStackTrace();
				return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.bluecove.obex.OBEXBaseEmulatorTestCase#createRequestHandler()
	 */
	@Override
	protected ServerRequestHandler createRequestHandler() {
		return new RequestHandler();
	}

	public void testGETBinaryHeader() throws IOException {

		ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
		HeaderSet hsConnectReply = clientSession.connect(null);
		assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());

		HeaderSet hs = clientSession.createHeaderSet();
		String name = "Hello.txt";
		hs.setHeader(HeaderSet.NAME, name);

		// Create GET Operation
		Operation get = clientSession.get(hs);

		OutputStream osBinHeader = get.openOutputStream();
		osBinHeader.write(simpleHeaderData);
		osBinHeader.close();

		// request portion is done
		HeaderSet headers = get.getReceivedHeaders();

		InputStream is = get.openInputStream();
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int data;
		while ((data = is.read()) != -1) {
			buf.write(data);
		}
		byte serverData[] = buf.toByteArray();

		is.close();

		get.close();

		clientSession.disconnect(null);

		clientSession.close();

		assertEquals("NAME", name, serverHeaders.getHeader(HeaderSet.NAME));
		assertEquals("data in header", simpleHeaderData, serverHeaderData);
		assertEquals("data in responce", simpleData, serverData);
		assertEquals("LENGTH", new Long(serverData.length), headers.getHeader(HeaderSet.LENGTH));
		assertEquals("invocations", 1, serverRequestHandlerInvocations);
	}
}
