/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth.rmi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

class RemoteServiceImpl extends UnicastRemoteObject implements RemoteService {

	private static final long serialVersionUID = 1L;

	public RemoteServiceImpl() throws RemoteException {
	}

	public boolean verify(String interfaceName) throws RemoteException {
		try {
			Class.forName(interfaceName + "Impl");
		} catch (Throwable e) {
			throw new RemoteException("Service for " + interfaceName + " not ready", e);
		}
		return true;
	}

	public ServiceResponse execute(ServiceRequest request) {
		try {
			Class<?> c = Class.forName(request.getClassName() + "Impl");
			Method m = c.getDeclaredMethod(request.getMethodName(), request.getParameterTypes());
			ServiceResponse response = new ServiceResponse();
			try {
				response.setReturnValue(m.invoke(c.newInstance(), request.getParameters()));
			} catch (InvocationTargetException e) {
				response.setException(e.getTargetException());
			}
			return response;
		} catch (Throwable e) {
			return new ServiceResponse(e);
		}
	}

}
