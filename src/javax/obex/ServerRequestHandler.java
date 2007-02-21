package javax.obex;
/** 
 * The <code>ServerRequestHandler</code> class defines an event
 * listener that will respond to OBEX requests made to the server.
 * <P>
 * The <code>onConnect()</code>, <code>onSetPath()</code>, <code>onDelete()</code>,
 * <code>onGet()</code>,
 * and <code>onPut()</code> methods may return any response code defined
 * in the <code>ResponseCodes</code> class except for
 * <code>OBEX_HTTP_CONTINUE</code>.  If <code>OBEX_HTTP_CONTINUE</code> or
 * a value not defined in the <code>ResponseCodes</code> class is returned,
 * the server implementation will send an <code>OBEX_HTTP_INTERNAL_ERROR</code>
 * response to the client.
 * <P>
 * <STRONG>Connection ID and Target Headers</STRONG>
 * <P>
 * According to the IrOBEX specification, a packet may not contain a Connection
 * ID and Target header.  Since the Connection ID header is managed by the
 * implementation, it will not send a Connection ID header, if a Connection ID
 * was specified, in a packet that has a Target header.  In other words, if an
 * application adds a Target header to a <code>HeaderSet</code> object used
 * in an OBEX operation and a Connection ID was specified, no Connection ID
 * will be sent in the packet containing the Target header.
 * <P>
 * <STRONG>CREATE-EMPTY Requests</STRONG>
 * <P>
 * A CREATE-EMPTY request allows clients to create empty objects on the server.
 * When a CREATE-EMPTY request is received, the <code>onPut()</code> method
 * will be called by the implementation.  To differentiate between a normal
 * PUT request and a CREATE-EMPTY request, an application must open the
 * <code>InputStream</code> from the <code>Operation</code> object passed
 * to the <code>onPut()</code> method.  For a PUT request, the application
 * will be able to read Body data from this <code>InputStream</code>.  For
 * a CREATE-EMPTY request, there will be no Body data to read.  Therefore,
 * a call to <code>InputStream.read()</code> will return -1.
 */
 
public class ServerRequestHandler {

	/**
	 * Creates a ServerRequestHandler.
	 *
	 */
	protected ServerRequestHandler() {
		
	}
	/**
	 * Creates a {@link HeaderSet} object that may be used in put and get operations.
	 * 
	 * @return the {@link HeaderSet} object to use in put and get operations.
	 */
	public final HeaderSet createHeaderSet(){
		return null;
	}
	

}
