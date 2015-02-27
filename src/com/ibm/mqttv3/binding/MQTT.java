package com.ibm.mqttv3.binding;

public class MQTT {
	
	/*
	 * The REST mappings: GET, POST, PUT, DELETE and RESET.
	 */
	public enum Operation {
		GET,
		POST,
		PUT,
		DELETE,
		RESET;
	};
	
	/*
	 * In order to map entire LwM2M operations into the REST, we may need
	 * to group some of the operations to REST GET and PUT/POST,
	 * 
	 * For example, the GET operation will represent following 3 requests,
	 * 0 – Represents attributes of the object/resource - DiscoverRequest, 
     * 1 – Represents Read Request 
     * 2 – Represents Observe request
	 */
	public enum GET {
		DISCOVER(0),
		READ(1),
		OBSERVE(2);
		
		public final int value;
		
		GET(int value) {
			this.value = value;
		}
		
		public static GET valueOf(int value) {
			switch (value) {
				case 0: return DISCOVER;
				case 1: return READ;
				case 2: return OBSERVE;
				default: return null;
			}
		}
		
		public int getValue() {
			return value;
		}
		
		public String toString() {
			return Integer.toString(value);
		}

		public static GET value(String value) {
			return valueOf(Integer.parseInt(value));
		}
	};
	
	/*
	 * In order to map entire LwM2M operations into the REST, we may need
	 * to group some of the operations to REST GET and PUT/POST,
	 * 
	 * For example, the PUT operation will represent following 3 requests,
	 * 0 – Represents attributes of the object/resource - WriteAttributesRequest, 
     * 1 – Represents write Request 
	 */
	public enum PUT {
		ATTRIBUTES(0),
		WRITE(1);
		
		public final int value;
		
		PUT(int value) {
			this.value = value;
		}
		
		public static PUT valueOf(int value) {
			switch (value) {
				case 0: return ATTRIBUTES;
				case 1: return WRITE;
				default: return null;
			}
		}
		
		public static PUT value(String value) {
			return valueOf(Integer.parseInt(value));
		}
		
		public int getValue() {
			return value;
		}
		
		public String toString() {
			return Integer.toString(value);
		}
	};

}
