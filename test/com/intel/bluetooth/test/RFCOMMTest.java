/**
 *  BlueCove - Java library for Bluetooth
 *	Copyright (C) 2007 Eric Wagner
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
 *
 */
package com.intel.bluetooth.test;

import java.io.IOException;
import java.util.Properties;

import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.swing.SwingUtilities;

import com.intel.bluetooth.BluetoothRFCOMMConnection;

public class RFCOMMTest implements DiscoveryListener {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RFCOMMTest		worker = new RFCOMMTest();
		
		worker.doWork();
	}
	
	public RFCOMMTest(){
	}
	
	public void doWork() {
		
		try {
		Properties		sysProps;
		
		LocalDevice.getLocalDevice();
		sysProps = System.getProperties();
		System.out.print(sysProps);
		System.out.println("\n\n-------------------------------------------\n\n");
		System.out.println("Local Address: "+LocalDevice.getLocalDevice().getBluetoothAddress());
		System.out.println("Local Name: " +LocalDevice.getLocalDevice().getFriendlyName());
		

			LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC,
	                this);
		
		Thread.sleep(6000000);
		
		} catch (Exception exp) {
			exp.printStackTrace();
		}
	}
	public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod){
		System.out.println("deviceDiscovered:" + btDevice.toString());
		System.out.println("deviceDiscovered DeviceClass: " + cod.toString());
		/* search for RFCOMM */
		int[]			attrs = new int[]{
				0x0005, 0x0006, 7, 8, 9, 10, 11, 12, 13, 0x200, 0x300, 0x301, 0x302,
				0x303, 0x304, 0x305, 0x306, 0x307, 0x308, 0x30C, 0x30A, 0x309, 0x311,
				0x312, 0x313, 0x656e, 0x656f, 0x6570, 0x6672, 0x6673, 0x6674, 0x6573, 0x6574, 0x6575,
				0x7074, 0x7075, 0x7076};
		
		UUID[]			allKnown = new UUID[]{UUID.RFCOMM_PROTOCOL_UUID};
		try {
			
		
			LocalDevice.getLocalDevice().getDiscoveryAgent().searchServices(attrs,
				allKnown, btDevice, this);
		} catch (Exception exp) {
			exp.printStackTrace();
		}
		
	}

	
	public void servicesDiscovered(int transID, ServiceRecord[] servRecord){
		
		if(servRecord != null) {
			int				i, count;
			Connection 	aConn;
			count = servRecord.length;
			for(i=0;i<count;i++) {
//				System.out.println("\n\nService Discovered: \n\t" + servRecord[i].toString());
				String		connectionURL;
				connectionURL = servRecord[i].getConnectionURL(2, false);
				System.out.println(connectionURL);
				try {
						aConn = Connector.open(connectionURL);
						System.out.println("Connector returned from open");
						SerialTerminal 	aTerm = new SerialTerminal((BluetoothRFCOMMConnection)	aConn);
						System.out.println("Asking Swing to execute later thread");
						SwingUtilities.invokeLater(aTerm);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				
			}
		} else System.out.println("No services discovered");
		
	
	}

	
	public void serviceSearchCompleted(int transID, int respCode){
		System.out.println("serviceSearchCompleted");
		
	}
	public void inquiryCompleted(int discType){
		System.out.println("inquiryCompleted");
	
		
		
	}
	
}
