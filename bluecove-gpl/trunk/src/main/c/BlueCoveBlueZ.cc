#include "BluetoothStackBlueZ.h"
#include "DiscoveryListener.h"
#include "BlueCoveBlueZ.h"
#include <bluetooth/bluetooth.h>
#include <bluetooth/hci.h>
#include <bluetooth/hci_lib.h>

int deviceClassBytesToInt(uint8_t* deviceClass)
{
	return ((deviceClass[2] & 0xff)<<16)|((deviceClass[1] & 0xff)<<8)|(deviceClass[0] & 0xff);
}
