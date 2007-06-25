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
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;

import net.sf.bluecove.Logger;
import net.sf.bluecove.util.IOUtils;

public class ObexClientConnectionThread extends Thread  {

	private String serverURL;
	
	private String text;
	
	boolean isRunning = false;
	
	String status;
	
	private boolean stoped = false;
	
	private ClientSession clientSession;
	
	public ObexClientConnectionThread(String serverURL, String text) {
		this.serverURL = serverURL;
		this.text = text;
	}
	
	public void run() {
		isRunning = true;
		try {
			status = "Connecting...";
			clientSession = (ClientSession) Connector.open(serverURL);
			if (stoped) {
				return;
			}
			status = "Connected";
			HeaderSet hs = clientSession.connect(clientSession.createHeaderSet());

			byte data[] = text.getBytes("iso-8859-1");
			hs.setHeader (HeaderSet.NAME, "test.txt");
			hs.setHeader (HeaderSet.TYPE, "text");

			if (stoped) {
				return;
			}
			status = "Sending";
			Operation po = clientSession.put(hs);

			po.openOutputStream().write(data);
			
			po.close();
			clientSession.disconnect(null);
			
			status = "Finished";
			
		} catch (IOException e) {
			status = "Communication error";
			Logger.error("Communication error", e);
		} finally {
			isRunning = false;
			IOUtils.closeQuietly(clientSession);
			clientSession = null;
			if (stoped) {
				status = "Terminated";
			}
		}
	}
	
	public void shutdown() {
		stoped = true;
		if (clientSession != null) {
			IOUtils.closeQuietly(clientSession);
			clientSession = null;
		}
	}
}
