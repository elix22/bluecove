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
package net.sf.bluecove;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;
import javax.obex.SessionNotifier;

import net.sf.bluecove.util.BluetoothTypesInfo;
import net.sf.bluecove.util.IOUtils;
import net.sf.bluecove.util.StringUtils;
import net.sf.bluecove.util.TimeUtils;

public class TestResponderServerOBEX extends ServerRequestHandler implements Runnable {

	private SessionNotifier serverConnection;
	
	private boolean isStoped = false;
	
	private boolean isRunning = false;
	
	private Object connectionLock = new Object(); 
	
	private boolean isConnected = false;
	
	private TestResponderServerOBEX() {
		
	}
	
	public static TestResponderServerOBEX startServer() {
		TestResponderServerOBEX srv = new TestResponderServerOBEX();
		Thread thread = new Thread(srv);
		thread.start();
		return srv;
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	public void run() {
		isStoped = false;
		try {
			if (Configuration.testServerOBEX_TCP) {
				serverConnection = (SessionNotifier) Connector
						.open(BluetoothTypesInfo.PROTOCOL_SCHEME_TCP_OBEX + "://");
			} else {
				serverConnection = (SessionNotifier) Connector.open(BluetoothTypesInfo.PROTOCOL_SCHEME_BT_OBEX
						+ "://localhost:" + Configuration.blueCoveOBEXUUID() + ";name=" + Consts.RESPONDER_SERVERNAME + "_ox" + 
						";authenticate=" + (Configuration.authenticate.booleanValue() ? "true" : "false")
						+ ";encrypt=" + (Configuration.encrypt.booleanValue() ? "true" : "false") 
						+ ";authorize=" + (Configuration.authorize ? "true" : "false"));
				if (Configuration.testServiceAttributes.booleanValue()) {
					ServiceRecord record = LocalDevice.getLocalDevice().getRecord(serverConnection);
					if (record == null) {
						Logger.warn("Bluetooth ServiceRecord is null");
					} else {
						TestResponderServer.buildServiceRecord(record);
						try {
							LocalDevice.getLocalDevice().updateRecord(record);
							Logger.debug("OBEX ServiceRecord updated");
						} catch (Throwable e) {
							Logger.error("OBEX Service Record update error", e);
						}
					}
				}
			}
		} catch (Throwable e) {
			Logger.error("OBEX Server start error", e);
			isStoped = true;
			return;
		}
		
		try {
			int errorCount = 0;
			isRunning = true;
			while (!isStoped) {
				Connection cconn;
				synchronized (connectionLock) {
					try {
						Logger.info("Accepting OBEX connections");
						cconn = serverConnection.acceptAndOpen(this);
					} catch (InterruptedIOException e) {
						isStoped = true;
						break;
					} catch (Throwable e) {
						if (errorCount > 3) {
							isStoped = true;
						}
						if (isStoped) {
							return;
						}
						errorCount++;
						Logger.error("acceptAndOpen ", e);
						continue;
					}
					errorCount = 0;
					Logger.info("Received OBEX connection");
					Timer notConnectedTimer = new Timer(); 
					try {
						notConnectedTimer.schedule(new TimerTask() {
							public void run() {
								notConnectedClose();
							}
						}, 1000 * 30);
						connectionLock.wait();
					} catch (InterruptedException e) {
						isStoped = true;
					} finally {
						notConnectedTimer.cancel();
					}
				}
				IOUtils.closeQuietly(cconn);
				isConnected = false;
			}
		} finally {
			close();
			Logger.info("L2CAP Server finished! " + TimeUtils.timeNowToString());
			isRunning = false;
		}
	}
	
	private void notConnectedClose() {
		if (!isConnected) {
			Logger.debug("OBEX connection timeout");	
			synchronized (connectionLock) {
				connectionLock.notify();
			}	
		}
	}
	
	void close() {
		try {
			if (serverConnection != null) {
				serverConnection.close();
			}
			Logger.debug("OBEX ServerConnection closed");
			synchronized (connectionLock) {
				connectionLock.notify();
			}
		} catch (Throwable e) {
			Logger.error("OBEX Server stop error", e);
		}
	}
	
	void closeServer()  {
		isStoped = true;
		close();
	}
	
	public int onConnect(HeaderSet request, HeaderSet reply) {
		isConnected = true;
		Logger.debug("OBEX onConnect");
		return ResponseCodes.OBEX_HTTP_OK;
	}
	
	public void onDisconnect(HeaderSet request, HeaderSet reply) {
		Logger.debug("OBEX onDisconnect");
		synchronized (connectionLock) {
			connectionLock.notify();
		}
	}
	
	public int onSetPath(HeaderSet request, HeaderSet reply, boolean backup, boolean create) {
		Logger.debug("OBEX onSetPath");
		return super.onSetPath(request, reply, backup, create);
	}
	
	public int onDelete(HeaderSet request, HeaderSet reply) {
		Logger.debug("OBEX onDelete");
		return super.onDelete(request, reply);
	}
	
	public int onPut(Operation op) {
		Logger.debug("OBEX onPut");

		try {
			InputStream is = op.openInputStream();

			StringBuffer buf = new StringBuffer();
			while (!isStoped) {
				int data = is.read();
				if (data == -1) {
					Logger.debug("EOS recived");
					break;
				}
				char c = (char) data;
				buf.append(c);
				if ((c == '\n') || (buf.length() > 30)) {
					Logger.debug("cc:" + StringUtils.toBinaryText(buf));
					buf = new StringBuffer();
				}
			}
			if (buf.length() > 0) {
				Logger.debug("cc:" + StringUtils.toBinaryText(buf));
			}
			op.close();
			return ResponseCodes.OBEX_HTTP_OK;
		} catch (IOException e) {
			Logger.error("OBEX Server onPut error", e);
			return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
		}
	}
	
	public int onGet(Operation op) {
		Logger.debug("OBEX onGet");
		String message = "Hello client! now " + new Date().toString();
		try {
			HeaderSet hs = op.getReceivedHeaders();
			String name = (String)hs.getHeader(HeaderSet.NAME);
			if (name != null) {
				message += "\nYou ask for [" + name + "]";
			}
			byte[] messageBytes = message.getBytes();
			
			OutputStream os = op.openOutputStream();
			os.write(messageBytes);
			os.flush();
			os.close();
			op.close();
			return ResponseCodes.OBEX_HTTP_OK;
		} catch (IOException e) {
			Logger.error("OBEX Server onGet error", e);
			return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
		}
	}
}
