package com.ibm.mqttv3.binding;

import com.ibm.mqttv3.binding.MQTT.Operation;

/**
 * The Class MQTTExchange represents an exchange of MQTT request and response
 * 
 */
public class MQTTExchange {

	private Request request;
	
	private Response response;
	
	private Resource resource;
	
	private MQTTWrapper mqttClient;

	public static boolean bSERVER = true;

	
	public MQTTExchange(Request request, Response response) {
		this.request = request;
		this.response = response;
	}
	
	public MQTTExchange(MQTTExchange exchange, Resource Resource) {
		
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}
	
	public Resource getResource() {
		return this.resource;
	}
	
	
	/**
	 * Gets the request code: <tt>GET</tt>, <tt>POST</tt>, <tt>PUT</tt> or
	 * <tt>DELETE</tt>.
	 * 
	 * @return the request code
	 */
	public Operation getRequestCode() {
		return getRequest().getOperation();
	}
	
	
	/**
	 * Gets the request payload as byte array.
	 *
	 * @return the request payload
	 */
	public byte[] getRequestPayload() {
		return getRequest().getPayloadContent();
	}
	
	/**
	 * Gets the request payload as string.
	 *
	 * @return the request payload string
	 */
	public String getRequestText() {
		return getRequest().getPayloadText();
	}
	
	
	/**
	 * Respond the specified response code and no payload. Allowed response codes are:
	 * <ul>
	 *   <li>GET: Content (2.05), Valid (2.03)</li>
	 *   <li>POST: Created (2.01), Changed (2.04), Deleted (2.02) </li>
	 *   <li>PUT: Created (2.01), Changed (2.04)</li>
	 *   <li>DELETE: Deleted (2.02)</li>
	 * </ul>
	 *
	 * @param code the code
	 */
	public void respond(ResponseCode code) {
		respond(new Response(code));
	}
	
	/**
	 * Respond with response code 2.05 (Content) and the specified payload.
	 * <ul>
	 *   <li>GET: Content (2.05), Valid (2.03)</li>
	 *   <li>POST: Created (2.01), Changed (2.04), Deleted (2.02) </li>
	 *   <li>PUT: Created (2.01), Changed (2.04)</li>
	 *   <li>DELETE: Deleted (2.02)</li>
	 * </ul>
	 *
	 * @param payload the payload as string
	 */
	public void respond(String payload) {
		respond(ResponseCode.CONTENT, payload);
	}
	
	/**
	 * Respond with the specified response code and the specified payload.
	 * <ul>
	 *   <li>GET: Content (2.05), Valid (2.03)</li>
	 *   <li>POST: Created (2.01), Changed (2.04), Deleted (2.02) </li>
	 *   <li>PUT: Created (2.01), Changed (2.04)</li>
	 *   <li>DELETE: Deleted (2.02)</li>
	 * </ul>
	 *
	 * @param code the response code
	 * @param payload the payload
	 */
	public void respond(ResponseCode code, String payload) {
		Response response = new Response(code);
		response.setPayload(payload);
		respond(response);
	}
	
	/**
	 * Respond with the specified response code and the specified payload.
	 * <ul>
	 *   <li>GET: Content (2.05), Valid (2.03)</li>
	 *   <li>POST: Created (2.01), Changed (2.04), Deleted (2.02) </li>
	 *   <li>PUT: Created (2.01), Changed (2.04)</li>
	 *   <li>DELETE: Deleted (2.02)</li>
	 * </ul>
	 *
	 * @param code the response code
	 * @param payload the payload
	 */
	public void respond(ResponseCode code, byte[] payload) {
		Response response = new Response(code);
		response.setPayload(payload);
		respond(response);
	}

	/**
	 * Respond with the specified response code and the specified payload.
	 * <ul>
	 *   <li>GET: Content (2.05), Valid (2.03)</li>
	 *   <li>POST: Created (2.01), Changed (2.04), Deleted (2.02) </li>
	 *   <li>PUT: Created (2.01), Changed (2.04)</li>
	 *   <li>DELETE: Deleted (2.02)</li>
	 * </ul>
	 *
	 * @param code the response code
	 * @param payload the payload
	 * @param contentFormat the Content-Format of the payload
	 */
	public void respond(ResponseCode code, byte[] payload, int contentFormat) {
		Response response = new Response(code);
		response.setPayload(payload);
		respond(response);
	}
	
	/**
	 * Respond with the specified response code and the specified payload.
	 * <ul>
	 *   <li>GET: Content (2.05), Valid (2.03)</li>
	 *   <li>POST: Created (2.01), Changed (2.04), Deleted (2.02) </li>
	 *   <li>PUT: Created (2.01), Changed (2.04)</li>
	 *   <li>DELETE: Deleted (2.02)</li>
	 * </ul>
	 *
	 * @param code the response code
	 * @param payload the payload
	 * @param contentFormat the Content-Format of the payload
	 */
	public void respond(ResponseCode code, String payload, int contentFormat) {
		Response response = new Response(code);
		response.setPayload(payload);
		respond(response);
	}
	
	/**
	 * Respond with the specified response.
	 * <ul>
	 *   <li>GET: Content (2.05), Valid (2.03)</li>
	 *   <li>POST: Created (2.01), Changed (2.04), Deleted (2.02) </li>
	 *   <li>PUT: Created (2.01), Changed (2.04)</li>
	 *   <li>DELETE: Deleted (2.02)</li>
	 * </ul>
	 *
	 * @param response the response
	 */
	public void respond(Response response) {
		
		/*"LWMS/IBM/10/Example/message-id"*/
		String topic;
		
		topic = Request.RESPONSE_TOPIC_STARTER + "/" +
				this.request.getOrganizationID() + "/" +
				request.getRequestorEndpointID() + "/" +
				request.getRequestorApplicationID() + "/" +
				request.getMessageID();
		
		mqttClient.publish(topic, response.getMessage());
		
	}
	
	public Request getRequest() {
		return this.request;
	}


	public void setMqttClient(MQTTWrapper mqttClinet) {
		this.mqttClient = mqttClinet;
		
	}
}
