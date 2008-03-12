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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

	private static String rmiRegistryHost = "localhost";

	private static int rmiRegistryPort = Server.rmiRegistryPort;

	private static RemoteService remoteService;

	private static class ServiceProxy implements InvocationHandler {

		public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
			ServiceRequest request = new ServiceRequest(m.getDeclaringClass().getCanonicalName(), m.getName(), m
					.getParameterTypes(), args);
			ServiceResponse response = execute(request, m);
			if (response.getException() == null) {
				return response.getReturnValue();
			} else {
				throw response.getException();
			}
		}
	}

	public static Object getService(Class interfaceClass) {
		Class[] allInterfaces = new Class[interfaceClass.getInterfaces().length + 1];
		allInterfaces[0] = interfaceClass;
		System.arraycopy(interfaceClass.getInterfaces(), 0, allInterfaces, 1, interfaceClass.getInterfaces().length);
		return Proxy.newProxyInstance(interfaceClass.getClassLoader(), allInterfaces, new ServiceProxy());
	}

	private static ServiceResponse execute(ServiceRequest request, Method method) throws Throwable {
		synchronized (Client.class) {
			if (remoteService == null) {
				remoteService = getRemoteService();
			}
		}
		return remoteService.execute(request);
	}

	private static RemoteService getRemoteService() throws Throwable {
		Registry registry = LocateRegistry.getRegistry(rmiRegistryHost, rmiRegistryPort);
		return (RemoteService) registry.lookup(RemoteService.SERVICE_NAME);
	}
}
