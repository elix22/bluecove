/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2008 Mina Shokry
 *  Copyright (C) 2007 Vlad Skarzhevskyy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @version $Id$
 */
#define CPP__FILE "BlueCoveBlueZ_RFCOMM.cc"

#include "BlueCoveBlueZ.h"
#include <bluetooth/rfcomm.h>

#include <unistd.h>
#include <errno.h>

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_connectionRfOpenClientConnectionImpl
  (JNIEnv* env, jobject, jint deviceDescriptor, jlong address, jint channel, jboolean authenticate, jboolean encrypt, jint timeout) {
    debug("RFCOMM connect, channel %d", channel);
    // allocate socket
    int handle = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);
    if (handle < 0) {
        throwIOException(env, "Failed to create socket. [%d] %s", errno, strerror(errno));
        return 0;
    }

    //TODO setsockopt
    //bind local address
    sockaddr_rc localAddr;
    localAddr.rc_family = AF_BLUETOOTH;
    localAddr.rc_channel = 0;
    // TODO use deviceDescriptor to get local address for selected device
    bacpy(&localAddr.rc_bdaddr, BDADDR_ANY);

    if (bind(handle, (sockaddr *)&localAddr, sizeof(localAddr)) < 0) {
		throwIOException(env, "Failed to  bind socket. [%d] %s", errno, strerror(errno));
		close(handle);
		return 0;
	}

    sockaddr_rc remoteAddr;
    remoteAddr.rc_family = AF_BLUETOOTH;
	longToDeviceAddr(address, &remoteAddr.rc_bdaddr);
	remoteAddr.rc_channel = channel;

    // connect to server
    if (connect(handle, (sockaddr*)&remoteAddr, sizeof(remoteAddr)) != 0) {
        throwIOException(env, "Failed to connect. [%d] %s", errno, strerror(errno));
        close(handle);
        return 0;
    }
    debug("RFCOMM connected, handle %li", handle);
    return handle;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_connectionRfCloseClientConnection
  (JNIEnv* env, jobject, jlong handle) {
    debug("RFCOMM disconnect, handle %li", handle);
    // Closing channel, further sends and receives will be disallowed.
    if (shutdown(handle, SHUT_RDWR) < 0) {
        debug("shutdown failed. [%d] %s", errno, strerror(errno));
    }
    if (close(handle) < 0) {
        throwIOException(env, "Failed to close socket. [%d] %s", errno, strerror(errno));
    }
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_connectionRfRead__J
  (JNIEnv* env, jobject, jlong handle) {
    unsigned char c;
    int rc = recv(handle, (char *)&c, 1, 0);
    if (rc < 0) {
        throwIOException(env, "Failed to read. [%d] %s", errno, strerror(errno));
        return 0;
    } else if (rc == 0) {
		debug("Connection closed");
		// See InputStream.read();
		return -1;
	}
    return (int)c;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_connectionRfRead__J_3BII
  (JNIEnv* env, jobject peer, jlong handle, jbyteArray b, jint off, jint len ) {
    jbyte *bytes = env->GetByteArrayElements(b, 0);
	int done = 0;
	while (done < len) {
		int count = recv(handle, (char *)(bytes + off + done), len - done, 0);
		if (count < 0) {
			throwIOException(env, "Failed to read. [%d] %s", errno, strerror(errno));
			done = 0;
			break;
		} else if (count == 0) {
			debug("Connection closed");
			if (done == 0) {
				// See InputStream.read();
				done = -1;
			}
			break;
		}
		done += count;
		if (isCurrentThreadInterrupted(env, peer)) {
			break;
		}
	}
	env->ReleaseByteArrayElements(b, bytes, 0);
	return done;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_connectionRfReadAvailable
  (JNIEnv* env, jobject, jlong handle) {
    return 0;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_connectionRfWrite__JI
  (JNIEnv* env, jobject, jlong handle, jint b) {
    char c = (char)b;
    if (send(handle, &c, 1, 0) != 1) {
        throwIOException(env, "Failed to write. [%d] %s", errno, strerror(errno));
    }
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_connectionRfWrite__J_3BII
  (JNIEnv* env, jobject peer, jlong handle, jbyteArray b, jint off, jint len) {

    jbyte *bytes = env->GetByteArrayElements(b, 0);
	int done = 0;
	while(done < len) {
		int count = send(handle, (char *)(bytes + off + done), len - done, 0);
		if (count < 0) {
			throwIOException(env, "Failed to write. [%d] %s", errno, strerror(errno));
			break;
		}
		if (isCurrentThreadInterrupted(env, peer)) {
			break;
		}
		done += count;
	}
	env->ReleaseByteArrayElements(b, bytes, 0);
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_connectionRfFlush
  (JNIEnv* env, jobject, jlong handle) {
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_getConnectionRfRemoteAddress
  (JNIEnv* env, jobject, jlong handle) {
    sockaddr_rc remoteAddr;
    socklen_t len = sizeof(remoteAddr);
    if (getpeername(handle, (sockaddr*)&remoteAddr, &len) < 0) {
        throwIOException(env, "Failed to get peer name. [%d] %s", errno, strerror(errno));
		return -1;
	}
    return deviceAddrToLong(&remoteAddr.rc_bdaddr);
}
