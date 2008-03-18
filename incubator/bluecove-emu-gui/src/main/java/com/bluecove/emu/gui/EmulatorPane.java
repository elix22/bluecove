package com.bluecove.emu.gui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

public class EmulatorPane extends JPanel {

	private static final long serialVersionUID = 1L;

	private GraphPane graphPane;

	private GraphModel graphModel;

	private JPanel detailsPane;

	private JPanel connectionsPane;

	public EmulatorPane() {
		super();
		setLayout(new BorderLayout());
		
		graphModel = new GraphModel();
		
		graphPane = new GraphPane(graphModel);
		detailsPane = new JPanel();
		detailsPane.add(new JLabel("DETAILS"));
		connectionsPane = new JPanel();
		connectionsPane.add(new JLabel("CONNECTIONS"));
		
		
		
		JPanel upperPanel = new JPanel();
		upperPanel.setLayout(new BorderLayout());
		
		JSplitPane horisontalSplit = createSplitPane(
				graphPane, detailsPane, JSplitPane.HORIZONTAL_SPLIT);
		upperPanel.add(horisontalSplit, BorderLayout.CENTER);
		
		JSplitPane verticalSplit = createSplitPane(
				upperPanel, connectionsPane, JSplitPane.VERTICAL_SPLIT);
		add(verticalSplit);
	}

	
	
	public JSplitPane createSplitPane(Component first, Component second,
			int orientation) {
		JSplitPane splitPane = new JSplitPane(orientation, first, second);
		splitPane.setBorder(null);
		splitPane.setFocusable(false);
		splitPane.setOneTouchExpandable(true);
		splitPane.setResizeWeight(0.2);
		return splitPane;
	}

}
