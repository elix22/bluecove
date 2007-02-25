package com.intel.bluetooth.test;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import com.intel.bluetooth.BluetoothRFCOMMConnection;

public class SerialTerminal extends JFrame implements DocumentListener {
	JTextArea		tRecArea, tSenArea;
	OutputStream	out;
	
	public		SerialTerminal(BluetoothRFCOMMConnection	aConnection) {
		
		JLabel			aSendLable = new JLabel("Send:");
		JLabel			aRecvLable = new JLabel("Receive:");
		Container		contentPane = getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
		tSenArea = new JTextArea();
		tRecArea = new JTextArea();
		tRecArea.setEditable(false);
		
		JScrollPane		Rview = new JScrollPane(tRecArea);
		JScrollPane		Sview = new JScrollPane(tSenArea);
		tSenArea.getDocument().addDocumentListener(this);
		Rview.setSize(500, 300);
		Rview.setPreferredSize(new Dimension(500,300));
		Rview.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		Sview.setSize(500, 300);
		Sview.setPreferredSize(new Dimension(500,300));
		Sview.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		contentPane.add(aRecvLable);
		contentPane.add(Rview);
		contentPane.add(aSendLable);
		contentPane.add(Sview);
		
		this.setLocationRelativeTo(null);
		tRecArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		tSenArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		try {
		InputStream		inStream = aConnection.openInputStream();
		out = aConnection.openOutputStream();
		new Listener(inStream).start();
		} catch (Exception exp) {
			exp.printStackTrace();
		}
		pack();
		setVisible(true);
	}
	public void insertUpdate(DocumentEvent e) {
		int		start, len;
		Document		aDoc;
		String			insert = null;
		
		len = e.getLength();
		start = e.getOffset();
		aDoc = e.getDocument();
		try {
			insert = aDoc.getText(start, len);
			if(insert != null) {
				byte[]		someBytes = insert.getBytes();
				out.write(someBytes);
			}
		} catch (Exception exp ) {
			exp.printStackTrace();
		}
		
	}
	public void removeUpdate(DocumentEvent e) {
		
	}
	public void changedUpdate(DocumentEvent e) {
		
	}
	public class Listener extends Thread {
		InputStream		myStream;
		Listener(InputStream in) {
			myStream = in;
			setName("A Serial Terminal Listener");
		}
		public void run() {
			while(true) {
				byte[]		aByte = new byte[1];
				try {
					aByte[0] = (byte) myStream.read();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String	aString = new String(aByte);
				tRecArea.append(aString);
			}
		}
		
		
	}
	
}
