package com.ibm.lwm2m.objects;

import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.lwm2m.ObserveSpec;
import com.ibm.lwm2m.client.LwM2MClient;
import com.ibm.lwm2m.objects.BooleanResource;
import com.ibm.lwm2m.objects.ExecResource;
import com.ibm.lwm2m.objects.IntegerResource;
import com.ibm.lwm2m.objects.StringResource;
import com.ibm.mqttv3.binding.MQTTResource;
import com.ibm.mqttv3.binding.Resource;

/*
 * Represents the LwM2M Server Object /1
 * 
 * For this example, we assume there is only one instanceof server object
 * and the instanceid is 10 - /1/10
 */
public class LwM2MServerObject extends MQTTResource {
	
	public static final String RESOURCE_NAME = "1";
	ObserveSpec observeSpec = new ObserveSpec();
	private static final AtomicInteger instanceCounter = new AtomicInteger(0); 
	
	public LwM2MServerObject(String name, boolean bInstance) {
		super(name);
		
		if(true == bInstance) {
			serverId = new IntegerResource("0", false, 10);
			lifttime = new IntegerResource("1", true, 86400); 
			minPeriod = new IntegerResource("2", true, 1);
			maxPeriod = new IntegerResource("3", true, 10);
			disable = new ExecResource("4"); 
			disableTimeout = new IntegerResource("5", true, 10);
			bstoreNotification = new BooleanResource("6", true, false);
			binding = new StringResource("7", true, "MQTT");
			bRegistrationUpdateTrigger = new ExecResource("8");
		}
	}
	
	private IntegerResource serverId;
	private IntegerResource lifttime; 
	private IntegerResource minPeriod;
	private IntegerResource maxPeriod;
	private ExecResource disable; 
	private IntegerResource disableTimeout;
	private BooleanResource bstoreNotification;
	private StringResource binding;
	private ExecResource bRegistrationUpdateTrigger;
	
	public static LwM2MServerObject createObject() {
		LwM2MServerObject to = new LwM2MServerObject(RESOURCE_NAME, false);
		LwM2MClient.getRootResource().add(to);
		return to;
	}
	
	public static LwM2MServerObject createObjectInstance() {
		LwM2MServerObject to = new LwM2MServerObject(
				Integer.toString(instanceCounter.getAndIncrement()), true);
		Resource resource = LwM2MClient.getRootResource().getChild(RESOURCE_NAME);
		resource.add(to);
		to.add(to.serverId);
		to.add(to.lifttime);
		to.add(to.minPeriod);
		to.add(to.maxPeriod);
		to.add(to.disable);
		to.add(to.disableTimeout);
		to.add(to.bstoreNotification);
		to.add(to.binding);
		to.add(to.bRegistrationUpdateTrigger);
		return to;
	}

	public ObserveSpec getObserveSpec() {
		
		if(this.observeSpec == null) {
			observeSpec = new ObserveSpec(Integer.valueOf(minPeriod.getValue()),
					Integer.valueOf(maxPeriod.getValue()));
		} else {
			observeSpec.setMaxPeriod(Integer.valueOf(maxPeriod.getValue()));
			observeSpec.setMinPeriod(Integer.valueOf(minPeriod.getValue()));
		}
		
		return observeSpec;
		
	}
	
}
