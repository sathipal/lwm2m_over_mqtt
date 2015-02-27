
package com.ibm.mqttv3.binding;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MQTTResource implements Resource {

	/** The logger. */
	private static final Logger LOG = LoggerFactory.getLogger(MQTTResource.class);
	
	/* The resource id. */
	private String id;
	
	/* The child resources.
	 * We need a ConcurrentHashMap to have stronger guarantees in a
	 * multi-threaded environment
	 */
	private ConcurrentHashMap<String, Resource> children;
	
	/* The parent of this resource. */
	private Resource parent;
	
	/**
	 * Constructs a new resource with the specified id.
	 *
	 * @param id the id
	 */
	public MQTTResource(String id) {
		this.id = id;
		this.children = new ConcurrentHashMap<String, Resource>();
	}
	

	/**
	 * Handles the GET request in the given MQTTExchange
	 */
	public void handleGET(MQTTExchange exchange) {
		exchange.respond(ResponseCode.METHOD_NOT_ALLOWED);
	}
	
	/**
	 * Handles the POST request in the given MQTTExchange
	 */
	public void handlePOST(MQTTExchange exchange) {
		exchange.respond(ResponseCode.METHOD_NOT_ALLOWED);
	}
	
	/**
	 * Handles the PUT request in the given MQTTExchange
	 */
	public void handlePUT(MQTTExchange exchange) {
		exchange.respond(ResponseCode.METHOD_NOT_ALLOWED);
	}
	
	/**
	 * Handles the DELETE request in the given MQTTExchange
	 */
	public void handleDELETE(MQTTExchange exchange) {
		exchange.respond(ResponseCode.METHOD_NOT_ALLOWED);
	}
	
	@Override
	public synchronized void add(Resource child) {
		if (child.getName() == null)
			throw new NullPointerException("Child must have a id");
		if (child.getParent() != null)
			child.getParent().remove(child);
		children.put(child.getName(), child);
		child.setParent(this);
	}
	
	/**
	 * Adds the specified resource as child.
	 * 
	 * @param child the child to add
	 * @return this
	 */
	public synchronized MQTTResource add(MQTTResource child) {
		add( (Resource) child);
		return this;
	}
	
	/**
	 * Adds the specified resource as child. 
	 * 
	 * @param children the children to add
	 * @return this
	 */
	public synchronized MQTTResource add(MQTTResource... children) {
		for (MQTTResource child:children)
			add(child);
		return this;
	}
	
	@Override
	public synchronized boolean remove(Resource child) {
		Resource removed = remove(child.getName());
		if (removed == child) {
			child.setParent(null);
			return true;
		}
		return false;
	}
	
	/**
	 * Removes the child with the specified id and returns it. If no child
	 * with the specified id is found, the return value is null.
	 * 
	 * @param id the id
	 * @return the removed resource or null
	 */
	public synchronized Resource remove(String id) {
		return children.remove(id);
	}
	
	/**
	 * Delete this resource from its parent
	 */
	public synchronized void delete() {
		Resource parent = getParent();
		if (parent != null) {
			parent.remove(this);
		}
		
	}
	
	/*
	 * 
	 * Returns the parent of this resource
	 */
	@Override
	public Resource getParent() {
		return parent;
	}
	
	/*
	 * Sets the parent of this resource to the given value
	 */
	public void setParent(Resource parent) {
		this.parent = parent;
	}
	
	/* 
	 * Returns the child with the given id
	 */
	@Override
	public Resource getChild(String id) {
		return children.get(id);
	}

	/* 
	 * Returns the id of this resource
	 */
	@Override
	public String getName() {
		return id;
	}

	public synchronized void setName(String id) {
		if (id == null)
			throw new NullPointerException();
		String old = this.id;
		Resource parent = getParent();
		synchronized (parent) {
			parent.remove(this);
			this.id = id;
			parent.add(this);
		}
	}
	
	@Override // should be used for read-only
	public Collection<Resource> getChildren() {
		return children.values();
	}


	@Override
	public void handleRESET(MQTTExchange exchange) {
		// TODO Auto-generated method stub
		
	}
}
