#!/bin/sh
#
#  BlueCove - Java library for Bluetooth
#  Copyright (C) 2007 Eric Wagner
#
#  This library is free software; you can redistribute it and/or
#  modify it under the terms of the GNU Lesser General Public
#  License as published by the Free Software Foundation; either
#  version 2.1 of the License, or (at your option) any later version.
#
#  This library is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
#  Lesser General Public License for more details.
#
#  You should have received a copy of the GNU Lesser General Public
#  License along with this library; if not, write to the Free Software
#  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
#
#

read CURRENT  <../currentVersion
CURRENT=`expr $CURRENT + 1`

echo "Increasing build version number to $CURRENT"

echo $CURRENT >../currentVersion
echo "/* DO NOT EDIT, this file is machine generated */" > ../Version.h
echo "#define BUILD_VERSION \"$CURRENT\"" >> ../Version.h

echo "Starting java header build"

javah -classpath ../../bluecove/bin -force -o ../JavaHeaders.h com.intel.bluetooth.BluetoothInputStream com.intel.bluetooth.BluetoothL2CAPConnection com.intel.bluetooth.BluetoothOBEXConnection com.intel.bluetooth.BluetoothRFCOMMConnection com.intel.bluetooth.DiscoveryAgentImpl com.intel.bluetooth.LocalDeviceImpl com.intel.bluetooth.RemoteDeviceImpl com.intel.bluetooth.ServiceRecordImpl
echo "Java headers complete"








