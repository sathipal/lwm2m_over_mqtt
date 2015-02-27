package com.ibm.lwm2m.objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.lwm2m.client.LocalResource;
import com.ibm.mqttv3.binding.MQTTExchange;
import com.ibm.mqttv3.binding.MQTTResource;
import com.ibm.mqttv3.binding.Request;
import com.ibm.mqttv3.binding.ResponseCode;

public class ExecResource extends MQTTResource implements LocalResource {

	private static final Logger LOG = LoggerFactory.getLogger(ExecResource.class);
	
	private String value = "0";
	
	public ExecResource(String name) {
		super(name);
	}
	
	@Override
	public String getValue() {
		return value;
	}
	
	@Override
	public void setValue(String value) {
		this.value = value;
	}

	
	
	@Override
	public void handlePOST(MQTTExchange exchange) {
		exchange.respond(ResponseCode.CHANGED, " Resource successfully executed");	
	}

	@Override
	public void handlePUT(MQTTExchange exchange) {
		exchange.respond(ResponseCode.CHANGED, " Resource successfully executed");
	}

	@Override
	public void handleGET(MQTTExchange exchange) {
		Request request = exchange.getRequest();

        LOG.info("GET received : {} " + request);
        if(request.isObserve()) {
        	// Its a notifyRequest so build a mechanism
        	exchange.respond(ResponseCode.METHOD_NOT_ALLOWED, "");
        	
        } else if(request.isRead()) {
        	exchange.respond(ResponseCode.CONTENT, this.getValue());
        }
	}
	
	@Override
	public void handleRESET(MQTTExchange exchange) {
		exchange.respond(ResponseCode.METHOD_NOT_ALLOWED, "");
	}

	
	@Override
	public void handleDELETE(MQTTExchange exchange) {
		exchange.respond(ResponseCode.METHOD_NOT_ALLOWED, "");
		
	}
}