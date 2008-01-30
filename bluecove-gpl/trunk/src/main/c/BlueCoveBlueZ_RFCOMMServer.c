/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2008 Mina Shokry
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
#define CPP__FILE "BlueCoveBlueZ_RFCOMMServer.c"

#include "BlueCoveBlueZ.h"

#include <sys/socket.h>
#include <sys/unistd.h>
#include <bluetooth/rfcomm.h>

int dynamic_bind_rc(int sock, struct sockaddr_rc *sockaddr, uint8_t *port) {
	int err;
	for(*port=1;*port<=31;*port++) {
		sockaddr->rc_channel=*port;
		err=bind(sock,(struct sockaddr *)sockaddr,sizeof(sockaddr));
		if(!err)
			break;
	}
	if(*port==31) {
		err=-1;
	}
	return err;
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_rfServerOpenImpl
  (JNIEnv* env, jobject peer, jlong localDeviceBTAddress, jboolean authorize, jboolean authenticate, jboolean encrypt, jboolean master, jboolean timeouts, jint backlog) {
    // allocate socket
    int handle = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);
    if (handle < 0) {
        throwIOException(env, "Failed to create socket. [%d] %s", errno, strerror(errno));
        return 0;
    }

    struct sockaddr_rc localAddr;
    //bind local address
    localAddr.rc_family = AF_BLUETOOTH;
    // TODO for kernel versions 2.6.6 and before use dynamic_bind_rc
    localAddr.rc_channel = 0;
    //bacpy(&localAddr.rc_bdaddr, BDADDR_ANY);
    longToDeviceAddr(localDeviceBTAddress, &localAddr.rc_bdaddr);

    if (bind(handle, (struct sockaddr *)&localAddr, sizeof(localAddr)) < 0) {
		throwIOException(env, "Failed to  bind socket. [%d] %s", errno, strerror(errno));
		close(handle);
		return 0;
	}

    // TODO verify how this works, I think device needs to paird before this can be setup.
    // Set link security options
    if (encrypt || authenticate || authorize || master) {
		int socket_opt = 0;
		socklen_t len = sizeof(socket_opt);
        if (getsockopt(handle, SOL_RFCOMM, RFCOMM_LM, &socket_opt, &len) < 0) {
            throwIOException(env, "Failed to read RFCOMM server mode. [%d] %s", errno, strerror(errno));
            close(handle);
            return 0;
        }
		if (master) {
			socket_opt |= RFCOMM_LM_MASTER;
		}
		if (authenticate) {
			socket_opt |= RFCOMM_LM_AUTH;
			debug("RFCOMM set authenticate");
		}
		if (encrypt) {
			socket_opt |= RFCOMM_LM_ENCRYPT;
		}
		if (authorize) {
			socket_opt |= RFCOMM_LM_SECURE;
		}

		if ((socket_opt != 0) && setsockopt(handle, SOL_RFCOMM, RFCOMM_LM, &socket_opt, sizeof(socket_opt)) < 0) {
			throwIOException(env, "Failed to set RFCOMM server mode. [%d] %s", errno, strerror(errno));
            close(handle);
            return 0;
		}
    }

    // put socket into listening mode
    if (listen(handle, backlog) < 0) {
        throwIOException(env, "Failed to listen for RFCOMM connections. [%d] %s", errno, strerror(errno));
        close(handle);
        return 0;
    }

    return handle;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_rfServerGetChannelIDImpl
  (JNIEnv* env, jobject peer, jlong handle) {
    struct sockaddr_rc localAddr;
    socklen_t len = sizeof(localAddr);
    if (getsockname(handle, (struct sockaddr*)&localAddr, &len) < 0) {
        throwIOException(env, "Failed to get rc_channel. [%d] %s", errno, strerror(errno));
		return -1;
	}
	return localAddr.rc_channel;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_rfServerCloseImpl
  (JNIEnv* env, jobject peer, jlong handle, jboolean quietly) {
    debug("RFCOMM close server handle %li", handle);
    // Closing channel, further sends and receives will be disallowed.
    if (shutdown(handle, SHUT_RDWR) < 0) {
        debug("server shutdown failed. [%d] %s", errno, strerror(errno));
    }
    if (close(handle) < 0) {
        if (quietly) {
            debug("Failed to close server socket. [%d] %s", errno, strerror(errno));
        } else {
            throwIOException(env, "Failed to close server socket. [%d] %s", errno, strerror(errno));
        }
    }
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_rfServerAcceptAndOpenRfServerConnection
  (JNIEnv* env, jobject peer, jlong handle) {
    struct sockaddr_rc remoteAddr;
	socklen_t  remoteAddrLen = sizeof(remoteAddr);
	int client_socket = accept(handle, (struct sockaddr*)&remoteAddr, &remoteAddrLen);
	if (client_socket < 0) {
	    throwIOException(env, "Failed to accept RFCOMM client connection. [%d] %s", errno, strerror(errno));
	    return 0;
	}
	debug("RFCOMM client accepted, handle %li", client_socket);
	return client_socket;
}
