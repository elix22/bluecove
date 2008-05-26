/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
package net.sf.bluecove.se;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import net.sf.bluecove.util.Storage;

/**
 * @author vlads
 * 
 */
public class FileStorage implements Storage {

	private Properties properties;

	private long propertiesFileLoadedLastModified = 0;

	private File propertyFile;

	public FileStorage() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.bluecove.util.Storage#retriveData(java.lang.String)
	 */
	public String retriveData(String name) {
		return getProperties().getProperty(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.bluecove.util.Storage#storeData(java.lang.String,
	 *      java.lang.String)
	 */
	public void storeData(String name, String value) {
		Properties p = getProperties();
		if (name != null) {
			if (value == null) {
				if (p.remove(name) == null) {
					// Not updated
					return;
				}
			} else {
				if (value.equals(p.put(name, value))) {
					// Not updated
					return;
				}
			}
		}
		File f = getPropertyFile();
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(f);
			// we run on Java 1.1
			p.save(out, "");
		} catch (FileNotFoundException ignore) {
		}
		try {
			out.close();
		} catch (Throwable ignore) {
		}
		propertiesFileLoadedLastModified = f.lastModified();
	}

	private File getPropertyFile() {
		if (propertyFile != null) {
			return propertyFile;
		}
		String tmpDir = System.getProperty("java.io.tmpdir");
		// Position and history for different stacks and device IDs different
		// for testing convenience
		String id = "";
		try {
			String stack = System.getProperty("bluecove.stack");
			if (stack != null) {
				id = stack;
			}
		} catch (SecurityException ignore) {

		}
		try {
			String deviceID = System.getProperty("bluecove.deviceID");
			if (deviceID != null) {
				id += deviceID;
			}
		} catch (SecurityException ignore) {

		}
		propertyFile = new File(tmpDir, "bluecove-tester" + id + ".properties");
		return propertyFile;
	}

	private Properties getProperties() {
		File f = getPropertyFile();
		long lastModified = 0;

		if (f.exists()) {
			lastModified = f.lastModified();
		}

		if ((properties != null) && (propertiesFileLoadedLastModified == lastModified)) {
			return properties;
		}
		Properties p = new Properties();
		if (f.exists()) {
			FileInputStream in = null;
			try {
				in = new FileInputStream(f);
				p.load(in);
			} catch (IOException ignore) {
			} finally {
				try {
					in.close();
				} catch (Throwable ignore) {
				}
			}
		}
		propertiesFileLoadedLastModified = lastModified;
		properties = p;
		return properties;
	}

}
