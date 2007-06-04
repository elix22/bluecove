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
    
    String name;
    
    int gracePeriod = 0;
    
    TestTimeOutMonitor(String name, CanShutdown testThread, int gracePeriod) {
        //CLDC_1_0 super(name + "Monitor");
    	
        this.name = name;
        this.testThread = testThread;
        this.gracePeriod = gracePeriod;
        if (this.gracePeriod != 0)  {
        	super.start();
        }
    }
    
    public void run() {
    	if (gracePeriod == 0) {
    		return;
    	}
        
        while ((!testFinished) && (System.currentTimeMillis() < (testThread.lastActivityTime() + this.gracePeriod  * 60 * 1000))) {
        	try {
        		sleep(20 * 1000);    
            } catch (InterruptedException e) {
                return;
            }
        }

        if (!testFinished) {
        	Logger.info("shutdown " + name + " by TimeOut");
        	testThread.shutdown();
        }
    }
    
    public void finish() {
        testFinished = true;
    }
}