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

	boolean isRunning = false;
	
	public static boolean searchOnlyBluecoveUuid = true;
	/**
	 * Limit connections to precompiled list of test devices.
	 */
	private static boolean onlyWhiteDevices = false;
	
	private static Hashtable whiteDeviceNames = null;
	
    static {
		whiteDeviceNames = new Hashtable();
		whiteDeviceNames.put("00E003506231", "Nokia D1");
		whiteDeviceNames.put("00E0035046C1", "Nokia D2");
		whiteDeviceNames.put("0015A8DDF300", "Moto M1");
		whiteDeviceNames.put("0050F2E8D4A6", "Desk MS");
		whiteDeviceNames.put("000D3AA5E36C", "Lapt MS");
		whiteDeviceNames.put("0020E027CE32", "Lapt HP");
	}
    
	public static class BluetoothInquirer implements DiscoveryListener {
	    
		boolean inquiring;

		Vector devices;
		
		Vector serverURLs;

	    public static int[] attrIDs = new int[] { 
	    	0x0100,
	    	0x0101,
	    	Consts.TEST_SERVICE_ATTRIBUTE_INT_ID, 
	    	Consts.TEST_SERVICE_ATTRIBUTE_STR_ID,
	    	Consts.TEST_SERVICE_ATTRIBUTE_URL_ID 
			};

	    public static final UUID L2CAP = new UUID(0x0100);

	    public static final UUID RFCOMM = new UUID(0x0003);
	    
		private UUID searchUuidSet[];
		
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
	            LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, this);
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
				return startServicesInquiry();
			} finally {
		        inquiring = false;
			}
	    }

        public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass cod) {
        	if (onlyWhiteDevices && !isWhiteDevice(remoteDevice.getBluetoothAddress())) {
        		return;
        	}
        	if ((cod.getMajorDeviceClass() == Consts.DEVICE_COMPUTER) || (cod.getMajorDeviceClass() == Consts.DEVICE_PHONE)) {
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
			    		DiscoveryAgent discoveryAgent = LocalDevice.getLocalDevice().getDiscoveryAgent();
			    		discoveryAgent.searchServices(attrIDs, searchUuidSet, remoteDevice, this);
			        } catch(BluetoothStateException e) {
			        	Logger.error("Cannot start searchServices", e);
				    }
			        try {
						wait();
					} catch (InterruptedException e) {
						break;
					}
		        }				
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

						for (int j = 0; j < attributeIDs.length; j++) {
							int id = attributeIDs[j];
							try {
								DataElement attrDataElement = servRecord[i].getAttributeValue(id);
								Assert.assertNotNull("attrValue null", attrDataElement);
								switch (id) {
								case 0x0100:
									foundName = true;
									Assert.assertEquals("name", Consts.RESPONDER_SERVERNAME, attrDataElement.getValue());
									isBlueCoveTestService = true;
									break;
								case Consts.TEST_SERVICE_ATTRIBUTE_INT_ID:
									foundInt = true;
									Assert.assertEquals("int", Consts.TEST_SERVICE_ATTRIBUTE_INT_VALUE, attrDataElement.getLong());
									isBlueCoveTestService = true;
									break;
								case Consts.TEST_SERVICE_ATTRIBUTE_STR_ID:
									foundStr = true;
									Assert.assertEquals("str", Consts.TEST_SERVICE_ATTRIBUTE_STR_VALUE, attrDataElement.getValue());
									isBlueCoveTestService = true;
									break;
								case Consts.TEST_SERVICE_ATTRIBUTE_URL_ID:
									foundUrl = true;
									Assert.assertEquals("url", Consts.TEST_SERVICE_ATTRIBUTE_URL_VALUE, attrDataElement.getValue());
									isBlueCoveTestService = true;
									break;
								default:
									 Logger.debug("attribute " + id + " " 
											 + BluetoothTypes.getDataElementType(attrDataElement.getDataType()));
								}

							} catch (AssertionFailedError e) {
								Logger.warn("attr " + id + " " + e.getMessage());
								countFailure++;
								hadError = true;
							}
						}
						if (!foundName) {
							Logger.warn("srv name attr. not found");
							countFailure++;
						}
						if (!foundInt) {
							Logger.warn("srv INT attr. not found");
							countFailure++;
						}
						if (!foundStr) {
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
					}
				} catch (Throwable e) {
					Logger.error("attrs", e);
				}
				
				if (searchOnlyBluecoveUuid || isBlueCoveTestService) {
					serverURLs.addElement(url);
				} else {
					Logger.info("is not TestService on " + niceDeviceName(servRecord[i].getHostDevice().getBluetoothAddress()));
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
			BluetoothInquirer bi = new BluetoothInquirer();

			while (true) {
				if (!bi.startDeviceInquiry()) {
					break;
				}
				while (bi.inquiring) {
					try {
						Thread.sleep(1000);
					} catch (Exception e) {
					}
				}
				if (bi.serverURLs != null) {
					for (Enumeration iter = bi.serverURLs.elements(); iter.hasMoreElements();) {
						String url = (String) iter.nextElement();
						connectAndTest(url);
					}
				}
				Logger.info("*Success:" + countSuccess + " Failure:" + countFailure);
				if ((countSuccess + countFailure > 0) && (!CommunicationTester.continuous)) {
					break;
				}
			}
		} finally {
			isRunning = false;
			Logger.info("Client finished");
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
