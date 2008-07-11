/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
package net.sf.bluecove.tests;

import java.io.IOException;

import net.sf.bluecove.Configuration;
import net.sf.bluecove.ConnectionHolderStream;
import net.sf.bluecove.Logger;
import net.sf.bluecove.util.IOUtils;
import net.sf.bluecove.util.TimeUtils;

/**
 * @author vlads
 * 
 */
public class RfTrafficGenerator {

	final static int sequenceSizeMin = 16;

	private static class Config {

		int sequenceSleep;

		int sequenceSize;

		int durationMSec = 0;

		boolean init(ConnectionHolderStream c, boolean server, String messagePrefix) throws IOException {
			if (server) {
				sequenceSleep = c.is.read();
				if (sequenceSleep == -1) {
					Logger.debug("EOF received");
					return false;
				}
				sequenceSize = c.is.read();
				if (sequenceSize == -1) {
					Logger.debug("EOF received");
					return false;
				}
				durationMSec = c.is.read();
				if (durationMSec == -1) {
					Logger.debug("EOF received");
					return false;
				}
			} else {
				sequenceSize = Configuration.tgSize & 0xFF;
				sequenceSleep = Configuration.tgSleep & 0xFF;
				durationMSec = Configuration.tgDurationMin;
			}
			sequenceSleep = sequenceSleep * 10;
			if (sequenceSize < sequenceSizeMin) {
				sequenceSize = sequenceSizeMin;
			}
			switch (sequenceSize) {
			case 251:
				// 1K
				sequenceSize = 0x400;
				break;
			case 252:
				// 2K
				sequenceSize = 0x800;
				break;
			case 253:
				// 3K
				sequenceSize = 0xC00;
				break;
			case 254:
				// 4K
				sequenceSize = 0x1000;
				break;
			case 255:
				// 5K
				sequenceSize = 0x1400;
				break;
			}
			Logger.debug(messagePrefix + " size selected " + sequenceSize + " byte");
			Logger.debug(messagePrefix + " duration " + durationMSec + " minutes");
			durationMSec *= 60000;

			return true;
		}
	}

	public static void trafficGeneratorClientInit(ConnectionHolderStream c) throws IOException {
		byte sequenceSleep = (byte) (Configuration.tgSleep & 0xFF);
		byte sequenceSize = (byte) (Configuration.tgSize & 0xFF);
		byte durationMin = (byte) (Configuration.tgDurationMin & 0xFF);
		c.os.write(sequenceSleep);
		c.os.write(sequenceSize);
		c.os.write(durationMin);
		c.os.flush();
	}

	public static void trafficGeneratorWrite(ConnectionHolderStream c, boolean server) throws IOException {
		Config cf = new Config();
		if (!cf.init(c, server, "RF write")) {
			return;
		}
		if (cf.sequenceSleep > 0) {
			Logger.debug("RF write sleep selected " + cf.sequenceSleep + " msec");
		} else {
			Logger.debug("RF write no sleep");
		}
		long sequenceSentCount = 0;
		int reportedSize = 0;

		// Create test data
		byte[] data = new byte[cf.sequenceSize];
		for (int i = 1; i < cf.sequenceSize; i++) {
			data[i] = (byte) i;
		}

		long start = System.currentTimeMillis();
		long reported = start;
		try {
			mainLoop: do {
				IOUtils.long2Bytes(sequenceSentCount, 8, data, 0);
				long sendTime = System.currentTimeMillis();
				IOUtils.long2Bytes(sendTime, 8, data, 8);
				c.os.write(data);
				sequenceSentCount++;
				reportedSize += cf.sequenceSize;
				c.active();
				long now = System.currentTimeMillis();
				if (now - reported > 5 * 1000) {
					Logger.debug("RF Sent " + sequenceSentCount + " array(s) " + TimeUtils.bps(reportedSize, reported));
					reported = now;
					reportedSize = 0;
				}
				if ((cf.durationMSec != 0) && (now > start + cf.durationMSec)) {
					break;
				}
				if (cf.sequenceSleep > 0) {
					try {
						Thread.sleep(cf.sequenceSleep);
					} catch (InterruptedException e) {
						break mainLoop;
					}
					c.active();
				}
			} while (true);
		} finally {
			Logger.debug("RF Total " + sequenceSentCount + " array(s)");
			long totalB = sequenceSentCount * cf.sequenceSize;
			Logger.debug("RF Total " + (totalB / 1024) + " KBytes");
			Logger.debug("RF Total write speed " + TimeUtils.bps(totalB, start));
		}
	}

	public static void trafficGeneratorReadStart(final ConnectionHolderStream c, final boolean server) {
		Runnable r = new Runnable() {
			public void run() {
				try {
					trafficGeneratorRead(c, server);
				} catch (IOException e) {
					Logger.error("reader", e);
				}
			}
		};
		Thread t = Configuration.cldcStub.createNamedThread(r, "RFtgReciver");
		t.start();
	}

	public static void trafficGeneratorRead(ConnectionHolderStream c, boolean server) throws IOException {
		Config cf = new Config();
		if (!cf.init(c, server, "RF read")) {
			return;
		}
		long totalSize = 0;
		long sequenceReceivedCount = 0;
		long start = System.currentTimeMillis();
		long reported = start;
		long reportedSize = 0;
		byte[] byteAray = new byte[cf.sequenceSize];

		try {
			while (true) {
				int read = c.is.read(byteAray);
				if (read == -1) {
					break;
				}
				sequenceReceivedCount++;
				totalSize += read;
				reportedSize += read;
				c.active();
				long now = System.currentTimeMillis();
				if (now - reported > 5 * 1000) {
					Logger.debug("RF Received " + sequenceReceivedCount + " array(s) "
							+ TimeUtils.bps(reportedSize, reported));
					reported = now;
					reportedSize = 0;
				}
			}

		} finally {
			Logger.debug("RF Total " + sequenceReceivedCount + " array(s)");
			Logger.debug("RF Total " + (totalSize / 1024) + " KBytes");
			Logger.debug("RF Total read speed " + TimeUtils.bps(totalSize, start));
		}

	}

}
