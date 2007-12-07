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

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;

import net.sf.bluecove.util.BluetoothTypesInfo;

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

    private static Thread tckRFCOMMThread;

    private static Thread tckL2CALthread;

    private static Thread tckGOEPThread;

    private static Thread tckOBEXThread;

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
        return isTCKRunning() || ((server != null) && TestResponderServer.discoverable && server.isRunning());
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
        interruptThread(thread);
        synchronized (this) {
            notifyAll();
        }
        instance = null;
    }

    public static void interruptThread(Thread thread) {
        if (Configuration.cldcStub != null) {
            Configuration.cldcStub.interruptThread(thread);
        }
    }

    public static Thread createThreadByName(String className) {
        try {
            Class c = Class.forName(className);
            return (Thread) c.newInstance();
        } catch (Throwable e) {
            Logger.debug(className, e);
            return null;
        }
    }

    public static void startTCKAgent() {
        try {
            LocalDevice localDevice = LocalDevice.getLocalDevice();
            Logger.info("address:" + localDevice.getBluetoothAddress());
            Logger.info("name:" + localDevice.getFriendlyName());
            Logger.info("class:" + BluetoothTypesInfo.toString(localDevice.getDeviceClass()));
            localDevice.setDiscoverable(DiscoveryAgent.GIAC);
        } catch (BluetoothStateException e) {
            Logger.error("start", e);
        }

        if (Configuration.likedTCKAgent) {
            tckRFCOMMThread = new BluetoothTCKAgent.RFCOMMThread("RFCOMMThread");
            if (tckRFCOMMThread == null) {
                Logger.info("Due to the License we do not include the TCK agent in distribution");
            } else {
                tckRFCOMMThread.start();

                try {
                    tckL2CALthread = new BluetoothTCKAgent.L2CAPThread("L2CAPThread");
                    if (tckL2CALthread != null) {
                        tckL2CALthread.start();
                    }
                } catch (Throwable e) {
                    Logger.debug("Fail to start L2CAP", e);
                }

                try {
                    tckGOEPThread = new BluetoothTCKAgent.GOEPThread("GOEPThread");
                    if (tckGOEPThread != null) {
                        tckGOEPThread.start();
                    }
                } catch (Throwable e) {
                    Logger.debug("Fail to start GOEP srv", e);
                }

                try {
                    tckOBEXThread = new OBEXTCKAgent.OBEXTCKAgentApp("10", Configuration.testServerOBEX_TCP ? "tcpobex"
                            : "btgoep");
                    if (tckOBEXThread != null) {
                        tckOBEXThread.start();
                    }
                } catch (Throwable e) {
                    Logger.debug("Fail to start OBEX srv", e);
                }
            }
        }
    }

    public static boolean isTCKRunning() {
        return (tckRFCOMMThread != null) || (tckL2CALthread != null) || (tckGOEPThread != null)
                || (tckOBEXThread != null);
    }

    static void stopTCK() {
        interruptThread(tckRFCOMMThread);
        tckRFCOMMThread = null;

        interruptThread(tckL2CALthread);
        tckL2CALthread = null;

        interruptThread(tckGOEPThread);
        tckGOEPThread = null;

        interruptThread(tckOBEXThread);
        tckOBEXThread = null;
    }

    public static TestResponderClient createClient() {
        try {
            if (client == null) {
                client = new TestResponderClient();
            }
            if (!client.isRunning) {
                client.configured = false;
                client.discoveryOnce = false;
                client.useDiscoveredDevices = false;
                client.searchOnlyBluecoveUuid = Configuration.searchOnlyBluecoveUuid;
                clientStartCount++;
                (client.thread = new Thread(client)).start();
                return client;
            } else {
                if (Configuration.isJ2ME) {
                    BlueCoveTestMIDlet.message("Warn", "Client is already Running");
                } else {
                    Logger.warn("Client is already Running");
                }
                return null;
            }
        } catch (Throwable e) {
            Logger.error("start error ", e);
            return null;
        }
    }

    public static void startTwoClients() {
        try {
            client = new TestResponderClient();
            client.configured = false;
            client.discoveryOnce = false;
            client.useDiscoveredDevices = false;
            client.searchOnlyBluecoveUuid = Configuration.searchOnlyBluecoveUuid;
            client.thread = new Thread(client);
            client.configured();

            TestResponderClient client2 = new TestResponderClient();
            client2.configured = false;
            client2.discoveryOnce = false;
            client2.useDiscoveredDevices = false;
            client2.searchOnlyBluecoveUuid = Configuration.searchOnlyBluecoveUuid;
            client2.thread = new Thread(client2);
            client2.configured();

            client.thread.start();
            client2.thread.start();
        } catch (Throwable e) {
            Logger.error("start error ", e);
        }
    }

    public static void startDiscovery() {
        TestResponderClient client = createClient();
        if (client != null) {
            client.discoveryOnce = true;
            client.useDiscoveredDevices = false;
            client.searchOnlyBluecoveUuid = Configuration.discoverySearchOnlyBluecoveUuid;
            client.configured();
        }
    }

    public static void startServicesSearch() {
        TestResponderClient client = createClient();
        if (client != null) {
            client.discoveryOnce = true;
            client.useDiscoveredDevices = true;
            client.searchOnlyBluecoveUuid = Configuration.discoverySearchOnlyBluecoveUuid;
            client.configured();
        }
    }

    public static void startClient() {
        try {
            createClient();
            if (client != null) {
                client.configured();
            }
        } catch (Throwable e) {
            Logger.error("startClient", e);
        }
    }

    public static int runClient() {
        createClient();
        if (client != null) {
            client.connectOnce = true;
            client.configured();
            try {
                client.thread.join();
            } catch (InterruptedException e) {
                return 2;
            }
            if (TestResponderClient.failure.countFailure > 0) {
                return 2;
            } else if (TestResponderClient.countSuccess == 0) {
                return 3;
            }
            return 1;
        } else {
            return 2;
        }
    }

    public static void startClientStress() {
        if ((client != null) && client.isRunning) {
            Logger.warn("Client is already Running");
            return;
        }
        createClient();
        if (client != null) {
            client.runStressTest = true;
            client.configured();
        }
    }

    public static void startClientLastURl() {
        if (Configuration.storage == null) {
            Logger.warn("no storage");
            return;
        }
        if ((client != null) && client.isRunning) {
            Logger.warn("Client is already Running");
            return;
        }
        String lastURL = Configuration.getLastServerURL();
        if (lastURL != null) {
            createClient();
            if (client != null) {
                client.connectURL = lastURL;
                client.configured();
            }
        } else {
            Logger.warn("no recent Connections");
        }
    }

    public static void startClientSelectService() {
        if ((client != null) && client.isRunning) {
            Logger.warn("Client is already Running");
            return;
        }
        createClient();
        if (client != null) {
            client.connectURL = "";
            client.configured();
        }
    }

    public static void startClientLastDevice() {
        if (Configuration.storage == null) {
            Logger.warn("no storage");
            return;
        }
        if ((client != null) && client.isRunning) {
            Logger.warn("Client is already Running");
            return;
        }
        String lastURL = Configuration.getLastServerURL();
        if (lastURL != null) {
            createClient();
            if (client != null) {
                client.connectDevice = BluetoothTypesInfo.extractBluetoothAddress(lastURL);
                client.configured();
            }
        } else {
            Logger.warn("no recent Connections");
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
            if (!server.isRunning()) {
                serverStartCount++;
                (server.thread = new Thread(server)).start();
            } else {
                if (Configuration.canCloseServer) {
                    if (Configuration.isJ2ME) {
                        BlueCoveTestMIDlet.message("Warn", "Server is already running");
                    } else {
                        Logger.warn("Server is already running");
                    }
                } else {
                    serverStartCount++;
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
        stopTCK();
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
