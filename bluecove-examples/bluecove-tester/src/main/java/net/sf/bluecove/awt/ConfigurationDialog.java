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
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Vector;

import javax.bluetooth.UUID;

import net.sf.bluecove.Configuration;
import net.sf.bluecove.Logger;

/**
 * @author vlads
 *
 */
public class ConfigurationDialog extends OkCancelDialog {

	private static final long serialVersionUID = 1L;
	
	Panel panelItems;
	
	Vector configItems = new Vector(); 
	
	private class ConfigurationComponent {
		
		String name;
		
		Component guiComponent;
		
		Field configField;
		
	}

	
	public ConfigurationDialog(Frame owner) {
		super(owner, "Configuration", true);

		panelItems = new BorderPanel();
		this.add(panelItems, BorderLayout.NORTH);
		
		addConfig("deviceClassFilter");
		addConfig("discoverDevicesComputers");
		addConfig("discoverDevicesPhones");
		addConfig("discoveryUUID");
		
		addConfig("clientContinuous");
		addConfig("clientContinuousDiscovery");
		addConfig("clientContinuousDiscoveryDevices");
		addConfig("clientContinuousServicesSearch");

		addConfig("TEST_CASE_FIRST");
		addConfig("TEST_CASE_LAST");
		addConfig("STERSS_TEST_CASE");
		addConfig("clientSleepBetweenConnections");
		addConfig("serverSleepB4ClosingConnection");
		addConfig("testServiceAttributes");
		addConfig("testIgnoreNotWorkingServiceAttributes");
		addConfig("testAllServiceAttributes");

		panelItems.setLayout(new GridLayout(configItems.size(), 2));
		
		updateGUI();
		
		this.pack();
		Rectangle b = owner.getBounds();
		this.setLocation(b.x + (int)((b.getWidth() - this.getWidth())/2), b.y + 60);
	}
	
	protected void onClose(boolean isCancel) {
		if (!isCancel) {
			updateConfig();
		}
		setVisible(false);
	}
	
	private void updateGUI() {
		for (Iterator iter = configItems.iterator(); iter.hasNext();) {
			ConfigurationComponent cc = (ConfigurationComponent) iter.next();
			Class type = cc.configField.getType();
			
			try {
				if (type.equals(boolean.class)) {
					Checkbox c = (Checkbox) cc.guiComponent;
					c.setState(cc.configField.getBoolean(Configuration.class));
				} else if (type.equals(UUID.class)) {
					TextField tf = (TextField) cc.guiComponent;
					tf.setText(cc.configField.get(Configuration.class).toString());
				} else if (type.equals(String.class)) {
					TextField tf = (TextField) cc.guiComponent;
					tf.setText(cc.configField.get(Configuration.class).toString());
				} else if (type.equals(int.class)) {
					TextField tf = (TextField) cc.guiComponent;
					tf.setText(String.valueOf(cc.configField.getInt(Configuration.class)));
				}
			} catch (Throwable e) {
				Logger.error("internal error for " + cc.name, e);
			}
		}
	}
	
	private void updateConfig() {
		for (Iterator iter = configItems.iterator(); iter.hasNext();) {
			ConfigurationComponent cc = (ConfigurationComponent) iter.next();
			Class type = cc.configField.getType();
			try {
				if (type.equals(boolean.class)) {
					Checkbox c = (Checkbox) cc.guiComponent;
					cc.configField.setBoolean(Configuration.class, c.getState());
				} else if (type.equals(String.class)) {
					TextField tf = (TextField) cc.guiComponent;
					cc.configField.set(Configuration.class, tf.getText());
				} else if (type.equals(int.class)) {
					TextField tf = (TextField) cc.guiComponent;
					cc.configField.setInt(Configuration.class, Integer.valueOf(tf.getText()).intValue());
				} else if (type.equals(UUID.class)) {
					TextField tf = (TextField) cc.guiComponent;
					UUID uuid = new UUID(tf.getText(), false);
					cc.configField.set(Configuration.class, uuid); 
				}
			} catch (Throwable e) {
				Logger.error("internal error for " + cc.name, e);
			}
		}
	}
	
	private void addConfig(String name) {
		final ConfigurationComponent cc = new ConfigurationComponent();
		cc.name = name;
		
		try {
			cc.configField = Configuration.class.getDeclaredField(name);
			
		} catch (Throwable e) {
			Logger.error("internal error for " + name, e);
			return;
		}
		
		Class type = cc.configField.getType();
		
		if (type.equals(boolean.class)) {
			Checkbox c = new Checkbox();
			cc.guiComponent = c;
		} else if ((type.equals(String.class)) || (type.equals(UUID.class)) || (type.equals(int.class))) {
			TextField tf = new TextField(); 
			cc.guiComponent = tf; 
		} else {
			Logger.error("internal error for " + name + " unsupported class " + type.getName());
			return;
		}

		Label l = new Label(name);
		panelItems.add(l);
		panelItems.add(cc.guiComponent);
		
		l.addMouseListener(new MouseAdapter() {
			Component guiComponent = cc.guiComponent;
			public void mouseClicked(MouseEvent e) {
				guiComponent.requestFocus();
			}
		});
		
		configItems.addElement(cc);
	}


}
