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

import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import net.sf.bluecove.Logger.LoggerAppender;

public class BlueCoveTestCanvas extends Canvas implements CommandListener, LoggerAppender {

	static final Command exitCommand = new Command("Exit", Command.EXIT, 1);
	
	static final Command startClientCommand = new Command("Start client", Command.ITEM, 5);
	static final Command startServerCommand = new Command("Start server", Command.ITEM, 6);
	static final Command clearCommand = new Command("Clear", Command.ITEM, 7);
	
	private boolean showLogDebug = true;
	
	private int line;
	
	private int lineOffsetY;
	
	private int lineOffsetX;
	
	private TestResponderClient client;
	
	private TestResponderServer server;
	
	private Vector logMessages = new Vector();
	
	private int logLine = 0;
	
	private int logScrollX;
	
	private int logVisibleLines = 0;
	
	private boolean logLastEvenVisible = true;
	
	public BlueCoveTestCanvas() {
		super();
		super.setTitle("BlueCove");
		
		addCommand(exitCommand);
		addCommand(startClientCommand);
		addCommand(startServerCommand);
		addCommand(clearCommand);
		setCommandListener(this);
		Logger.addAppender(this);
	}
	
	public int writeln(Graphics g, String s) {
		int h = (g.getFont().getHeight() + 1);
		int y = lineOffsetY + h * line;
		g.drawString(s, lineOffsetX, y, Graphics.LEFT | Graphics.TOP);
		line ++;
		return y + h;
	}
	
	protected void paint(Graphics g) {
		lineOffsetY = 0;
		lineOffsetX = 0;
		line = 0;
		int width = getWidth();
        int height = getHeight();

		g.setGrayScale(255);
		g.fillRect(0, 0, width, height);
		
		g.setColor(0);
		int lastY = writeln(g, "BlueCove Tester");

		Font font = Font.getFont(Font.FACE_PROPORTIONAL,  Font.STYLE_PLAIN, Font.SIZE_SMALL);
		g.setFont(font);
		line = 0;
		lineOffsetY = lastY;
		int lineHeight = g.getFont().getHeight() + 1;
		logVisibleLines = (height - lastY ) / lineHeight;
		lineOffsetX = logScrollX;
		int logIndex = logLine;
		while (((lastY) < height) && (logIndex < logMessages.size())) {
			String message = (String)logMessages.elementAt(logIndex);
			lastY = writeln(g, message);
			logIndex ++;
		} 
		logLastEvenVisible = (logIndex == logMessages.size());
	}
	
	public void appendLog(int level, String message, Throwable throwable) {
		if (!showLogDebug && (level == Logger.DEBUG)) {
			return;
		}
		StringBuffer buf = new StringBuffer();
		switch (level) {
		case Logger.ERROR:
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
			buf.append(':');
			buf.append(throwable.getMessage());
		}
		
		logMessages.addElement(buf.toString());
		if (logLastEvenVisible) {
			if (((logLine + logVisibleLines) < logMessages.size())) {
				logLine = logMessages.size() - logVisibleLines;
				if (logLine < 0) {
					logLine = 0;
				}
			}
			BlueCoveTestMIDlet.display.flashBacklight(0);
			repaint();
		}
	}

	protected void keyPressed(int keyCode) {
		int action = getGameAction(keyCode);
		switch (action) {
		case UP:
			if (logLine > 0) {
				logLine--;
			}
			break;
		case DOWN:
			if ((logLine + logVisibleLines - 1) < logMessages.size()) {
				logLine++;
			}
			break;
		case RIGHT:
			if (logScrollX > -300) {
				logScrollX-=5;
			}
			break;
		case LEFT:
			if (logScrollX < 0) {
				logScrollX+=5;
			}
			break;
		}
		repaint();
	}
	
	public void commandAction(Command c, Displayable d) {
		if (c == exitCommand) {
			BlueCoveTestMIDlet.exit();
		} else if (c == clearCommand) {
			logMessages.removeAllElements();
			logLine = 0;
			logScrollX = 0;
			repaint();
		} else if (c == startClientCommand) {
			try {
				if (client == null) {
					client = new TestResponderClient();
				}
				if (!client.isRunning) {
					new Thread(client).start();
				} else {
					BlueCoveTestMIDlet.message("Warn", "Client isRunning");
				}
			} catch (Throwable e) {
				Logger.error("start error ", e);
			}
		} else if (c == startServerCommand) {
			try {
				if (server == null) {
					server = new TestResponderServer();
				}
				if (!server.isRunning) {
					new Thread(server).start();
				} else {
					BlueCoveTestMIDlet.message("Warn", "Server isRunning");
				}
			} catch (Throwable e) {
				Logger.error("start error ", e);
			}
		}
	}

}
