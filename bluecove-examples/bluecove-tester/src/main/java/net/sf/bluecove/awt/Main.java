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

import java.awt.*;
import java.awt.event.*;

import com.intel.bluetooth.BlueCoveImpl;

import net.sf.bluecove.JavaSECommon;
import net.sf.bluecove.Logger;
import net.sf.bluecove.Switcher;
import net.sf.bluecove.Logger.LoggerAppender;

/**
 * @author vlads
 *
 */
public class Main extends Frame implements LoggerAppender, com.intel.bluetooth.DebugLog.LoggerAppender {

	private static final long serialVersionUID = 1L;

	TextArea output = null;

	ScrollPane scrollPane;

	public static void main(String[] args) {
		//System.setProperty("bluecove.debug", "true");
		System.getProperties().put("bluecove.debug", "true");
		
		//BlueCoveImpl.instance().getBluetoothPeer().enableNativeDebug(true);
		JavaSECommon.initOnce();
		Main app = new Main();
		app.setVisible(true);
		Logger.debug("Stated app");
	}
	
	public Main() {
		Logger.addAppender(this);
		com.intel.bluetooth.DebugLog.addAppender(this);
		
		Logger.debug("Stating app");
		
		this.setTitle("BlueCove tester");
		
		MenuBar menuBar = new MenuBar();
		Menu menu = new Menu("Bluetooth");
		
		addMenu(menu, "Server Start", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startServer();
			}
		});

		addMenu(menu, "Server Stop", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.serverShutdown();
			}
		});

		
		addMenu(menu, "Client Start", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startClient();
			}
		});

		addMenu(menu, "Client Stop", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.clientShutdown();
			}
		});

		addMenu(menu, "Discovery", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startDiscovery();
			}
		});

		addMenu(menu, "Services Search", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startServicesSearch();
			}
		});
		
		addMenu(menu, "Quit", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				quit();
			}
		});

		
		menuBar.add(menu);
		setMenuBar(menuBar);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent w) {
				quit();
			}
		});
		
		// Create a scrolled text area.
        output = new TextArea(5, 30);
        output.setEditable(false);
        this.add(output);
//        scrollPane = new ScrollPane();
//        scrollPane.add(output);
//        this.add(scrollPane);
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (screenSize.width > 600) {
			screenSize.setSize(240, 320);
		}
		setSize(screenSize);
	}
	
	private void addMenu(Menu menu,String name, ActionListener l) {
		MenuItem clientStart = new MenuItem(name);
		clientStart.addActionListener(l);
		menu.add(clientStart);
	}

	private void quit() {
		Switcher.clientShutdown();
		Switcher.serverShutdownOnExit();
		this.dispose();
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
			buf.append(className.substring(className.lastIndexOf('.')));
			if (throwable.getMessage() != null) {
				buf.append(':');
				buf.append(throwable.getMessage());
			}
		}
		buf.append("\n");
		output.append(buf.toString());
	}

}
