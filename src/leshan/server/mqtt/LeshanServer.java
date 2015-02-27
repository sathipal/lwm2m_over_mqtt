/*
 * Copyright (c) 2013, Sierra Wireless
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
package leshan.server.mqtt;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import leshan.core.objectspec.Resources;
import leshan.core.response.ClientResponse;
import leshan.core.response.ExceptionConsumer;
import leshan.core.response.ResponseConsumer;
import leshan.server.LwM2mServer;
import leshan.server.client.Client;
import leshan.server.client.ClientRegistry;
import leshan.server.client.ClientRegistryListener;
import leshan.server.impl.ClientRegistryImpl;
import leshan.server.impl.ObservationRegistryImpl;
import leshan.server.impl.SecurityRegistryImpl;
import leshan.server.mqtt.impl.MQTTLwM2mRequestSender;
import leshan.server.mqtt.impl.RegisterResource;
import leshan.server.observation.ObservationRegistry;
import leshan.server.request.LwM2mRequest;
import leshan.server.security.SecurityRegistry;
import leshan.util.Validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mqttv3.binding.MQTTWrapper;
import com.ibm.mqttv3.binding.MqttV3MessageReceiver;
import com.ibm.mqttv3.binding.Request;

/**
 * A Lightweight M2M server.
 * <p>
 * This MQTT server defines a /rd resources as described in the CoRE RD specification. A {@link ClientRegistry} must be
 * provided to host the description of all the registered LW-M2M clients.
 * </p>
 * <p>
 * A {@link RequestHandler} is provided to perform server-initiated requests to LW-M2M clients.
 * </p>
 */
public class LeshanServer implements LwM2mServer {

    
    public static final String SUBSCRIBE_TOPIC_START = "LWM/+";
    /** Default MQTT port is 1883 but I am using 1885 as 1883 has something else running*/
    public static final int PORT = 1885;

	private static final int QOS = 2;

	private static final Logger LOG = LoggerFactory.getLogger(LeshanServer.class);

    private final MQTTWrapper mqttClient;
    
    private final MQTTLwM2mRequestSender requestSender;

    private final ClientRegistry clientRegistry;

    private final ObservationRegistry observationRegistry;
    
    private final String endpointID;
    private final String applicationID;

	private MqttV3MessageReceiver callback;

    /**
     * Initialize a server which will bind to default UDP port for CoAP (5684).
     */
    public LeshanServer(String endpointID, String applicationID) {
        this(null, null, endpointID, applicationID);
        
    }

    /**
     * Initialize a server which will bind to the specified address and port.
     *
     * @param brokerAddress the address to bind the CoAP server.
     * @param brokerAddressSecure the address to bind the CoAP server for DTLS connection.
     */
    public LeshanServer(final InetSocketAddress brokerAddress, 
    		String endpointID, String applicationID) {
        this(brokerAddress, null, null, endpointID, applicationID);
    }

    /**
     * Initialize a server which will bind to default UDP port for CoAP (5684).
     */
    public LeshanServer(final ClientRegistry clientRegistry, 
            final ObservationRegistry observationRegistry, 
            String endpointID, String applicationID) {
       
    	this(new InetSocketAddress("localhost", PORT), 
                clientRegistry, observationRegistry, endpointID, applicationID);
    }

    /**
     * Initialize a server which will bind to the specified address and port.
     *
     * @param brokerAddress the address to bind the CoAP server.
     * @param brokerAddressSecure the address to bind the CoAP server for DTLS connection.
     */
    public LeshanServer(final InetSocketAddress brokerAddress, 
            final ClientRegistry clientRegistry, 
            final ObservationRegistry observationRegistry,
            String endpointID, String applicationID) {
        Validate.notNull(brokerAddress, "IP address cannot be null");

        this.endpointID = endpointID;
        this.applicationID = applicationID;
        // init registry
        if (clientRegistry == null) {
			this.clientRegistry = new ClientRegistryImpl();
		} else {
			this.clientRegistry = clientRegistry;
		}

        if (observationRegistry == null) {
			this.observationRegistry = new ObservationRegistryImpl();
		} else {
			this.observationRegistry = observationRegistry;
		}

        // Cancel observations on client unregistering
        this.clientRegistry.addListener(new ClientRegistryListener() {

            @Override
            public void updated(final Client clientUpdated) {
            }

            @Override
            public void unregistered(final Client client) {
                LeshanServer.this.observationRegistry.cancelObservations(client);
            }

            @Override
            public void registered(final Client client) {
            }
        });

        // init MQTT server
        mqttClient = new MQTTWrapper(brokerAddress, this.endpointID);
        
        // define /rd resource
        final RegisterResource rdResource = new RegisterResource(this.clientRegistry);
        mqttClient.add(rdResource);

        requestSender = new MQTTLwM2mRequestSender(endpointID, applicationID, this.observationRegistry);
    }

    /**
     * Starts the server and binds it to the specified port.
     */
    @Override
	public void start() {
        // load resource definitions
        Resources.load();

        mqttClient.start();
        
        // Create a server message receiver and set the callback
        callback = new MqttV3MessageReceiver(mqttClient);
        mqttClient.setCallBack(callback);
        requestSender.setMqttV3MessageReceiver(callback);
        requestSender.setMqttClient(mqttClient);
        
        // Subscribe a topic to broker
        StringBuilder sb = new StringBuilder(20);
        sb.append(SUBSCRIBE_TOPIC_START+"/+/")
          .append(this.endpointID)
          .append("/")
          .append(this.applicationID)
          .append("/#");
        
        mqttClient.subscribe(sb.toString(), QOS);
        
        LOG.info("LW-M2M server started");

        // start client registry
        if (clientRegistry instanceof ClientRegistryImpl) {
			((ClientRegistryImpl) clientRegistry).start();
		}
    }

    /**
     * Stops the server and unbinds it from assigned ports (can be restarted).
     */
    @Override
	public void stop() {
        mqttClient.stop();

        if (clientRegistry instanceof ClientRegistryImpl) {
            try {
                ((ClientRegistryImpl) clientRegistry).stop();
            } catch (final InterruptedException e) {
                LOG.info("LW-M2M server started");
            }
        }
    }

    /**
     * Stops the server and unbinds it from assigned ports.
     */
    public void destroy() {
        mqttClient.destroy();

        if (clientRegistry instanceof ClientRegistryImpl) {
            try {
                ((ClientRegistryImpl) clientRegistry).stop();
            } catch (final InterruptedException e) {
                LOG.info("LW-M2M server started");
            }
        }
    }

    @Override
    public ClientRegistry getClientRegistry() {
        return this.clientRegistry;
    }

    @Override
    public ObservationRegistry getObservationRegistry() {
        return this.observationRegistry;
    }

    @Override
    public SecurityRegistry getSecurityRegistry() {
        return null;
    }

    @Override
    public <T extends ClientResponse> T send(final LwM2mRequest<T> request) {
        return requestSender.send(request);
    }

    @Override
    public <T extends ClientResponse> void send(final LwM2mRequest<T> request, final ResponseConsumer<T> responseCallback,
            final ExceptionConsumer errorCallback) {
        requestSender.send(request, responseCallback, errorCallback);
    }
}
