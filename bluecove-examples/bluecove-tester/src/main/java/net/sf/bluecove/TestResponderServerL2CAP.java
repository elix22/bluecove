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

import java.io.IOException;
import java.io.InterruptedIOException;

import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.L2CAPConnectionNotifier;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.microedition.io.Connector;

import net.sf.bluecove.util.BluetoothTypesInfo;
import net.sf.bluecove.util.IOUtils;
import net.sf.bluecove.util.TimeUtils;

/**
 * @author vlads
 */
public class TestResponderServerL2CAP extends Thread {

	private L2CAPConnectionNotifier serverConnection;
	
	private boolean isStoped = false;
	
	private TestResponderServerL2CAP() {
		
	}
	
	public static TestResponderServerL2CAP startServer() {
		TestResponderServerL2CAP srv = new TestResponderServerL2CAP();
		srv.start();
		return srv;
	}
	
	public void run() {
		isStoped = false;
		try {
			serverConnection =  (L2CAPConnectionNotifier) Connector.open(BluetoothTypesInfo.PROTOCOL_SCHEME_L2CAP + "://localhost:"
					+ Configuration.blueCoveUUID()
					+ ";name="
					+ Consts.RESPONDER_SERVERNAME
					+ ";authenticate=" + (Configuration.authenticate.booleanValue()?"true":"false")
					+ ";encrypt=" + (Configuration.encrypt.booleanValue()?"true":"false")
					+ ";authorize=" + (Configuration.authorize?"true":"false"));
			if (Configuration.testServiceAttributes) {
				ServiceRecord record = LocalDevice.getLocalDevice().getRecord(serverConnection);
				if (record == null) {
					Logger.warn("Bluetooth ServiceRecord is null");
				} else {
					TestResponderServer.buildServiceRecord(record);
					try {
						LocalDevice.getLocalDevice().updateRecord(record);
						Logger.debug("L2CAP ServiceRecord updated");
					} catch (Throwable e) {
						Logger.error("L2CAP Service Record update error", e);
					}
				}
			}
		} catch (Throwable e) {
			Logger.error("L2CAP Server start error", e);
			isStoped = true;
			return;
		}
		try {
			int errorCount = 0;
			while (!isStoped) {
				L2CAPConnection channel;
				try {
					Logger.info("Accepting L2CAP connections");
					channel = serverConnection.acceptAndOpen();
				} catch (InterruptedIOException e) {
					isStoped = true;
					break;
				} catch (Throwable e) {
					if (errorCount > 3) {
						isStoped = true;
					}
					if (isStoped) {
						return;
					}
					errorCount ++;
					Logger.error("acceptAndOpen ", e);
					continue;
				}
				errorCount = 0;
				receive(channel);
				IOUtils.closeQuietly(channel);
			}
		} finally {
			close();
			Logger.info("L2CAP Server finished! " + TimeUtils.timeNowToString());
		}
	}
	
	void receive(L2CAPConnection channel) {
		try {
			int receiveLengthMax = channel.getReceiveMTU();
            byte[] buffer = new byte[receiveLengthMax];

            int receivedLength = channel.receive(buffer);
		
            if (receivedLength == 0) {
            	Logger.debug("a zero length L2CAP packet is received");
            } else {
            	processData(channel, buffer, receivedLength);
            }
            
		} catch (Throwable e) {
			if (isStoped) {
				return;
			}
			Logger.error("L2CAP receive", e);
		} 
	}
	
	private void processData(L2CAPConnection channel, byte[] buffer, int receivedLength) throws IOException {
		if ((receivedLength < 3) || (buffer[0] != Consts.SEND_TEST_START)) {
			Logger.debug("not a test client connected, will echo");
			runEcho(channel, buffer, receivedLength);
			return;
		}
		int testType = buffer[1];
		TestStatus testStatus = new TestStatus(testType);
		ConnectionHolderL2CAP c = new ConnectionHolderL2CAP(channel);
		
		TestTimeOutMonitor monitorConnection = new TestTimeOutMonitor("test" + testType, c, Configuration.serverTestTimeOutSec);
		
		
		byte[] initialData = new byte[receivedLength];
		System.arraycopy(buffer, 0, initialData, 0, receivedLength - 2);
		
		try {
			CommunicationTesterL2CAP.runTest(testType, true, c, initialData, testStatus);
			
			TestResponderServer.countSuccess++;
			
			Logger.debug("Test# " + testType + " " + testStatus.getName() + " ok");
		} catch (Throwable e) {
			if (!isStoped) {
				TestResponderServer.failure.addFailure("test " + testType  + " " + testStatus.getName(), e);
			}
			Logger.error("Test# " + testType  + " " + testStatus.getName() + " error", e);			
		} finally {
			monitorConnection.finish();
		}
	}
	
	private void runEcho(L2CAPConnection channel, byte[] buffer, int receivedLength) throws IOException {
		echo(channel, buffer, receivedLength);
		mainLoop:
		while (true) {
			while (!channel.ready()){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break mainLoop;
				}
			}
			int receiveMTU = channel.getReceiveMTU();
			byte[] data = new byte[receiveMTU];
			int length = channel.receive(data);
			echo(channel, data, length);
		}
	}
	
	private void echo(L2CAPConnection channel, byte[] buffer, int receivedLength) throws IOException {
		boolean cBufHasBinary = false;
		for (int k = 0; k < receivedLength; k++) {
			char c = (char) buffer[k];
			if (c < ' ') {
				if (((c == '\n') && (k == receivedLength - 1)) || ((c == '\r') && (k == receivedLength - 2))) {
					receivedLength --;
					break;
				}
				cBufHasBinary = true;
				break;
			}
		}
		String message = new String(buffer, 0, receivedLength);
		StringBuffer buf = new StringBuffer(message);
		if (cBufHasBinary) {
			buf.append(" [");
			for (int k = 0; k < receivedLength; k++) {
				buf.append(Integer.toHexString(buffer[k])).append(' ');
			}
			buf.append("]");
		}
		Logger.debug("|" + buf.toString());
		
		byte[] reply = new byte[receivedLength];
		System.arraycopy(buffer, 0, reply, 0, receivedLength); 
		channel.send(reply);
	}

	void close() {
		try {
			if (serverConnection != null) {
				serverConnection.close();
			}
			Logger.debug("L2CAP ServerConnection closed");
		} catch (Throwable e) {
			Logger.error("L2CAP Server stop error", e);
		}
	}
	
	public void interrupt()  {
		isStoped = true;
		super.interrupt();
	}
}
