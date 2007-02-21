package javax.obex;
/**
 * 
 * The <code>ResponseCodes</code> class contains the list of valid
 * response codes a server may send to a client.
 * <P>
 * <STRONG>IMPORTANT NOTE</STRONG>
 * <P>
 * It is important to note that these values are different then those defined
 * in <code>javax.microedition.io.HttpConnection</code>.  The values in this
 * interface represent the values defined in the IrOBEX specification.  The
 * values in <code>javax.microedition.io.HttpConnection</code> represent values
 * defined in the HTTP specification.
 * <P>
 * <code>OBEX_DATABASE_FULL</code> and <code>OBEX_DATABASE_LOCKED</code> require
 * further description since they are not defined in HTTP.  The server will send
 * an <code>OBEX_DATABASE_FULL</code> message when the client requests that
 * something be placed into a database but the database is full (cannot take
 * more data).   <code>OBEX_DATABASE_LOCKED</code> will be returned when the
 * client wishes to access a database, database table, or database record that
 * has been locked.
 * <P>
 *
 */
public class ResponseCodes {

	/**
	 * Defines the OBEX_HTTP_OK response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_OK} is 0xA0 (160).
	 */
	public static final int OBEX_HTTP_OK					= 160;

	/**
	 * Defines the OBEX_HTTP_CREATED response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_CREATED} is 0xA1 (161).
	 */
	public static final int OBEX_HTTP_CREATED				= 161;

	/**
	 * Defines the OBEX_HTTP_ACCEPTED response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_ACCEPTED} is 0xA2 (162).
	 */
	public static final int OBEX_HTTP_ACCEPTED				= 162;

	/**
	 * Defines the OBEX_HTTP_NOT_AUTHORITATIVE response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_NOT_AUTHORITATIVE} is 0xA3 (163).
	 */
	public static final int OBEX_HTTP_NOT_AUTHORITATIVE		= 163;

	/**
	 * Defines the OBEX_HTTP_NO_CONTENT response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_NO_CONTENT} is 0xA4 (164).
	 */
	public static final int OBEX_HTTP_NO_CONTENT			= 164;

	/**
	 * Defines the OBEX_HTTP_RESET response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_RESET} is 0xA5 (165).
	 */
	public static final int OBEX_HTTP_RESET					= 165;

	/**
	 * Defines the OBEX_HTTP_PARTIAL response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_PARTIAL} is 0xA6 (166).
	 */
	public static final int OBEX_HTTP_PARTIAL				= 166;

	/**
	 * Defines the OBEX_HTTP_MULT_CHOICE response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_MULT_CHOICE} is 0xB0 (176).
	 */
	public static final int OBEX_HTTP_MULT_CHOICE			= 176;

	/**
	 * Defines the OBEX_HTTP_MOVED_PERM response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_MOVED_PERM} is 0xB1 (177).
	 */
	public static final int OBEX_HTTP_MOVED_PERM			= 177;

	/**
	 * Defines the OBEX_HTTP_MOVED_TEMP response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_MOVED_TEMP} is 0xB2 (178).
	 */
	public static final int OBEX_HTTP_MOVED_TEMP			= 178;

	/**
	 * Defines the OBEX_HTTP_SEE_OTHER response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_SEE_OTHER} is 0xB3 (179).
	 */
	public static final int OBEX_HTTP_SEE_OTHER				= 179;

	/**
	 * Defines the OBEX_HTTP_NOT_MODIFIED response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_NOT_MODIFIED} is 0xB4 (180).
	 */
	public static final int OBEX_HTTP_NOT_MODIFIED			= 180;

	/**
	 * Defines the OBEX_HTTP_USE_PROXY response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_USE_PROXY} is 0xB5 (181).
	 */
	public static final int OBEX_HTTP_USE_PROXY				= 181;

	/**
	 * Defines the OBEX_HTTP_BAD_REQUEST response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_BAD_REQUEST} is 0xC0 (192).
	 */
	public static final int OBEX_HTTP_BAD_REQUEST			= 192;

	/**
	 * Defines the OBEX_HTTP_UNAUTHORIZED response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_UNAUTHORIZED} is 0xC1 (193).
	 */
	public static final int OBEX_HTTP_UNAUTHORIZED			= 193;

	/**
	 * Defines the OBEX_HTTP_PAYMENT_REQUIRED response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_PAYMENT_REQUIRED} is 0xC2 (194).
	 */
	public static final int OBEX_HTTP_PAYMENT_REQUIRED		= 194;

	/**
	 * Defines the OBEX_HTTP_FORBIDDEN response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_FORBIDDEN} is 0xC3 (195).
	 */
	public static final int OBEX_HTTP_FORBIDDEN				= 195;

	/**
	 * Defines the OBEX_HTTP_NOT_FOUND response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_NOT_FOUND} is 0xC4 (196).
	 */
	public static final int OBEX_HTTP_NOT_FOUND				= 196;

	/**
	 * Defines the OBEX_HTTP_BAD_METHOD response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_BAD_METHOD} is 0xC5 (197).
	 */
	public static final int OBEX_HTTP_BAD_METHOD			= 197;

	/**
	 * Defines the OBEX_HTTP_NOT_ACCEPTABLE response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_NOT_ACCEPTABLE} is 0xC6 (198).
	 */
	public static final int OBEX_HTTP_NOT_ACCEPTABLE		= 198;

	/**
	 * Defines the OBEX_HTTP_PROXY_AUTH response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_PROXY_AUTH} is 0xC7 (199).
	 */
	public static final int OBEX_HTTP_PROXY_AUTH			= 199;

	/**
	 * Defines the OBEX_HTTP_TIMEOUT response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_TIMEOUT} is 0xC8 (200).
	 */
	public static final int OBEX_HTTP_TIMEOUT				= 200;

	/**
	 * Defines the OBEX_HTTP_CONFLICT response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_CONFLICT} is 0xC9 (201).
	 */
	public static final int OBEX_HTTP_CONFLICT				= 201;

	/**
	 * Defines the OBEX_HTTP_GONE response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_GONE} is 0xCA (202).
	 */
	public static final int OBEX_HTTP_GONE					= 202;

	/**
	 * Defines the OBEX_HTTP_LENGTH_REQUIRED response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_LENGTH_REQUIRED} is 0xCB (203).
	 */
	public static final int OBEX_HTTP_LENGTH_REQUIRED		= 203;

	/**
	 * Defines the OBEX_HTTP_PRECON_FAILED response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_PRECON_FAILED} is 0xCC (204).
	 */
	public static final int OBEX_HTTP_PRECON_FAILED			= 204;

	/**
	 * Defines the OBEX_HTTP_ENTITY_TOO_LARGE response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_ENTITY_TOO_LARGE} is 0xCD (205).
	 */
	public static final int OBEX_HTTP_ENTITY_TOO_LARGE		= 205;

	/**
	 * Defines the OBEX_HTTP_REQ_TOO_LARGE response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_REQ_TOO_LARGE} is 0xCE (206).
	 */
	public static final int OBEX_HTTP_REQ_TOO_LARGE			= 206;

	/**
	 * Defines the OBEX_HTTP_UNSUPPORTED_TYPE response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_UNSUPPORTED_TYPE} is 0xCF (207).
	 */
	public static final int OBEX_HTTP_UNSUPPORTED_TYPE		= 207;

	/**
	 * Defines the OBEX_HTTP_INTERNAL_ERROR response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_INTERNAL_ERROR} is 0xD0 (208).
	 */
	public static final int OBEX_HTTP_INTERNAL_ERROR		= 208;

	/**
	 * Defines the OBEX_HTTP_NOT_IMPLEMENTED response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_NOT_IMPLEMENTED} is 0xD1 (209).
	 */
	public static final int OBEX_HTTP_NOT_IMPLEMENTED		= 209;

	/**
	 * Defines the OBEX_HTTP_BAD_GATEWAY response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_BAD_GATEWAY} is 0xD2 (210).
	 */
	public static final int OBEX_HTTP_BAD_GATEWAY			= 210;

	/**
	 * Defines the OBEX_HTTP_UNAVAILABLE response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_UNAVAILABLE} is 0xD3 (211).
	 */
	public static final int OBEX_HTTP_UNAVAILABLE			= 211;

	/**
	 * Defines the OBEX_HTTP_GATEWAY_TIMEOUT response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_GATEWAY_TIMEOUT} is 0xD4 (212).
	 */
	public static final int OBEX_HTTP_GATEWAY_TIMEOUT		= 212;

	/**
	 * Defines the OBEX_HTTP_VERSION response code.
	 * <p>
	 * The value of {@code OBEX_HTTP_VERSION} is 0xD5 (223).
	 */
	public static final int OBEX_HTTP_VERSION				= 213;

	/**
	 * Defines the OBEX_DATABASE_FULL response code.
	 * <p>
	 * The value of {@code OBEX_DATABASE_FULL} is 0xE0 (224).
	 */
	public static final int OBEX_DATABASE_FULL				= 224;

	/**
	 * Defines the OBEX_DATABASE_LOCKED response code.
	 * <p>
	 * The value of {@code OBEX_DATABASE_LOCKED} is 0xE1 (225).
	 */
	public static final int OBEX_DATABASE_LOCKED			= 225;


}
