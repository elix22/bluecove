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
	
	public static int countFailure = 0;

	public static int discoveryCount = 0;
	
	private boolean stoped = false;
	
	boolean isRunning = false;
	
	public static boolean searchOnlyBluecoveUuid = true;
	/**
	 * Limit connections to precompiled list of test devices.
	 */
	private static boolean onlyWhiteDevices = false;
	
	private static boolean devicesComputers = false;
	
	private static Hashtable whiteDeviceNames = null;
	
	BluetoothInquirer bluetoothInquirer;
	
    static {
		whiteDeviceNames = new Hashtable();
		whiteDeviceNames.put("00E003506231", "Nokia D1");
		whiteDeviceNames.put("00E0035046C1", "Nokia D2");
		whiteDeviceNames.put("0015A8DDF300", "Moto M1 v360");
		whiteDeviceNames.put("0050F2E8D4A6", "Desk MS");
		whiteDeviceNames.put("000D3AA5E36C", "Lapt MS");
		whiteDeviceNames.put("0020E027CE32", "Lapt HP");
		
        whiteDeviceNames.put("0017841C5A8F", "Moto L7");
        whiteDeviceNames.put("00123755AE71", "N 6265i (t)");
        whiteDeviceNames.put("0013706C93D3", "N 6682 (r)");
        whiteDeviceNames.put("0017005354DB", "M i870 (t)");
        whiteDeviceNames.put("001700F07CF2", "M i605 (t)");  
        
        whiteDeviceNames.put("001813184E8B", "SE W810i (r-ml)");
        
        
        whiteDeviceNames.put("00149ABD52E7", "Anya");
        whiteDeviceNames.put("00149ABD538D", "Natasha");
        whiteDeviceNames.put("0007E05387E5", "Palm");
        
        whiteDeviceNames.put("000B0D1796FC", "GPS");
        whiteDeviceNames.put("000D3AA4F7F9", "My Keyboard");
        whiteDeviceNames.put("0050F2E7EDC8", "My Mouse 1");
        whiteDeviceNames.put("0020E03AC5B2", "bob1");
        whiteDeviceNames.put("000D88C03ACA", "bob2");
	}
    
	public class BluetoothInquirer implements DiscoveryListener {
	    
		boolean inquiring;

		Vector devices;
		
		Vector serverURLs;

	    public int[] attrIDs = new int[] { 
	    	0x0100,
	    	0x0101,
	    	Consts.TEST_SERVICE_ATTRIBUTE_INT_ID, 
	    	Consts.TEST_SERVICE_ATTRIBUTE_STR_ID,
	    	Consts.TEST_SERVICE_ATTRIBUTE_URL_ID 
			};

	    public final UUID L2CAP = new UUID(0x0100);

	    public final UUID RFCOMM = new UUID(0x0003);
	    
		private UUID searchUuidSet[];
		
		DiscoveryAgent discoveryAgent;
		
		int servicesSearchTransID;
		
		public BluetoothInquirer() {
			if (searchOnlyBluecoveUuid) {
				searchUuidSet = new UUID[] { L2CAP, RFCOMM, CommunicationTester.uuid };
			} else {
				searchUuidSet = new UUID[] { L2CAP };
			}
		}
		
	    public boolean startDeviceInquiry() {
			Logger.debug("Starting Device inquiry");
	    	devices = new Vector();
	    	serverURLs = new Vector();
	    	try {
	    		discoveryAgent = LocalDevice.getLocalDevice().getDiscoveryAgent();
	    		discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
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
					return startServicesInquiry();
				} else {
					return true;
				}
			} finally {
		        inquiring = false;
			}
	    }

	    public void shutdown() {
	    	if (inquiring && (discoveryAgent != null)) {
	    		try {
	    			discoveryAgent.cancelInquiry(this);
				} catch (Throwable e) {
				}
				try {
					if (servicesSearchTransID != 0) {
						discoveryAgent.cancelServiceSearch(servicesSearchTransID);
						servicesSearchTransID = 0;
					}
				} catch (Throwable e) {
				}
	    	}
	    }
	    
        public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass cod) {
        	if (onlyWhiteDevices && !isWhiteDevice(remoteDevice.getBluetoothAddress())) {
        		return;
        	}
        	if ((devicesComputers && (cod.getMajorDeviceClass() == Consts.DEVICE_COMPUTER)) 
        			|| (cod.getMajorDeviceClass() == Consts.DEVICE_PHONE)) {
	        	devices.addElement(remoteDevice);				
			} else {
				return;
			}
//        	String name = null;
//        	try {
//        		name = remoteDevice.getFriendlyName(false);
//			} catch (IOException e) {
//				Logger.debug("er.getFriendlyName," + remoteDevice.getBluetoothAddress(), e);
//			}
			Logger.debug("deviceDiscovered " + niceDeviceName(remoteDevice.getBluetoothAddress()));
        }

	    public boolean startServicesInquiry() {
	        Logger.debug("Starting Services inquiry");
	        for (Enumeration iter = devices.elements(); iter.hasMoreElements();) {
	        	if (stoped) {
	        		break;
	        	}
				RemoteDevice remoteDevice = (RemoteDevice) iter.nextElement();
	        	String name = "";
	        	try {
	        		name = remoteDevice.getFriendlyName(false);
				} catch (IOException e) {
					//Logger.debug("er.getFriendlyName," + remoteDevice.getBluetoothAddress(), e);
				}
				Logger.debug("Search Services on " + niceDeviceName(remoteDevice.getBluetoothAddress()) + " " + name);

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
					}
		        }	
				servicesSearchTransID = 0;
			}
	        Logger.debug("Inquiry completed");
	        return true;
	    }

        public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
			for (int i = 0; i < servRecord.length; i++) {
				boolean hadError = false;
				boolean isBlueCoveTestService = false;
				String url = servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
				Logger.info("*found server " + url);
				try {
					if (CommunicationTester.testServiceAttributes) {
						int[] attributeIDs = servRecord[i].getAttributeIDs();
						// Logger.debug("attributes " + attributeIDs.length);

						boolean foundName = false;
						boolean foundInt = false;
						boolean foundStr = false;
						boolean foundUrl = false;

						boolean foundIntOK = false;
						boolean foundUrlOK = false;
						
						for (int j = 0; j < attributeIDs.length; j++) {
							int id = attributeIDs[j];
							try {
								DataElement attrDataElement = servRecord[i].getAttributeValue(id);
								Assert.assertNotNull("attrValue null", attrDataElement);
								switch (id) {
								case 0x0100:
									foundName = true;
									if (CommunicationTester.testIgnoreNotWorkingServiceAttributes) {
										Assert.assertEquals("name", Consts.RESPONDER_SERVERNAME, attrDataElement.getValue());
										isBlueCoveTestService = true;
									}
									break;
								case Consts.TEST_SERVICE_ATTRIBUTE_INT_ID:
									foundInt = true;
									Assert.assertEquals("int", Consts.TEST_SERVICE_ATTRIBUTE_INT_VALUE, attrDataElement.getLong());
									isBlueCoveTestService = true;
									foundIntOK = true;
									break;
								case Consts.TEST_SERVICE_ATTRIBUTE_STR_ID:
									foundStr = true;
									if (CommunicationTester.testIgnoreNotWorkingServiceAttributes) {
										Assert.assertEquals("str", Consts.TEST_SERVICE_ATTRIBUTE_STR_VALUE, attrDataElement.getValue());
										isBlueCoveTestService = true;
									}
									break;
								case Consts.TEST_SERVICE_ATTRIBUTE_URL_ID:
									foundUrl = true;
									Assert.assertEquals("url", Consts.TEST_SERVICE_ATTRIBUTE_URL_VALUE, attrDataElement.getValue());
									isBlueCoveTestService = true;
									foundUrlOK = true;
									break;
								default:
									 Logger.debug("attribute " + id + " " 
											 + BluetoothTypes.getDataElementType(attrDataElement.getDataType()));
								}

							} catch (AssertionFailedError e) {
								Logger.warn("attr " + id + " " + e.getMessage());
								//countFailure++;
								hadError = true;
							}
						}
						if ((!CommunicationTester.testIgnoreNotWorkingServiceAttributes) && (!foundName)) {
							Logger.warn("srv name attr. not found");
							countFailure++;
						}
						if (!foundInt) {
							Logger.warn("srv INT attr. not found");
							countFailure++;
						}
						if ((CommunicationTester.testIgnoreNotWorkingServiceAttributes) && (!foundStr)) {
							Logger.warn("srv STR attr. not found");
							countFailure++;
						}
						if (!foundUrl) {
							Logger.warn("srv URL attr. not found");
							countFailure++;
						}
						if (foundName && foundUrl && foundInt && foundStr && !hadError) {
							Logger.info("service Attr OK");
							countSuccess++;
						}
						if (foundIntOK && foundUrlOK) {
							Logger.info("Common Service Attr OK");
							discoveryCount++;
							Logger.info("Found " + niceDeviceName(servRecord[i].getHostDevice().getBluetoothAddress()));
						}
					}
				} catch (Throwable e) {
					Logger.error("attrs", e);
				}
				
				if (searchOnlyBluecoveUuid || isBlueCoveTestService) {
					serverURLs.addElement(url);
				} else {
					Logger.info("is not TestService on " + niceDeviceName(servRecord[i].getHostDevice().getBluetoothAddress()));
				}
				if (isBlueCoveTestService) {
					RemoteDeviceInfo.deviceFound(servRecord[i].getHostDevice());
					break;
				}
			}
		}

        public synchronized void serviceSearchCompleted(int transID, int respCode) {
        	notifyAll();
        }

        public synchronized void inquiryCompleted(int discType) {
        	notifyAll();
        }
	    
	}
	
	public TestResponderClient() throws BluetoothStateException {
		
		LocalDevice localDevice = LocalDevice.getLocalDevice();
		Logger.info("address:" + localDevice.getBluetoothAddress());
		Logger.info("name:" + localDevice.getFriendlyName());
 	    
		Assert.assertNotNull("BT Address", localDevice.getBluetoothAddress());
		Assert.assertNotNull("BT Name", localDevice.getFriendlyName());
		
	}
	
	 public static boolean isWhiteDevice(String bluetoothAddress) {
		String addr = bluetoothAddress.toUpperCase();
		return (whiteDeviceNames.get(addr) != null);
	}

	 public static String niceDeviceName(String bluetoothAddress) {
		 String w = getWhiteDeviceName(bluetoothAddress);
		 return (w != null)?w:bluetoothAddress;
	}
	 
	public static String getWhiteDeviceName(String bluetoothAddress) {
		if ((bluetoothAddress == null) || (whiteDeviceNames == null)) {
			return null;
		}
		String addr = bluetoothAddress.toUpperCase();
		return (String) whiteDeviceNames.get(addr);
	}
	
	public void connectAndTest(String serverURL) {
		for(int testType = Consts.TEST_START; testType <= Consts.TEST_LAST; testType ++) {
			StreamConnection conn = null;
			InputStream is = null;
			OutputStream os = null;
			try {
				Logger.debug("test connect:" + testType);
				int connectionOpenTry = 0;
				while (conn == null) {
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
				os = conn.openOutputStream();
				Logger.debug("test run:" + testType);
				os.write(testType);
				os.flush();
				
				is = conn.openInputStream();

				CommunicationTester.runTest(testType, false, is, os);
				os.flush();
				Logger.debug("read server status");
				int ok = is.read();
				Assert.assertEquals("Test reply ok", Consts.TEST_REPLY_OK, ok);
				int conformTestType = is.read();
				Assert.assertEquals("Test reply conform", testType, conformTestType);
				countSuccess ++;
				Logger.debug("test ok:" + testType);
			} catch (Throwable e) {
				countFailure ++;
				Logger.error("test " + testType, e);
			}
			IOUtils.closeQuietly(os);
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(conn);
			// Let the server restart
			try {
				Thread.sleep(Consts.clientReconnectSleep);
			} catch (InterruptedException e) {
				break;
			}
		} 
		if (!CommunicationTester.continuous) {
			stopServer(serverURL);
		}
	}
	
	public void stopServer(String serverURL) {
		StreamConnection conn = null;
		InputStream is = null;
		OutputStream os = null;
		try {
			Logger.debug("stopServer");
			conn = (StreamConnection) Connector.open(serverURL);
			os = conn.openOutputStream();
			
			os.write(Consts.TEST_TERMINATE);
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
		Logger.debug("Client started...");
		isRunning = true;
		try {
			bluetoothInquirer = new BluetoothInquirer();

			while (!stoped) {
				if (!bluetoothInquirer.startDeviceInquiry()) {
					break;
				}
				while (bluetoothInquirer.inquiring) {
					try {
						Thread.sleep(1000);
					} catch (Exception e) {
					}
				}
				if ((CommunicationTester.testConnections) && (bluetoothInquirer.serverURLs != null)) {
					for (Enumeration iter = bluetoothInquirer.serverURLs.elements(); iter.hasMoreElements();) {
						String url = (String) iter.nextElement();
						connectAndTest(url);
					}
				}
				Logger.info("*Success:" + countSuccess + " Failure:" + countFailure);
				if ((countSuccess + countFailure > 0) && (!CommunicationTester.continuous)) {
					break;
				}
				Switcher.yield(this);
			}
		} finally {
			isRunning = false;
			Logger.info("Client finished!");
			Switcher.yield(this);
		}
	}
	
	public void shutdown() {
		Logger.info("shutdownClient");
		stoped = true;
		bluetoothInquirer.shutdown();
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
