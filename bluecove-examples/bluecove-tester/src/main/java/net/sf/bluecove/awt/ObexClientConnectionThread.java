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
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;

import net.sf.bluecove.Logger;
import net.sf.bluecove.util.BluetoothTypesInfo;
import net.sf.bluecove.util.IOUtils;

public class ObexClientConnectionThread extends Thread  {

	private String serverURL;
	
	private String name;
	
	private String text;
	
	boolean isPut;
	
	boolean isRunning = false;
	
	String status;
	
	private boolean stoped = false;
	
	private ClientSession clientSession;
	
	public ObexClientConnectionThread(String serverURL, String name, String text, boolean isPut) {
		this.serverURL = serverURL;
		this.name = name;
		this.text = text;
		this.isPut = isPut;
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
			HeaderSet chs = clientSession.connect(clientSession.createHeaderSet());
			Logger.debug("connect responseCode " + BluetoothTypesInfo.toStringObexResponseCodes(chs.getResponseCode()));


			HeaderSet hs = clientSession.createHeaderSet();
			hs.setHeader(HeaderSet.NAME, name);
			hs.setHeader(HeaderSet.TYPE, "text");

			if (stoped) {
				return;
			}
			if (isPut) {
				byte data[] = text.getBytes("iso-8859-1");
				status = "Putting";
				Operation po = clientSession.put(hs);

				OutputStream os = po.openOutputStream();
				os.write(data);
				os.close();
			
				Logger.debug("put responseCode " + BluetoothTypesInfo.toStringObexResponseCodes(po.getResponseCode()));
			
				po.close();
			} else {
				status = "Getting";
				Operation po = clientSession.get(hs);

				InputStream is = po.openInputStream();
				StringBuffer buf = new StringBuffer();
				while (!stoped) {
					int i = is.read();
					if (i == -1) {
						break;
					}
					buf.append((char) i);
				}
				if (buf.length() > 0) {
					Logger.debug("got:" + buf);
				}
				is.close();
			
				Logger.debug("get responseCode " + BluetoothTypesInfo.toStringObexResponseCodes(po.getResponseCode()));
			
				po.close();
			}
			
			HeaderSet dhs =  clientSession.disconnect(null);
			Logger.debug("disconnect responseCode " + BluetoothTypesInfo.toStringObexResponseCodes(dhs.getResponseCode()));
			
			status = "Finished";
			
		} catch (IOException e) {
			status = "Communication error " + e.toString();
			Logger.error("Communication error", e);
		} catch (Throwable e) {
			status = "Error " + e.toString();
			Logger.error("Error", e);
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
