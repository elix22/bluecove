/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
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
package com.intel.bluetooth.emu;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author vlads
 * 
 */
abstract class ConnectionBuffer {

	protected long remoteAddress;

	protected int securityOpt;

	protected InputStream is;

	protected OutputStream os;

	protected ConnectionBuffer connected;

	protected ConnectionBuffer(long remoteAddress, InputStream is, OutputStream os) {
		super();
		this.remoteAddress = remoteAddress;
		this.is = is;
		this.os = os;
	}

	void connect(ConnectionBuffer pair) {
		connected = pair;
		pair.connected = this;
	}

	long getRemoteAddress() throws IOException {
		return remoteAddress;
	}

	void setSecurityOpt(int securityOpt) {
		this.securityOpt = securityOpt;
	}

	int getSecurityOpt(int expected) throws IOException {
		return securityOpt;
	}

	boolean encrypt(long remoteAddress, boolean on) throws IOException {
		if (this.remoteAddress != remoteAddress) {
			throw new IllegalArgumentException("Connection not to this device");
		}
		return false;
	}

	void close() throws IOException {
		try {
			os.close();
		} finally {
			is.close();
		}
	}
}
