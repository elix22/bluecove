package net.sf.bluecove;

public class Logger {

	public static void info(String message) {
		System.out.println(message);
	}
	
	public static void error(String message, Throwable t) {
		System.out.println("error " + message + " " + t);
		t.printStackTrace(System.out);
	}
}
