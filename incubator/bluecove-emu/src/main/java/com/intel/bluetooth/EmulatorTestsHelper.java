/**
 *  MicroEmulator
 *  Copyright (C) 2001-2007 Bartek Teodorczyk <barteo@barteo.net>
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
package com.intel.bluetooth;

import javax.bluetooth.BluetoothStateException;

/**
 * @author vlads
 * 
 */
public class EmulatorTestsHelper {

	private static int threadNumber;

	private static synchronized int nextThreadNum() {
		return threadNumber++;
	}

	public static void startInProcessServer() {
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_EMULATOR_PORT, "0");
	}

	public static void stopInProcessServer() {
		EmulatorHelper.getService().shutdown();
	}

	public static void useThreadLocalEmulator() throws BluetoothStateException {
		BlueCoveImpl.useThreadLocalBluetoothStack();
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_STACK, BlueCoveImpl.STACK_EMULATOR);
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_EMULATOR_PORT, "0");
		BlueCoveImpl.getThreadBluetoothStackID();
	}

	private static class RunBefore implements Runnable {

		private Runnable runnable;

		private Object startedEvent = new Object();

		private boolean started = false;

		private BluetoothStateException startException;

		RunBefore(Runnable runnable) {
			this.runnable = runnable;
		}

		public void run() {
			try {
				useThreadLocalEmulator();
			} catch (BluetoothStateException e) {
				startException = e;
			} finally {
				started = true;
				synchronized (startedEvent) {
					startedEvent.notifyAll();
				}
			}
			runnable.run();
		}
	}

	public static Thread runNewEmulatorStack(Runnable runnable) throws BluetoothStateException {
		RunBefore r = new RunBefore(runnable);
		Thread t = new Thread(r, "TestHelperThread-" + nextThreadNum());
		synchronized (r.startedEvent) {
			t.start();
			while (!r.started) {
				try {
					r.startedEvent.wait();
				} catch (InterruptedException e) {
					throw (BluetoothStateException) UtilsJavaSE.initCause(new BluetoothStateException(e.getMessage()),
							e);
				}
				if (r.startException != null) {
					throw r.startException;
				}
			}
		}
		return t;
	}
}
