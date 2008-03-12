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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server {

	static int rmiRegistryPort = 8090;

	// Prevents GC
	private static Server server;

	private Registry registry = null;

	private Remote srv;

	public static void main(String[] args) {
		server = new Server();
		server.run();
	}

	private void run() {
		startRMIRegistry();
		startRMIService();
		// wit for RMI threads to start up
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}
	}

	private void startRMIRegistry() {
		try {
			registry = LocateRegistry.createRegistry(rmiRegistryPort);
		} catch (RemoteException e) {
			throw new Error("Fails to start RMIRegistry", e);
		}
	}

	private void startRMIService() {
		try {
			srv = new RemoteServiceImpl();
			registry.rebind(RemoteService.SERVICE_NAME, srv);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
