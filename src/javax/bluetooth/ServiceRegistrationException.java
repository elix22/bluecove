package javax.bluetooth;

import java.io.IOException;

/**
 * The {@code ServiceRegistrationException} is thrown when there is a failure 
 * to add a service record to the local Service Discovery Database (SDDB) or 
 * to modify an existing service record in the SDDB. The failure could be 
 * because the SDDB has no room for new records or because the modification 
 * being attempted to a service record violated one of the rules about service 
 * record updates. This exception will also be thrown if it was not possible 
 * to obtain an RFCOMM server channel needed for a {@code btspp} service record.
 *
 */
public class ServiceRegistrationException extends IOException {

	/**
	 * Creates a {@code ServiceRegistrationException} without a detailed message.
	 *
	 */
	public ServiceRegistrationException(){
		super();
	}
	/**
	 * Creates a {@code ServiceRegistrationException} with a detailed message.
	 * @param msg the reason for the exception
	 */
	public ServiceRegistrationException(java.lang.String msg){
		super(msg);
	}

}
