package com.bluecove.emu.gui.graph;

import java.util.HashMap;

import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.GraphModel;

public class GraphPane extends JGraph {

	private static final long serialVersionUID = 1L;

	public GraphPane(GraphModel model) {
		super(model);
		
		getGraphLayoutCache().setFactory(new DeviceCellViewFactory());
		
		DeviceCell dev1 = new DeviceCell("dev1");
		insertDevice(dev1);
		DeviceCell dev2 = new DeviceCell("dev2dev2dev2[123456789012]");
		insertDevice(dev2);
		DeviceCell dev3 = new DeviceCell("dev3");
		insertDevice(dev3);
		
		ConnectionEdge connection1 = new ConnectionEdge("", dev1, dev2);
		insertConnection(connection1);
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
