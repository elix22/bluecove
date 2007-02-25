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
package javax.bluetooth;

import java.io.IOException;

/**
 * The <code>BluetoothStateException</code> is thrown when
 * a request is made to the Bluetooth system that
 * the system cannot support in its present state.  If, however, the
 * Bluetooth system was not in this state, it could support this operation.
 * For example, some Bluetooth systems do not allow the device to go into
 * inquiry mode if a connection is established.  This exception would be
 * thrown if <code>startInquiry()</code> were called.
 *
 */
public class BluetoothStateException extends IOException {

	private static final long serialVersionUID = 1L;

	/**
     * Creates a new <code>BluetoothStateException</code> without a detail
     * message.
     */
	public BluetoothStateException() {
	}

    /**
     * Creates a <code>BluetoothStateException</code> with the specified
     * detail message.
     *
     * @param msg the reason for the exception
     */
	public BluetoothStateException(String msg) {
		super(msg);
	}
}