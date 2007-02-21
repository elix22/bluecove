package javax.obex;
/**
 * 
 * This class holds user name and password combinations.
 *
 */
public class PasswordAuthentication extends Object {

	private		byte[]	password, userName;
	
	/**
	 * Creates a new {@code PasswordAuthentication} with the user name 
	 * and password provided.
	 * 
	 * @param userName the user name to include; this may be {@code null}
	 * @param password the password to include in the response
	 * @throws java.lang.NullPointerException if {@code password} is {@code null}
	 */
	public PasswordAuthentication(byte[] userName, byte[] password)
			throws NullPointerException {
		if(password == null) 
			throw new NullPointerException("PasswordAuthentication requires a non-null password");
		this.password = password;
		this.userName = userName;
	}
	
	/**
	 * Retrieves the password.
	 * 
	 * @return the password
	 */
	public byte[]	getPassword(){
		return password;
	}
	
	/**
	 * Retrieves the user name that was specified in the constructor. 
	 * The user name may be {@code null}.
	 * 
	 * @return the user name
	 */
	public byte[]	getUserName(){
		return userName;
	}
}
