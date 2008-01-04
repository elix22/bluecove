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

import net.sf.bluecove.util.IOUtils;
import net.sf.bluecove.util.TimeStatistic;
import net.sf.bluecove.util.TimeUtils;

import junit.framework.Assert;

/**
 * @author vlads
 * 
 */
public class CommunicationTesterL2CAP extends CommunicationData {

	public static final int INITIAL_DATA_PREFIX_LEN = 2;

	private static byte[] startPrefix(int testType, byte[] data) {
		byte[] dataToSend = new byte[data.length + INITIAL_DATA_PREFIX_LEN];
		dataToSend[0] = Consts.SEND_TEST_START;
		dataToSend[1] = (byte) testType;
		System.arraycopy(data, 0, dataToSend, INITIAL_DATA_PREFIX_LEN, data.length);
		Logger.debug("send L2CAP packet", dataToSend);
		return dataToSend;
	}

	public static void runTest(int testType, boolean server, ConnectionHolderL2CAP c, byte[] initialData,
			TestStatus testStatus) throws IOException {
		switch (testType) {
		case 1:
			testStatus.setName("l2byteAray");
			if (!server) {
				c.channel.send(startPrefix(testType, byteAray));
			} else {
				Assert.assertEquals("byteAray.len", byteAray.length, initialData.length);
				for (int i = 0; i < byteAray.length; i++) {
					Assert.assertEquals("byte[" + i + "]", byteAray[i], initialData[i]);
				}
			}
			break;
		case 2:
			testStatus.setName("l2sequence");
			if (server) {
				sequenceRecive(c, initialData);
			} else {
				sequenceSend(testType, c);
			}
			break;
		case 3:
			testStatus.setName("l2maxMTU");
			if (server) {
				maxMTURecive(c, initialData);
			} else {
				maxMTUSend(testType, c);
			}
			break;
		case TRAFIC_GENERATOR_WRITE:
			testStatus.setName("l2genW");
			if (server) {
				traficGeneratorWrite(c, initialData);
			} else {
				traficGeneratorClientInit(c, testType);
				traficGeneratorRead(c, initialData);
			}
			break;
		case TRAFIC_GENERATOR_READ:
			testStatus.setName("l2genR");
			if (server) {
				traficGeneratorRead(c, initialData);
			} else {
				traficGeneratorClientInit(c, testType);
				traficGeneratorWrite(c, initialData);
			}
			break;
		case TRAFIC_GENERATOR_READ_WRITE:
			testStatus.setName("l2genRW");
			if (!server) {
				traficGeneratorClientInit(c, testType);
			}
			traficGeneratorReadStart(c, initialData);
			traficGeneratorWrite(c, initialData);
			break;
		default:
			Assert.fail("Invalid test#" + testType);
		}

	}

	private static void sequenceSend(int testType, ConnectionHolderL2CAP c) throws IOException {
		final int sequenceSize = 77;
		int sequenceRecivedCount = 0;
		int sequenceSentCount = 0;
		c.channel.send(startPrefix(testType, new byte[] { sequenceSize }));
		int receiveMTU = c.channel.getReceiveMTU();
		int transmitMTU = c.channel.getTransmitMTU();
		Assert.assertTrue("ReceiveMTU " + receiveMTU, sequenceSize <= receiveMTU);
		Assert.assertTrue("TransmitMTU " + transmitMTU, sequenceSize <= transmitMTU);
		try {
			mainLoop: for (int i = 1; i <= sequenceSize; i++) {
				byte[] data = new byte[i];
				data[0] = (byte) i;
				for (int j = 1; j < data.length; j++) {
					data[j] = (byte) (j + aKnowndNegativeByte);
				}
				c.channel.send(data);
				sequenceSentCount++;
				while (!c.channel.ready()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						break mainLoop;
					}
				}
				byte[] dataRecived = new byte[receiveMTU];
				int lengthdataRecived = c.channel.receive(dataRecived);
				Assert.assertTrue("lengthdataRecived", lengthdataRecived >= 1);
				Assert.assertEquals("sequence", (byte) i, dataRecived[0]);
				Assert.assertEquals("lengthdataRecived", i, lengthdataRecived);
				for (int j = 1; j < lengthdataRecived; j++) {
					Assert.assertEquals("recived, byte [" + j + "]", (byte) (j + aKnowndPositiveByte), dataRecived[j]);
				}
				sequenceRecivedCount++;
			}
		} finally {
			if (sequenceSentCount != sequenceSize) {
				Logger.debug("Sent only " + sequenceSentCount + " packet(s) from " + sequenceSize);
			}
			if (sequenceRecivedCount != sequenceSize) {
				Logger.debug("Recived only " + sequenceRecivedCount + " packet(s) from " + sequenceSize);
			}
		}
	}

	private static void sequenceRecive(ConnectionHolderL2CAP c, byte[] initialData) throws IOException {
		Assert.assertEquals("initialData.len", 1, initialData.length);
		final int sequenceSize = initialData[0];
		int receiveMTU = c.channel.getReceiveMTU();
		int transmitMTU = c.channel.getTransmitMTU();
		Assert.assertTrue("ReceiveMTU " + receiveMTU, sequenceSize <= receiveMTU);
		Assert.assertTrue("TransmitMTU " + transmitMTU, sequenceSize <= transmitMTU);

		int sequenceRecivedCount = 0;
		int sequenceSentCount = 0;
		try {
			mainLoop: for (int i = 1; i <= sequenceSize; i++) {
				while (!c.channel.ready()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						break mainLoop;
					}
				}
				byte[] dataRecived = new byte[receiveMTU];
				int lengthdataRecived = c.channel.receive(dataRecived);
				Assert.assertTrue("lengthdataRecived", lengthdataRecived >= 1);
				Assert.assertEquals("sequence", (byte) i, dataRecived[0]);
				Assert.assertEquals("lengthdataRecived", i, lengthdataRecived);
				for (int j = 1; j < lengthdataRecived; j++) {
					Assert.assertEquals("recived, byte [" + j + "]", (byte) (j + aKnowndNegativeByte), dataRecived[j]);
				}
				sequenceRecivedCount++;

				byte[] data = new byte[i];
				data[0] = (byte) i;
				for (int j = 1; j < data.length; j++) {
					data[j] = (byte) (j + aKnowndPositiveByte);
				}
				c.channel.send(data);
				sequenceSentCount++;
			}
		} finally {
			if (sequenceRecivedCount != sequenceSize) {
				Logger.debug("Recived only " + sequenceRecivedCount + " packet(s) from " + sequenceSize);
			}
			if (sequenceSentCount != sequenceSize) {
				Logger.debug("Sent only " + sequenceSentCount + " packet(s) from " + sequenceSize);
			}
		}
	}

	private static void maxMTUSend(int testType, ConnectionHolderL2CAP c) throws IOException {
		int receiveMTU = c.channel.getReceiveMTU();
		int transmitMTU = c.channel.getTransmitMTU();
		if (transmitMTU < receiveMTU) {
			receiveMTU = transmitMTU;
		}
		final int sequenceSize = 10;
		c.channel.send(startPrefix(testType, new byte[] { sequenceSize, IOUtils.hiByte(receiveMTU),
				IOUtils.loByte(receiveMTU) }));

		int sequenceRecivedCount = 0;
		int sequenceSentCount = 0;
		try {
			mainLoop: for (int i = 1; i <= sequenceSize; i++) {
				byte[] data = new byte[receiveMTU];
				data[0] = (byte) i;
				for (int j = 1; j < data.length; j++) {
					data[j] = (byte) (j + aKnowndNegativeByte);
				}
				c.channel.send(data);
				sequenceSentCount++;
				while (!c.channel.ready()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						break mainLoop;
					}
				}
				byte[] dataRecived = new byte[receiveMTU];
				int lengthdataRecived = c.channel.receive(dataRecived);
				Assert.assertTrue("lengthdataRecived", lengthdataRecived >= 1);
				Assert.assertEquals("sequence", (byte) i, dataRecived[0]);
				Assert.assertEquals("lengthdataRecived", receiveMTU, lengthdataRecived);
				for (int j = 1; j < lengthdataRecived; j++) {
					Assert.assertEquals("recived, byte [" + j + "]", (byte) (j + aKnowndPositiveByte), dataRecived[j]);
				}
				sequenceRecivedCount++;
			}
		} finally {
			if (sequenceSentCount != sequenceSize) {
				Logger.debug("Sent only " + sequenceSentCount + " packet(s) from " + sequenceSize);
			}
			if (sequenceRecivedCount != sequenceSize) {
				Logger.debug("Recived only " + sequenceRecivedCount + " packet(s) from " + sequenceSize);
			}
		}
	}

	private static void maxMTURecive(ConnectionHolderL2CAP c, byte[] initialData) throws IOException {
		Assert.assertEquals("initialData.len", 3, initialData.length);
		final int sequenceSize = initialData[0];
		int clientMTU = IOUtils.bytesToShort(initialData[1], initialData[2]);
		int receiveMTU = c.channel.getReceiveMTU();
		int transmitMTU = c.channel.getTransmitMTU();
		Assert.assertTrue("ReceiveMTU " + receiveMTU, clientMTU <= receiveMTU);
		Assert.assertTrue("TransmitMTU " + transmitMTU, clientMTU <= transmitMTU);

		int sequenceRecivedCount = 0;
		int sequenceSentCount = 0;
		try {
			mainLoop: for (int i = 1; i <= sequenceSize; i++) {
				while (!c.channel.ready()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						break mainLoop;
					}
				}
				byte[] dataReceived = new byte[receiveMTU];
				int lengthdataReceived = c.channel.receive(dataReceived);
				Assert.assertTrue("lengthdataReceived", lengthdataReceived >= 1);
				Assert.assertEquals("sequence", (byte) i, dataReceived[0]);
				Assert.assertEquals("lengthdataReceived", clientMTU, lengthdataReceived);
				for (int j = 1; j < lengthdataReceived; j++) {
					Assert
							.assertEquals("received, byte [" + j + "]", (byte) (j + aKnowndNegativeByte),
									dataReceived[j]);
				}
				sequenceRecivedCount++;

				byte[] data = new byte[clientMTU];
				data[0] = (byte) i;
				for (int j = 1; j < data.length; j++) {
					data[j] = (byte) (j + aKnowndPositiveByte);
				}
				c.channel.send(data);
				sequenceSentCount++;
			}
		} finally {
			if (sequenceRecivedCount != sequenceSize) {
				Logger.debug("Received only " + sequenceRecivedCount + " packet(s) from " + sequenceSize);
			}
			if (sequenceSentCount != sequenceSize) {
				Logger.debug("Sent only " + sequenceSentCount + " packet(s) from " + sequenceSize);
			}
		}
	}

	private static void traficGeneratorClientInit(ConnectionHolderL2CAP c, int testType) throws IOException {
		byte sequenceSleep = 2;
		byte sequenceSize = 77;
		c.channel.send(startPrefix(testType, new byte[] { sequenceSleep, sequenceSize }));
	}

	private static void traficGeneratorWrite(ConnectionHolderL2CAP c, byte[] initialData) throws IOException {
		int sequenceSleep = 100;
		final int sequenceSizeMin = 16;
		int sequenceSize = 77;
		if (initialData != null) {
			if (initialData.length > 1) {
				sequenceSleep = initialData[0] * 10;
			}
			if (initialData.length > 2) {
				sequenceSize = initialData[1];
				if (sequenceSize < sequenceSizeMin) {
					sequenceSize = sequenceSizeMin;
				}
			}
		}

		long sequenceSentCount = 0;
		int reportedSize = 0;
		long reported = System.currentTimeMillis();
		try {
			mainLoop: do {
				byte[] data = new byte[sequenceSize];
				for (int i = 1; i < sequenceSize; i++) {
					data[i] = (byte) i;
				}
				IOUtils.long2Bytes(sequenceSentCount, 8, data, 0);
				long sendTime = System.currentTimeMillis();
				IOUtils.long2Bytes(sendTime, 8, data, 8);
				c.channel.send(data);
				sequenceSentCount++;
				reportedSize += sequenceSize;
				c.active();
				long now = System.currentTimeMillis();
				if (now - reported > 5 * 1000) {
					Logger.debug("Sent " + sequenceSentCount + " packet(s) " + TimeUtils.bps(reportedSize, reported));
					reported = now;
					reportedSize = 0;
				}
				if (sequenceSleep > 0) {
					try {
						Thread.sleep(sequenceSleep);
					} catch (InterruptedException e) {
						break mainLoop;
					}
				}
			} while (true);
		} finally {
			Logger.debug("Total " + sequenceSentCount + " packet(s)");
		}
	}

	private static void traficGeneratorReadStart(final ConnectionHolderL2CAP c, final byte[] initialData) {
		Thread t = new Thread() {
			public void run() {
				try {
					traficGeneratorRead(c, initialData);
				} catch (IOException e) {
					Logger.error("reader", e);
				}
			}
		};
		t.start();
	}

	private static void traficGeneratorRead(ConnectionHolderL2CAP c, byte[] initialData) throws IOException {
		long sequenceRecivedCount = 0;
		long sequenceRecivedNumberLast = -1;
		long sequenceOutOfOrderCount = 0;
		TimeStatistic delay = new TimeStatistic();
		long reported = System.currentTimeMillis();
		long receiveTimeLast = 0;
		try {
			int receiveMTU = c.channel.getReceiveMTU();
			mainLoop: do {
				while (!c.channel.ready()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						break mainLoop;
					}
				}
				byte[] dataReceived = new byte[receiveMTU];
				int lengthdataReceived = c.channel.receive(dataReceived);
				c.active();
				long receiveTime = System.currentTimeMillis();
				sequenceRecivedCount++;
				long sendTime = 0;

				if (lengthdataReceived > 8) {
					long sequenceRecivedNumber = IOUtils.bytes2Long(dataReceived, 0, 8);
					if (sequenceRecivedNumberLast + 1 != sequenceRecivedNumber) {
						sequenceOutOfOrderCount++;
					} else if (lengthdataReceived > 18) {
						sendTime = IOUtils.bytes2Long(dataReceived, 8, 8);
					}
					sequenceRecivedNumberLast = sequenceRecivedNumber;
				} else {
					sequenceOutOfOrderCount++;
				}

				if (receiveTimeLast != 0) {
					delay.add(receiveTimeLast - receiveTime);
					receiveTimeLast = receiveTime;
				}

				long now = receiveTime;
				if (now - reported > 5 * 1000) {
					Logger.debug("Received " + sequenceRecivedCount + "/" + sequenceOutOfOrderCount + "(er) packet(s) "
							+ delay.avg() + " msec");
					reported = now;
				}

			} while (true);
		} finally {
			Logger.debug("Received  " + sequenceRecivedCount + " packet(s)");
			Logger.debug("Misplaced " + sequenceOutOfOrderCount + " packet(s)");
			Logger.debug(" avg interval " + delay.avg() + " msec");
		}
	}

}
