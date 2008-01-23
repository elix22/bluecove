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

import net.sf.bluecove.ConnectionHolderStream;
import net.sf.bluecove.Logger;
import net.sf.bluecove.util.IOUtils;
import net.sf.bluecove.util.TimeUtils;

/**
 * @author vlads
 * 
 */
public class RfTrafficGenerator {

	public static void write(ConnectionHolderStream c) throws IOException {
		int sequenceSleep = c.is.read();
		if (sequenceSleep == -1) {
			Logger.debug("EOF recived");
			return;
		}
		sequenceSleep = sequenceSleep * 10;
		final int sequenceSizeMin = 16;
		int sequenceSize = c.is.read();
		if (sequenceSize == -1) {
			Logger.debug("EOF recived");
			return;
		}
		if (sequenceSize < sequenceSizeMin) {
			sequenceSize = sequenceSizeMin;
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
				c.os.write(data);
				sequenceSentCount++;
				reportedSize += sequenceSize;
				c.active();
				long now = System.currentTimeMillis();
				if (now - reported > 5 * 1000) {
					Logger.debug("Sent " + sequenceSentCount + " array(s) " + TimeUtils.bps(reportedSize, reported));
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
			Logger.debug("Total " + sequenceSentCount + " array(s)");
			long totalB = (sequenceSentCount * sequenceSize / 8);
			Logger.debug("Total " + totalB + " KBytes");
		}
	}
}
