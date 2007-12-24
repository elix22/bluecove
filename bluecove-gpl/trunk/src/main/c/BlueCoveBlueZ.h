// 
// File:   BlueCoveBlueZ.h
// Author: mina
//
// Created on December 24, 2007, 4:17 PM
//

#ifndef _BLUECOVEBLUEZ_H
#define	_BLUECOVEBLUEZ_H

#include "BluetoothStackBlueZ.h"
#include "DiscoveryListener.h"
#include "BlueCoveBlueZ.h"
#include <bluetooth/bluetooth.h>
#include <bluetooth/hci.h>
#include <bluetooth/hci_lib.h>

const int TIMEOUT=5000;
const int DEVICE_NAME_MAX_SIZE=248;

int deviceClassBytesToInt(uint8_t* deviceClass);

#endif	/* _BLUECOVEBLUEZ_H */

