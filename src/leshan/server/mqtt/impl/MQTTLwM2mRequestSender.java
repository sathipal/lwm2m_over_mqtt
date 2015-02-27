package leshan.server.mqtt.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

//import leshan.client.response.OperationResponse;
import leshan.core.response.ClientResponse;
import leshan.core.response.ExceptionConsumer;
import leshan.core.response.ResponseConsumer;
import leshan.server.observation.ObservationRegistry;
import leshan.server.request.LwM2mRequest;
import leshan.server.request.LwM2mRequestSender;
import leshan.util.Validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mqttv3.binding.AbstractRequestObserver;
import com.ibm.mqttv3.binding.MQTTWrapper;
import com.ibm.mqttv3.binding.MqttV3MessageReceiver;
import com.ibm.mqttv3.binding.Request;
import com.ibm.mqttv3.binding.Response;

public class MQTTLwM2mRequestSender implements LwM2mRequestSender {

    private static final Logger LOG = LoggerFactory.getLogger(MQTTLwM2mRequestSender.class);
    private static final int REQUEST_TIMEOUT_MILLIS = 5000;

    private final ObservationRegistry observationRegistry;
    private final String applicationID;
    private final String endpointID;
    private final long timeoutMillis;
	private MqttV3MessageReceiver messageObserver;
	private MQTTWrapper mqttClient;

    /**
     * @param observationRegistry the registry for keeping track of observed resources
     */
    public MQTTLwM2mRequestSender(String endpointID, String applicationID, 
    		final ObservationRegistry observationRegistry) {
        this(endpointID, applicationID, observationRegistry, REQUEST_TIMEOUT_MILLIS);
    }

    /**
     * @param endpoints the CoAP endpoints to use for sending requests
     * @param observationRegistry the registry for keeping track of observed resources
     * @param timeoutMillis timeout for synchronously sending of CoAP request
     */
    public MQTTLwM2mRequestSender(String endpointID, String applicationID, 
    		final ObservationRegistry observationRegistry,
            final long timeoutMillis) {
        Validate.notNull(endpointID);
        Validate.notNull(applicationID);
        Validate.notNull(observationRegistry);
        this.observationRegistry = observationRegistry;
        this.endpointID = endpointID;
        this.applicationID = applicationID;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public <T extends ClientResponse> T send(final LwM2mRequest<T> request) {
        // Create the CoAP request from LwM2m request
        final MQTTRequestBuilder MQTTRequestBuilder = new MQTTRequestBuilder(this.endpointID,
        		this.applicationID);
        request.accept(MQTTRequestBuilder);
        final Request mqttRequest = MQTTRequestBuilder.getRequest();

        // Send CoAP request synchronously
        final SyncRequestObserver<T> syncMessageObserver = new SyncRequestObserver(mqttRequest, messageObserver, 
                timeoutMillis) {
            @Override
            public T buildResponse(final Response mqttResponse) {
                // Build LwM2m response
                final LwM2mResponseBuilder<T> lwm2mResponseBuilder = new LwM2mResponseBuilder<T>(mqttRequest, mqttResponse,
                        observationRegistry);
                lwm2mResponseBuilder.setMessageObserver(messageObserver);
                request.accept(lwm2mResponseBuilder);
                return lwm2mResponseBuilder.getResponse();
            }
        };
        messageObserver.addRequest(mqttRequest.getMessageID(), syncMessageObserver);
        mqttRequest.setMqttClient(mqttClient);
        mqttClient.publish(mqttRequest.getTopic(), mqttRequest.getMessageAsString());
        // Wait for response, then return it
        return syncMessageObserver.waitForResponse();
    }

    @Override
    public <T extends ClientResponse> void send(final LwM2mRequest<T> request, final ResponseConsumer<T> responseCallback,
            final ExceptionConsumer errorCallback) {
    }

   
    private abstract class SyncRequestObserver<T> extends AbstractRequestObserver {

        protected CountDownLatch latch = new CountDownLatch(1);
        protected AtomicReference<T> ref = new AtomicReference<T>(null);
        protected AtomicBoolean mqttTimeout = new AtomicBoolean(false);
        protected AtomicReference<RuntimeException> exception = new AtomicReference<>();

        protected long timeout;
        protected MqttV3MessageReceiver messageObserver; 

        public SyncRequestObserver(final Request mqttRequest, MqttV3MessageReceiver messageObserver, final long timeout) {
            super(mqttRequest);
            this.timeout = timeout;
            this.messageObserver = messageObserver;
        }
        
        public abstract T buildResponse(Response mqttResponse);

        @Override
        public void onResponse(final Response mqttResponse) {
            LOG.info("Received response: " + mqttResponse);
            try {
                final T lwM2mResponseT = buildResponse(mqttResponse);
                if (lwM2mResponseT != null) {
                    ref.set(lwM2mResponseT);
                }
            } catch (final RuntimeException e) {
                exception.set(e);
            } finally {
                latch.countDown();
            }
        }

        @Override
        public void onError(final Response mqttResponse) {
        	try {
                final T lwM2mResponseT = buildResponse(mqttResponse);
                if (lwM2mResponseT != null) {
                    ref.set(lwM2mResponseT);
                }
            } catch (final RuntimeException e) {
                exception.set(e);
            } finally {
                latch.countDown();
            }
        }

        public T waitForResponse() {
            try {
                final boolean latchTimeout = latch.await(timeout, TimeUnit.MILLISECONDS);
                if (!latchTimeout || mqttTimeout.get()) {
                    if (exception.get() != null) {
                        throw exception.get();
                    } else {
                        throw new RuntimeException("Request Timed Out: " + mqttRequest + " (timeout)");
                    }
                }
            } catch (final InterruptedException e) {
                // no idea why some other thread should have interrupted this thread
                // but anyway, go ahead as if the timeout had been reached
                LOG.info("Caught an unexpected InterruptedException during execution of CoAP request " + e);
            } finally {
            	if(messageObserver.getRequest(mqttRequest.getMessageID()) instanceof SyncRequestObserver)
            		messageObserver.removeRequest(mqttRequest.getMessageID());
            }

            if (exception.get() != null) {
                throw exception.get();
            }
            return ref.get();
        }
    }

	public void setMqttV3MessageReceiver(MqttV3MessageReceiver callback) {
		this.messageObserver = callback;
		
	}

	public void setMqttClient(MQTTWrapper mqttClient) {
		this.mqttClient = mqttClient;
		
	}
}
