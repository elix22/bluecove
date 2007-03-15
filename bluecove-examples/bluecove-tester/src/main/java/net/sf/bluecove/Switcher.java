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

import java.util.Hashtable;
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
		return (server != null) && server.isRunning;
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

				try {
					Thread.sleep(2000);
				} catch (Exception e) {
					break;
				}

				startServer();

				try {
					int sec = randomTTL(30, 80);
					Logger.info("switch to client in " + sec + " sec");
					Thread.sleep(sec * 1000);
				} catch (Exception e) {
					break;
				}

				yield(server);

				try {
					Thread.sleep(2000);
				} catch (Exception e) {
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
		synchronized (this) {
			notifyAll();
		}
		instance = null;
	}

	public static void startClient() {
		try {
			if (client == null) {
				client = new TestResponderClient();
			}
			if (!client.isRunning) {
				clientStartCount++;
				new Thread(client).start();
			} else {
				BlueCoveTestMIDlet.message("Warn", "Client isRunning");
			}
		} catch (Throwable e) {
			Logger.error("start error ", e);
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
				new Thread(server).start();
			} else {
				BlueCoveTestMIDlet.message("Warn", "Server isRunning");
			}
		} catch (Throwable e) {
			Logger.error("start error ", e);
		}
	}

	public static void serverShutdown() {
		if (server != null) {
			server.shutdown();
			server = null;
		}
	}

}
