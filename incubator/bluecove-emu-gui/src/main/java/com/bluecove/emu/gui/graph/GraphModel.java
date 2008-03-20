package com.bluecove.emu.gui.graph;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.Hashtable;
import java.util.Map;

import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;

public class GraphModel extends DefaultGraphModel {

	private static final long serialVersionUID = 1L;

	public GraphModel() {
		super();
	}

	public void updateModel2() {
		ConnectionSet cs = new ConnectionSet();
		Map attributes = new Hashtable();
		// Styles For Implement/Extend/Aggregation
		AttributeMap implementStyle = new AttributeMap();
		GraphConstants.setLineBegin(implementStyle,
				GraphConstants.ARROW_TECHNICAL);
		GraphConstants.setBeginSize(implementStyle, 10);
		GraphConstants.setDashPattern(implementStyle, new float[] { 3, 3 });
		if (GraphConstants.DEFAULTFONT != null) {
			GraphConstants.setFont(implementStyle, GraphConstants.DEFAULTFONT
					.deriveFont(10));
		}
		AttributeMap extendStyle = new AttributeMap();
		GraphConstants
				.setLineBegin(extendStyle, GraphConstants.ARROW_TECHNICAL);
		GraphConstants.setBeginFill(extendStyle, true);
		GraphConstants.setBeginSize(extendStyle, 10);
		if (GraphConstants.DEFAULTFONT != null) {
			GraphConstants.setFont(extendStyle, GraphConstants.DEFAULTFONT
					.deriveFont(10));
		}
		AttributeMap aggregateStyle = new AttributeMap();
		GraphConstants.setLineBegin(aggregateStyle,
				GraphConstants.ARROW_DIAMOND);
		GraphConstants.setBeginFill(aggregateStyle, true);
		GraphConstants.setBeginSize(aggregateStyle, 6);
		GraphConstants.setLineEnd(aggregateStyle, GraphConstants.ARROW_SIMPLE);
		GraphConstants.setEndSize(aggregateStyle, 8);
		GraphConstants.setLabelPosition(aggregateStyle, new Point2D.Double(500,
				0));
		if (GraphConstants.DEFAULTFONT != null) {
			GraphConstants.setFont(aggregateStyle, GraphConstants.DEFAULTFONT
					.deriveFont(10));
		}
		//
		// The Swing MVC Pattern
		//
		// Model Column
		DefaultGraphCell gm = new DefaultGraphCell("GraphModel");
		attributes.put(gm,
				JGraph.createBounds(new AttributeMap(), 20, 100, Color.blue));
		gm.addPort(null, "GraphModel/Center");
		DefaultGraphCell dgm = new DefaultGraphCell("DefaultGraphModel");
		attributes.put(dgm, JGraph.createBounds(new AttributeMap(), 20, 180,
				Color.blue));
		dgm.addPort(null, "DefaultGraphModel/Center");
		DefaultEdge dgmImplementsGm = new DefaultEdge("implements");
		cs.connect(dgmImplementsGm, gm.getChildAt(0), dgm.getChildAt(0));
		attributes.put(dgmImplementsGm, implementStyle);
		DefaultGraphCell modelGroup = new DefaultGraphCell("ModelGroup");
		modelGroup.add(gm);
		modelGroup.add(dgm);
		modelGroup.add(dgmImplementsGm);
		// JComponent Column
		DefaultGraphCell jc = new DefaultGraphCell("JComponent");
		attributes.put(jc, JGraph.createBounds(new AttributeMap(), 180, 20,
				Color.green));
		jc.addPort(null, "JComponent/Center");
		DefaultGraphCell jg = new DefaultGraphCell("JGraph");
		attributes.put(jg, JGraph.createBounds(new AttributeMap(), 180, 100,
				Color.green));
		jg.addPort(null, "JGraph/Center");
		DefaultEdge jgExtendsJc = new DefaultEdge("extends");
		cs.connect(jgExtendsJc, jc.getChildAt(0), jg.getChildAt(0));
		attributes.put(jgExtendsJc, extendStyle);
		// UI Column
		DefaultGraphCell cu = new DefaultGraphCell("ComponentUI");
		attributes
				.put(cu, JGraph.createBounds(new AttributeMap(), 340, 20, Color.red));
		cu.addPort(null, "ComponentUI/Center");
		DefaultGraphCell gu = new DefaultGraphCell("GraphUI");
		attributes.put(gu,
				JGraph.createBounds(new AttributeMap(), 340, 100, Color.red));
		gu.addPort(null, "GraphUI/Center");
		DefaultGraphCell dgu = new DefaultGraphCell("BasicGraphUI");
		attributes.put(dgu, JGraph.createBounds(new AttributeMap(), 340, 180,
				Color.red));
		dgu.addPort(null, "BasicGraphUI/Center");
		DefaultEdge guExtendsCu = new DefaultEdge("extends");
		cs.connect(guExtendsCu, cu.getChildAt(0), gu.getChildAt(0));
		attributes.put(guExtendsCu, extendStyle);
		DefaultEdge dguImplementsDu = new DefaultEdge("implements");
		cs.connect(dguImplementsDu, gu.getChildAt(0), dgu.getChildAt(0));
		attributes.put(dguImplementsDu, implementStyle);
		DefaultGraphCell uiGroup = new DefaultGraphCell("UIGroup");
		uiGroup.add(cu);
		uiGroup.add(gu);
		uiGroup.add(dgu);
		uiGroup.add(dguImplementsDu);
		uiGroup.add(guExtendsCu);
		// Aggregations
		DefaultEdge jgAggregatesGm = new DefaultEdge("model");
		cs.connect(jgAggregatesGm, jg.getChildAt(0), gm.getChildAt(0));
		attributes.put(jgAggregatesGm, aggregateStyle);
		DefaultEdge jcAggregatesCu = new DefaultEdge("ui");
		cs.connect(jcAggregatesCu, jc.getChildAt(0), cu.getChildAt(0));
		attributes.put(jcAggregatesCu, aggregateStyle);
		// Insert Cells into model
		Object[] cells = new Object[] { jgAggregatesGm, jcAggregatesCu,
				modelGroup, jc, jg, jgExtendsJc, uiGroup };
		insert(cells, attributes, cs, null, null);	
	}
	
}
