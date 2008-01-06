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

import java.awt.Choice;
import java.util.Enumeration;

import javax.bluetooth.DataElement;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

import net.sf.bluecove.RemoteDeviceInfo;
import net.sf.bluecove.TestResponderCommon;
import net.sf.bluecove.util.BluetoothTypesInfo.UUIDConsts;

/**
 * @author vlads
 * 
 */
public class ServiceRecords {

	public static void populateChoice(Choice choice) {
		for (Enumeration en = RemoteDeviceInfo.services.keys(); en.hasMoreElements();) {
			String url = (String) en.nextElement();
			int k = url.indexOf(';');
			if (k == -1) {
				continue;
			}
			String info = url.substring(0, k + 1);
			ServiceRecord serviceRecord = (ServiceRecord) RemoteDeviceInfo.services.get(url);
			while (info.length() < 28) {
				info += " ";
			}
			info += " " + TestResponderCommon.niceDeviceName(serviceRecord.getHostDevice().getBluetoothAddress());
			info += " " + UUIDName(serviceRecord);
			choice.add(info);
		}
	}

	public static String getChoiceURL(Choice choice) {
		String info = choice.getSelectedItem();
		int k = info.indexOf(';');
		if (k != -1) {
			return info.substring(0, k);
		}
		return null;
	}

	public static String UUIDName(ServiceRecord serviceRecord) {
		DataElement d = serviceRecord.getAttributeValue(1);
		if ((d == null) || (d.getDataType() != DataElement.DATSEQ)) {
			return "n/a";
		}
		DataElement uuid = null;
		Enumeration en = (Enumeration) (d.getValue());
		while (en.hasMoreElements()) {
			uuid = (DataElement) en.nextElement();
		}
		if (uuid == null) {
			return "n/a";
		}
		return UUIDConsts.getName((UUID) uuid.getValue());
	}
}
