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

/**
 * TODO Create an editable Form for this Configuarion settings.
 * 
 * @author vlads
 *
 */
public class Configuration {

	public static boolean discoverDevicesComputers = true;
	
	public static boolean discoverDevicesPhones = true;

	public static boolean searchOnlyBluecoveUuid = true;

	
	/**
	 * Limit connections to precompiled list of test devices.
	 */
	public static boolean discoverOnlyTestDevices = true;
	
	/**
	 * This may hung forever on some Nokia devices.
	 */
	public static boolean discoveryGetDeviceFriendlyName = false;
	
	public static Hashtable testDeviceNames = null;

	public static boolean serverAcceptWhileConnected = false;
	
	public static boolean serverAcceptWhileConnectedOnJavaSE = true;

	public static boolean continuous = true;

	public static boolean continuousDiscovery = true;
	
	public static boolean testConnections = true;
	
	// This test concurrrent connections if you have Multiple servers running.
	public static boolean testConnectionsMultipleThreads = true;
	
	public static boolean testServiceAttributes = true;

	
	public static final int TEST_START = 1;
	
	public static final int TEST_LAST = 17;
	
	public static final int STERSS_TEST_CASE = Consts.TEST_BYTE;
	
	/**
	 * Apperantly Motorola Service Attribute STRING is not working.
	 * INT_4 not working on some Nokia and breakes its discovery by Motorola.
	 * INT_16 are truncated in discovery by WIDCOMM
	 * Service attributes are not supported on BlueSoleil
	 */
	public static boolean testIgnoreNotWorkingServiceAttributes = true;

	public static boolean testServerForceDiscoverable = true;

	public static int serverMAXTimeSec = 80;
	
	public static Storage storage;
	/**
	 * Apperantly on Motorola iDEN serverConnection.acceptAndOpen() never returns.
	 */
	public static boolean canCloseServer = true;
	
	public static boolean isBlueCove = false;
	
	public static boolean windowsXP = false;
	
	public static boolean windowsCE = false;
	
	public static boolean linux = false;
	
	public static boolean macOSx = false;
	
	public static boolean stackWIDCOMM = false;
	
    static {
		testDeviceNames = new Hashtable();
		// This is the list of my test devices with names for My convenience
		testDeviceNames.put("00E003506231", "Nokia D1");
		testDeviceNames.put("00E0035046C1", "Nokia D2");
		testDeviceNames.put("0015A8DDF300", "Moto M1 v360");
		testDeviceNames.put("0050F2E8D4A6", "Desk MS");
		testDeviceNames.put("000D3AA5E36C", "Lapt MS");
		testDeviceNames.put("0020E027CE32", "Lapt WC");
		
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
        
        testDeviceNames.put("000B0D1796FC", "GPS");
        testDeviceNames.put("000D3AA4F7F9", "My Keyboard");
        testDeviceNames.put("0050F2E7EDC8", "My Mouse 1");
        testDeviceNames.put("0020E03AC5B2", "bob1");
        testDeviceNames.put("000D88C03ACA", "bob2");
        
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
}
