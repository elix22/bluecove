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

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import net.sf.bluecove.Logger;
import net.sf.bluecove.StreamConnectionHolder;

/**
 * @author vlads
 *
 */
public class ClientConnectionThread extends Thread {

	private static int connectionCount = 0;
	
	private String serverURL;
	
	private StreamConnectionHolder c = new StreamConnectionHolder();
	
	private boolean stoped = false;
	
	boolean isRunning = false;
	
	boolean isConnecting = false;
	
	int receivedCount = 0;
	
	public static final int interpretDataChars = 0;
	
	int interpretData = 0;
	
	ClientConnectionThread(String serverURL) {
		super("ClientConnectionThread" + (++connectionCount));
		this.serverURL = serverURL;
	}
	
	public void run() {
		try {
			try {
				isConnecting = true;
				Logger.debug("Connecting:" + serverURL + " ...");
				c.conn = (StreamConnection) Connector.open(serverURL);
			} catch (IOException e) {
				Logger.error("Connection error", e);
				return;
			} finally {
				isConnecting = false;
			}
			c.is = c.conn.openInputStream();
			c.os = c.conn.openOutputStream();
			isRunning = true;
			StringBuffer buf = new StringBuffer(); 
			while (!stoped) {
				int data = c.is.read();
				if (data == -1) {
					Logger.debug("EOF recived");
					break;
				}
				receivedCount ++;
				switch (interpretData) {
					case interpretDataChars:
						char c = (char)data;
						if (c == '\n') {
							Logger.debug("cc:" + buf.toString());
							buf = new StringBuffer();
						} else {
							buf.append(c);
						}
						break;
				}
			}
		} catch (IOException e) {
			Logger.error("Communication error", e);
		} catch (Throwable e) {
			Logger.error("Error", e);
		} finally {
			isRunning = false;
			c.shutdown();
		}
	}
	
	public void shutdown() {
		stoped = true;
		c.shutdown();
	}


	public void send(final byte data[]) {
		Thread t = new Thread("ClientConnectionSendThread" + (++connectionCount)) {
			public void run() {
				try {
					c.os.write(data);
					Logger.debug("data sent");
				} catch (IOException e) {
					Logger.error("Communication error", e);
				}
			}
		};
		t.setDaemon(true);
		t.start();
		
	}
}
