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

import java.util.Hashtable;

import javax.bluetooth.UUID;

import net.sf.bluecove.util.CLDCStub;
import net.sf.bluecove.util.Storage;

/**
 *
 * This define different cleint and server work parterns to identify problem in native code.
 *
 * TODO Create an editable Form for this Configuarion settings.
 *
 * @author vlads
 *
 */
public class Configuration {

	public static boolean useShortUUID = false;

	public static boolean deviceClassFilter = true;

	public static boolean discoverDevicesComputers = true;

	public static boolean discoverDevicesPhones = true;

	public static boolean searchOnlyBluecoveUuid = true;

	public static boolean discoverySearchOnlyBluecoveUuid = false;

	/**
	 * Limit connections to precompiled list of test devices.
	 */
	public static boolean discoverOnlyTestDevices = false;

	/**
	 * This may hung forever on some Nokia devices.
	 */
	public static boolean discoveryGetDeviceFriendlyName = false;

	public static UUID discoveryUUID = new UUID(0x0100); // L2CAP

	public static Hashtable testDeviceNames = null;

	public static boolean serverAcceptWhileConnected = false;

	public static boolean serverAcceptWhileConnectedOnJavaSE = true;

	public static boolean serverContinuous = true;

	public static boolean clientContinuous = true;

	public static boolean clientContinuousDiscovery = true;

	public static boolean clientContinuousDiscoveryDevices = true;

	public static boolean clientContinuousServicesSearch = true;

	public static boolean clientTestConnections = true;

	// This test concurrrent connections if you have Multiple servers running.
	public static boolean clientTestConnectionsMultipleThreads = true;

	public static int TEST_CASE_FIRST = 1;

	public static int TEST_CASE_LAST = Consts.TEST_LAST_WORKING;

	public static int STERSS_TEST_CASE = Consts.TEST_BYTE;

	public static boolean testServiceAttributes = true;

	public static boolean testAllServiceAttributes = false;

	/**
	 * Apperantly Motorola Service Attribute STRING is not working.
	 * INT_4 not working on some Nokia and breakes its discovery by Motorola.
	 * INT_16 are truncated in discovery by WIDCOMM
	 * Service attributes are not supported on BlueSoleil
	 */
	public static boolean testIgnoreNotWorkingServiceAttributes = false;

	public static boolean testServerForceDiscoverable = true;

	public static int clientSleepBetweenConnections = 4100;

	public static int serverSleepB4ClosingConnection = 1000;

	public static int clientTestTimeOutSec = 60;

	public static int serverTestTimeOutSec = 60;

	public static int serverMAXTimeSec = 80;

	public static int clientSleepOnConnectionRetry = 500;

	public static int clientSleepOnDeviceInquiryError = 10000;

	public static Storage storage;

	private static String lastServerURL = null;

	public static CLDCStub cldcStub;

	/**
	 * Apperantly on Motorola iDEN serverConnection.acceptAndOpen() never returns.
	 */
	public static boolean canCloseServer = true;

	public static boolean isJ2ME = false;

	public static boolean isBlueCove = false;

	public static boolean windowsXP = false;

	public static boolean windowsCE = false;

	public static boolean linux = false;

	public static boolean macOSx = false;

	public static boolean stackWIDCOMM = false;

	public static boolean CLDC_1_0 = false;

	public static boolean logTimeStamp = false;
	
	public static boolean screenSizeSmall = false;

	static {
		testDeviceNames = new Hashtable();

		boolean testOnlyOneDevice = false;
		if (testOnlyOneDevice) {
			discoverOnlyTestDevices = true;
			//testDeviceNames.put("000D3AA5E36C", "Lapt MS");
			//testDeviceNames.put("0020E027CE32", "Lapt WC");
			//testDeviceNames.put("0050F2E8D4A6", "Desk MS");
			//testDeviceNames.put("000B0D4AECDE", "Desk WC");
			testDeviceNames.put("0019639C4007", "SE K790(r)");
			//testDeviceNames.put("00123755AE71", "N 6265i (t)");
			//testDeviceNames.put("0015E96A02DE", "D-Link");
		} else {

			// This is the list of my test devices with names for My convenience
			testDeviceNames.put("00E003506231", "Nokia D1");
			testDeviceNames.put("00E0035046C1", "Nokia D2");
			testDeviceNames.put("0015A8DDF300", "Moto M1 v360");
			testDeviceNames.put("0050F2E8D4A6", "Desk MS");
			testDeviceNames.put("000D3AA5E36C", "Lapt MS");
			testDeviceNames.put("0020E027CE32", "Lapt WC");
			testDeviceNames.put("000B0D4AECDE", "Desk WC");
			testDeviceNames.put("0015E96A02DE", "D-Link");

			testDeviceNames.put("0017841C5A8F", "Moto L7");
			testDeviceNames.put("00123755AE71", "N 6265i (t)");
			testDeviceNames.put("0013706C93D3", "N 6682 (r)");
			testDeviceNames.put("0017005354DB", "M i870 (t)");
			testDeviceNames.put("001700F07CF2", "M i605 (t)");

			testDeviceNames.put("001813184E8B", "SE W810i (r-ml)");

			testDeviceNames.put("001ADBBFCA67", "Mi880(t-b)");
			testDeviceNames.put("0019639C4007", "SE K790(r)");
			testDeviceNames.put("001ADBBFCEED", "Mi880(t-m)");

			testDeviceNames.put("00149ABD52E7", "M V551 A");
			testDeviceNames.put("00149ABD538D", "M V551 N");
			testDeviceNames.put("0007E05387E5", "Palm");
			testDeviceNames.put("0010C65C08A3", "AximX30");
			testDeviceNames.put("00022B001234", "MPx220");

			if (deviceClassFilter) {
				testDeviceNames.put("000B0D1796FC", "GPS");
				testDeviceNames.put("000D3AA4F7F9", "My Keyboard");
				testDeviceNames.put("0050F2E7EDC8", "My Mouse 1");
			}
		}

		String sysName = System.getProperty("os.name");
		if (sysName != null) {
			sysName = sysName.toLowerCase();
			if (sysName.indexOf("windows") != -1) {
				if (sysName.indexOf("ce") != -1) {
					windowsCE = true;
				} else {
					windowsXP = true;
				}
			} else if (sysName.indexOf("mac os x") != -1) {
				macOSx = true;
			} else if (sysName.indexOf("linux") != -1) {
				linux = true;
			}
		}
	}

	public static UUID blueCoveUUID() {
		if (useShortUUID) {
			return Consts.uuidShort;
		} else {
			return Consts.uuidLong;
		}
	}

	public static String getLastServerURL() {
		if (lastServerURL == null) {
			lastServerURL = Configuration.storage.retriveData(Storage.configLastServiceURL);
		}
		return lastServerURL;
	}

	public static void setLastServerURL(String lastServerURL) {
		Configuration.lastServerURL = lastServerURL;
		if (Configuration.storage != null) {
			Configuration.storage.storeData(Storage.configLastServiceURL, lastServerURL);
		}
	}
}
