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
package net.sf.bluecove.obex;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;


/**
 * @author vlads
 *
 */
public class Main extends JFrame implements ActionListener {
	
	private static final long serialVersionUID = 1L;

	private static final int BLUETOOTH_DISCOVERY_STD_SEC = 11;
	
	private JLabel iconLabel;
	
	private String status;
	
	JProgressBar progressBar;
	
	private JComboBox cbDevices;
	
	private JButton btFindDevice;
	
	private JButton btSend;
	 
	private JButton btCancel;
	
	private BluetoothInquirer bluetoothInquirer;
	
	private Hashtable devices = new Hashtable(); 
	
	private String fileName;
	
	private byte[] data;
	
	protected Main() {
		super("BlueCove OBEX Push");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setIconImage(Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/icon.png")));
		
		JPanel contentPane = (JPanel)this.getContentPane();
		contentPane.setLayout(new BorderLayout(10, 10));
		contentPane.setBorder(new EmptyBorder(10,10,10,10));
		
		JPanel progressPanel = new JPanel();
		progressPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		iconLabel = new JLabel();
		iconLabel.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/icon.png"))));
		c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
		progressPanel.add(iconLabel, c);
		
		progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        
        c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
        progressPanel.add(progressBar, c);
        
	    getContentPane().add(progressPanel, BorderLayout.NORTH);
	    
		JPanel optionsPanel = new JPanel();

		JLabel deviceLabel = new JLabel("Send to:");
		optionsPanel.add(deviceLabel);
		cbDevices = new JComboBox();
		cbDevices.addItem("{no device found}");
		cbDevices.setEnabled(false);
		optionsPanel.add(cbDevices);
		optionsPanel.add(btFindDevice = new JButton("Find"));
		btFindDevice.addActionListener(this);
		
	    getContentPane().add(optionsPanel, BorderLayout.CENTER);

	    JPanel actionPanel = new JPanel();
	    actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.LINE_AXIS));
	    actionPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
	    actionPanel.add(Box.createHorizontalGlue());
	    actionPanel.add(btSend = new JButton("Send"));
	    btSend.addActionListener(this);
	    actionPanel.add(Box.createRigidArea(new Dimension(10, 0)));
	    actionPanel.add(btCancel = new JButton("Cancel"));
	    btCancel.addActionListener(this);

    	contentPane.add(actionPanel, BorderLayout.SOUTH);
	    btSend.setEnabled(false);
	}
	
	private static void createAndShowGUI(final String[] args) {
		final Main app = new Main();
		app.pack();
		app.center();
		app.setVisible(true);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (app.initializeBlueCove()) {
					if (args.length != 0) {
						app.downloadJar(args[0]);
					}
				}
			}
		});
	}
	
	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI(args);
			}
		});
	}

	private void center() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation(((screenSize.width - this.getWidth()) / 2), ((screenSize.height - this.getHeight()) / 2));
	}
	
	protected void setStatus(final String message) {
		status = message;
		progressBar.setString(message);
	}
	
	void setProgressValue(int n) {
		progressBar.setValue(n);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progressBar.setString(status);
			}
		});
	}
	
	protected void disabledBluetooth() {
		btFindDevice.setEnabled(false);
		cbDevices.setEnabled(false);
		setStatus("BlueCove not avalable");
		btSend.setEnabled(false);
	}
	
	protected boolean initializeBlueCove() {
		try {
			LocalDevice localDevice = LocalDevice.getLocalDevice();
			if ("000000000000".equals(localDevice.getBluetoothAddress())) {
				throw new Exception();
			}
			bluetoothInquirer = new BluetoothInquirer(this);
			setStatus("BlueCove Ready");
			return true;
		} catch (Throwable e) {
			debug(e);
			disabledBluetooth();
			return false;
		}
	}
	
	static void debug(Throwable e) {
		System.out.println(e.getMessage());
		e.printStackTrace();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btFindDevice) {
			bluetoothDiscovery();
		} else if (e.getSource() == btCancel) {
			shutdown();
			System.exit(0);
		} else if (e.getSource() == btSend) {
			obexSend();
		}
		
	}


	private class DiscoveryTimerListener implements ActionListener {
		int seconds = 0;
		public void actionPerformed(ActionEvent e) {
			if (seconds < BLUETOOTH_DISCOVERY_STD_SEC) {
				seconds ++;
				setProgressValue(seconds);
			}
		}
	}
	
	private static class DeviceInfo {
		String btAddress; 
		String name; 
		String obexUrl;
		
		public String toString() {
			if ((name != null) && (name.length() > 0)) {
				return name;		
			} else {
				return btAddress;
			}
		}
	}

	private void addDevice(String btAddress, String name, String obexUrl) {
		DeviceInfo di = new DeviceInfo();
		di.btAddress = btAddress;
		di.name = name;
		di.obexUrl = obexUrl; 
		devices.put(btAddress.toLowerCase(), di);
	}

	private void updateDevices() {
		cbDevices.removeAllItems();
		if (devices.size() == 0) {
			cbDevices.addItem("{no device found}");
			btSend.setEnabled(false);
			cbDevices.setEnabled(false);
		} else {
			for (Enumeration i = devices.keys(); i.hasMoreElements();) {
				String addr = (String) i.nextElement();
				DeviceInfo di = (DeviceInfo)devices.get(addr);
				cbDevices.addItem(di);		
			}
			cbDevices.setEnabled(true);
			btSend.setEnabled(true);
		}
	}

	private void bluetoothDiscovery() {
		final Timer timer = new Timer(1000, new DiscoveryTimerListener());
		progressBar.setMaximum(BLUETOOTH_DISCOVERY_STD_SEC);
		setProgressValue(0);
		Thread t = new Thread() {
			public void run() {
				if (bluetoothInquirer.startInquiry()) {
					setStatus("Bluetooth discovery started");
					timer.start();
					while (bluetoothInquirer.inquiring) {
						try {
							Thread.sleep(1000);
						} catch (Exception e) {
						}
					}
					timer.stop();
					//setStatus("Bluetooth discovery finished");
					
					setProgressValue(0);
					progressBar.setMaximum(bluetoothInquirer.devices.size());
					for (Iterator iter = bluetoothInquirer.devices.iterator(); iter.hasNext();) {
						RemoteDevice dev = (RemoteDevice) iter.next();
						String obexUrl = bluetoothInquirer.findOBEX(dev.getBluetoothAddress());
						if (obexUrl != null){ 
							addDevice(dev.getBluetoothAddress(), BluetoothInquirer.getFriendlyName(dev), obexUrl);
						}
					}
					setProgressValue(0);
					updateDevices();
				}
			}
		};
		t.start();
	}
	
	private void obexSend() {
		if (fileName == null) {
			setStatus("No file selected");
			return;
		}
		final ObexBluetoothClient o = new ObexBluetoothClient(this, fileName, data);
		final String serverURL = ((DeviceInfo)cbDevices.getSelectedItem()).obexUrl;
		Thread t = new Thread() {
			public void run() {
				o.obexPut(serverURL);
			}
		};
		t.start();
	}

	private static String simpleFileName(String filePath) {
		int idx = filePath.lastIndexOf('/');
		if (idx == -1) {
			idx = filePath.lastIndexOf('\\');
		}
		if (idx == -1) {
			return filePath;
		}
		return filePath.substring(idx + 1);
	}
	
	private void downloadJar(final String filePath) {
		Thread t = new Thread() {
			public void run() {
				try {
					URL url = new URL(filePath);
					InputStream is = url.openConnection().getInputStream();  
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					byte[] buffer = new byte[0xFF];
					int i = is.read(buffer);
					int done = 0;
					while (i != -1) {
						bos.write(buffer, 0, i);
						done += i;
						//setProgressValue(done);
						i = is.read(buffer);
					}
					data = bos.toByteArray();
					fileName = simpleFileName(url.getFile());
					setStatus((data.length/1024) +"k " + fileName);
				} catch (Throwable e) {
					debug(e);
					setStatus("Download error" +  e.getMessage());
				}
			}
		};
		t.start();
		
	}

	private void shutdown() {
		if (bluetoothInquirer != null) {
			bluetoothInquirer.shutdown();
			bluetoothInquirer = null;
		}
	}
}
