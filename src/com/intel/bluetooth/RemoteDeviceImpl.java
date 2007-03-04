/*
 Copyright 2004 Intel Corporation

 This file is part of Blue Cove.

 Blue Cove is free software; you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation; either version 2.1 of the License, or
 (at your option) any later version.

 Blue Cove is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with Blue Cove; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.intel.bluetooth;

import java.io.IOException;

import javax.bluetooth.L2CAPConnectionNotifier;
import javax.bluetooth.RemoteDevice;
import javax.microedition.io.Connection;
/**
 * The {@code RemoteDevice} class represents a remote Bluetooth device. It provides 
 * basic information about a remote device including the device's Bluetooth address 
 * and its friendly name.
 *
 */
public class RemoteDeviceImpl extends RemoteDevice {

	long address;
/*
	private RemoteDevice(String name, long address) {
		DebugLog.debug("new RemoteDevice", name);
		this.name = name;
		this.address = address;
	}
*/
	/* added for debugging */
	public String toString() {
		String	summary = "RemoteDevice(address: " + getBluetoothAddress() +
				", trusted: " + Boolean.valueOf(isTrustedDevice()).toString()+
				", encrypted: " + Boolean.valueOf(isEncrypted()).toString();
		// don't add friendly name, toString shouldn't cause network activity
		
		return summary;
	}
	/**
	 * Creates a Bluetooth device based upon its address. The Bluetooth address
	 * must be 12 hex characters long. Valid characters are 0-9, a-f, and A-F.
	 * There is no preceding "0x" in the string. For example, valid Bluetooth
	 * addresses include but are not limited to: <code>
	 * 008037144297 
	 * 00af8300cd0b
	 * 014bd91DA8FC 
	 * </code>
	 * @param address  the address of the Bluetooth device as
	 * a 12 character hex string 
	 * @throws java.lang.NullPointerException  if address is
	 * {@code null} 
	 * @throws java.lang.IllegalArgumentException if address is the address of
	 *  					the local device or is not a valid Bluetooth address
	 */

	protected RemoteDeviceImpl(String address) {
		super(address);
		this.address = Long.parseLong(address, 16);
	}

	/**
	 * Determines if this is a trusted device according to the BCC. 
	 *
	 * @return {@code true} if the device is a trusted device, otherwise {@code false}
	 */

	public boolean isTrustedDevice() {
		// TODO not yet implemented
		return false;
	}

	/**
	 * Returns the name of this device. The Bluetooth specification calls this
	 * name the "Bluetooth device name" or the "user-friendly name". This method
	 * will only contact the remote device if the name is not known or 
	 * {@code alwaysAsk} is {@code true}
	 * 
	 * @param alwaysAsk  if {@code true} then the device will be
	 * contacted for its name, otherwise, if there exists a known name for this
	 * device, the name will be returned without contacting the remote device
	 * @return  the name of the device, or {@code null} if the Bluetooth system does 
	 * not support this feature; if the local device is able to contact the remote
	 * device, the result will never be {@code null}; if the remote device does not have
	 * a name then an empty string will be returned
	 * @throws java.io.IOException if the remote device can not be contacted or the 
	 * remote device could not provide its name
	 */

	public native String getFriendlyName(boolean alwaysAsk) throws IOException;
	
	

	/**
	 * Retrieves the Bluetooth device that is at the other end of the Bluetooth
	 * Serial Port Profile connection, L2CAP connection, or OBEX over RFCOMM
	 * connection provided. This method will never return {@code null}. 
	 * 
	 * @param conn  the Bluetooth Serial Port connection, L2CAP connection, or OBEX over
	 * RFCOMM connection whose remote Bluetooth device is needed 
	 * @return the
	 * remote device involved in the connection 
	 * @throws java.lang.IllegalArgumentException 
	 * if conn is not a Bluetooth Serial Port Profile connection, L2CAP
	 * connection, or OBEX over RFCOMM connection; if {@code conn} is a
	 * {@link L2CAPConnectionNotifier}, {@link javax.microedition.io.StreamConnectionNotifier}, or 
	 * {@link javax.obex.SessionNotifier}
	 * @throws java.io.IOException  if the connection is closed 
	 * @throws java.lang.NullPointerException if conn is {@code null}
	 */

	public static RemoteDevice getRemoteDevice(Connection conn) throws IOException {
		if(conn == null) throw new NullPointerException();
			if(conn instanceof BluetoothL2CAPConnection) {
				String 	remoteAddr = ((BluetoothL2CAPConnection) conn).getRemoteTextAddress();
				return new RemoteDeviceImpl(remoteAddr);
			} else throw new IllegalArgumentException();
	}

	/**
	 * Attempts to authenticate this {@code RemoteDevice}. Authentication is a means of
	 * verifying the identity of a remote device. Authentication involves a
	 * device-to-device challenge and response scheme that requires a 128-bit
	 * common secret link key derived from a PIN code shared by both devices. If
	 * either side's PIN code does not match, the authentication process fails
	 * and the method returns {@code false}. The method will also return {@code false} if
	 * authentication is incompatible with the current security settings of the
	 * local device established by the BCC, if the stack does not support
	 * authentication at all, or if the stack does not support authentication
	 * subsequent to connection establishment. If this {@code RemoteDevice} has
	 * previously been authenticated, then this method returns {@code true} without
	 * attempting to re-authenticate this {@code RemoteDevice}.
	 * 
	 * @return {@code true} if authentication is successful; otherwise {@code false}
	 * @throws java.io.IOException - if there are no open connections between the local
	 * device and this {@code RemoteDevice}
	 */
	
	  public boolean authenticate() throws IOException { 
		  return false;
	  }
	 
	/**
	 * Determines if this {@code RemoteDevice} should be allowed to continue to access
	 * the local service provided by the Connection. In Bluetooth, authorization
	 * is defined as the process of deciding if device X is allowed to access
	 * service Y. The implementation of the @code{authorize(Connection conn)} method
	 * asks the Bluetooth Control Center (BCC) to decide if it is acceptable for
	 * {@code RemoteDevice} to continue to access a local service over the connection
	 * {@code conn}. In devices with a user interface, the BCC is expected to consult
	 * with the user to obtain approval. Some Bluetooth systems may allow the
	 * user to permanently authorize a remote device for all local services.
	 * When a device is authorized in this way, it is known as a "trusted
	 * device" -- see {@link #isTrustedDevice()}.
	 * <p>
	 * The {@code authorize()} method will also check that the identity of the
	 * {@code RemoteDevice} can be verified through authentication. If this {@code RemoteDevice}
	 * has been authorized for {@code conn} previously, then this method returns {@code true}
	 * without attempting to re-authorize this {@code RemoteDevice}.
	 * 
	 * @param conn the connection that this {@code RemoteDevice} is using to
	 * access a local service 
	 * @return {@code true} if this {@code RemoteDevice} is successfully
	 * authenticated and authorized, otherwise {@code false} if authentication or
	 * authorization fails 
	 * @throws java.lang.IllegalArgumentException if {@code conn} is not a
	 * connection to this {@code RemoteDevice}, or if the local device initiated the
	 * connection, i.e., the local device is the client rather than the server.
	 * This exception is also thrown if {@code conn} was created by {@code RemoteDevice} using a
	 * scheme other than btspp, btl2cap, or btgoep. This exception is thrown if
	 * {@code conn} is a notifier used by a server to wait for a client connection,
	 * since the notifier is not a connection to this {@code RemoteDevice}.
	 * @throws java.io.IOException  if {@code conn} is closed 
	 * @see #isTrustedDevice()
	 */
	
	 public boolean authorize(javax.microedition.io.Connection conn) throws
	  IOException { 
		 return false;
	 }
	 
	/**
	 * Attempts to turn encryption on or off for an existing connection. In the
	 * case where the parameter on is {@code true}, this method will first authenticate
	 * this {@code RemoteDevice} if it has not already been authenticated. Then it will
	 * attempt to turn on encryption. If the connection is already encrypted
	 * then this method returns {@code true}. Otherwise, when the parameter {@code on} is {@code true},
	 * either: 
	 * <ul><li>the method succeeds in turning on encryption for the connection
	 * and returns {@code true}, or</li>
	 * <li>the method was unsuccessful in turning on encryption
	 * and returns {@code false}. This could happen because the stack does not support
	 * encryption or because encryption conflicts with the user's security
	 * settings for the device. 
	 * </li></ul>
	 * In the case where the parameter on is {@code false},
	 * there are again two possible outcomes:
	 * <ul><li>encryption is turned off on the connection and {@code true} is returned, or</li>
	 * <li>encryption is left on for the connection and {@code false} is returned.</li></ul>
	 * Encryption may be left on following {@code encrypt(conn, false)} for a variety of
	 * reasons. The user's current security settings for the device may require
	 * encryption or the stack may not have a mechanism to turn off encryption.
	 * Also, the BCC may have determined that encryption will be kept on for the
	 * physical link to this {@code RemoteDevice}. The details of the BCC are
	 * implementation dependent, but encryption might be left on because other
	 * connections to the same device need encryption. (All of the connections
	 * over the same physical link must be encrypted if any of them are
	 * encrypted.) 
	 * <p>
	 * While attempting to turn encryption off may not succeed
	 * immediately because other connections need encryption on, there may be a
	 * delayed effect. At some point, all of the connections over this physical
	 * link needing encryption could be closed or also have had the method
	 * {@code encrypt(conn, false)} invoked for them. In this case, the BCC may turn off
	 * encryption for all connections over this physical link. (The policy used
	 * by the BCC is implementation dependent.) It is recommended that
	 * applications do {@code encrypt(conn, false)} once they no longer need encryption
	 * to allow the BCC to determine if it can reduce the overhead on
	 * connections to this {@code RemoteDevice}.
	 * <p>
	 * The fact that {@code encrypt(conn, false)} may not succeed in turning off
	 * encryption has very few consequences for applications. The stack handles
	 * encryption and decryption, so the application does not have to do
	 * anything different depending on whether the connection is still encrypted
	 * or not.
	 * 
	 * @param conn the connection whose need for encryption has changed
	 * @param on  {@code true} attempts to turn on encryption; {@code false} attempts to turn off
	 * encryption 
	 * @return {@code true} if the change succeeded, otherwise {@code false} if it
	 * failed 
	 * @throws java.io.IOException  if conn is closed
	 * @throws java.lang.IllegalArgumentException  if {@code conn} is not a connection to this
	 * {@code RemoteDevice}; if {@code conn} was created by the client side of the connection
	 * using a scheme other than btspp, btl2cap, or btgoep (for example, this
	 * exception will be thrown if {@code conn} was created using the file or http
	 * schemes.); if {@code conn} is a notifier used by a server to wait for a client
	 * connection, since the notifier is not a connection to this {@code RemoteDevice}
	 */
	
	 public boolean encrypt(javax.microedition.io.Connection conn, boolean on)
	  throws IOException {
		 return false;
	 }
	 
	/**
	 * Determines if this {@code RemoteDevice} has been authenticated. A device may have
	 * been authenticated by this application or another application.
	 * Authentication applies to an ACL link between devices and not on a
	 * specific L2CAP, RFCOMM, or OBEX connection. Therefore, if {@link #authenticate()}
	 * is performed when an L2CAP connection is made to device A, then
	 * {@code isAuthenticated()} may return {@code true} when tested as part of making an RFCOMM
	 * connection to device A.
	 * 
	 * @return {@code true} if this {@code RemoteDevice} has previously been authenticated;
	 * {@code false} if it has not been authenticated or there are no open connections
	 * between the local device and this {@code RemoteDevice}
	 */

	public boolean isAuthenticated() {
		throw new NotImplementedError();
	}

	/**
	 * Determines if this {@code RemoteDevice} has been authorized previously by the BCC
	 * of the local device to exchange data related to the service associated
	 * with the connection. Both clients and servers can call this method.
	 * However, for clients this method returns {@code false} for all legal values of
	 * the {@code conn} argument. 
	 * 
	 * @param conn  a connection that this {@code RemoteDevice}
	 * is using to access a service or provide a service 
	 * @return {@code true} if {@code conn}
	 * is a server-side connection and this {@code RemoteDevice} has been authorized;
	 * {@code false} if {@code conn} is a client-side connection, or a server-side connection
	 * that has not been authorized 
	 * @throws java.lang.IllegalArgumentException - if {@code conn}
	 * is not a connection to this {@code RemoteDevice}; if {@code conn} was not created using
	 * one of the schemes btspp, btl2cap, or btgoep; or if {@code conn} is a notifier
	 * used by a server to wait for a client connection, since the notifier is
	 * not a connection to this {@code RemoteDevice}. 
	 * @throws java.io.IOException - if {@code conn} is closed
	 */

	public boolean isAuthorized(javax.microedition.io.Connection conn)
			throws IOException {
		// TODO not yet implemented
		return false;
	}

	/**
	 * Determines if data exchanges with this {@code RemoteDevice} are currently being
	 * encrypted. Encryption may have been previously turned on by this or
	 * another application. Encryption applies to an ACL link between devices
	 * and not on a specific L2CAP, RFCOMM, or OBEX connection. Therefore, if
	 * {@link #encrypt(javax.microedition.io.Connection, boolean)} is performed with the on 
	 * parameter set to {@code true} when an L2CAP
	 * connection is made to device A, then {@code isEncrypted()} may return {@code true} when
	 * tested as part of making an RFCOMM connection to device A.
	 * 
	 * @return {@code true} if data exchanges with this {@code RemoteDevice} are being
	 * encrypted; {@code false} if they are not being encrypted, or there are no open
	 * connections between the local device and this {@code RemoteDevice}
	 */

	public boolean isEncrypted() {
		// TODO not yet implemented
		return false;
	}

}
