/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Hakan Lager
 *  Copyright (C) 2007 Vlad Skarzhevskyy
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
package com.intel.bluetooth;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

class BluetoothStackBlueZ implements BluetoothStack
{
	private int deviceID;
	private int deviceDescriptor;
	private Map<String,String> propertiesMap;
	private DiscoveryListener discoveryListener;
	private Vector<ReportedDevice> discoveredDevices;
	private boolean inquiryCanceled=false;

	private static class ReportedDevice
	{
		RemoteDevice remoteDevice;
		DeviceClass deviceClass;
	}

	ReportedDevice createReportedDevice(String deviceName,long address,int deviceClassRecord)
	{
		ReportedDevice reportedDevice=new ReportedDevice();
		reportedDevice.remoteDevice=RemoteDeviceHelper.createRemoteDevice(this,address,deviceName,false);
		reportedDevice.deviceClass=new DeviceClass(deviceClassRecord);
		return reportedDevice;
	}

	//Used mainly in Unit Tests
	static
	{
		NativeLibLoader.isAvailable(BlueCoveImpl.NATIVE_LIB_BLUEZ);
	}

	BluetoothStackBlueZ()
	{
	}

	//---------------------- Library initialization ----------------------
	public String getStackID()
	{
		return BlueCoveImpl.STACK_BLUEZ;
	}

	public int getLibraryVersion()
	{
		return 2000200;
	}

	public int detectBluetoothStack()
	{
		return 1<<5;
	}

	private native int nativeGetDeviceID();

	private native int nativeOpenDevice(int deviceID);

	public void initialize()
	{
		deviceID=nativeGetDeviceID();
		deviceDescriptor=nativeOpenDevice(deviceID);
		propertiesMap=new TreeMap<String,String>();
		propertiesMap.put("bluetooth.api.version","1.1");
	}

	private native void nativeCloseDevice(int deviceDescriptor);

	public void destroy()
	{
		nativeCloseDevice(deviceDescriptor);
	}

	public void enableNativeDebug(Class nativeDebugCallback,boolean on)
	{
	// I need more details on what this method should exactly do.
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#isCurrentThreadInterruptedCallback()
	 */
	public boolean isCurrentThreadInterruptedCallback()
	{
		return Thread.interrupted();
	}

	//---------------------- LocalDevice ----------------------
	private native String nativeGetDeviceBluetoothAddress(int deviceDescriptor);

	public String getLocalDeviceBluetoothAddress() throws BluetoothStateException
	{
		String address=nativeGetDeviceBluetoothAddress(deviceDescriptor);
		StringBuffer addressStringBuffer=new StringBuffer(address);
		int index;
		while((index=addressStringBuffer.indexOf(":"))!=-1)
			addressStringBuffer.delete(index,index+1);
		return addressStringBuffer.toString();
	}

	private native int nativeGetDeviceClass(int deviceDescriptor);

	public DeviceClass getLocalDeviceClass()
	{
		int record=nativeGetDeviceClass(deviceDescriptor);
		return new DeviceClass(record);
	}

	private native String nativeGetDeviceName(int deviceDescriptor);

	public String getLocalDeviceName()
	{
		return nativeGetDeviceName(deviceDescriptor);
	}

	public boolean isLocalDevicePowerOn()
	{
		return (deviceDescriptor=nativeOpenDevice(deviceID))>=0;
	}

	public String getLocalDeviceProperty(String property)
	{
		return propertiesMap.get(property);
	}

	private native int nativeGetLocalDeviceDiscoverable(int deviceDescriptor);

	public int getLocalDeviceDiscoverable()
	{
		return nativeGetLocalDeviceDiscoverable(deviceDescriptor);
	}

	private native int nativeSetLocalDeviceDiscoverable(int deviceDescriptor,int mode);

	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException
	{
		int error=nativeSetLocalDeviceDiscoverable(deviceDescriptor,mode);
		if(error!=0)
			throw new BluetoothStateException("Unable to change discovery mode. It may be because you aren't root");
		return true;
	}

	private native String nativeGetRemoteDeviceName(int deviceDescriptor,String address) throws IOException;

	public String getRemoteDeviceFriendlyName(long address) throws IOException
	{
		String addressLong=Long.toHexString(address).toUpperCase();
		StringBuffer addressStringBuffer=new StringBuffer("000000000000".substring(addressLong.length())+addressLong);
		for(int i=2;i<addressStringBuffer.length()-1;i+=2)
			addressStringBuffer.insert(i++,":");
		return nativeGetRemoteDeviceName(deviceDescriptor,addressStringBuffer.toString());
	}


	//---------------------- Device Inquiry ----------------------
	public boolean startInquiry(int accessCode,DiscoveryListener listener) throws BluetoothStateException
	{
		if(discoveryListener!=null)
			throw new BluetoothStateException("Another inquiry already running");
		discoveryListener=listener;
		discoveredDevices=new Vector<ReportedDevice>();
		return DeviceInquiryThread.startInquiry(this,accessCode,listener);
	}

	private native int nativeRunDeviceInquiry(int deviceID,int deviceDescriptor,int len,int accessCode,Vector<ReportedDevice> discoveredDevices);

	public int runDeviceInquiry(DeviceInquiryThread startedNotify,int accessCode,DiscoveryListener listener)
		throws BluetoothStateException
	{
		try
		{
			startedNotify.deviceInquiryStartedCallback();
			int discType=nativeRunDeviceInquiry(deviceID,deviceDescriptor,8,accessCode,discoveredDevices);
			if(inquiryCanceled)
			{
				discType=DiscoveryListener.INQUIRY_TERMINATED;
				inquiryCanceled=false;
			}
			else if(discType==DiscoveryListener.INQUIRY_COMPLETED)
			{
				for(ReportedDevice device:discoveredDevices)
				{
					listener.deviceDiscovered(device.remoteDevice,device.deviceClass);
				}
			}
			return discType;
		}
		catch(Exception e)
		{
//			e.printStackTrace();
			return DiscoveryListener.INQUIRY_ERROR;
		}
		finally
		{
			discoveryListener=null;
			discoveredDevices=null;
		}
	}

	public void deviceDiscoveredCallback(DiscoveryListener listener,long deviceAddr,int deviceClass,String deviceName,boolean paired)
	{
		throw new UnsupportedOperationException("deviceDiscoveredCallback : Not Implemented...");
	}

	public boolean cancelInquiry(DiscoveryListener listener)
	{
		if(discoveryListener!=null&&discoveryListener==listener)
		{
			try
			{
				Runtime runtime=Runtime.getRuntime();
				runtime.exec("hcitool cmd 0x01 0x0002");
				return inquiryCanceled=true;
			}
			catch(IOException ex)
			{
				return false;
			}
		}
		return false;
	}

	//---------------------- Service search ----------------------
	private native long[] nativeSearchServices(UUID[] uuids,long remoteDeviceAddress) throws SearchServicesException;

	public int runSearchServices(SearchServicesThread startedNotify,int[] attrSet,UUID[] uuidSet,
								  RemoteDevice device,DiscoveryListener listener) throws BluetoothStateException
	{
		startedNotify.searchServicesStartedCallback();
		long[] handles;
		try
		{
			handles=nativeSearchServices(uuidSet,RemoteDeviceHelper.getAddress(device));
		}
		catch(SearchServicesDeviceNotReachableException e)
		{
			return DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE;
		}
		catch(SearchServicesTerminatedException e)
		{
			return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
		}
		catch(SearchServicesException e)
		{
			return DiscoveryListener.SERVICE_SEARCH_ERROR;
		}
		if(handles==null)
			return DiscoveryListener.SERVICE_SEARCH_ERROR;
		else if(handles.length>0)
		{
			ServiceRecord[] records=new ServiceRecordImpl[handles.length];
			boolean hasError=false;
			for(int i=0;i<handles.length;i++)
			{
				records[i]=new ServiceRecordImpl(this,device,handles[i]);
				try
				{
					records[i].populateRecord(new int[]{0x0000,0x0001,0x0002,0x0003,0x0004});
					if(attrSet!=null)
						// crash here
						records[i].populateRecord(attrSet);
				}
				catch(Exception e)
				{
					System.out.println("\t\texception");
					e.printStackTrace();
					DebugLog.debug("populateRecord error",e);
					hasError=true;
				}
				if(startedNotify.isTerminated())
					return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
			}
			listener.servicesDiscovered(startedNotify.getTransID(),records);
			if(hasError)
				return DiscoveryListener.SERVICE_SEARCH_ERROR;
			else
				return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
		}
		else
			return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
	}

	public int searchServices(int[] attrSet,UUID[] uuidSet,RemoteDevice device,DiscoveryListener listener)
		throws BluetoothStateException
	{
		return SearchServicesThread.startSearchServices(this,attrSet,uuidSet,device,listener);
	}

	public boolean cancelServiceSearch(int transID)
	{
		// I didn't find a way to stop service search yet.
		return false;
	}

	private native boolean nativePopulateServiceRecordAttributeValues(long remoteDeviceAddress,long handle,int[] attrIDs,ServiceRecordImpl serviceRecord);
	
	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord,int[] attrIDs)
		throws IOException
	{
		long remoteDeviceAddress=RemoteDeviceHelper.getAddress(serviceRecord.getHostDevice());
		return nativePopulateServiceRecordAttributeValues(remoteDeviceAddress,serviceRecord.getHandle(),attrIDs,serviceRecord);
	}

	//---------------------- Client RFCOMM connections ----------------------
	public long connectionRfOpenClientConnection(BluetoothConnectionParams params)
		throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public void connectionRfCloseClientConnection(long handle) throws IOException
	{
	// TODO Auto-generated method stub

	}

	public int getSecurityOpt(long handle,int expected) throws IOException
	{
		return expected;
	}

	//---------------------- Server RFCOMM connections ----------------------
	public long rfServerOpen(BluetoothConnectionNotifierParams params,ServiceRecordImpl serviceRecord) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public void rfServerClose(long handle,ServiceRecordImpl serviceRecord) throws IOException
	{
	// TODO Auto-generated method stub

	}

	public void rfServerUpdateServiceRecord(long handle,ServiceRecordImpl serviceRecord,boolean acceptAndOpen) throws ServiceRegistrationException
	{
	// TODO Auto-generated method stub

	}

	public long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public void connectionRfCloseServerConnection(long handle) throws IOException
	{
	// TODO Auto-generated method stub
	}

	//---------------------- Shared Client and Server RFCOMM connections ----------------------
	public void connectionRfFlush(long handle) throws IOException
	{
	// TODO Auto-generated method stub

	}

	public int connectionRfRead(long handle) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public int connectionRfRead(long handle,byte[] b,int off,int len) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public int connectionRfReadAvailable(long handle) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public void connectionRfWrite(long handle,int b) throws IOException
	{
	// TODO Auto-generated method stub

	}

	public void connectionRfWrite(long handle,byte[] b,int off,int len) throws IOException
	{
	// TODO Auto-generated method stub

	}

	public long getConnectionRfRemoteAddress(long handle) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	// ---------------------- Client and Server L2CAP connections ----------------------

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2OpenClientConnection(com.intel.bluetooth.BluetoothConnectionParams, int, int)
	 */
	public long l2OpenClientConnection(BluetoothConnectionParams params,int receiveMTU,int transmitMTU) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseClientConnection(long)
	 */
	public void l2CloseClientConnection(long handle) throws IOException
	{
	// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerOpen(com.intel.bluetooth.BluetoothConnectionNotifierParams, int, int, com.intel.bluetooth.ServiceRecordImpl)
	 */
	public long l2ServerOpen(BluetoothConnectionNotifierParams params,int receiveMTU,int transmitMTU,ServiceRecordImpl serviceRecord) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerUpdateServiceRecord(long, com.intel.bluetooth.ServiceRecordImpl, boolean)
	 */
	public void l2ServerUpdateServiceRecord(long handle,ServiceRecordImpl serviceRecord,boolean acceptAndOpen) throws ServiceRegistrationException
	{
	// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerAcceptAndOpenServerConnection(long)
	 */
	public long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseServerConnection(long)
	 */
	public void l2CloseServerConnection(long handle) throws IOException
	{
	// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerClose(long, com.intel.bluetooth.ServiceRecordImpl)
	 */
	public void l2ServerClose(long handle,ServiceRecordImpl serviceRecord) throws IOException
	{
	// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2Ready(long)
	 */
	public boolean l2Ready(long handle) throws IOException
	{
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2receive(long, byte[])
	 */
	public int l2Receive(long handle,byte[] inBuf) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2send(long, byte[])
	 */
	public void l2Send(long handle,byte[] data) throws IOException
	{
	// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2GetReceiveMTU(long)
	 */
	public int l2GetReceiveMTU(long handle) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2GetTransmitMTU(long)
	 */
	public int l2GetTransmitMTU(long handle) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2RemoteAddress(long)
	 */
	public long l2RemoteAddress(long handle) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}
}
