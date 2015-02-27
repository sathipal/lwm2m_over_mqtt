package com.ibm.mqttv3.binding;

/**
 * Response codes defined by LWM2M enabler
 */
public enum ResponseCode {
	/** Resource correctly created */
    CREATED(2.01f),
    /** Resource correctly deleted */
    DELETED(2.02f),
    /** Resource correctly changed */
    CHANGED(2.04f),
    /** Content correctly delivered */
    CONTENT(2.05f),
    /** Operation not authorized */
    UNAUTHORIZED(4.01f),
    /** Cannot fulfill the request, it's incorrect */
    BAD_REQUEST(4f),
    /** This method (GET/PUT/POST/DELETE) is not allowed on this resource */
    METHOD_NOT_ALLOWED(4.05f),
    /** The End-point Client Name results in a duplicate entry on the LWM2M Server */
    CONFLICT(4.09f),
    /** Resource not found */
    NOT_FOUND(4.04f); 
    
	/** The code value. */
	public final float value;
			
	/**
	 * Instantiates a new response code with the specified float value.
	 *
	 * @param value the float value
	 */
	private ResponseCode(float value) {
		this.value = value;
	}
		
	/**
	 * Converts the specified float value to a response code.
	 *
	 * @param value the value
	 * @return the response code
	 * @throws IllegalArgumentException if float value is not recognized
	 */
	public static ResponseCode valueOf(float value) {
		if (value ==2.01f) { 
			return CREATED;
		} else if (value == 2.02f) { 
			return DELETED;
		} else if (value ==2.04f) { 
			return CHANGED;
		} else if (value ==2.05f) { 
			return CONTENT;
		} else if (value ==4.0f) { 
			return BAD_REQUEST;
		} else if (value ==4.01f) { 
			return UNAUTHORIZED;
		} else if (value ==4.04f) { 
			return NOT_FOUND;
		} else if (value ==4.05f) { 
			return METHOD_NOT_ALLOWED;
		}
			
		throw new IllegalArgumentException("Unknown LwM2M response code "+value);
	}
			
			
	public static boolean isSuccess(ResponseCode code) {
		return CREATED.value <= code.value && code.value <= CONTENT.value;
	}
		
	public static boolean isClientError(ResponseCode code) {
		return BAD_REQUEST.value <= code.value && code.value <= METHOD_NOT_ALLOWED.value;
	}
}
