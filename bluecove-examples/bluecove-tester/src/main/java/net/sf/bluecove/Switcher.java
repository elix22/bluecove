/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
package net.sf.bluecove;

import java.util.Random;


/**
 * @author vlads
 *
 */
public class Switcher implements Runnable {

	public static TestResponderClient client;

	public static TestResponderServer server;

	public static int clientStartCount = 0;
	
	public static int serverStartCount = 0;
	
	private boolean stoped = false;
	
	public Thread thread;
	
	boolean isRunning = false;
	
	private static Switcher instance;
	
	Random random = new Random();
	
	public Switcher() {
		instance = this;
	}
	
	public static synchronized void clear() {
		clientStartCount = 0;
		serverStartCount = 0;
	}
	
	public static void yield(TestResponderClient client) {
		if (instance != null) {
			clientShutdown();
			synchronized (instance) {
				instance.notifyAll();
			}
		}
	}

	public static void yield(TestResponderServer server) {
		if (instance != null) {
			while (server.hasRunningConnections()) {
				try {
					Thread.sleep(2000);
				} catch (Exception e) {
					break;
				}	
			}
			serverShutdown();
		}
	}

	public static boolean isRunning() {
		return (instance != null) && instance.isRunning;
	}
	
	public static boolean isRunningClient() {
		return (client != null) && client.isRunning;
	}
	
	public static boolean isRunningServer() {
		return (server != null) && TestResponderServer.discoverable && server.isRunning;
	}

	public void run() {
		Logger.debug("Switcher started...");
		isRunning = true;
		try {
			if (!isRunningClient()) {
				startClient();
			}
			while (!stoped) {
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
						break;
					}
				}
				if (stoped) {
					break;
				}
				try {
					Thread.sleep(2000);
				} catch (Exception e) {
					break;
				}
				if (stoped) {
					break;
				}
				startServer();
				if (stoped) {
					break;
				}
				try {
					int sec = randomTTL(30, Configuration.serverMAXTimeSec);
					Logger.info("switch to client in " + sec + " sec");
					Thread.sleep(sec * 1000);
				} catch (Exception e) {
					break;
				}

				yield(server);
				if (stoped) {
					break;
				}
				try {
					Thread.sleep(2000);
				} catch (Exception e) {
					break;
				}
				if (stoped) {
					break;
				}
				startClient();

			}
		} finally {
			isRunning = false;
			Logger.info("Switcher finished!");
		}
	}
	
	public int randomTTL(int min, int max) {
		int d = random.nextInt() % (max - min);
		if (d < 0) {
			d = -d;
		}
		return min + d;
	}
	
	public void shutdown() {
		Logger.info("shutdownSwitcher");
		stoped = true;
		thread.interrupt();
		synchronized (this) {
			notifyAll();
		}
		instance = null;
	}

	public static void startDiscovery() {
		try {
			if (client == null) {
				client = new TestResponderClient();
			}
			client.discoveryOnce = true;
			client.useDiscoveredDevices = false;
			Configuration.searchOnlyBluecoveUuid = false;
			if (!client.isRunning) {
				clientStartCount++;
				(client.thread = new Thread(client)).start();
			}
		} catch (Throwable e) {
			Logger.error("start error ", e);
		}
	}

	public static void startServicesSearch() {
		try {
			if (client == null) {
				client = new TestResponderClient();
			}
			client.discoveryOnce = true;
			client.useDiscoveredDevices = true;
			Configuration.searchOnlyBluecoveUuid = false;
			if (!client.isRunning) {
				clientStartCount++;
				(client.thread = new Thread(client)).start();
			}
		} catch (Throwable e) {
			Logger.error("start error ", e);
		}
	}
	
	public static void startClient() {
		try {
			if (client == null) {
				client = new TestResponderClient();
			}
			client.discoveryOnce = false;
			client.useDiscoveredDevices = false;
			Configuration.searchOnlyBluecoveUuid = true;
			if (!client.isRunning) {
				clientStartCount++;
				(client.thread = new Thread(client)).start();
			} else {
				BlueCoveTestMIDlet.message("Warn", "Client isRunning");
			}
		} catch (Throwable e) {
			Logger.error("start error ", e);
		}
	}
	
	public static void startClientStress() {
		startClient();
		if (client != null) {
			client.runStressTest = true;
		}
	}

	public static void startClient(String url) {
		startClient();
		if (client != null) {
			client.connectURL = url;
		}
	}
	
	public static void clientShutdown() {
		if (client != null) {
			client.shutdown();
			client = null;
		}
	}

	public static void startServer() {
		try {
			if (server == null) {
				server = new TestResponderServer();
			}
			if (!server.isRunning) {
				serverStartCount ++;
				(server.thread = new Thread(server)).start();
			} else {
				if (Configuration.canCloseServer) {
					BlueCoveTestMIDlet.message("Warn", "Server isRunning");
				} else {
					serverStartCount ++;
					server.updateServiceRecord();
					TestResponderServer.setDiscoverable();
				}
			}
		} catch (Throwable e) {
			Logger.error("start error ", e);
		}
	}

	public static void serverShutdown() {
		if (Configuration.canCloseServer) {
			serverShutdownForce();	
		} else {
			TestResponderServer.setNotDiscoverable();
		}
	}
	
	public static void serverShutdownOnExit() {
		serverShutdownForce();
	}
	
	public static void serverShutdownForce() {
		if (server != null) {
			server.shutdown();
			server = null;
		}
	}

}
