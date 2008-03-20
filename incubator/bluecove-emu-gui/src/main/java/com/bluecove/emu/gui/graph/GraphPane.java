package com.bluecove.emu.gui.graph;

import java.util.HashMap;

import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.GraphModel;

public class GraphPane extends JGraph {

	private static final long serialVersionUID = 1L;

	public static final int PANE_HEIGHT = 400;
	public static final int PANE_WIDTH = 400;

	
	public GraphPane(GraphModel model) {
		super(model);
		
		setSize(PANE_WIDTH, PANE_HEIGHT);
		
		getGraphLayoutCache().setFactory(new DeviceCellViewFactory());
		
		DeviceCell dev1 = new DeviceCell("dev1");
		insertDevice(dev1);
		DeviceCell dev2 = new DeviceCell("dev2");
		insertDevice(dev2);
		DeviceCell dev3 = new DeviceCell("dev3");
		insertDevice(dev3);
		DeviceCell dev4 = new DeviceCell("dev4");
		insertDevice(dev4);	
		DeviceCell dev5 = new DeviceCell("dev5");
		insertDevice(dev5);
		DeviceCell dev6 = new DeviceCell("dev6");
		insertDevice(dev6);
		DeviceCell dev7 = new DeviceCell("dev7");
		insertDevice(dev7);
		DeviceCell dev8 = new DeviceCell("dev8");
		insertDevice(dev8);
		
		ConnectionEdge connection1 = new ConnectionEdge("", dev1, dev2);
		insertConnection(connection1);
		ConnectionEdge connection2 = new ConnectionEdge("", dev1, dev3);
		insertConnection(connection2);
		ConnectionEdge connection3 = new ConnectionEdge("", dev4, dev5);
		insertConnection(connection3);
	}

	
	public void insertDevice(DeviceCell device) {
		device.beforeInsert();
		HashMap<DeviceCell, AttributeMap> at = new HashMap<DeviceCell, AttributeMap>();
		at.put(device, device.getAttributes());
		getGraphLayoutCache().insert(new Object[] { device }, at, null,
				null, null);
	}

	public void removeDevice(DeviceCell device) {
		device.afterRemove();
		getGraphLayoutCache().remove(new Object[] { device });

	}
	
	public void insertConnection(ConnectionEdge connection) {
		HashMap<ConnectionEdge, AttributeMap> at = new HashMap<ConnectionEdge, AttributeMap>();
		at.put(connection, connection.getAttributes());
		getGraphLayoutCache().insert(new Object[] { connection }, at, connection.getConnectionSet(),
				null, null);
	}

	public void removeConnection(ConnectionEdge connection) {
		getGraphLayoutCache().remove(new Object[] { connection });

	}
}
