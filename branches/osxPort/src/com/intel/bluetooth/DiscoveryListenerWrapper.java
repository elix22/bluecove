package com.intel.bluetooth;

import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

/**
 * A wrapper for performing callbacks to the user code. Since we don't want
 * the user having the capability to perform a deadlock we give them their
 * own thread rather than sharing the stack's thread.
 * 
 * 
 */
public class DiscoveryListenerWrapper extends Thread implements DiscoveryListener{

	DiscoveryListener		myCallbackObj;
	RemoteDevice			btDevice;
	DeviceClass				cod;
	int						transID, respCode, discType;
	ServiceRecord[]			servRecord;
	int						whichCallback;
	
	public void run() {
		switch(whichCallback) {
			case 1: 
				myCallbackObj.deviceDiscovered(btDevice, cod);
				break;
			case 2:
				myCallbackObj.servicesDiscovered(transID, servRecord);
				break;
			case 3:
				myCallbackObj.serviceSearchCompleted(transID, respCode);
				break;
			case 4:
				myCallbackObj.inquiryCompleted(discType);
				break;
		}
	}
	protected DiscoveryListenerWrapper(DiscoveryListener aListener) {
		myCallbackObj = aListener;
		this.setName("Bluecove Callback Thread");
	}

	public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod){
		this.btDevice = btDevice;
		this.cod = cod;
		whichCallback = 1;
		start();
	}

	public void servicesDiscovered(int transID, ServiceRecord[] servRecord){
		this.transID = transID;
		this.servRecord = servRecord;
		whichCallback = 2;
		start();
	}

	public void serviceSearchCompleted(int transID, int respCode){
		this.transID = transID;
		this.respCode = respCode;
		whichCallback = 3;
		start();
	}
	public void inquiryCompleted(int discType){
		this.discType = discType;
		whichCallback = 4;
		start();
	}
	
}
