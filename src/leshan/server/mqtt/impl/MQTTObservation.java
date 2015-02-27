package leshan.server.mqtt.impl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import leshan.ResponseCode;
import leshan.core.node.LwM2mNode;
import leshan.core.node.LwM2mPath;
import leshan.core.node.codec.InvalidValueException;
import leshan.core.node.codec.LwM2mNodeDecoder;
import leshan.core.request.ContentFormat;
import leshan.core.response.ValueResponse;
import leshan.server.client.Client;
import leshan.server.observation.Observation;
import leshan.server.observation.ObservationListener;
import leshan.util.Validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mqttv3.binding.AbstractRequestObserver;
import com.ibm.mqttv3.binding.Request;
import com.ibm.mqttv3.binding.Response;

public final class MQTTObservation extends AbstractRequestObserver implements Observation {
    private final Logger LOG = LoggerFactory.getLogger(MQTTObservation.class);

    private final List<ObservationListener> listeners = new CopyOnWriteArrayList<>();
    private final Client client;
    private final LwM2mPath path;

    public MQTTObservation(Request mqttRequest, Client client, LwM2mPath path) {
    	super(mqttRequest);
    	Validate.notNull(mqttRequest);
        Validate.notNull(client);
        Validate.notNull(path);
        
        this.mqttRequest = mqttRequest;
        this.client = client;
        this.path = path;
    }

    public MQTTObservation(Request mqttRequest, Client client, LwM2mPath path, ObservationListener listener) {
        this(mqttRequest, client, path);
        this.listeners.add(listener);
    }

    public MQTTObservation(Request mqttRequest, Client client, LwM2mPath path,
            List<ObservationListener> listeners) {
        this(mqttRequest, client, path);
        this.listeners.addAll(listeners);
    }

    @Override
    public void cancel() {
        mqttRequest.cancel();
    }

    public static ResponseCode fromMqttCode(final float code) {
        Validate.notNull(code);

        if (code == com.ibm.mqttv3.binding.ResponseCode.CREATED.value) {
            return ResponseCode.CREATED;
        } else if (code == com.ibm.mqttv3.binding.ResponseCode.DELETED.value) {
            return ResponseCode.DELETED;
        } else if (code == com.ibm.mqttv3.binding.ResponseCode.CHANGED.value) {
            return ResponseCode.CHANGED;
        } else if (code == com.ibm.mqttv3.binding.ResponseCode.CONTENT.value) {
            return ResponseCode.CONTENT;
        } else if (code == com.ibm.mqttv3.binding.ResponseCode.BAD_REQUEST.value) {
            return ResponseCode.BAD_REQUEST;
        } else if (code == com.ibm.mqttv3.binding.ResponseCode.UNAUTHORIZED.value) {
            return ResponseCode.UNAUTHORIZED;
        } else if (code == com.ibm.mqttv3.binding.ResponseCode.NOT_FOUND.value) {
            return ResponseCode.NOT_FOUND;
        } else if (code == com.ibm.mqttv3.binding.ResponseCode.METHOD_NOT_ALLOWED.value) {
            return ResponseCode.METHOD_NOT_ALLOWED;
        } else if (code == 137) {
            return ResponseCode.CONFLICT;
        } else {
            throw new IllegalArgumentException("Invalid CoAP code for LWM2M response: " + code);
        }
    }

    @Override
	public void onResponse(Response mqttResponse) {
        if (fromMqttCode(mqttResponse.getCode()) == ResponseCode.CHANGED) {
            try {
                LwM2mNode content = LwM2mNodeDecoder.decode(mqttResponse.getPayload(),
                		ContentFormat.fromCode(1541), path);
                ValueResponse response = new ValueResponse(ResponseCode.CHANGED, content);

                for (ObservationListener listener : listeners) {
                    listener.newValue(this, response.getContent());
                }
            } catch (InvalidValueException e) {
                String msg = String.format("[%s] ([%s])", e.getMessage(), e.getPath().toString());
                LOG.debug(msg);
            }
        }
    }

    @Override
    public void onCancel() {
        for (ObservationListener listener : listeners) {
            listener.cancelled(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Client getClient() {
        return client;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LwM2mPath getPath() {
        return path;
    }

    @Override
    public String toString() {
        return String.format("MQTTObservation [%s]", path);
    }

    @Override
    public void addListener(ObservationListener listener) {
        listeners.add(listener);

    }

    @Override
    public void removeListener(ObservationListener listener) {
        listeners.remove(listener);

    }

	@Override
	public void onError(final Response mqttResponse) {
		// TODO Auto-generated method stub
		
	}
}
