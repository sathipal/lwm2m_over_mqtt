package com.ibm.mqttv3.binding;

import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MQTTWrapper {
	private static final Logger LOG = LoggerFactory.getLogger(MQTTWrapper.class);
	
	private static int QOS = 2;
	
	// "tcp://localhost:1800";
    private final InetSocketAddress brokerAddress;
    private final String endpointID;
    
    /* root resource */
    private final Resource root;
    
	private IMqttClient mqttClient = null;
	private MqttCallback callback;

	public MQTTWrapper(InetSocketAddress brokerAddress, String endpointID) {
		this.brokerAddress = brokerAddress;
		this.endpointID = endpointID;
		this.root = new RootResource();
	}

	public void start() {
		MemoryPersistence persistence = new MemoryPersistence();
		try {
			String serverURI = "tcp://"+brokerAddress.getHostString()+":"+brokerAddress.getPort();
			mqttClient = new MqttClient(serverURI, endpointID, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            LOG.info("Connecting endpoint "+ endpointID + " to broker: "+serverURI);
            mqttClient.connect(connOpts);
            LOG.info("Connected");
		} catch(MqttException me) {
            LOG.error("reason "+me.getReasonCode());
            LOG.error("msg "+me.getMessage());
            LOG.error("loc "+me.getLocalizedMessage());
            LOG.error("cause "+me.getCause());
            LOG.error("excep "+me);
            me.printStackTrace();
        }
	}
	
	public void stop() {
		try {
            LOG.info("Disconnecting " + endpointID + " from broker");
            mqttClient.disconnect();
		} catch(MqttException me) {
			LOG.error("reason "+me.getReasonCode());
			LOG.error("msg "+me.getMessage());
			LOG.error("loc "+me.getLocalizedMessage());
			LOG.error("cause "+me.getCause());
			LOG.error("excep "+me);
            me.printStackTrace();
        }
	}
	
	public void setCallBack(MqttCallback callback) {
		mqttClient.setCallback(callback);
		this.callback = callback;
	}
	
	public void subscribe(String topic, int qos) {
		try {
			LOG.info("Subscribe to :: "+ topic);
			mqttClient.subscribe(topic, qos);
		} catch (MqttException me) {
			LOG.error("reason "+me.getReasonCode());
			LOG.error("msg "+me.getMessage());
			LOG.error("loc "+me.getLocalizedMessage());
			LOG.error("cause "+me.getCause());
			LOG.error("excep "+me);
            me.printStackTrace();
		}
	}
	
	public void subscribe(String[] topics, int[] qos) {
		try {
			for(int i = 0; i < topics.length; i++) {
				LOG.info("Subscribe to :: "+topics[i]);
			}
			mqttClient.subscribe(topics, qos);
		} catch (MqttException me) {
			LOG.error("reason "+me.getReasonCode());
			LOG.error("msg "+me.getMessage());
			LOG.error("loc "+me.getLocalizedMessage());
			LOG.error("cause "+me.getCause());
			LOG.error("excep "+me);
            me.printStackTrace();
		}
	}
	
	public void publish(String topic, String content) {
		try {
			MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(QOS);
            LOG.info("publish :: {"+topic+" ["+message+" ]}");
			mqttClient.publish(topic, message);
		} catch (MqttException me) {
			LOG.error("reason "+me.getReasonCode());
			LOG.error("msg "+me.getMessage());
			LOG.error("loc "+me.getLocalizedMessage());
			LOG.error("cause "+me.getCause());
			LOG.error("excep "+me);
            me.printStackTrace();
		}
	}
	
	public MQTTWrapper add(Resource... resources) {
		for (Resource r:resources) {
			LOG.info("adding resource "+ r.getName() +" under root");
			root.add(r);
		}
		return this;
	}

	public Resource getRoot() {
		// TODO Auto-generated method stub
		return root;
	}
	
	public MqttCallback getMqttCallback() {
		return this.callback;
	}
	
	private class RootResource extends MQTTResource {
		
		public RootResource() {
			super("");
		}
		
		@Override
		public void handleGET(MQTTExchange exchange) {
			exchange.respond(ResponseCode.CONTENT, "Hi, Am root resource");
		}
		
	}

	public void destroy() {
		try {
            LOG.info("Disconnecting " + endpointID + " from broker");
            mqttClient.disconnect();
            mqttClient.close();
		} catch(MqttException me) {
			LOG.error("reason "+me.getReasonCode());
			LOG.error("msg "+me.getMessage());
			LOG.error("loc "+me.getLocalizedMessage());
			LOG.error("cause "+me.getCause());
			LOG.error("excep "+me);
            me.printStackTrace();
        }
		
	}
}
