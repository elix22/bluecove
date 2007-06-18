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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.StreamConnection;

import net.sf.bluecove.util.IOUtils;

/**
 * @author vlads
 *
 */
public class StreamConnectionHolder implements CanShutdown {

	public StreamConnection conn = null;
	
	public InputStream is = null;
	
	public OutputStream os = null;
	
	long lastActivityTime;
	
	int concurrentCount = 0;
	
	private Vector concurrentConnections;
	
	public StreamConnectionHolder() {
		active();
	}
	
	StreamConnectionHolder(StreamConnection conn) {
		this();
		this.conn = conn;
	}
	
	public void active() {
		lastActivityTime = System.currentTimeMillis();
	}
	
	public long lastActivityTime() {
		return lastActivityTime;
	}

	public void shutdown() {
		IOUtils.closeQuietly(os);
		IOUtils.closeQuietly(is);
		IOUtils.closeQuietly(conn);
	}
	
	public void registerConcurrent(Vector concurrentConnections) {
		this.concurrentConnections = concurrentConnections;
		synchronized (concurrentConnections) {
			concurrentConnections.addElement(this);
		}
	}
	
	public void concurrentNotify() {
		synchronized (concurrentConnections) {
			int concurNow = concurrentConnections.size();
			setConcurrentCount(concurNow);
			if (concurNow > 1) {
				// Update all other working Threads
				for (Enumeration iter = concurrentConnections.elements(); iter.hasMoreElements();) {
					StreamConnectionHolder t = (StreamConnectionHolder) iter.nextElement();
					t.setConcurrentCount(concurNow);
				}
			}
		}
	}
	
	public void disconnected() {
		if (concurrentConnections != null) {
			synchronized (concurrentConnections) {
				concurrentConnections.removeElement(this);
			}
		}
	}
	
	private void setConcurrentCount(int concurNow) {
		if (concurrentCount < concurNow) {
			concurrentCount = concurNow;
		}
	}

}
