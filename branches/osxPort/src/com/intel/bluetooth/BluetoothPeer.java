/*
 Copyright 2004 Intel Corporation

 This file is part of Blue Cove.

 Blue Cove is free software; you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation; either version 2.1 of the License, or
 (at your option) any later version.

 Blue Cove is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with Blue Cove; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.intel.bluetooth;

import java.io.IOException;
import java.util.Properties;

import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

public class BluetoothPeer {
	
	static boolean		nativeIsAsync;
	static {
		nativeIsAsync = false;		
	}
	public BluetoothPeer() {
		/* moved this out of static since it can cause a deadlock condition if
		 * the native library tries to access the nativeIsAsync variable when
		 * loaded.
		 */
		NativeLibLoader.isAvailable();
	}
	class InquiryThread extends Thread {
		private int accessCode;

		private DiscoveryListener listener;

		public InquiryThread(int accessCode, DiscoveryListener listener) {
			this.accessCode = accessCode;
			this.listener = listener;
		}

		public void run() {
			listener.inquiryCompleted(doInquiry(accessCode, listener));
		}
	}

	class SearchServicesThread extends Thread {
		private int[] attrSet;

		private UUID[] uuidSet;

		private RemoteDevice device;

		private DiscoveryListener listener;

		public SearchServicesThread(int[] attrSet, UUID[] uuidSet,
				RemoteDevice device, DiscoveryListener listener) {
			this.attrSet = attrSet;
			this.uuidSet = uuidSet;
			this.device = device;
			this.listener = listener;
		}

		public void run() {
			int[] handles = getServiceHandles(uuidSet, Long.parseLong(device
					.getBluetoothAddress(), 16));

			if (handles == null)
				listener.serviceSearchCompleted(0,
						DiscoveryListener.SERVICE_SEARCH_ERROR);
			else if (handles.length > 0) {
				ServiceRecord[] records = new ServiceRecordImpl[handles.length];

				for (int i = 0; i < handles.length; i++) {
					records[i] = new ServiceRecordImpl(device, handles[i]);

					try {
						records[i].populateRecord(new int[] { 0x0000, 0x0001,
								0x0002, 0x0003, 0x0004 });

						if (attrSet != null)
							records[i].populateRecord(attrSet);
					} catch (Exception e) {
					}
				}

				listener.servicesDiscovered(0, records);
				listener.serviceSearchCompleted(0,
						DiscoveryListener.SERVICE_SEARCH_COMPLETED);
			} else
				listener.serviceSearchCompleted(0,
						DiscoveryListener.SERVICE_SEARCH_NO_RECORDS);
		}
	}

	public Boolean startInquiry(int accessCode, DiscoveryListener listener) {
		if(nativeIsAsync) return (0 != doInquiry(accessCode, listener));
		else {
			(new InquiryThread(accessCode, listener)).start();
			return true;
		}
		
	}

	public int startSearchServices(int[] attrSet, UUID[] uuidSet,
			RemoteDevice device, DiscoveryListener listener) {
		if(nativeIsAsync) return asyncSearchServices(attrSet, uuidSet, 
				device, listener);
		else {
			(new SearchServicesThread(attrSet, uuidSet, device, listener)).start();
			return 0;
		}
	}
	/**
	 * Request the library to stop an async Search. If the library only supports
	 * synchronous searches the method fails and returns false
	 * @param transID
	 * @return true if the service search transaction is terminated, else false if the transID does not represent an active asynchronous service search transaction
	 */
	
	public boolean cancelServiceSearch(int transID) {
		if(nativeIsAsync) return asyncStopSearchServices(transID);
		else return false;
	}

	/*
	 * perform synchronous inquiry
	 */

	public native int doInquiry(int accessCode, DiscoveryListener listener);

	/*
	 * cancel current inquiry (if any)
	 */

	public native boolean cancelInquiry(DiscoveryListener listener);

	/**
	 * Starts an asynchronous search of services for a device. Returns immediately.
	 * @param attrSet
	 * @param uuidSet
	 * @param device
	 * @param listener
	 * @return A reference to the initiated search or zero if async isn't enabled
	 * 			in the native library.
	 */
	public native int asyncSearchServices(int[] attrSet, UUID[] uuidSet,
			RemoteDevice device, DiscoveryListener listener);
	
	public native boolean asyncStopSearchServices(int transID);
	/*
	 * perform synchronous service discovery
	 */

	public native int[] getServiceHandles(UUID[] uuidSet, long address);

	/*
	 * get service attributes,
	 * NOT implemented on OS X, but shouldn't ever be called, will return null
	 */

	public native byte[] getServiceAttributes(int[] attrIDs, long address,
			int handle) throws IOException;

	public native int registerService(byte[] record) throws IOException;

	/*
	 * unregister service
	 */

	public native void unregisterService(int handle) throws IOException;

	/*
	 * socket operations
	 */

	public native int socket(boolean authenticate, boolean encrypt)
			throws IOException;

	public native long getsockaddress(int socket) throws IOException;

	public native int getsockchannel(int socket) throws IOException;

	public native void connect(int socket, long address, int channel)
			throws IOException;

	public native void listen(int socket) throws IOException;

	public native int accept(int socket) throws IOException;

	public native int recv(int socket) throws IOException;

	public native int recv(int socket, byte[] b, int off, int len)
			throws IOException;

	public native void send(int socket, int b) throws IOException;

	public native void send(int socket, byte[] b, int off, int len)
			throws IOException;

	public native void close(int socket) throws IOException;

	public native String getpeername(long address) throws IOException;

	public native long getpeeraddress(int socket) throws IOException;

	public native String getradioname(long address);
	
	public native Properties getAdjustedSystemProperties();
	
}