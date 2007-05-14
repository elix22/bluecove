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
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
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
import junit.framework.AssertionFailedError;


/**
 * @author vlads
 *
 */
public class TestResponderClient implements Runnable {

	public static int countSuccess = 0; 
	
	public static FailureLog failure = new FailureLog("Client failure");

	public static int discoveryCount = 0;
	
	public Thread thread;
	
	private boolean stoped = false;
	
	boolean discoveryOnce = false;
	
	boolean useDiscoveredDevices = false;
	
	boolean isRunning = false;
	
	boolean runStressTest = false;
	
	BluetoothInquirer bluetoothInquirer;
	
	public static synchronized void clear() {
		countSuccess = 0;
		failure.clear();
		discoveryCount = 0;
	}
    
	public class BluetoothInquirer implements DiscoveryListener {
	    
		boolean inquiring;

		Vector devices;
		
		Vector serverURLs;

	    public int[] attrIDs;

	    public final UUID L2CAP = new UUID(0x0100);

	    public final UUID RFCOMM = new UUID(0x0003);
	    
		private UUID searchUuidSet[];
		
		DiscoveryAgent discoveryAgent;
		
		int servicesSearchTransID;
		
		private String servicesOnDeviceName = null;
		
		private boolean servicesFound = false;
		
		private boolean anyServicesFound = false;
		
		public BluetoothInquirer() {
			if (Configuration.searchOnlyBluecoveUuid) {
				searchUuidSet = new UUID[] { L2CAP, RFCOMM, CommunicationTester.uuid };
			} else {
				searchUuidSet = new UUID[] { L2CAP };
			}
			if (Configuration.testIgnoreNotWorkingServiceAttributes) {
				attrIDs = new int[] { 
				    	Consts.TEST_SERVICE_ATTRIBUTE_INT_ID, 
				    	Consts.TEST_SERVICE_ATTRIBUTE_URL_ID,
				    	Consts.TEST_SERVICE_ATTRIBUTE_BYTES_ID,
				    	Consts.VARIABLE_SERVICE_ATTRIBUTE_BYTES_ID
						};
			} else {
				attrIDs = new int[] { 
				    	0x0100, // Service name
				    	Consts.TEST_SERVICE_ATTRIBUTE_INT_ID, 
				    	Consts.TEST_SERVICE_ATTRIBUTE_STR_ID,
				    	Consts.TEST_SERVICE_ATTRIBUTE_URL_ID,
				    	Consts.TEST_SERVICE_ATTRIBUTE_LONG_ID,
				    	Consts.TEST_SERVICE_ATTRIBUTE_BYTES_ID,
				    	Consts.VARIABLE_SERVICE_ATTRIBUTE_BYTES_ID
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
    			discoveryAgent.cancelInquiry(this);
			} catch (Throwable e) {
			}
	    }
	    
	    private void cancelServiceSearch() {
	    	try {
				if (servicesSearchTransID != 0) {
					discoveryAgent.cancelServiceSearch(servicesSearchTransID);
					servicesSearchTransID = 0;
				}
			} catch (Throwable e) {
			}
	    }
	    
	    public boolean startDeviceInquiry() {
			Logger.debug("Starting Device inquiry");
	    	devices = new Vector();
	    	serverURLs = new Vector();
	    	long start = System.currentTimeMillis();
	    	try {
	    		discoveryAgent = LocalDevice.getLocalDevice().getDiscoveryAgent();
	    		if (!useDiscoveredDevices) {
	    			discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
	    		} else {
	    			copyDiscoveredDevices();
	    			useDiscoveredDevices = false;
	    			return startServicesSearch();
	    		}
	        } catch(BluetoothStateException e) {
	        	Logger.error("Cannot start Device inquiry", e);
		        return false;
		    }
	        inquiring = true;
	        try {
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
						return false;
					}
				}
				if (!stoped) {
					cancelInquiry();
					Logger.debug("  Device inquiry took " + Logger.secSince(start));
					RemoteDeviceInfo.discoveryInquiryFinished(Logger.since(start));
					return startServicesSearch();
				} else {
					return true;
				}
			} finally {
				cancelInquiry();
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
				Logger.debug("ignore device " + niceDeviceName(remoteDevice.getBluetoothAddress()));
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
	        Logger.debug("Starting Services search " + Logger.timeNowToString());
	        long inquiryStart = System.currentTimeMillis();
	        for (Enumeration iter = devices.elements(); iter.hasMoreElements();) {
	        	if (stoped) {
	        		break;
	        	}
	        	servicesFound = false;
	        	anyServicesFound = false;
	        	long start = System.currentTimeMillis();
				RemoteDevice remoteDevice = (RemoteDevice) iter.nextElement();
	        	String name = "";
	        	if (Configuration.discoveryGetDeviceFriendlyName) {
					try {
						name = remoteDevice.getFriendlyName(false);
					} catch (Throwable e) {
						Logger.error("er.getFriendlyName," + remoteDevice.getBluetoothAddress(), e);
					}
				}
	        	servicesOnDeviceName = niceDeviceName(remoteDevice.getBluetoothAddress());
				Logger.debug("Search Services on " + servicesOnDeviceName + " " + name);

				synchronized (this) {
			    	try {
			    		servicesSearchTransID = discoveryAgent.searchServices(attrIDs, searchUuidSet, remoteDevice, this);
			        } catch(BluetoothStateException e) {
			        	Logger.error("Cannot start searchServices", e);
			        	return false;
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
				String msg = "";
				if (!anyServicesFound) {
					msg = "; no services";
				}
				Logger.debug("  Services Search took " + Logger.secSince(start) + msg);
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
				boolean hadError = false;
				boolean isBlueCoveTestService = false;
				byte variableData[] = null;
				String url = servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
				Logger.info("*found server " + url);
				try {
					if (Configuration.testServiceAttributes) {
						int[] attributeIDs = servRecord[i].getAttributeIDs();
						// Logger.debug("attributes " + attributeIDs.length);

						boolean foundName = false;
						boolean foundInt = false;
						boolean foundStr = false;
						boolean foundUrl = false;
						boolean foundLong = false;
						boolean foundBytes = false;
						
						boolean foundIntOK = false;
						boolean foundUrlOK = false;
						boolean foundBytesOK = false;
						
						for (int j = 0; j < attributeIDs.length; j++) {
							int id = attributeIDs[j];
							try {
								DataElement attrDataElement = servRecord[i].getAttributeValue(id);
								Assert.assertNotNull("attrValue null", attrDataElement);
								switch (id) {
								case 0x0100:
									foundName = true;
									if (!Configuration.testIgnoreNotWorkingServiceAttributes) {
										Assert.assertEquals("name", Consts.RESPONDER_SERVERNAME, attrDataElement.getValue());
										isBlueCoveTestService = true;
									}
									break;
								case Consts.TEST_SERVICE_ATTRIBUTE_INT_ID:
									foundInt = true;
									Assert.assertEquals("int type", Consts.TEST_SERVICE_ATTRIBUTE_INT_TYPE, attrDataElement.getDataType());
									Assert.assertEquals("int", Consts.TEST_SERVICE_ATTRIBUTE_INT_VALUE, attrDataElement.getLong());
									isBlueCoveTestService = true;
									foundIntOK = true;
									break;
								case Consts.TEST_SERVICE_ATTRIBUTE_LONG_ID:
									foundLong = true;
									Assert.assertEquals("long type", Consts.TEST_SERVICE_ATTRIBUTE_LONG_TYPE, attrDataElement.getDataType());
									if (!Configuration.testIgnoreNotWorkingServiceAttributes) {
										Assert.assertEquals("long", Consts.TEST_SERVICE_ATTRIBUTE_LONG_VALUE, attrDataElement.getLong());
										isBlueCoveTestService = true;
									}
									break;
								case Consts.TEST_SERVICE_ATTRIBUTE_STR_ID:
									foundStr = true;
									Assert.assertEquals("str type", DataElement.STRING, attrDataElement.getDataType());
									if (!Configuration.testIgnoreNotWorkingServiceAttributes) {
										Assert.assertEquals("str", Consts.TEST_SERVICE_ATTRIBUTE_STR_VALUE, attrDataElement.getValue());
										isBlueCoveTestService = true;
									}
									break;
								case Consts.TEST_SERVICE_ATTRIBUTE_URL_ID:
									foundUrl = true;
									int urlType = attrDataElement.getDataType();
									// URL is String on Widcomm
									Assert.assertTrue("url type", (DataElement.URL == urlType) || (DataElement.STRING == urlType));
									if (DataElement.URL != urlType) {
										Logger.warn("attr URL decoded as STRING");
									}
									Assert.assertEquals("url", Consts.TEST_SERVICE_ATTRIBUTE_URL_VALUE, attrDataElement.getValue());
									isBlueCoveTestService = true;
									foundUrlOK = true;
									break;
								case Consts.TEST_SERVICE_ATTRIBUTE_BYTES_ID:
									foundBytes = true;
									Assert.assertEquals("byte[] type", Consts.TEST_SERVICE_ATTRIBUTE_BYTES_TYPE, attrDataElement.getDataType());
									byte[] byteAray;
									try {
										byteAray = (byte[])attrDataElement.getValue();
									} catch (Throwable e) {
										Logger.warn("attr " + id + " " + e.getMessage());
										hadError = true;
										break;
									}
									Assert.assertEquals("byteAray.len", Consts.TEST_SERVICE_ATTRIBUTE_BYTES_VALUE.length, byteAray.length);
									for(int k = 0; k < byteAray.length; k++) {
										Assert.assertEquals("byte[" + k + "]",  Consts.TEST_SERVICE_ATTRIBUTE_BYTES_VALUE[k], byteAray[k]);
									}
									isBlueCoveTestService = true;
									foundBytesOK = true;
									break;	
								case Consts.VARIABLE_SERVICE_ATTRIBUTE_BYTES_ID:
									Assert.assertEquals("var byte[] type", DataElement.INT_16, attrDataElement.getDataType());
									try {
										variableData = (byte[])attrDataElement.getValue();
									} catch (Throwable e) {
										Logger.warn("attr " + id + " " + e.getMessage());
										hadError = true;
										break;
									}
								default:
									if (!Configuration.testIgnoreNotWorkingServiceAttributes) {
										Logger.debug("attribute " + id + " " + BluetoothTypes.getDataElementType(attrDataElement.getDataType()));
									}
								}

							} catch (AssertionFailedError e) {
								Logger.warn("attr " + id + " " + e.getMessage());
								//countFailure++;
								hadError = true;
							}
						}
						if ((!Configuration.testIgnoreNotWorkingServiceAttributes) && (!foundName)) {
							Logger.warn("srv name attr. not found");
							failure.addFailure("srv name attr. not found on " + servicesOnDeviceName);
						}
						if (!foundInt) {
							Logger.warn("srv INT attr. not found");
							failure.addFailure("srv INT attr. not found on " + servicesOnDeviceName);
						}
						if ((!Configuration.testIgnoreNotWorkingServiceAttributes) && (!foundLong)) {
							Logger.warn("srv long attr. not found");
							failure.addFailure("srv long attr. not found on " + servicesOnDeviceName);
						}
						if ((!Configuration.testIgnoreNotWorkingServiceAttributes) && (!foundStr)) {
							Logger.warn("srv STR attr. not found");
							failure.addFailure("srv STR attr. not found on " + servicesOnDeviceName);
						}
						if (!foundUrl) {
							Logger.warn("srv URL attr. not found");
							failure.addFailure("srv URL attr. not found on " + servicesOnDeviceName);
						}
						if (!foundBytes) {
							Logger.warn("srv byte[] attr. not found");
							failure.addFailure("srv byte[] attr. not found on " + servicesOnDeviceName);
						}
						if (variableData == null) {
							Logger.warn("srv data byte[] attr. not found");
							failure.addFailure("srv data byte[] attr. not found on " + servicesOnDeviceName);
						}
						if (foundName && foundUrl && foundInt && foundStr && foundLong && foundBytes && !hadError) {
							Logger.info("all service Attr OK");
							countSuccess++;
						} else if ((Configuration.testIgnoreNotWorkingServiceAttributes) && foundUrl && foundInt && foundBytes && !hadError) {
							Logger.info("service Attr found");
							countSuccess++;
						}
						if (foundIntOK && foundUrlOK && foundBytesOK) {
							Logger.info("Common Service Attr OK");
							discoveryCount++;
							Logger.info("Found BlueCove SRV:" + niceDeviceName(servRecord[i].getHostDevice().getBluetoothAddress()));
						}
					}
				} catch (Throwable e) {
					Logger.error("attrs", e);
				}
				
				if (Configuration.searchOnlyBluecoveUuid || isBlueCoveTestService) {
					serverURLs.addElement(url);
				} else {
					Logger.info("is not TestService on " + niceDeviceName(servRecord[i].getHostDevice().getBluetoothAddress()));
				}
				if (isBlueCoveTestService) {
					servicesFound = true;
					RemoteDeviceInfo.deviceServiceFound(servRecord[i].getHostDevice(), variableData);
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
			Logger.info("bluecove:" + bluecoveVersion);
			Logger.info("stack:" + LocalDevice.getProperty("bluecove.stack"));
			Logger.info("radio manufacturer:" + LocalDevice.getProperty("bluecove.radio.manufacturer"));
			Logger.info("radio version:" + LocalDevice.getProperty("bluecove.radio.version"));
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
		String deviceName = getWhiteDeviceName(extractBluetoothAddress(serverURL));
		if (deviceName == null) {
			deviceName = extractBluetoothAddress(serverURL);
		}
		long start = System.currentTimeMillis();
		Logger.debug("connect:" + deviceName + " " + serverURL);
		int connectionCount = 0;
		for(int testType = Consts.TEST_START; (!stoped) && (runStressTest || testType <= Consts.TEST_LAST); testType ++) {
			StreamConnection conn = null;
			InputStream is = null;
			OutputStream os = null;
			try {
				if (!runStressTest) {
					Logger.debug("test connect:" + testType);
				} else {
					testType = Consts.TEST_BYTE;	
				}
				int connectionOpenTry = 0;
				while ((conn == null) && (!stoped)) {
					try {
						conn = (StreamConnection) Connector.open(serverURL);
					} catch (IOException e) {
						connectionOpenTry ++;
						if (connectionOpenTry > CommunicationTester.clientConnectionOpenRetry) {
							throw e;
						}
						Logger.debug("connect try:" + connectionOpenTry);
					}
				}
				if (stoped) {
					return;
				}
				os = conn.openOutputStream();
				connectionCount ++;
				
				if (!runStressTest) {
					Logger.debug("test run:" + testType);
				} else {
					Logger.debug("connected:" + connectionCount);
					if (connectionCount % 5 == 0) {
						Logger.debug("Test time " + Logger.secSince(start));
					}
				}
				
				os.write(testType);
				os.flush();
				
				is = conn.openInputStream();

				CommunicationTester.runTest(testType, false, is, os);
				os.flush();
				Logger.debug("read server status");
				int ok = is.read();
				Assert.assertEquals("Test reply ok", Consts.SEND_TEST_REPLY_OK, ok);
				int conformTestType = is.read();
				Assert.assertEquals("Test reply conform", testType, conformTestType);
				countSuccess ++;
				Logger.debug("test ok:" + testType);
			} catch (Throwable e) {
				failure.addFailure(deviceName + " test " + testType + " " + e);
				Logger.error(deviceName + " test " + testType, e);
			} finally {
				IOUtils.closeQuietly(os);
				IOUtils.closeQuietly(is);
				IOUtils.closeQuietly(conn);
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
	
	public void run() {
		Logger.debug("Client started..." + Logger.timeNowToString());
		isRunning = true;
		try {
			bluetoothInquirer = new BluetoothInquirer();

			int startTry = 0;
			
			while (!stoped) {
				if ((!bluetoothInquirer.hasServers()) || (Configuration.continuousDiscovery) || (!Configuration.testConnections) ) {
					if (!bluetoothInquirer.startDeviceInquiry()) {
						startTry ++;
						if (startTry < 3) {
							try {
								Thread.sleep(1000);
							} catch (Exception e) {
								break;
							}
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
				if ((Configuration.testConnections) && (bluetoothInquirer.serverURLs != null)) {
					for (Enumeration iter = bluetoothInquirer.serverURLs.elements(); iter.hasMoreElements();) {
						if (stoped) {
							break;
						}
						String url = (String) iter.nextElement();
						connectAndTest(url);
					}
				}
				Logger.info("*Success:" + countSuccess + " Failure:" + failure.countFailure);
				if ((countSuccess + failure.countFailure > 0) && (!Configuration.continuous)) {
					break;
				}
				if (stoped || discoveryOnce) {
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
