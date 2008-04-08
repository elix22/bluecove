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
package net.sf.bluecove.awt;

import net.sf.bluecove.Configuration;
import net.sf.bluecove.Logger;
import net.sf.bluecove.util.CLDCStub;

public class JavaSECommon implements CLDCStub {

	private static boolean initialized = false;

	public static void initOnce() {
		if (initialized) {
			return;
		}
		initialized = true;
		Configuration.logTimeStamp = true;
		Logger.addAppender(new LoggerJavaSEAppender());

		// System.getProperties().put("bluecove.debug", "true");
		// System.getProperties().put("bluecove.native.path",
		// "../../bluecove/target/obj");

		if (Configuration.serverAcceptWhileConnectedOnJavaSE) {
			Configuration.serverAcceptWhileConnected = true;
			// Configuration.testIgnoreNotWorkingServiceAttributes = false;
		}

		Configuration.cldcStub = new JavaSECommon();
	}

	public void interruptThread(Thread t) {
		if (t != null) {
			t.interrupt();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.bluecove.util.CLDCStub#createNamedThread(java.lang.Runnable,
	 *      java.lang.String)
	 */
	public Thread createNamedThread(Runnable target, String name) {
		return new Thread(target, name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.bluecove.util.CLDCStub#setThreadLocalBluetoothStack(java.lang.Object)
	 */
	public void setThreadLocalBluetoothStack(Object id) {
		LocalDeviceManager.setThreadLocalBluetoothStack(id);
	}
}
