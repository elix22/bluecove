package javax.obex;

import java.io.IOException;

import javax.microedition.io.ContentConnection;

/**
 * 
 * The <code>Operation</code> interface provides ways to manipulate a single
 * OBEX PUT or GET operation.  The implementation of this interface sends
 * OBEX packets as they are built.  If during the operation the peer in the
 * operation ends the operation, an <code>IOException</code> is thrown on
 * the next read from the input stream, write to the output stream, or call to
 * <code>sendHeaders()</code>.
 * <P>
 * <STRONG>Definition of methods inherited from <code>ContentConnection</code></STRONG>
 * <P>
 * <code>getEncoding()</code> will always return <code>null</code>.
 * <BR><code>getLength()</code> will return the length specified by the OBEX Length
 * header or -1 if the OBEX Length header was not included.
 * <BR><code>getType()</code> will return the value specified in the OBEX Type
 * header or <code>null</code> if the OBEX Type header was not included.<BR>
 * <P>
 * <STRONG>How Headers are Handled</STRONG>
 * <P>
 * As headers are received, they may be retrieved through the
 * <code>getReceivedHeaders()</code> method.  If new headers are set during the
 * operation, the new headers will be sent during the next packet exchange.
 * <P>
 * <STRONG>PUT example</STRONG>
 * <P>
 * <PRE>
 * void putObjectViaOBEX(ClientSession conn, HeaderSet head, byte[] obj)
 *    throws IOException {
 * 
 *    // Include the length header
 *    head.setHeader(head.LENGTH, new Long(obj.length));
 * 
 *   // Initiate the PUT request
 *   Operation op = conn.put(head);
 * 
 *   // Open the output stream to put the object to it
 *   DataOutputStream out = op.openDataOutputStream();
 * 
 *   // Send the object to the server
 *   out.write(obj);
 * 
 *   // End the transaction
 *   out.close();
 *   op.close();
 * }
 * </PRE>
 * <P>
 * <STRONG>GET example</STRONG>
 * <P>
 * <PRE>
 * byte[] getObjectViaOBEX(ClientSession conn, HeaderSet head) throws IOException {
 * 
 *   // Send the initial GET request to the server
 *   Operation op = conn.get(head);
 * 
 *   // Retrieve the length of the object being sent back
 *   int length = op.getLength();
 * 
 *    // Create space for the object
 *    byte[] obj = new byte[length];
 * 
 *   // Get the object from the input stream
 *   DataInputStream in = trans.openDataInputStream();
 *   in.read(obj);
 * 
 *   // End the transaction
 *   in.close();
 *   op.close();
 * 
 *   return obj;
 * }
 * </PRE>
 * <H3>Client PUT Operation Flow</H3>
 * For PUT operations, a call to <code>close()</code> the <code>OutputStream</code>
 * returned from <code>openOutputStream()</code> or <code>openDataOutputStream()</code>
 * will signal that the request is done.  (In OBEX terms, the End-Of-Body header should
 * be sent and the final bit in the request will be set.)  At this point, the
 * reply from the server may begin to be processed.  A call to
 * <code>getResponseCode()</code> will do an implicit close on the
 * <code>OutputStream</code> and therefore signal that the request is done.
 * <H3>Client GET Operation Flow</H3>
 * For GET operation, a call to <code>openInputStream()</code> or
 * <code>openDataInputStream()</code> will signal that the request is done.  (In OBEX
 * terms, the final bit in the request will be set.)  A call to
 * <code>getResponseCode()</code> will cause an implicit close on the
 * <code>InputStream</code>.  No further data may be read at this point.
 *
 */
public interface Operation extends ContentConnection {

	/**
	 * Sends an ABORT message to the server. By calling this method, the corresponding 
	 * input and output streams will be closed along with this object. No headers are 
	 * sent in the abort request. This will end the operation since {@link #close()} 
	 * will be called by this method.
	 * 
	 * @throws IOException if the transaction has already ended or if an OBEX server 
	 * 				calls this method
	 */
	public void abort() throws IOException;
	
	/**
	 * Returns the headers that have been received during the operation. Modifying the 
	 * object returned has no effect on the headers that are sent or retrieved.
	 * 
	 * @return the headers received during this {@code Operation}
	 * @throws IOException if this {@code Operation} has been closed
	 */
	public HeaderSet getRecievedHeaders() throws IOException;
	
	/**
	 * Specifies the headers that should be sent in the next OBEX message that is sent.
	 * 
	 * @param headers the headers to send in the next message
	 * @throws IOException  if this {@code Operation} has been closed or the transaction 
	 * 					has ended and no further messages will be exchanged
	 * @throws java.lang.IllegalArgumentException if {@code headers} was not created by a call 
	 * 					to {@link ServerRequestHandler#createHeaderSet()} or 
	 * 						{@link ClientSession#createHeaderSet()}
	 * @throws java.lang.NullPointerException if {@code headers} if {@code null}
	 */
	public void sendHeaders(HeaderSet headers) throws IOException;
	
	/**
	 * Returns the response code received from the server. Response codes are defined in 
	 * the {@link ResponseCodes} class.
	 * 
	 * @return the response code retrieved from the server
	 * @throws IOException  if an error occurred in the transport layer during the 
	 * 						transaction; if this object was created by an OBEX server
	 * @see ResponseCodes
	 */
	public int getResponseCode() throws IOException;
	
}
