package com.ibm.mqttv3.binding;

import java.util.Collection;

public interface Resource {
	
	public String getName();

	public void handlePOST(MQTTExchange exchange);
	public void handlePUT(MQTTExchange exchange);
	public void handleGET(MQTTExchange exchange);
	public void handleDELETE(MQTTExchange exchange);
	
	public void handleRESET(MQTTExchange exchange);

	void add(Resource child);
	public Resource getParent();
	public boolean remove(Resource child);
	public void setParent(Resource resource);

	Resource getChild(String name);

	Collection<Resource> getChildren();

}
