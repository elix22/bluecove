/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth.obex;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.obex.HeaderSet;

class OBEXClientOperationPut extends OBEXClientOperation implements OBEXOperationDelivery {

	OBEXClientOperationPut(OBEXClientSessionImpl session, HeaderSet sendHeaders) throws IOException {
		super(session, OBEXOperationCodes.PUT);
		this.inputStream = new OBEXOperationInputStream(this);
		startOperation(sendHeaders);
	}

	public InputStream openInputStream() throws IOException {
		validateOperationIsOpen();
		if (inputStreamOpened) {
			throw new IOException("input stream already open");
		}
		this.inputStreamOpened = true;
		this.operationInProgress = true;
		return this.inputStream;
	}

	public OutputStream openOutputStream() throws IOException {
		validateOperationIsOpen();
		if (outputStreamOpened) {
			throw new IOException("output already open");
		}
		outputStreamOpened = true;
		outputStream = new OBEXOperationOutputStream(session.mtu, this);
		this.operationInProgress = true;
		return outputStream;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.obex.OBEXOperationDelivery#deliverPacket(boolean,
	 *      byte[])
	 */
	public void deliverPacket(boolean finalPacket, byte buffer[]) throws IOException {
		if (requestEnded) {
			return;
		}
		if (SHORT_REQUEST_PHASE && (this.startOperationHeaders != null)) {
			exchangePacket(OBEXHeaderSetImpl.toByteArray(this.startOperationHeaders));
			this.startOperationHeaders = null;
		}
		int dataHeaderID = OBEXHeaderSetImpl.OBEX_HDR_BODY;
		if (finalPacket) {
			this.operationId |= OBEXOperationCodes.FINAL_BIT;
			dataHeaderID = OBEXHeaderSetImpl.OBEX_HDR_BODY_END;
			requestEnded = true;
		}
		HeaderSet dataHeaders = session.createHeaderSet();
		dataHeaders.setHeader(dataHeaderID, buffer);
		exchangePacket(OBEXHeaderSetImpl.toByteArray(dataHeaders));
	}

	public void closeStream() throws IOException {
		this.operationInProgress = false;
		if (outputStream != null) {
			synchronized (lock) {
				if (outputStream != null) {
					outputStream.close();
				}
				outputStream = null;
			}
		}
	}

}