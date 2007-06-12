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
    
    private boolean testFinished = false;
    
    private boolean shutdownCalled = false;
    
    private CanShutdown testThread;
    
    private String name;
    
    int gracePeriodSeconds = 0;
    
    TestTimeOutMonitor(String name, CanShutdown testThread, int gracePeriodSeconds) {
        //CLDC_1_0 super(name + "Monitor");
    	
        this.name = name;
        this.testThread = testThread;
        this.gracePeriodSeconds = gracePeriodSeconds;
        if (this.gracePeriodSeconds != 0)  {
        	super.start();
        }
    }
    
    public void run() {
    	if (gracePeriodSeconds == 0) {
    		return;
    	}
        
        while ((!testFinished) && (System.currentTimeMillis() < (testThread.lastActivityTime() + this.gracePeriodSeconds  * 1000))) {
        	try {
        		sleep(10 * 1000);    
            } catch (InterruptedException e) {
                return;
            }
        }

        if (!testFinished) {
        	shutdownCalled = true;
        	Logger.info("shutdown " + name + " by TimeOut");
        	testThread.shutdown();
        }
    }
    
    
    public void finish() {
        testFinished = true;
    }

	public boolean isShutdownCalled() {
		return shutdownCalled;
	}
}