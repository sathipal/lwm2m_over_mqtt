package com.ibm.lwm2m.objects;

import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.lwm2m.ObserveSpec;
import com.ibm.lwm2m.ObserveSpecParser;
import com.ibm.lwm2m.client.LwM2MClient;
import com.ibm.mqttv3.binding.MQTTExchange;
import com.ibm.mqttv3.binding.MQTTResource;
import com.ibm.mqttv3.binding.Request;
import com.ibm.mqttv3.binding.Resource;
import com.ibm.mqttv3.binding.ResponseCode;
import com.ibm.mqttv3.binding.MQTT.PUT;


/**
 * IPSO Object 3303
 * 
 * Temperature sensor: This IPSO object is used over a temperature sensor
 * to report a remote temperature measurement,  It defines resources 
 * for minimum/maximum measured values since the sensor is on 
 *
 */
public class TemperatureSensorObject extends MQTTResource {
	
	public static final String RESOURCE_NAME = "3303";
	
	private boolean bInstance = false;
	private static final AtomicInteger instanceCounter = new AtomicInteger(0);
	
	private FloatResource minMeasuredValue; 
	private FloatResource maxMeasuredValue;
	private FloatResource minRangeValue;
	private FloatResource maxRangeValue;
	private FloatResource sensorValue;

	private ObserveSpec observeSpec;
	
	public TemperatureSensorObject(String name, boolean bInstance) {
		super(name);
		this.bInstance = bInstance;
		
		/* only object-instance will hold the resources */
		if(bInstance == true) {
			minMeasuredValue = new FloatResource("5601", false, 0f); 
			maxMeasuredValue = new FloatResource("5602", false, 0f);
			minRangeValue = new FloatResource("5603", false, 0f);
			maxRangeValue = new FloatResource("5604", false, 0f);
			sensorValue = new FloatResource("5700", false, 0f);
		}
	}
	
	@Override
	public void handlePOST(MQTTExchange exchange) {
		Request request = exchange.getRequest();
		TemperatureSensorObject.createObjectInstance(Integer.valueOf(request.getObjectInstanceId()));
		if(!bInstance)
			exchange.respond(ResponseCode.CREATED, "");
		else 
			exchange.respond(ResponseCode.CHANGED, "");
		LwM2MClient.getClient().updateRegisteration();
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
			exchange.respond(ResponseCode.CHANGED);
		} else {
			exchange.respond(ResponseCode.METHOD_NOT_ALLOWED, "");	
		}
	}

	public static TemperatureSensorObject createObject() {
		TemperatureSensorObject to = new TemperatureSensorObject(RESOURCE_NAME, false);
		LwM2MClient.getRootResource().add(to);
		return to;
	}
	
	public static synchronized TemperatureSensorObject createObjectInstance() {
		TemperatureSensorObject to = new TemperatureSensorObject(
				Integer.toString(instanceCounter.getAndIncrement()), true);
		
		Resource resource = LwM2MClient.getRootResource().getChild(RESOURCE_NAME);
		resource.add(to);
		to.add(to.minMeasuredValue);
		to.add(to.maxMeasuredValue);
		to.add(to.minRangeValue);
		to.add(to.maxRangeValue);
		to.add(to.sensorValue);
		return to;
	}
	
	public static synchronized TemperatureSensorObject createObjectInstance(int id) {
		TemperatureSensorObject to = new TemperatureSensorObject(
				Integer.toString(id), true);
		instanceCounter.set(id + 1);
		Resource resource = LwM2MClient.getRootResource().getChild(RESOURCE_NAME);
		resource.add(to);
		to.add(to.minMeasuredValue);
		to.add(to.maxMeasuredValue);
		to.add(to.minRangeValue);
		to.add(to.maxRangeValue);
		to.add(to.sensorValue);
		return to;
	}
	
	@Override
	public void handleDELETE(MQTTExchange exchange) {
		// If its the instance delete request, delete it
		if(bInstance) {
			Resource resource = this.getParent();
			resource.remove(this);
			exchange.respond(ResponseCode.DELETED, "");
		} else {
			exchange.respond(ResponseCode.METHOD_NOT_ALLOWED, "");
		}
		LwM2MClient.getClient().updateRegisteration();		
	}


}
