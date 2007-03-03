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

/**
 * @author vlads
 *
 */
public class TestTimeOutMonitor extends Thread {
    
    boolean testFinished = false;
    
    CanShutdown testThread;
    
    int gracePeriod = 0;
    
    TestTimeOutMonitor(CanShutdown testThread, int gracePeriod) {
        super("TestMonitor");
        this.testThread = testThread;
        this.gracePeriod = gracePeriod;
        super.start();
    }
    
    public void run() {
        try {
            if (gracePeriod != 0) {
                sleep(gracePeriod * 60 * 1000);    
            }
        } catch (InterruptedException e) {
            return;
        }

        while (!testFinished) {
        	testThread.shutdown();
        }
    }
    
    public void finish() {
        testFinished = true;
        interrupt();
    }
}