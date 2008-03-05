package com.intel.bluetooth.emu;

import java.io.Serializable;

public class DeviceDescriptor implements Serializable {

	private static final long serialVersionUID = 1L;

	private long address;
	
	private String name;
	
	private int clazz;
	
	public DeviceDescriptor(long address, String name, int clazz) {
		super();
		this.address = address;
		this.name = name;
		this.clazz = clazz;
	}

	public long getAddress() {
		return address;
	}
	
	public String getName() {
		return name;
	}

	public int getDeviceClass() {
		return clazz;
	}

	public String toString() {
		return "[address="+address+"; name="+name+"; clazz="+clazz+"]";
	}
	
}
