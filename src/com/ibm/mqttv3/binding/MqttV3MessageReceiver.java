package com.ibm.mqttv3.binding;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.ibm.mqttv3.binding.MQTT.Operation;

public class MqttV3MessageReceiver implements MqttCallback {

	private MQTTWrapper mqttClient;
	private static final Logger LOG = LoggerFactory.getLogger(MqttV3MessageReceiver.class);
	private static ConcurrentHashMap<Long, AbstractRequestObserver> requestObservers = new ConcurrentHashMap();
	private static ScheduledThreadPoolExecutor executor = 
			new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors() + 1);
	
	public MqttV3MessageReceiver(MQTTWrapper mqttClient) {
		this.mqttClient = mqttClient;
	}
	
	@Override
	public void messageArrived(final String topic, final MqttMessage message)
			throws Exception {
		
		executor.execute(new Runnable() {
			public void run() {
				try {
					handleMessage(topic, message);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});
	}
	
	

	protected void handleMessage(String topic, MqttMessage message) {
		LOG.info("MSG { "+topic + " ["+message.toString()+"]}");
		
		if(topic.startsWith(Request.RESPONSE_TOPIC_STARTER)) {
			String[] paths = topic.split("/");
			Long messageID = Long.valueOf(paths[paths.length - 1]);
			// the last one must contain the message-id
			AbstractRequestObserver requestObserver = 
					requestObservers.get(messageID);
			if(requestObserver != null) {
				Response response = new Response(message);
				if(ResponseCode.isSuccess(ResponseCode.valueOf(response.getCode()))) {
					requestObserver.onResponse(response);
				} else {
					requestObserver.onError(response);
				}
			}
			return;
		}
		
		Request request = new Request(topic, message);
		MQTTExchange exchange = new MQTTExchange(request, null);
		exchange.setMqttClient(this.mqttClient);
		
		Resource resource = getResource(request);
		if(resource == null) {
			// Check if its a POST operation, in which case
			// we need to return the parent resource to create
			// the new instance
			if(request.getOperation() == Operation.POST) {
				resource = getParentResource(request);
			}
			if(resource == null) {
				exchange.respond(ResponseCode.NOT_FOUND);
				return;
			}
		}
		exchange.setResource(resource);
		
		switch(request.getOperation()) {
			case POST:
				resource.handlePOST(exchange);
				break;
				
			case PUT:
				resource.handlePUT(exchange);
				break;
				
			case DELETE:
				resource.handleDELETE(exchange);
				break;
				
			case GET:
				resource.handleGET(exchange);
				break;
				
			case RESET:
				resource.handleRESET(exchange);
				break;
		}
		
	}

	private Resource getResource(Request request) {
		Resource resource = this.mqttClient.getRoot();
		LOG.debug(" root-resource:: "+resource);
		
		
		String id = request.getObjectId();
		LOG.debug(" getObjectId:: "+id);
		if(id == null) {
			return null;
		} else {
			resource = resource.getChild(id);
		}
		
		
		id = request.getObjectInstanceId(); 
		LOG.debug(" getObjectInstanceId:: "+id);
		if(id != null && resource != null) {
			resource = resource.getChild(id);
		}
		
		id = request.getResourceId(); 
		if(id != null && resource != null) {
			resource = resource.getChild(id);
		}
		
		return resource;
	}
	
	private Resource getParentResource(Request request) {
		Resource resource = this.mqttClient.getRoot();
		LOG.debug(" root-resource:: "+resource);
		
		String id = request.getObjectId();
		LOG.debug(" getObjectId:: "+id);
		if(id == null) {
			return null;
		} else {
			resource = resource.getChild(id);
		}
		
		return resource;
	}

	public void addRequest(Long messageID,
			AbstractRequestObserver requestObserver) {
		requestObservers.put(messageID, requestObserver);
	}
	
	public void removeRequest(Long messageID) {
		requestObservers.remove(messageID);
	}
	
	public AbstractRequestObserver getRequest(Long messageID) {
		return requestObservers.get(messageID);
	}

	
	public void connectionLost(Throwable cause) {
		LOG.info("Connection lost "+cause.getMessage());
		cause.printStackTrace();
		
	}

	public void deliveryComplete(IMqttDeliveryToken token) {
		//System.out.println("DeliveryComplte :: "+token.toString());
		
	}
	
	public MQTTWrapper getMqttClinet() {
		// TODO Auto-generated method stub
		return mqttClient;
	}

	public void cancel(long messageID) {
		AbstractRequestObserver obs = this.requestObservers.get(messageID);
		obs.onCancel();
	}

}
