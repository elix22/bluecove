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
package net.sf.bluecove.obex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;


/**
 * @author vlads
 *
 */
public class ObexBluetoothClient {

	private Main mainInstance;
	
	private String fileName;
	
	private byte[] data;


	public ObexBluetoothClient(Main mainInstance, String fileName, byte[] data) {
		super();
		this.mainInstance = mainInstance;
		this.fileName = fileName;
		this.data = data;
	}
	
	public void obexPut(String serverURL) {
		ClientSession clientSession = null;
		try {
			//System.setProperty("bluecove.debug", "true");
			mainInstance.setStatus("Connecting ...");
			clientSession = (ClientSession) Connector.open(serverURL);
			HeaderSet hs = clientSession.connect(clientSession.createHeaderSet());

			hs.setHeader(HeaderSet.NAME, fileName);
			String type = ObexTypes.getObexFileType(fileName);
			if (type != null) {
				hs.setHeader(HeaderSet.TYPE, type);
			}
			hs.setHeader(HeaderSet.LENGTH, new Long(data.length));
			
			mainInstance.progressBar.setMaximum(data.length);
			mainInstance.setProgressValue(0);

			mainInstance.setStatus("Sending " + fileName + " ...");
			Operation po = clientSession.put(hs);

			OutputStream os = po.openOutputStream();
			
			ByteArrayInputStream is = new ByteArrayInputStream(data);
			byte[] buffer = new byte[0xFF];
			int i = is.read(buffer);
			int done = 0;
			while (i != -1) {
				os.write(buffer, 0, i);
				done += i;
				mainInstance.setProgressValue(done);
				i = is.read(buffer);
			}
			os.flush();
			os.close();
			
			//log.debug("put responseCode " + po.getResponseCode());
			
			po.close();
			clientSession.disconnect(null);
			//log.debug("disconnect responseCode " + hs.getResponseCode());
			
			if (hs.getResponseCode() == ResponseCodes.OBEX_HTTP_OK) {
				mainInstance.setStatus("Finished successfully");
			}
			
		} catch (IOException e) {
			Main.debug(e);
			mainInstance.setStatus("Communication error " + e.getMessage());
		} catch (Throwable e) {
			Main.debug(e);
			mainInstance.setStatus("Error" + e.getMessage());
		} finally {
			if (clientSession != null) {
				try {
					clientSession.close();
				} catch (IOException ignore) {
				}
			}
			clientSession = null;
			mainInstance.setProgressValue(0);
		}
	}
	
	
}
