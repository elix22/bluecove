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
package net.sf.bluecove.awt;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.ScrollPane;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.bluetooth.LocalDevice;

import net.sf.bluecove.Configuration;
import net.sf.bluecove.JavaSECommon;
import net.sf.bluecove.Logger;
import net.sf.bluecove.RemoteDeviceInfo;
import net.sf.bluecove.Storage;
import net.sf.bluecove.Switcher;
import net.sf.bluecove.TestResponderClient;
import net.sf.bluecove.TestResponderServer;
import net.sf.bluecove.Logger.LoggerAppender;

import com.intel.bluetooth.BlueCoveImpl;

/**
 * @author vlads
 *
 */
public class Main extends Frame implements LoggerAppender, Storage, com.intel.bluetooth.DebugLog.LoggerAppender {

	private static final long serialVersionUID = 1L;

	TextArea output = null;

	ScrollPane scrollPane;
	
	int lastKeyCode;
	
	MenuItem debugOn;

	public static void main(String[] args) {
		//System.setProperty("bluecove.debug", "true");
		//System.getProperties().put("bluecove.debug", "true");
		
		//BlueCoveImpl.instance().getBluetoothPeer().enableNativeDebug(true);
		JavaSECommon.initOnce();
		Main app = new Main();
		app.setVisible(true);
		Logger.debug("Stated app");
		Logger.debug("OS:" + System.getProperty("os.name") + "|" + System.getProperty("os.version") + "|" + System.getProperty("os.arch"));
		
		Configuration.storage = app;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("--stack")) {
				// This is used in WebStart when system properties cant be defined.
				i ++;
				BlueCoveImpl.instance().setBluetoothStack(args[i]);
				app.updateTitle();
			} else if (args[i].equalsIgnoreCase("--runonce")) {
				int rc = Switcher.runClient();
				Logger.debug("Finished app " + rc);
				System.exit(rc);
			}
		}
	}
	
	public Main() {
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent w) {
				quit();
			}
		});
		
		Logger.addAppender(this);
		com.intel.bluetooth.DebugLog.addAppender(this);
		
		Logger.debug("Stating app");
		
		this.setTitle("BlueCove tester");
		
		MenuBar menuBar = new MenuBar();
		Menu menuBluetooth = new Menu("Bluetooth");
		
		addMenu(menuBluetooth, "Server Start", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startServer();
				updateTitle();
			}
		});

		addMenu(menuBluetooth, "Server Stop", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.serverShutdown();
			}
		});

		
		addMenu(menuBluetooth, "Client Start", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startClient();
				updateTitle();
			}
		});

		addMenu(menuBluetooth, "Client Stop", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.clientShutdown();
			}
		});

		addMenu(menuBluetooth, "Discovery", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startDiscovery();
				updateTitle();
			}
		});

		addMenu(menuBluetooth, "Services Search", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startServicesSearch();
				updateTitle();
			}
		});

		addMenu(menuBluetooth, "Client Stress Start", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startClientStress();
				updateTitle();
			}
		});
		
		String lastURL = retriveData("lastURL");
		if (lastURL != null) {
			addMenu(menuBluetooth, "Client Last service Start", new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Switcher.startClient(retriveData("lastURL"));
					updateTitle();
				}
			});
		}
		
		addMenu(menuBluetooth, "Quit", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				quit();
			}
		});

		
		menuBar.add(menuBluetooth);
		
		Menu menuLogs = new Menu("Logs");
		
		debugOn = addMenu(menuLogs, "Debug ON", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean dbg = !com.intel.bluetooth.DebugLog.isDebugEnabled();
				com.intel.bluetooth.DebugLog.setDebugEnabled(dbg);
				com.intel.bluetooth.BlueCoveImpl.instance().getBluetoothPeer().enableNativeDebug(dbg);
				if (dbg) {
					com.intel.bluetooth.DebugLog.debug("Debug enabled");
					debugOn.setLabel("Debug OFF");
				} else {
					com.intel.bluetooth.DebugLog.debug("Debug disabled");
					debugOn.setLabel("Debug ON");
				}
			}
		});
		
		addMenu(menuLogs, "Clear Log", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clear();
			}
		});
		
		addMenu(menuLogs, "Print FailureLog", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				printFailureLog();
			}
		});
		
		addMenu(menuLogs, "Clear Stats", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearStats();
			}
		});
		
		menuBar.add(menuLogs);
		
		setMenuBar(menuBar);

		
		// Create a scrolled text area.
        output = new TextArea("");
        output.setEditable(false);
        this.add(output);
		
        output.addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent e) {
				//Logger.debug("key:" + e.getKeyCode() + " " + KeyEvent.getKeyText(e.getKeyCode()));
				Main.this.keyPressed(e.getKeyCode());
			}

			public void keyReleased(KeyEvent e) {
			}

			public void keyTyped(KeyEvent e) {
			}});
        
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (screenSize.width > 600) {
			screenSize.setSize(240, 320);
		}
		setSize(screenSize);
	}
	
	private void updateTitle() {
		String title ="BlueCove tester";
		String bluecoveVersion = LocalDevice.getProperty("bluecove");
		if (bluecoveVersion != null) {
			title += " on " + LocalDevice.getProperty("bluecove.stack");
		}
		this.setTitle(title);
	}
	
	private void printFailureLog() {
		Logger.info("*Client Success:" + TestResponderClient.countSuccess + " Failure:" + TestResponderClient.failure.countFailure);
		TestResponderClient.failure.writeToLog();
		Logger.info("*Server Success:" + TestResponderServer.countSuccess + " Failure:" + TestResponderServer.failure.countFailure);
		TestResponderServer.failure.writeToLog();
	}
	
	private void clearStats() {
		TestResponderClient.clear();
		TestResponderServer.clear();
		Switcher.clear();
		RemoteDeviceInfo.clear();
		clear();
	}
	
	
	private MenuItem addMenu(Menu menu,String name, ActionListener l) {
		MenuItem menuItem = new MenuItem(name);
		menuItem.addActionListener(l);
		menu.add(menuItem);
		return menuItem;
	}

	protected void keyPressed(int keyCode) {
		switch (keyCode) {
		case '1':
			//printStats();
			break;
		case '4':
			printFailureLog();
			break;
		case '0':
			//logScrollX = 0;
			//setLogEndLine();
			break;
		case '*':
		case 119:
			Switcher.startDiscovery();
			break;
		case '7':
			Switcher.startServicesSearch();
			break;
		case '2':
			Switcher.startClient();
			break;
		case '3':
			Switcher.clientShutdown();
			break;
		case '5':
			Switcher.startServer();
			break;
		case '6':
			Switcher.serverShutdown();
			break;
		case '8':
			//startSwitcher();
			break;
		case '9':
			//stopSwitcher();
			break;
		case '#':
		case 120:
			if (lastKeyCode == keyCode) {
				quit();
			}
			clear();
			break;
		}
		lastKeyCode = keyCode; 
	}
	
	private void clear() {
		if (output == null) {
			return;
		}
		output.setText("");
	}
	
	private void quit() {
		Logger.debug("quit");
		Switcher.clientShutdown();
		Switcher.serverShutdownOnExit();
		
		Logger.removeAppender(this);
		com.intel.bluetooth.DebugLog.removeAppender(this);
		
		//this.dispose();
		System.exit(0);
	}
	
	public void appendLog(int level, String message, Throwable throwable) {
		if (output == null) {
			return;
		}
		StringBuffer buf = new StringBuffer();
		switch (level) {
		case Logger.ERROR:
			//errorCount ++;
			buf.append("e.");
			break;
		case Logger.WARN:
			buf.append("w.");
			break;
		case Logger.INFO:
			buf.append("i.");
			break;
		}
		buf.append(message);
		if (throwable != null) {
			buf.append(' ');
			String className = throwable.getClass().getName();
			buf.append(className.substring(1 + className.lastIndexOf('.')));
			if (throwable.getMessage() != null) {
				buf.append(':');
				buf.append(throwable.getMessage());
			}
		}
		buf.append("\n");
		synchronized (output) {
			output.append(buf.toString());
		}
	}

	public String retriveData(String name) {
		Properties p = new Properties(); 
		String tmpDir = System.getProperty("java.io.tmpdir");
		File f = new File(tmpDir, "bluecove-tester.properties");
		if (f.exists()) {
			FileInputStream in = null;
			try {
				in = new FileInputStream(f); 
				p.load(in);
			} catch (IOException ignore) {
			} finally {
				try {
					in.close();
				} catch (Throwable ignore) {
				}
			}
		}
		return p.getProperty(name);
	}

	public void storeData(String name, String value) {
		Properties p = new Properties(); 
		String tmpDir = System.getProperty("java.io.tmpdir");
		File f = new File(tmpDir, "bluecove-tester.properties");
		if (f.exists()) {
			FileInputStream in = null;
			try {
				in = new FileInputStream(f); 
				p.load(in);
			} catch (IOException ignore) {
			} finally {
				try {
					in.close();
				} catch (Throwable ignore) {
				}
			}
		}
		p.setProperty(name, value);
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(f);
			p.save(out, "");
		} catch (FileNotFoundException ignore) {
		}
		try {
			out.close();
		} catch (Throwable ignore) {
		}
	}

}
