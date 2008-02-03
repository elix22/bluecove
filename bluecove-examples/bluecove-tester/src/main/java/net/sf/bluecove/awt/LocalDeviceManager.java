/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
package net.sf.bluecove.awt;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;

import net.sf.bluecove.Logger;
import net.sf.bluecove.TestResponderServer;

/**
 * @author vlads
 * 
 */
public class LocalDeviceManager {

	public static void setNotDiscoverable() {
		TestResponderServer.setNotDiscoverable();
		getDiscoverable();
	}

	public static void setDiscoverableGIAC() {
		TestResponderServer.setDiscoverable(DiscoveryAgent.GIAC);
		getDiscoverable();
	}

	public static void setDiscoverableLIAC() {
		TestResponderServer.setDiscoverable(DiscoveryAgent.LIAC);
		getDiscoverable();
	}

	public static void getDiscoverable() {
		try {
			LocalDevice localDevice = LocalDevice.getLocalDevice();
			int mode = localDevice.getDiscoverable();
			String modeStr;
			if (DiscoveryAgent.GIAC == mode) {
				modeStr = "GIAC";
			} else if (DiscoveryAgent.LIAC == mode) {
				modeStr = "LIAC";
			} else if (DiscoveryAgent.NOT_DISCOVERABLE == mode) {
				modeStr = "NOT_DISCOVERABLE";
			} else {
				modeStr = "0x" + Integer.toHexString(mode);
			}
			Logger.debug("LocalDevice Discoverable " + modeStr);
		} catch (Throwable e) {
			Logger.error("getDiscoverable error", e);
		}
	}

}
