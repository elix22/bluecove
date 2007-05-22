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

import java.util.Enumeration;

import javax.bluetooth.DataElement;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

/**
 * @author vlads
 *
 */
public class ServiceRecordTester {

	public static final int ServiceClassIDList = 0x0001;
	
	public static boolean hasServiceClassUUID(ServiceRecord servRecord, UUID uuid) {
		DataElement attrDataElement = servRecord.getAttributeValue(ServiceClassIDList);
		if ((attrDataElement == null) || (attrDataElement.getDataType() != DataElement.DATSEQ) || attrDataElement.getSize() == 0) {
			return false;
		}
		
		Object value = attrDataElement.getValue();
		if ((value == null) || (value instanceof Enumeration)) {
			return false;
		}
		//Logger.debug("DATSEQ class " + value.getClass().getName());
		for (Enumeration e = (Enumeration)value; e.hasMoreElements();) {
			Object element = e.nextElement();
			if (!(element instanceof DataElement)) {
				Logger.warn("Bogus element in DATSEQ, " + value.getClass().getName());
				continue;
			}
			DataElement dataElement = (DataElement) element;
			if ((dataElement.getDataType() == DataElement.UUID) && (CommunicationTester.uuid.equals(dataElement.getValue()))) {
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean testServiceAttributes(ServiceRecord servRecord, String servicesOnDeviceName) {
		
		boolean isBlueCoveTestService = false;
		
		boolean hadError = false;
		
		long variableData = 0;
		
		if (!Configuration.testServiceAttributes || ("0".equals(LocalDevice.getProperty("bluetooth.sd.attr.retrievable.max")))) {
			return hasServiceClassUUID(servRecord, CommunicationTester.uuid);
		}
		try {
				int[] attributeIDs = servRecord.getAttributeIDs();
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
						DataElement attrDataElement = servRecord.getAttributeValue(id);
						Assert.assertNotNull("attrValue null", attrDataElement);
						switch (id) {
						case 0x0001:
							if (!hasServiceClassUUID(servRecord, CommunicationTester.uuid)) {
								TestResponderClient.failure.addFailure("ServiceClassUUID not found on " + servicesOnDeviceName);
							} else {
								isBlueCoveTestService = true;
							}
							break;
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
							String byteArrayTypeName = BluetoothTypes.getDataElementType(Consts.TEST_SERVICE_ATTRIBUTE_BYTES_TYPE);
							Assert.assertEquals("byte[] " + byteArrayTypeName + " type", Consts.TEST_SERVICE_ATTRIBUTE_BYTES_TYPE, attrDataElement.getDataType());
							byte[] byteAray;
							try {
								byteAray = (byte[])attrDataElement.getValue();
							} catch (Throwable e) {
								Logger.warn("attr  " + byteArrayTypeName + " " + id + " " + e.getMessage());
								hadError = true;
								break;
							}
							Assert.assertEquals("byteAray.len of " + byteArrayTypeName, Consts.TEST_SERVICE_ATTRIBUTE_BYTES_VALUE.length, byteAray.length);
							for(int k = 0; k < byteAray.length; k++) {
								if (Configuration.testIgnoreNotWorkingServiceAttributes && Configuration.stackWIDCOMM && k >= 4) {
									// INT_16 are truncated in discovery
									break;
								}
								Assert.assertEquals("byte[" + k + "] of " + byteArrayTypeName ,  Consts.TEST_SERVICE_ATTRIBUTE_BYTES_VALUE[k], byteAray[k]);
							}
							isBlueCoveTestService = true;
							foundBytesOK = true;
							break;	
						case Consts.VARIABLE_SERVICE_ATTRIBUTE_BYTES_ID:
							Assert.assertEquals("var U_INT_4 type", DataElement.U_INT_4, attrDataElement.getDataType());
							try {
								variableData = attrDataElement.getLong();
								//Logger.debug("Var info:" + variableData);
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
					TestResponderClient.failure.addFailure("srv name attr. not found on " + servicesOnDeviceName);
				}
				if (!foundInt) {
					Logger.warn("srv INT attr. not found");
					TestResponderClient.failure.addFailure("srv INT attr. not found on " + servicesOnDeviceName);
				}
				if ((!Configuration.testIgnoreNotWorkingServiceAttributes) && (!foundLong)) {
					Logger.warn("srv long attr. not found");
					TestResponderClient.failure.addFailure("srv long attr. not found on " + servicesOnDeviceName);
				}
				if ((!Configuration.testIgnoreNotWorkingServiceAttributes) && (!foundStr)) {
					Logger.warn("srv STR attr. not found");
					TestResponderClient.failure.addFailure("srv STR attr. not found on " + servicesOnDeviceName);
				}
				if (!foundUrl) {
					Logger.warn("srv URL attr. not found");
					TestResponderClient.failure.addFailure("srv URL attr. not found on " + servicesOnDeviceName);
				}
				if (!foundBytes) {
					Logger.warn("srv byte[] attr. not found");
					TestResponderClient.failure.addFailure("srv byte[] attr. not found on " + servicesOnDeviceName);
				}
				if (variableData == 0) {
					Logger.warn("srv var data attr. not found");
					TestResponderClient.failure.addFailure("srv var data attr. not found on " + servicesOnDeviceName);
				}
				if (foundName && foundUrl && foundInt && foundStr && foundLong && foundBytes && !hadError) {
					Logger.info("all service Attr OK");
					TestResponderClient.countSuccess++;
				} else if ((Configuration.testIgnoreNotWorkingServiceAttributes) && foundUrl && foundInt && foundBytes && !hadError) {
					Logger.info("service Attr found");
					TestResponderClient.countSuccess++;
				}
				if (foundIntOK && foundUrlOK && foundBytesOK) {
					Logger.info("Common Service Attr OK");
				}
		} catch (Throwable e) {
			Logger.error("attrs", e);
		}
		
		if (isBlueCoveTestService) {
			RemoteDeviceInfo.deviceServiceFound(servRecord.getHostDevice(), variableData);
		}
		
		return isBlueCoveTestService;
	}
}
