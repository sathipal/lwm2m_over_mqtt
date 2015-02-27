package com.ibm.mqttv3.binding;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.ibm.mqttv3.binding.MQTT.GET;
import com.ibm.mqttv3.binding.MQTT.Operation;

public class Request {

	private static final Logger LOG = LoggerFactory.getLogger(Request.class);
	
	public static final String REQUEST_TOPIC_STARTER = "LWM/S";
	public static final String RESPONSE_TOPIC_STARTER = "LWM/R";
	
	private String organizationID;
	private String endpointID;
	private String applicationID;
	private Operation operation;
	private String resource = "";
	private MqttPayload payload;
	
	private LwM2MNode node;

	private MQTTWrapper mqttClient;

	private class LwM2MNode {
		private String objectId;
		private String objectIdInstance;
		private String resourceId;
	}
	
	private class MqttPayload {
		private long messageID;
		private String requestorEndpointID;
		private String requestorAppID;
		private int option;
		
		private StringBuilder content = new StringBuilder();
	}
	
	
	public Request(String topic, MqttMessage message) {
		parseTopic(topic);
		parseContent(message);
	}
	
	public Request(Operation operation) {
		this.operation = operation;
		payload = new MqttPayload();
	}

	public String getResourcePath() {
		return resource;
	}
	
	private void parseContent(MqttMessage message) {
		try {
			String content = new String(message.getPayload(), "UTF-8");
			String[] parms = content.split(" ", 4);
			
			this.payload = new MqttPayload(); 
			payload.messageID = Long.parseLong(parms[0]);
			payload.requestorEndpointID = parms[1];
			payload.requestorAppID = parms[2];
			if(parms.length >=4)
				payload.content = payload.content.append(parms[3]);
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	private void parseTopic(String topic) {
		
		// LWM/S/ORG_ID/Device_ID/App_ID/MethodName/PATH?queryString
		
		String[] levels = topic.split("/");
		
		// get org id now
		
		this.organizationID = levels[2];
		this.endpointID = levels[3];
		this.applicationID = levels[4];
		this.operation = Operation.valueOf(levels[5]);
		
		int length = levels[0].length() + levels[1].length() +
					 levels[2].length() + levels[3].length() +
					 levels[4].length() + levels[5].length() + 6;
		
		
		resource = topic.substring(length);

		// extract the objectid, object-instanceid and resource-id
        String[] resources = resource.split("/");
        
        if(resources == null || resources.length == 0) {
        	return;
        }
        
        node = new LwM2MNode();
        node.objectId = resources[0];
        
        // extract the objectid instance
        if(resources.length > 1) {
        	node.objectIdInstance = resources[1];
        }
        
        if(resources.length > 2) {
           node.resourceId = resources[2];
        }
	}

	public List<String> getURIPaths() {
		ArrayList list = new ArrayList();
		String paths[] = this.resource.split("/");
		for(int i = 0; i < paths.length; i++) {
			list.add(paths[i]);
		}
		return list;
	}

	
	public Operation getOperation() {
		return this.operation;
	}

	public String getEndpointId() {
		return endpointID;
	}
	
	public void setEndPointId(String endPointId) {
		this.endpointID = endPointId;
	}
	
	public String toString() {
		return this.getTopic() +" [" + this.getMessageAsString() + "]";
	}

	public String getObjectId() {
		if(null != node) {
			return node.objectId;
		}
		return null;
	}
	
	public String getObjectInstanceId() {
		if(null != node) {
			return node.objectIdInstance;
		}
		return null;
	}
	
	public String getResourceId() {
		if(null != node) {
			return node.resourceId;
		}
		return null;
	}

	public boolean isObserve() {
		if(( getOperation() == Operation.GET) &&
				GET.value(getPayloadText()) == GET.OBSERVE) {
			return true;
		}
		return false;
	}
	
	public boolean isRead() {
		if(( getOperation() == Operation.GET) &&
				GET.value(getPayloadText()) == GET.READ) {
			return true;
		}
		return false;
	}
	
	public boolean isDiscover() {
		if(( getOperation() == Operation.GET) &&
				GET.value(getPayloadText()) == GET.DISCOVER) {
			return true;
		}
		return false;
	}
	
	public String getPayloadText() {
		return this.payload.content.toString();
	}
	
	public byte[] getPayloadContent() {
		if(this.payload.content == null) {
			return null;
		}
		return this.payload.content.toString().getBytes(); 
	}

	public static Request newPost() {
		Request req = new Request(Operation.POST);
		req.payload = req.new MqttPayload();
		req.payload.messageID = MessageID.get();
		return req;
	}

	public void addURIPath(String resource) {
		this.resource = this.resource +"/"+resource;
	}

	public static Request newPut() {
		Request req = new Request(Operation.PUT);
		req.payload = req.new MqttPayload();
		req.payload.messageID = MessageID.get();
		return req;
	}

	public static Request newDelete() {
		Request req = new Request(Operation.DELETE);
		req.payload = req.new MqttPayload();
		req.payload.messageID = MessageID.get();
		return req;
	}
	
	public static Request newGet() {
		Request req = new Request(Operation.GET);
		req.payload = req.new MqttPayload();
		req.payload.messageID = MessageID.get();
		return req;
	}

	public void setPayloadContent(String content) {
		this.payload.content.append(content);
	}
	
	public long getMessageID() {
		return this.payload.messageID;
	}

	public String getMessageAsString() {
		return this.payload.messageID +" "+
				this.payload.requestorEndpointID +" "+
				this.payload.requestorAppID +" "+
				this.payload.content;
	}
	
	public String getTopic() {
		StringBuilder sb = new StringBuilder(50);
		
		sb.append(REQUEST_TOPIC_STARTER);
		sb.append("/");
		sb.append(this.organizationID);
		sb.append("/");
		sb.append(this.endpointID);
		sb.append("/");
		sb.append(this.applicationID);
		sb.append("/");
		sb.append(this.operation);
		if(this.resource.charAt(0) != '/') {
			sb.append("/");	
		}
		sb.append(this.resource);
		return sb.toString();
	}

	public void setOrganizationID(String organizationID) {
		this.organizationID = organizationID; 
		
	}

	public void setApplicationID(String applicationID) {
		this.applicationID = applicationID;
	}
	
	public void setRequestorApplicationID(String applicationID) {
		this.payload.requestorAppID = applicationID;
	}
	
	public void setRequestorEndpointID(String endpointID) {
		this.payload.requestorEndpointID = endpointID;
	}
	
	public String getRequestorApplicationID() {
		return this.payload.requestorAppID;
	}
	
	public String getRequestorEndpointID() {
		return this.payload.requestorEndpointID;
	}
	
	public String getOrganizationID() {
		return this.organizationID;
	}

	public void setPayloadContent(byte[] content) {
		try {
			if(content != null  && content.length > 0)
				this.payload.content.append(new String(content, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void addPayloadContent(byte[] content) {
		try {
			if(content != null  && content.length > 0) {
				this.payload.content.append(" ");
				this.payload.content.append(new String(content, "UTF-8"));
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * Cancel the observation
	 */
	public void cancel() {
		this.operation = Operation.RESET;
		mqttClient.publish(this.getTopic(), this.getMessageAsString());
		((MqttV3MessageReceiver)mqttClient.getMqttCallback()).cancel(this.getMessageID());
	}

	public void setMqttClient(MQTTWrapper mqttClient) {
		this.mqttClient = mqttClient;
		
	}

	public static List<String> getParameters(String content) {
    	String[] parms = content.split("&");
		List<String> queryParms = new ArrayList();
		for(int i = 0; i < parms.length ; i++) {
			queryParms.add(parms[i]);
		}
		return queryParms;
	}

}
