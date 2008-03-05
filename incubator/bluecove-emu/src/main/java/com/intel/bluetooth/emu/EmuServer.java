package com.intel.bluetooth.emu;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.pyx4j.rpcoverhttp.server.HTTPServer;

public class EmuServer extends HTTPServer {

	private final static Logger logger = Logger.getLogger(EmuServer.class);

	public EmuServer() throws Exception {
		super();
	}

	public static void main(String[] args) throws Exception {
		try {
			new EmuServer().start();
		} catch (IOException ioe) {
			logger.error("Couldn't start server:", ioe);
			System.exit(-1);
		}
	}

}
