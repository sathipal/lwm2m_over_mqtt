package com.ibm.lwm2m.objects;

import com.ibm.lwm2m.ObserveNotify;
import com.ibm.lwm2m.ObserveSpec;
import com.ibm.lwm2m.ObserveSpecParser;
import com.ibm.lwm2m.client.LocalResource;
import com.ibm.mqttv3.binding.MQTTExchange;
import com.ibm.mqttv3.binding.MQTTResource;
import com.ibm.mqttv3.binding.Request;
import com.ibm.mqttv3.binding.ResponseCode;
import com.ibm.mqttv3.binding.MQTT.PUT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringResource extends MQTTResource implements LocalResource {

	private static final Logger LOG = LoggerFactory.getLogger(StringResource.class);
	
	private String value = "";
	private ObserveSpec observeSpec;
	private ObserveNotify observer;
	boolean bWrite;
	
	public StringResource(String name, boolean bWrite, String value) {
		super(name);
		this.bWrite = bWrite;
		this.value = value;
	}
	
	@Override
	public String getValue() {
		return value;
	}
	
	private void notifyObserver() {
		if(null != observer) {
			if(null != observeSpec)
				observer.setObserveSpec(observeSpec);
			observer.run();
		}
		
	}

	@Override
	public void setValue(String value) {
		this.value = value;
		notifyObserver();
	}

	
	
	@Override
	public void handlePOST(MQTTExchange exchange) {
		if(this.bWrite){
			exchange.respond(ResponseCode.CONTENT, value);
		} else {
			exchange.respond(ResponseCode.METHOD_NOT_ALLOWED, "");	
		}
	}

	@Override
	public void handlePUT(MQTTExchange exchange) {
		/*
    	 * Content will have following 2 parameters delimited by space,
    	 * 1st parameter to carry whether its a normal write request
    	 * or the write attribute request
    	 * 
    	 * 0 - write attribute request
    	 * 1 - write request 
    	 * 
    	 * 2nd parameter - value or the list of attributes
    	 * 
    	 */
    	String[] parameters = exchange.getRequest().getPayloadText().split(" ", 2);
    	// check whether its a write attribute request
    	if(PUT.value(parameters[0]) == PUT.ATTRIBUTES) {
    		this.observeSpec = ObserveSpecParser.parse(Request.getParameters(parameters[1]));
			if(observer != null) {
				observer.setObserveSpec(observeSpec);
			}
			exchange.respond(ResponseCode.CHANGED);
		} else if(this.bWrite){
			this.setValue(parameters[1]);
			exchange.respond(ResponseCode.CHANGED, this.getValue());
		} else {
			exchange.respond(ResponseCode.METHOD_NOT_ALLOWED, "");	
		}
	}

	@Override
	public void handleGET(MQTTExchange exchange) {
		Request request = exchange.getRequest();

        if(request.isObserve()) {
        	// Its a notifyRequest so build a mechanism
        	if(observer == null) {
        		this.observer = new ObserveNotify(exchange, this, 
        			observeSpec, this.getValue());
        		observer.scheduleNext();
        	}
        	exchange.respond(ResponseCode.CONTENT, this.getValue());
        	
        } else if(request.isRead()) {
        	exchange.respond(ResponseCode.CONTENT, this.getValue());
        }
	}
	
	@Override
	public void handleRESET(MQTTExchange exchange) {
		if(observer != null) {
			observer.cancel();
			observer = null;
		}
	}

	
	@Override
	public void handleDELETE(MQTTExchange exchange) {
		exchange.respond(ResponseCode.METHOD_NOT_ALLOWED, "");
		
	}
	
}