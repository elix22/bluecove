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

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Timer;
import java.util.TimerTask;

import net.sf.bluecove.Configuration;
import net.sf.bluecove.util.Storage;

/**
 * @author vlads
 *
 */
public class ClientConnectionDialog extends Dialog {

	private static final long serialVersionUID = 1L;

	private static final String configConnectionURL = "connectionURL";
	
	Button btnConnect, btnDisconnect, btnCancel, btnSend; 
	
	TextField tfURL; 
	
	TextField tfData;
	
	Choice choiceDataSendType;
	
	Choice choiceDataReceiveType;
	
	Label status;
	
	Timer monitorTimer;
	
	ClientConnectionThread thread;
	
	private class ConnectionMonitor extends TimerTask {

		boolean wasConnected = false;
		
		boolean wasStarted = false;
		
		public void run() {
			if (thread == null) {
				if (wasConnected || wasStarted) {
					status.setText("Idle");
					btnDisconnect.setEnabled(false);
					btnConnect.setEnabled(true);
					btnSend.setEnabled(false);
					wasConnected = false;
					wasStarted = false;
				}
			} else if (thread.isRunning) {
				if (!wasConnected) {
					btnSend.setEnabled(true);
				}
				wasConnected = true;
				if (thread.receivedCount == 0) {
					status.setText("Connected");	
				} else {
					status.setText("Received " + thread.receivedCount);
				}
			} else {
				wasStarted = true;
				if (thread.isConnecting) {
					status.setText("Connecting");
				} else {
					status.setText("Disconnected");
				}
			}
		}
		
	}
	
	public ClientConnectionDialog(Frame owner) {
		super(owner, "Client Connection", false);
		
		GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;

		Panel panelItems = new BorderPanel(gridbag);
		this.add(panelItems, BorderLayout.NORTH);
		
		Label l = new Label("URL:");
		panelItems.add(l);
		panelItems.add(tfURL = new TextField("", 25));
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(tfURL, c);
		
		
		if (Configuration.storage != null) {
			String url = Configuration.storage.retriveData(configConnectionURL);
			if (url == null) {
				url = Configuration.storage.retriveData(Storage.configLastServiceURL);
			}
			tfURL.setText(url);
		}
		
		Label l2 = new Label("Data:");
		panelItems.add(l2);
		panelItems.add(tfData = new TextField());
		c.gridwidth = 1;
		gridbag.setConstraints(l2, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(tfData, c);
		
		Label l3 = new Label("");
		panelItems.add(l3);
		c.gridwidth = 1;
		gridbag.setConstraints(l3, c);
		
		panelItems.add(btnSend = new Button("Send"));
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				send();
			}
		});
		btnSend.setEnabled(false);
		c.gridwidth = 1;
		gridbag.setConstraints(btnSend, c);
		
		choiceDataSendType = new Choice();
		choiceDataSendType.add("as String.getBytes()+CR");
		choiceDataSendType.add("as String.getBytes()");
		choiceDataSendType.add("as parseByte(text)");
		//choiceDataType.add("as byte list");
		panelItems.add(choiceDataSendType);
		c.gridwidth = 1;
		gridbag.setConstraints(choiceDataSendType, c);
		
		Label l3r = new Label("  Receive:");
		panelItems.add(l3r);
		c.gridwidth = 1;
		gridbag.setConstraints(l3r, c);
		
		choiceDataReceiveType = new Choice();
		choiceDataReceiveType.add("as Chars");
		//choiceDataType.add("as byte list");
		panelItems.add(choiceDataReceiveType);
		c.gridwidth = 1;
		gridbag.setConstraints(choiceDataReceiveType, c);
		
		Label l3x = new Label("");
		panelItems.add(l3x);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l3x, c);

		
		Label l4 = new Label("Status:");
		panelItems.add(l4);
		c.gridwidth = 1;
		gridbag.setConstraints(l4, c);
		
		status = new Label("Idle");
		panelItems.add(status);
		c.gridwidth = 2;
		gridbag.setConstraints(status, c);
		
		Panel panelBtns = new Panel();
		this.add(panelBtns, BorderLayout.SOUTH);

		panelBtns.add(btnConnect = new Button("Connect"));
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				connect();
			}
		});
		
		panelBtns.add(btnDisconnect = new Button("Disconnect"));
		btnDisconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				shutdown();
			}
		});
		btnDisconnect.setEnabled(false);
		
		panelBtns.add(btnCancel = new Button("Cancel"));
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onClose();
			}
		});
		
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				onClose();
			}
		});
		this.pack();
		OkCancelDialog.centerParent(this);
		
		monitorTimer = new Timer();
		monitorTimer.schedule(new ConnectionMonitor(), 1000, 700);
	}
	
	protected void connect() {
		if (thread != null) {
			thread.shutdown();
			thread = null;
		}
		if (Configuration.storage != null) {
			Configuration.storage.storeData(configConnectionURL, tfURL.getText());
		}
		thread = new ClientConnectionThread(tfURL.getText());
		thread.setDaemon(true);
		thread.start();
		btnDisconnect.setEnabled(true);
		btnConnect.setEnabled(false);
	}
	
	protected void send() {
		if (thread != null) {
			String text = tfData.getText();
			int type = choiceDataSendType.getSelectedIndex();
			byte data[];
			switch (type) {
				case 0: data = (text + "\n").getBytes(); break;
				case 1: data = text.getBytes(); break;
				case 2: data = new byte[]{Byte.parseByte(text)}; break;
				default:
					data = new byte[]{0};
			}
			thread.send(data);
		}
	}
	
	public void shutdown() {
		if (thread != null) {
			thread.shutdown();
			thread = null;
		}
	}

	protected void onClose() {
		shutdown();
		monitorTimer.cancel();
		setVisible(false);
	}

}
