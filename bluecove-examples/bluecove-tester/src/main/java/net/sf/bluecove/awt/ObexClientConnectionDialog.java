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
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import net.sf.bluecove.Configuration;

public class ObexClientConnectionDialog extends Dialog {

	private static final long serialVersionUID = 1L;
	
	private static final String configObexConnectionURL = "obexConnectionURL";
	
	Button btnCancel, btnSend, btnDisconnect; 
	
	TextField tfURL; 
	
	TextField tfData;
	
	Label status;
	
	ObexClientConnectionThread thread;
	
	Timer monitorTimer;
	
	private class ObexConnectionMonitor extends TimerTask {

		public void run() {
			if (thread != null) {
				status.setText(thread.status);
				if (!thread.isRunning) {
					btnDisconnect.setEnabled(false);
					btnSend.setEnabled(true);
				}
			} else {
				status.setText("Idle");
			}
		}
	}
	
	public ObexClientConnectionDialog(Frame owner) {
		super(owner, "OBEX Client", false);
		
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
			tfURL.setText(Configuration.storage.retriveData(configObexConnectionURL));
		}
		
		Label l2 = new Label("Data:");
		panelItems.add(l2);
		panelItems.add(tfData = new TextField("Test Obex Message " + new Date().toString()));
		c.gridwidth = 1;
		gridbag.setConstraints(l2, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(tfData, c);
		
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

		panelBtns.add(btnSend = new Button("Send"));
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				send();
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
		
		monitorTimer = new Timer();
		monitorTimer.schedule(new ObexConnectionMonitor(), 1000, 700);
	}
	
	protected void send() {
		if (thread != null) {
			thread.shutdown();
			thread = null;
		}
		if (Configuration.storage != null) {
			Configuration.storage.storeData(configObexConnectionURL, tfURL.getText());
		}
		thread = new ObexClientConnectionThread(tfURL.getText(), tfData.getText());
		thread.setDaemon(true);
		thread.start();
		btnDisconnect.setEnabled(true);
		btnSend.setEnabled(false);
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
