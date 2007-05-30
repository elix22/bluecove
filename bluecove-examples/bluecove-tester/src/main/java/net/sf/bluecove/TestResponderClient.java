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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import junit.framework.Assert;


/**
 * @author vlads
 *
 */
public class TestResponderClient implements Runnable {

	public static int countSuccess = 0; 
	
	public static FailureLog failure = new FailureLog("Client failure");

	public static int discoveryCount = 0;
	
	public static int connectionCount = 0;
	
	public static int discoveryDryCount = 0;
	
	public static int discoverySuccessCount = 0;
	
	public static long lastSuccessfulDiscovery;
	
	public static int countConnectionThreads = 0;
	
	private int connectedConnections = 0;
	
	private int connectedConnectionsExpect = 1;
	
	private int connectedConnectionsInfo = 1;
	
	public Thread thread;
	
	private boolean stoped = false;
	
	boolean discoveryOnce = false;
	
	boolean connectOnce = false;
	
	boolean useDiscoveredDevices = false;
	
	boolean isRunning = false;
	
	boolean runStressTest = false;
	
	BluetoothInquirer bluetoothInquirer;
	
	public static Hashtable recentDeviceNames = new Hashtable/*<BTAddress,Name>*/();
	
	public String connectURL = null;
	
	public static synchronized void clear() {
		countSuccess = 0;
		failure.clear();
		discoveryCount = 0;
	}
    
	public class BluetoothInquirer implements DiscoveryListener {
	    
		boolean inquiring;

		Vector devices = new Vector();
		
		Vector serverURLs = new Vector();

	    public int[] attrIDs;

	    public final UUID L2CAP = new UUID(0x0100);

	    public final UUID RFCOMM = new UUID(0x0003);
	    
		private UUID searchUuidSet[];
		
		DiscoveryAgent discoveryAgent;
		
		int servicesSearchTransID;
		
		private String servicesOnDeviceName = null;
		
		private String servicesOnDeviceAddress = null;
		
		private boolean servicesFound = false;
		
		private boolean anyServicesFound = false;
		
		public BluetoothInquirer() {
			if (Configuration.searchOnlyBluecoveUuid) {
				searchUuidSet = new UUID[] { L2CAP, RFCOMM, CommunicationTester.uuid };
			} else {
				searchUuidSet = new UUID[] { L2CAP };
			}
			if (!Configuration.testServiceAttributes) {
				attrIDs = null;
			} else if (Configuration.testIgnoreNotWorkingServiceAttributes) {
				attrIDs = new int[] { 
				    	Consts.TEST_SERVICE_ATTRIBUTE_INT_ID, 
				    	Consts.TEST_SERVICE_ATTRIBUTE_URL_ID,
				    	Consts.TEST_SERVICE_ATTRIBUTE_BYTES_ID,
				    	Consts.VARIABLE_SERVICE_ATTRIBUTE_BYTES_ID,
				    	Consts.SERVICE_ATTRIBUTE_BYTES_SERVER_INFO
						};
			} else {
				attrIDs = new int[] { 
				    	0x0100, // Service name
				    	Consts.TEST_SERVICE_ATTRIBUTE_INT_ID, 
				    	Consts.TEST_SERVICE_ATTRIBUTE_STR_ID,
				    	Consts.TEST_SERVICE_ATTRIBUTE_URL_ID,
				    	Consts.TEST_SERVICE_ATTRIBUTE_LONG_ID,
				    	Consts.TEST_SERVICE_ATTRIBUTE_BYTES_ID,
				    	Consts.VARIABLE_SERVICE_ATTRIBUTE_BYTES_ID,
				    	Consts.SERVICE_ATTRIBUTE_BYTES_SERVER_INFO
						};
			}
		}
	    
		public boolean hasServers() {
	    	return ((serverURLs != null) && (serverURLs.size() >= 1));
	    }

	    public void shutdown() {
	    	if (inquiring && (discoveryAgent != null)) {
	    		cancelInquiry();
	    		cancelServiceSearch();
	    	}
	    }
	    
	    private synchronized void cancelInquiry() {
	    	try {
	    		if (discoveryAgent != null) {
	    			discoveryAgent.cancelInquiry(this);
	    		}
			} catch (Throwable e) {
			}
	    }
	    
	    private void cancelServiceSearch() {
	    	try {
				if ((servicesSearchTransID != 0) && (discoveryAgent != null)) {
					discoveryAgent.cancelServiceSearch(servicesSearchTransID);
					servicesSearchTransID = 0;
				}
			} catch (Throwable e) {
			}
	    }
	    
	    public boolean runDeviceInquiry() {
			boolean needToFindDevice = Configuration.continuousDiscoveryDevices
					|| ((devices.size() == 0) && (serverURLs.size() == 0));
			try {
				if (useDiscoveredDevices) {
					copyDiscoveredDevices();
					useDiscoveredDevices = false;
				} else if (needToFindDevice) {
					Logger.debug("Starting Device inquiry");
					devices.removeAllElements();
					long start = System.currentTimeMillis();
					try {
						discoveryAgent = LocalDevice.getLocalDevice().getDiscoveryAgent();
						discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
					} catch (BluetoothStateException e) {
						Logger.error("Cannot start Device inquiry", e);
						return false;
					}
					inquiring = true;
					synchronized (this) {
						try {
							wait();
						} catch (InterruptedException e) {
							return false;
						}
					}
					if (stoped) {
						return true;
					}
					cancelInquiry();
					Logger.debug("  Device inquiry took " + Logger.secSince(start));
					RemoteDeviceInfo.discoveryInquiryFinished(Logger.since(start));
				}

				if (Configuration.continuousDiscoveryService || serverURLs.size() == 0) {
					serverURLs.removeAllElements();
					try {
						return startServicesSearch();
					} finally {
						cancelServiceSearch();
					}
				} else {
					return true;
				}
			} finally {
				inquiring = false;
			}
		}
	    
	    private void copyDiscoveredDevices() {
	    	if (RemoteDeviceInfo.devices.size() == 0) {
	    		Logger.warn("No device in history, run Discovery");
	    	}
	    	for (Enumeration iter = RemoteDeviceInfo.devices.elements(); iter.hasMoreElements();) {
				RemoteDeviceInfo dev = (RemoteDeviceInfo) iter.nextElement();
				devices.addElement(dev.remoteDevice);	
	    	}
	    }
	    
        public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass cod) {
        	if (stoped) {
        		return;
        	}
        	if (Configuration.discoverOnlyTestDevices && !isWhiteDevice(remoteDevice.getBluetoothAddress())) {
        		return;
        	}
        	if ((Configuration.discoverDevicesComputers && (cod.getMajorDeviceClass() == Consts.DEVICE_COMPUTER)) 
        			|| ((Configuration.discoverDevicesPhones && (cod.getMajorDeviceClass() == Consts.DEVICE_PHONE)))) {
	        	devices.addElement(remoteDevice);				
			} else {
				Logger.debug("ignore device " + niceDeviceName(remoteDevice.getBluetoothAddress()) + " " + cod);
				return;
			}
//        	String name = null;
//        	try {
//        		name = remoteDevice.getFriendlyName(false);
//			} catch (IOException e) {
//				Logger.debug("er.getFriendlyName," + remoteDevice.getBluetoothAddress(), e);
//			}
        	RemoteDeviceInfo.deviceFound(remoteDevice);
			Logger.debug("deviceDiscovered " + niceDeviceName(remoteDevice.getBluetoothAddress()));
        }

	    private boolean startServicesSearch() {
	    	if (devices.size() == 0) {
	    		return true;
	    	}
	        Logger.debug("Starting Services search " + Logger.timeNowToString());
	        long inquiryStart = System.currentTimeMillis();
	        nextDevice:
	        for (Enumeration iter = devices.elements(); iter.hasMoreElements();) {
	        	if (stoped) {
	        		break;
	        	}
	        	servicesFound = false;
	        	anyServicesFound = false;
	        	long start = System.currentTimeMillis();
				RemoteDevice remoteDevice = (RemoteDevice) iter.nextElement();
	        	String name = "";
	        	if (Configuration.discoveryGetDeviceFriendlyName || Configuration.isBlueCove) {
					try {
						name = remoteDevice.getFriendlyName(false);
						if ((name != null) && (name.length() > 0)) {
							recentDeviceNames.put(remoteDevice.getBluetoothAddress().toUpperCase(), name);
						}
					} catch (Throwable e) {
						Logger.error("er.getFriendlyName," + remoteDevice.getBluetoothAddress(), e);
					}
				}
	        	servicesOnDeviceAddress = remoteDevice.getBluetoothAddress();
	        	servicesOnDeviceName = niceDeviceName(servicesOnDeviceAddress);
				Logger.debug("Search Services on " + servicesOnDeviceName + " " + name);

				int transID;
				
				synchronized (this) {
			    	try {
			    		discoveryAgent = LocalDevice.getLocalDevice().getDiscoveryAgent();
			    		servicesSearchTransID = discoveryAgent.searchServices(attrIDs, searchUuidSet, remoteDevice, this);
			    		transID = servicesSearchTransID; 
			    		if (transID <= 0) {
			    			Logger.warn("servicesSearch TransID mast be positive, " + transID);
			    		}
			        } catch(BluetoothStateException e) {
			        	Logger.error("Cannot start searchServices", e);
			        	continue nextDevice;
				    }
			        try {
						wait();
					} catch (InterruptedException e) {
						break;
					} finally {
			    		cancelServiceSearch();
					}
		        }	
				RemoteDeviceInfo.searchServices(remoteDevice, servicesFound, Logger.since(start));
				String msg = (anyServicesFound)?"; service found":"; no services";
				Logger.debug("  Services Search " + transID + " took " + Logger.secSince(start) + msg);
			}
	        Logger.debug("Services search completed " + Logger.secSince(inquiryStart));
	        return true;
	    }

        public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
        	if (stoped) {
        		return;
        	}
			for (int i = 0; i < servRecord.length; i++) {
				anyServicesFound = true;
				String url = servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
				Logger.info("*found server " + url);
				if (url == null) {
					// Bogus service Record
					continue;
				}
				
				boolean isBlueCoveTestService;
				
				if (Configuration.searchOnlyBluecoveUuid) {
					isBlueCoveTestService = ServiceRecordTester.testServiceAttributes(servRecord[i], servicesOnDeviceName, servicesOnDeviceAddress);
				} else {
					isBlueCoveTestService = ServiceRecordTester.hasServiceClassUUID(servRecord[i], CommunicationTester.uuid);
					if (isBlueCoveTestService) {
						ServiceRecordTester.testServiceAttributes(servRecord[i], servicesOnDeviceName, servicesOnDeviceAddress);
					}
				}
				
				if (isBlueCoveTestService) {
					discoveryCount++;
					Logger.info("Found BlueCove SRV:" + niceDeviceName(servRecord[i].getHostDevice().getBluetoothAddress()));
				}
				
				if (Configuration.searchOnlyBluecoveUuid || isBlueCoveTestService) {
					serverURLs.addElement(url);
				} else {
					Logger.info("is not TestService on " + niceDeviceName(servRecord[i].getHostDevice().getBluetoothAddress()));
				}
				if (isBlueCoveTestService) {
					servicesFound = true;
				}
			}
		}

        public synchronized void serviceSearchCompleted(int transID, int respCode) {
			switch (respCode) {
			case SERVICE_SEARCH_ERROR:
				Logger.error("error occurred while processing the service search");
				break;
			case SERVICE_SEARCH_TERMINATED:
				Logger.info("SERVICE_SEARCH_TERMINATED");
				break;
			case SERVICE_SEARCH_DEVICE_NOT_REACHABLE:
				Logger.info("SERVICE_SEARCH_DEVICE_NOT_REACHABLE");
				break;
			}
			notifyAll();
		}

        public synchronized void inquiryCompleted(int discType) {
        	if (discType == INQUIRY_ERROR) {
        		Logger.error("inquiry ended abnormally"); 
        	}
        	notifyAll();
        }
	    
	}
	
	public TestResponderClient() throws BluetoothStateException {
		
		LocalDevice localDevice = LocalDevice.getLocalDevice();
		Logger.info("address:" + localDevice.getBluetoothAddress());
		Logger.info("name:" + localDevice.getFriendlyName());
		Logger.info("class:" + localDevice.getDeviceClass());

		printProperty("bluetooth.api.version");
		printProperty("bluetooth.sd.trans.max");
		printProperty("bluetooth.sd.attr.retrievable.max");
		printProperty("bluetooth.connected.devices.max");
		printProperty("bluetooth.connected.inquiry.scan");
		printProperty("bluetooth.connected.page.scan");
		printProperty("bluetooth.connected.inquiry");
		
		String bluecoveVersion = LocalDevice.getProperty("bluecove");
		if (bluecoveVersion != null) {
			Configuration.isBlueCove = true;
			
			Logger.info("bluecove:" + bluecoveVersion);
			Logger.info("stack:" + LocalDevice.getProperty("bluecove.stack"));
			Logger.info("radio manufacturer:" + LocalDevice.getProperty("bluecove.radio.manufacturer"));
			Logger.info("radio version:" + LocalDevice.getProperty("bluecove.radio.version"));
			
			Configuration.stackWIDCOMM = "WIDCOMM".equalsIgnoreCase(LocalDevice.getProperty("bluecove.stack"));
		}
		
		Assert.assertNotNull("BT Address", localDevice.getBluetoothAddress());
		if (!Configuration.windowsCE) {
			Assert.assertNotNull("BT Name", localDevice.getFriendlyName());
		}
		
	}
	
	public static void printProperty(String property) {
		String val = LocalDevice.getProperty(property);
		if (val != null) {
			Logger.info(property + ":" + val);
		}
	}
	
	 public static boolean isWhiteDevice(String bluetoothAddress) {
		String addr = bluetoothAddress.toUpperCase();
		return (Configuration.testDeviceNames.get(addr) != null);
	}

	 public static String niceDeviceName(String bluetoothAddress) {
		 String w = getWhiteDeviceName(bluetoothAddress);
		 if (w == null) {
			 w = (String)recentDeviceNames.get(bluetoothAddress.toUpperCase());
		 }
		 return (w != null)?w:bluetoothAddress;
	}
	 
	public static String getWhiteDeviceName(String bluetoothAddress) {
		if ((bluetoothAddress == null) || (Configuration.testDeviceNames == null)) {
			return null;
		}
		String addr = bluetoothAddress.toUpperCase();
		return (String) Configuration.testDeviceNames.get(addr);
	}
	
	public static String extractBluetoothAddress(String serverURL) {
		int start = serverURL.indexOf("//");
		if (start == -1) {
			return null;
		}
		start += 2;
		int end = serverURL.indexOf(":", start);
		if (end == -1) {
			return null;
		}
		return serverURL.substring(start, end);
	}
	
	public void connectAndTest(String serverURL) {
		String deviceName = niceDeviceName(extractBluetoothAddress(serverURL));
		long start = System.currentTimeMillis();
		Logger.debug("connect:" + deviceName + " " + serverURL);
		for(int testType = Configuration.TEST_START; (!stoped) && (runStressTest || testType <= Configuration.TEST_LAST); testType ++) {
			StreamConnectionTimeOut c = new StreamConnectionTimeOut();
			TestStatus testStatus = new TestStatus();
			TestTimeOutMonitor monitor = null;
			boolean connectedConnectionsInc = false;
			try {
				if (!runStressTest) {
					Logger.debug("test " + testType + " connects");
				} else {
					testType = Configuration.STERSS_TEST_CASE;	
				}
				int connectionOpenTry = 0;
				while ((c.conn == null) && (!stoped)) {
					try {
						c.conn = (StreamConnection) Connector.open(serverURL);
					} catch (IOException e) {
						connectionOpenTry ++;
						if (connectionOpenTry > CommunicationTester.clientConnectionOpenRetry) {
							throw e;
						}
						Thread.sleep(500);
						Logger.debug("connect try:" + connectionOpenTry);
					}
				}
				if (stoped) {
					return;
				}
				c.os = c.conn.openOutputStream();
				connectionCount ++;
				connectedConnections ++;
				connectedConnectionsInc = true;
				if (connectedConnectionsInfo < connectedConnections) {
					connectedConnectionsInfo = connectedConnections;
					Logger.info("now connected:" + connectedConnectionsInfo);
				}
				c.active();
				monitor = new TestTimeOutMonitor("test" + testType, c, 2);
				if (!runStressTest) {
					Logger.debug("test run:" + testType);
				} else {
					Logger.debug("connected:" + connectionCount);
					if (connectionCount % 5 == 0) {
						Logger.debug("Test time " + Logger.secSince(start));
					}
				}
				
				c.os.write(testType);
				c.os.flush();
				
				c.is = c.conn.openInputStream();
				c.active();

				CommunicationTester.runTest(testType, false, c.conn, c.is, c.os, testStatus);
				c.active();

				if (testStatus.isSuccess) {
					countSuccess++;
					Logger.debug("test " + testType + " " + testStatus.getName() + ": OK");
				} else if (testStatus.streamClosed) {
					Logger.debug("see server log");
				} else {
					c.os.flush();
					Logger.debug("read server status");
					int ok = c.is.read();
					Assert.assertEquals("Server reply", Consts.SEND_TEST_REPLY_OK, ok);
					int conformTestType = c.is.read();
					Assert.assertEquals("Test reply conform", testType, conformTestType);
					countSuccess++;
					Logger.debug("test " + testType + " " + testStatus.getName() + ": OK");
				}
				if (connectionCount % 5 == 0) {
					Logger.info("*Success:" + countSuccess + " Failure:" + failure.countFailure);
				}
				if (Configuration.storage != null) {
					Configuration.storage.storeData("lastURL", serverURL);
				}
				
				// Dellay to see if many connections are made.
				if (connectedConnectionsInfo < connectedConnectionsExpect) {
					synchronized (TestResponderClient.this) {
						try {
							TestResponderClient.this.wait(5 * 1000);
						} catch (InterruptedException e) {
							break;
						}
					}
				}
			} catch (Throwable e) {
				failure.addFailure(deviceName + " test " + testType  + " " + testStatus.getName(), e);
				Logger.error(deviceName + " test " + testType + " " + testStatus.getName(), e);
			} finally {
				if (connectedConnectionsInc) {
					connectedConnections --;
				}
				if (monitor != null) {
					monitor.finish();
				}
				IOUtils.closeQuietly(c.os);
				IOUtils.closeQuietly(c.is);
				IOUtils.closeQuietly(c.conn);
			}
			// Let the server restart
			try {
				Thread.sleep(Consts.clientReconnectSleep);
			} catch (InterruptedException e) {
				break;
			}
		} 
		if (!Configuration.continuous) {
			sendStopServerCmd(serverURL);
		}
	}
	
	public void sendStopServerCmd(String serverURL) {
		StreamConnection conn = null;
		InputStream is = null;
		OutputStream os = null;
		try {
			Logger.debug("stopServer");
			conn = (StreamConnection) Connector.open(serverURL);
			os = conn.openOutputStream();
			
			os.write(Consts.TEST_SERVER_TERMINATE);
			os.flush();
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}
		} catch (Throwable e) {
			Logger.error("stopServer error", e);
		}
		IOUtils.closeQuietly(os);
		IOUtils.closeQuietly(is);
		IOUtils.closeQuietly(conn);
	}

	
	private class ClientConnectionTread extends Thread {
		
		String url;
		
		ClientConnectionTread(String url) {
			super("ClientConnectionTread" + (++countConnectionThreads));
			this.url = url;
		}
		
		public void run() {
			connectAndTest(url);
		}
	}
	
	private void connectAndTest(int numberOfURLs, Enumeration urls) {
		if ((!Configuration.testConnectionsMultipleThreads) || (numberOfURLs == 1)) {
			connectedConnectionsExpect = 1;
			connectedConnectionsInfo = 1;
			for (; urls.hasMoreElements();) {
				if (stoped) {
					break;
				}
				String url = (String) urls.nextElement();
				connectAndTest(url);
			}
		} else {
			connectedConnectionsExpect = numberOfURLs;
			connectedConnectionsInfo = 1;
			Logger.debug("start " + numberOfURLs + " threads");
			Vector threads = new Vector();
			for (; urls.hasMoreElements();) {
				ClientConnectionTread t = new ClientConnectionTread((String) urls.nextElement());
				t.start();
				threads.addElement(t);
			}
			for(Enumeration en = threads.elements(); en.hasMoreElements();) {
				Thread t = (Thread)en.nextElement();
				try {
					t.join();
				} catch (InterruptedException e) {
					break;
				}
			}
			if (connectedConnectionsInfo < connectedConnectionsExpect) {
				if (!stoped) {
					failure.addFailure("Fails to establish " + connectedConnectionsExpect + " connections same time");
					Logger.error("Fails to establish " + connectedConnectionsExpect + " connections same time");
				}
			} else {
				Logger.info("Established " + connectedConnectionsExpect + " connections same time");
			}
		}
	}
	
	public void run() {
		Logger.debug("Client started..." + Logger.timeNowToString());
		isRunning = true;
		try {
			bluetoothInquirer = new BluetoothInquirer();

			int startTry = 0;
			if (connectURL != null) {
				bluetoothInquirer.serverURLs.addElement(connectURL);
			}
			
			while (!stoped) {
				if ((!bluetoothInquirer.hasServers()) || (Configuration.continuousDiscovery && (connectURL == null)) || (!Configuration.testConnections) ) {
					if (!bluetoothInquirer.runDeviceInquiry()) {
						startTry ++;
						try {
							Thread.sleep(2000);
						} catch (Exception e) {
							break;
						}
						if (startTry < 3) {
							continue;
						}
						Switcher.yield(this);
					} else {
						startTry = 0;
					}
					while (bluetoothInquirer.inquiring) {
						try {
							Thread.sleep(1000);
						} catch (Exception e) {
							break;
						}
					}
				}
				if ((Configuration.testConnections) && (bluetoothInquirer.hasServers())) {
					discoveryDryCount = 0;
					discoverySuccessCount ++;
					lastSuccessfulDiscovery = System.currentTimeMillis();
					if (!discoveryOnce) {
						connectAndTest(bluetoothInquirer.serverURLs.size(), bluetoothInquirer.serverURLs.elements());
					}
				} else {
					discoveryDryCount ++;
					if ((discoveryDryCount % 5 == 0) && (lastSuccessfulDiscovery != 0)) {
						Logger.debug("No services " + discoveryDryCount + " times for " + Logger.secSince(lastSuccessfulDiscovery) + " " + discoverySuccessCount);
					}
				}
				Logger.info("*Success:" + countSuccess + " Failure:" + failure.countFailure);
				if ((countSuccess + failure.countFailure > 0) && (!Configuration.continuous)) {
					break;
				}
				if (stoped || discoveryOnce || connectOnce) {
					break;
				}
				Switcher.yield(this);
			}
		} catch (Throwable e) {
			if (!stoped) {
				Logger.error("cleint error ", e);
			}
		} finally {
			isRunning = false;
			Logger.info("Client finished! " + Logger.timeNowToString());
			Switcher.yield(this);
		}
	}
	
	public void shutdown() {
		Logger.info("shutdownClient");
		stoped = true;
		thread.interrupt();
		if (bluetoothInquirer != null) {
			bluetoothInquirer.shutdown();
		}
	}
	
	public static void main(String[] args) {
		JavaSECommon.initOnce();
		try {
			(new TestResponderClient()).run();
			//System.exit(0);
		} catch (Throwable e) {
			Logger.error("start error ", e);
		}
	}
}
