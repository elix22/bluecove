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
import java.io.OutputStream;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import junit.framework.Assert;

public class TestResponderServer implements CanShutdown, Runnable {
	
	public static int countSuccess = 0; 
	
	public static int countFailure = 0;
	
	public static int countConnection = 0;
	
	private boolean stoped = false;
	
	boolean isRunning = false;
	
	private StreamConnectionNotifier server;
	
	private TestTimeOutMonitor monitor;
	
	private class ConnectionTread extends Thread {
		
		StreamConnection conn;
		
		boolean isRunning = true;
		
		ConnectionTread(StreamConnection conn) {
			super("ConnectionTread" + (++countConnection));
			this.conn = conn;
		}
		
		public void run() {
			InputStream is = null;
			OutputStream os = null;
			int testType = 0;
			try {
				is = conn.openInputStream();
				os = conn.openOutputStream();
				testType = is.read();

				if (testType == Consts.TEST_TERMINATE) {
					Logger.info("Stop requested");
					shutdown();
					return;
				}
				CommunicationTester.runTest(testType, true, is, os);
				os.write(Consts.TEST_REPLY_OK);
				os.write(testType);
				os.flush();
				countSuccess++;
				Logger.debug("Test# " + testType + " ok");
				try {
					Thread.sleep(Consts.serverSendSleep);
				} catch (InterruptedException e) {
				}
			} catch (Throwable e) {
				countFailure++;
				Logger.error("Test# " + testType + " error", e);
			} finally {
				IOUtils.closeQuietly(is);
				IOUtils.closeQuietly(os);
				IOUtils.closeQuietly(conn);
				isRunning = false;
				synchronized (this) {
					notifyAll();
				}
			}
			Logger.info("*Test Success:" + countSuccess + " Failure:" + countFailure);
		}
		
	}
	
	public TestResponderServer() throws BluetoothStateException {
		
		LocalDevice localDevice = LocalDevice.getLocalDevice();
		Logger.info("address:" + localDevice.getBluetoothAddress());
		Logger.info("name:" + localDevice.getFriendlyName());
 	    
		Assert.assertNotNull("BT Address", localDevice.getBluetoothAddress());
		Assert.assertNotNull("BT Name", localDevice.getFriendlyName());
		
		localDevice.setDiscoverable(DiscoveryAgent.GIAC);

	}
	
	public void run() {
		stoped = false;
		isRunning = true;
		if (!CommunicationTester.continuous) {
			monitor = new TestTimeOutMonitor(this, Consts.serverTimeOutMin);
		}
		try {
			server = (StreamConnectionNotifier) Connector
					.open("btspp://localhost:"
							+ CommunicationTester.uuid
							+ ";name="
							+ Consts.RESPONDER_SERVERNAME
							//;authenticate=false;encrypt=false
							+ ";authorize=false");

			Logger.info("ResponderServer started");
			if (CommunicationTester.testServiceAttributes) {
				ServiceRecord record = LocalDevice.getLocalDevice().getRecord(server);
				if (record == null) {
					Logger.warn("Bluetooth ServiceRecord is null");
				} else {
					buildServiceRecord(record, server);
				}
			}
			
			while (!stoped) {
				Logger.info("Accepting connection");
				StreamConnection conn = server.acceptAndOpen();
				if (!stoped) {
					Logger.info("Received connection");
					ConnectionTread t = new ConnectionTread(conn);
					t.start();
					if (!CommunicationTester.acceptWhileConnected) {
						while (t.isRunning) {
							 synchronized (t) {
								 try {
									t.wait();
								} catch (InterruptedException e) {
									break;
								}
							 }
						}
					}
				} else {
					IOUtils.closeQuietly(conn);
				}
			}

			server.close();
			server = null;
		} catch (IOException e) {
			if (!stoped) {
				Logger.error("Server start error", e);
			}
		} finally {
			Logger.info("Server finished");
			isRunning = false;
		}
		if (monitor != null) {
			monitor.finish();
		}
	}

	public void shutdown() {
		Logger.info("shutdownServer");
		stoped = true;
		if (server != null) {
			try {
				server.close();
			} catch (IOException e) {
			}
		}
	}
	
    public void buildServiceRecord(ServiceRecord record, StreamConnectionNotifier notifier) throws ServiceRegistrationException {
        String id = "";
    	try {
    		id = "pub";
			buildServiceRecordPub(record);
			id = "int";
			record.setAttributeValue(Consts.TEST_SERVICE_ATTRIBUTE_INT_ID,
			        new DataElement(DataElement.INT_1, Consts.TEST_SERVICE_ATTRIBUTE_INT_VALUE));
			id = "str";
            record.setAttributeValue(Consts.TEST_SERVICE_ATTRIBUTE_STR_ID,
            new DataElement(DataElement.STRING, Consts.TEST_SERVICE_ATTRIBUTE_STR_VALUE));
			id = "url";
			record.setAttributeValue(Consts.TEST_SERVICE_ATTRIBUTE_URL_ID,
			        new DataElement(DataElement.URL, Consts.TEST_SERVICE_ATTRIBUTE_URL_VALUE));
		} catch (Throwable e) {
			Logger.error("ServiceRecord " + id, e);
		}
    }
    
    public void setAttributeValue(ServiceRecord record, int attrID, DataElement attrValue) {
        try {
            if (!record.setAttributeValue(attrID, attrValue)) {
                Logger.error("SrvReg attrID=" + attrID);
            }
        } catch (Exception e) {
            Logger.error("SrvReg attrID=" + attrID, e);
        }
    }
    
    public void buildServiceRecordPub(ServiceRecord record) throws ServiceRegistrationException {
        final short UUID_PUBLICBROWSE_GROUP = 0x1002;
        final short ATTR_BROWSE_GRP_LIST = 0x0005;
        // Add the service to the 'Public Browse Group'
        DataElement browseClassIDList = new DataElement(DataElement.DATSEQ);
        UUID browseClassUUID = new UUID(UUID_PUBLICBROWSE_GROUP);
        browseClassIDList.addElement(new DataElement(DataElement.UUID, browseClassUUID));
        setAttributeValue(record, ATTR_BROWSE_GRP_LIST, browseClassIDList);
    }
    
	public static void main(String[] args) {
		JavaSECommon.initOnce();
		try {
			(new TestResponderServer()).run();
			if (TestResponderServer.countFailure > 0) {
				System.exit(1);
			} else {
				System.exit(0);
			}
		} catch (Throwable e) {
			Logger.error("start error ", e);
			System.exit(1);
		}
	}

}
