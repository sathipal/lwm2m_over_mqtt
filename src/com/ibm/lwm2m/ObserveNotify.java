/*
 * Copyright (c) 2013, Sierra Wireless,
 * Copyright (c) 2014, Zebra Technologies,
 * 
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *     * Neither the name of {{ project }} nor the names of its contributors
 *       may be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ibm.lwm2m;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.ibm.lwm2m.client.LocalResource;
import com.ibm.lwm2m.client.LwM2MClient;
import com.ibm.lwm2m.objects.LwM2MServerObject;
import com.ibm.mqttv3.binding.MQTTExchange;
import com.ibm.mqttv3.binding.Resource;
import com.ibm.mqttv3.binding.ResponseCode;

public class ObserveNotify implements Runnable {

    private static final long SECONDS_TO_MILLIS = 1000;

    private ObserveSpec observeSpec;
    private MQTTExchange exchange;
    private LocalResource node;
    private byte[] previousValue;
    private Date previousTime;
    private final static ScheduledExecutorService service = 
    		Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    private boolean cancel = false; 
    
    public ObserveNotify(final MQTTExchange exchange, 
    					 LocalResource node, 
    					 ObserveSpec observeSpec,
    					 String value) {
    	this.exchange = exchange;
        this.node = node;
        updatePrevious(value.getBytes());
        
        // Take the default observespec from the server object
        if(null != observeSpec) {
        	this.observeSpec = observeSpec;
        } else {
        	Resource resource = LwM2MClient.getRootResource().getChild("1");
        	String id = exchange.getRequest().getRequestorEndpointID();
        	Collection<Resource> childrens = resource.getChildren();
        	Iterator<Resource> itr = childrens.iterator();
        	LwM2MServerObject serverObj = null;
        	while(itr.hasNext()) {
        		serverObj = ((LwM2MServerObject) itr.next());
        		// retrieve the server id
        		LocalResource shortIDResource = (LocalResource) serverObj.getChild("0");
        		if(id.equals(shortIDResource.getValue())) {
        			break;
        		}
        	}
        	this.observeSpec = serverObj.getObserveSpec();
        }
    }

   

    private void updatePrevious(byte[] value) {
        previousValue = value;
        previousTime = new Date();
    }

    private boolean shouldNotify(byte[] value) {
        final long diff = getTimeDiff();
        final Integer pmax = observeSpec.getMaxPeriod();
        if (pmax != null && diff > pmax * SECONDS_TO_MILLIS) {
            return true;
        }
        return !Arrays.equals(value, previousValue);
    }

    private void sendNotify(byte[] value) {
        updatePrevious(value);
        exchange.respond(ResponseCode.CHANGED, value);
    }

    public void setObserveSpec(final ObserveSpec observeSpec) {
        this.observeSpec = observeSpec;
    }

    public void scheduleNext() {
        if (observeSpec.getMaxPeriod() != null) {
            long diff = getTimeDiff();
            service.schedule(this, observeSpec.getMaxPeriod() * SECONDS_TO_MILLIS - diff, TimeUnit.MILLISECONDS);
        }
    }

    private long getTimeDiff() {
        return new Date().getTime() - previousTime.getTime();
    }

    @Override
    public synchronized void run() {
    	/*
    	 * Cancel the observation if the server cancels it
    	 */
    	if(cancel) {
    		return;
    	}
    	
        byte[] value = node.getValue().getBytes();
        if (shouldNotify(value)) {
            sendNotify(value);
        }
        scheduleNext();
    }



	public void cancel() {
		this.cancel = true;
		
	}

}
