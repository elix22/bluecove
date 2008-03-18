/*
 * @(#)EditorGraph.java 3.3 23-APR-04
 *  
 * Copyright (c) 2001-2005, Gaudenz Alder
 * 
 * See LICENSE file in distribution for licensing details of this source file
 */
package com.bluecove.emu.gui;

import org.jgraph.JGraph;
import org.jgraph.graph.GraphModel;

public class GraphPane extends JGraph {

	private static final long serialVersionUID = 1L;

	/** 
	* Constructs a EditorGraph for <code>model</code>. 
	*/
	public GraphPane(GraphModel model) {
		super(model);
	}

}
