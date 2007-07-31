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
package net.sf.bluecove.awt;

import java.io.IOException;

import javax.bluetooth.L2CAPConnection;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import net.sf.bluecove.ConnectionHolder;
import net.sf.bluecove.ConnectionHolderL2CAP;
import net.sf.bluecove.Logger;
import net.sf.bluecove.ConnectionHolderStream;
import net.sf.bluecove.util.BluetoothTypesInfo;
import net.sf.bluecove.util.StringUtils;

/**
 * @author vlads
 *
 */
public class ClientConnectionThread extends Thread {

	private static int connectionCount = 0;

	private String serverURL;

	private ConnectionHolder c;

	private boolean stoped = false;

	boolean isRunning = false;

	boolean isConnecting = false;

	int receivedCount = 0;

	boolean rfcomm;

	public static final int interpretDataChars = 0;

	int interpretData = 0;

	ClientConnectionThread(String serverURL) {
		super("ClientConnectionThread" + (++connectionCount));
		this.serverURL = serverURL;
	}

	public void run() {
		try {
			rfcomm = BluetoothTypesInfo.isRFCOMM(serverURL);
			if (!rfcomm && !BluetoothTypesInfo.isL2CAP(serverURL)) {
				Logger.error("unsupported connection type " + serverURL);
				return;
			}
			Connection conn = null;
			try {
				isConnecting = true;
				Logger.debug("Connecting:" + serverURL + " ...");
				conn = Connector.open(serverURL);
			} catch (IOException e) {
				Logger.error("Connection error", e);
				return;
			} finally {
				isConnecting = false;
			}
			if (rfcomm) {
				ConnectionHolderStream cs = new ConnectionHolderStream((StreamConnection) conn);
				c = cs;
				cs.is = cs.conn.openInputStream();
				cs.os = cs.conn.openOutputStream();
				isRunning = true;
				StringBuffer buf = new StringBuffer();
				while (!stoped) {
					int data = cs.is.read();
					if (data == -1) {
						Logger.debug("EOF recived");
						break;
					}
					receivedCount++;
					switch (interpretData) {
					case interpretDataChars:
						char c = (char) data;
						buf.append(c);
						if ((c == '\n') || (buf.length() > 30)) {
							Logger.debug("cc:" + StringUtils.toBinaryText(buf));
							buf = new StringBuffer();
						}
						break;
					}
				}
				if (buf.length() > 0) {
					Logger.debug("cc:" + StringUtils.toBinaryText(buf));
				}
			} else { // l2cap
				ConnectionHolderL2CAP lc = new ConnectionHolderL2CAP((L2CAPConnection) conn);
				isRunning = true;
				c = lc;
				while (!stoped) {
					while ((!lc.channel.ready()) && (!stoped)) {
						Thread.sleep(100);
					}
					if (stoped) {
						break;
					}
					int receiveMTU = lc.channel.getReceiveMTU();
					byte[] data = new byte[receiveMTU];
					int length = lc.channel.receive(data);
					int messageLength = length; 
					if ((length > 0) && (data[length - 1] == '\n')) {
						messageLength = length-1;
					}
					StringBuffer buf = new StringBuffer();
					if (messageLength != 0) {
						buf.append(StringUtils.toBinaryText(new StringBuffer(new String(data, 0, messageLength))));
					}
					buf.append(" (").append(length).append(")");
					Logger.debug("cc:" + buf.toString());
				}
			}
		} catch (IOException e) {
			if (!stoped) {
				Logger.error("Communication error", e);
			}
		} catch (Throwable e) {
			Logger.error("Error", e);
		} finally {
			isRunning = false;
			if (c != null) {
				c.shutdown();
			}
		}
	}

	public void shutdown() {
		stoped = true;
		if (c != null) {
			c.shutdown();
		}
		c = null;
	}

	public void send(final byte data[]) {
		Thread t = new Thread("ClientConnectionSendThread" + (++connectionCount)) {
			public void run() {
				try {
					if (rfcomm) {
						((ConnectionHolderStream) c).os.write(data);
					} else {
						((ConnectionHolderL2CAP) c).channel.send(data);
					}
					Logger.debug("data " + data.length + " sent");
				} catch (IOException e) {
					Logger.error("Communication error", e);
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}
}
