package leshan.server.mqtt.impl;

import leshan.ResponseCode;
import leshan.core.node.LwM2mNode;
import leshan.core.node.LwM2mPath;
import leshan.core.node.codec.InvalidValueException;
import leshan.core.node.codec.LwM2mNodeDecoder;
import leshan.core.request.ContentFormat;
import leshan.core.response.ClientResponse;
import leshan.core.response.CreateResponse;
import leshan.core.response.DiscoverResponse;
import leshan.core.response.ValueResponse;
import leshan.server.client.Client;
import leshan.server.observation.ObservationRegistry;
import leshan.server.request.CreateRequest;
import leshan.server.request.DeleteRequest;
import leshan.server.request.DiscoverRequest;
import leshan.server.request.ExecuteRequest;
import leshan.server.request.LwM2mRequestVisitor;
import leshan.server.request.ObserveRequest;
import leshan.server.request.ReadRequest;
import leshan.server.request.ResourceAccessException;
import leshan.server.request.WriteAttributesRequest;
import leshan.server.request.WriteRequest;
import leshan.util.Validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mqttv3.binding.MqttV3MessageReceiver;
import com.ibm.mqttv3.binding.Request;
import com.ibm.mqttv3.binding.Response;

public class LwM2mResponseBuilder<T extends ClientResponse> implements LwM2mRequestVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mResponseBuilder.class);

    private ClientResponse lwM2mresponse;
    private final Request mqttRequest;
    private final Response mqttResponse;
    private final ObservationRegistry observationRegistry;

	private MqttV3MessageReceiver messageObserver;

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

    public LwM2mResponseBuilder(final Request mqttRequest, final Response mqttResponse, final ObservationRegistry observationRegistry) {
        super();
        this.mqttRequest = mqttRequest;
        this.mqttResponse = mqttResponse;
        this.observationRegistry = observationRegistry;
    }

    @Override
    public void visit(final ReadRequest request) {
        switch (fromMqttCode(mqttResponse.getCode())) {
        case CONTENT:
            lwM2mresponse = buildContentResponse(request.getPath(), mqttResponse);
            break;
        case UNAUTHORIZED:
        case NOT_FOUND:
        case METHOD_NOT_ALLOWED:
            lwM2mresponse = new ValueResponse(fromMqttCode(mqttResponse.getCode()));
            break;
        default:
            handleUnexpectedResponseCode(request.getClient(), mqttRequest, mqttResponse);
        }
    }

    @Override
    public void visit(final DiscoverRequest request) {
        switch (fromMqttCode(mqttResponse.getCode())) {
        case CONTENT:
           /* LinkObject[] links = null;
            if (MediaTypeRegistry.APPLICATION_LINK_FORMAT != mqttResponse.getOptions().getContentFormat()) {
                LOG.debug("Expected LWM2M Client [{}] to return application/link-format [{}] content but got [{}]",
                        request.getClient().getEndpoint(), MediaTypeRegistry.APPLICATION_LINK_FORMAT, mqttResponse
                                .getOptions().getContentFormat());
                links = new LinkObject[] {}; // empty list
            } else {
                links = LinkObject.parse(mqttResponse.getPayload());
            }
            lwM2mresponse = new DiscoverResponse(fromMqttCode(mqttResponse.getCode().value), links);*/
            break;
        case NOT_FOUND:
        case UNAUTHORIZED:
        case METHOD_NOT_ALLOWED:
            lwM2mresponse = new DiscoverResponse(fromMqttCode(mqttResponse.getCode()));
            break;
        default:
            handleUnexpectedResponseCode(request.getClient(), mqttRequest, mqttResponse);
        }
    }

    @Override
    public void visit(final WriteRequest request) {
        switch (fromMqttCode(mqttResponse.getCode())) {
        case CHANGED:
            lwM2mresponse = new ClientResponse(fromMqttCode(mqttResponse.getCode()));
            break;
        case BAD_REQUEST:
        case NOT_FOUND:
        case UNAUTHORIZED:
        case METHOD_NOT_ALLOWED:
            lwM2mresponse = new ClientResponse(fromMqttCode(mqttResponse.getCode()));
            break;
        default:
            handleUnexpectedResponseCode(request.getClient(), mqttRequest, mqttResponse);
        }
    }

    @Override
    public void visit(final WriteAttributesRequest request) {
        switch (fromMqttCode(mqttResponse.getCode())) {
        case CHANGED:
            lwM2mresponse = new ClientResponse(fromMqttCode(mqttResponse.getCode()));
            break;
        case BAD_REQUEST:
        case NOT_FOUND:
        case UNAUTHORIZED:
        case METHOD_NOT_ALLOWED:
            lwM2mresponse = new ClientResponse(fromMqttCode(mqttResponse.getCode()));
            break;
        default:
            handleUnexpectedResponseCode(request.getClient(), mqttRequest, mqttResponse);
        }
    }

    @Override
    public void visit(final ExecuteRequest request) {
        switch (fromMqttCode(mqttResponse.getCode())) {
        case CHANGED:
            lwM2mresponse = new ClientResponse(fromMqttCode(mqttResponse.getCode()));
            break;
        case BAD_REQUEST:
        case UNAUTHORIZED:
        case NOT_FOUND:
        case METHOD_NOT_ALLOWED:
            lwM2mresponse = new ClientResponse(fromMqttCode(mqttResponse.getCode()));
            break;
        default:
            handleUnexpectedResponseCode(request.getClient(), mqttRequest, mqttResponse);
        }

    }

    @Override
    public void visit(final CreateRequest request) {
        switch (fromMqttCode(mqttResponse.getCode())) {
        case CREATED:
            /*lwM2mresponse = new CreateResponse(fromMqttCode(mqttResponse.getCode()), mqttResponse.
                    .getLocationPathString());*/
            break;
        case BAD_REQUEST:
        case UNAUTHORIZED:
        case NOT_FOUND:
        case METHOD_NOT_ALLOWED:
            lwM2mresponse = new CreateResponse(fromMqttCode(mqttResponse.getCode()));
            break;
        default:
            handleUnexpectedResponseCode(request.getClient(), mqttRequest, mqttResponse);
        }
    }

    @Override
    public void visit(final DeleteRequest request) {
        switch (fromMqttCode(mqttResponse.getCode())) {
        case DELETED:
            lwM2mresponse = new ClientResponse(fromMqttCode(mqttResponse.getCode()));
            break;
        case UNAUTHORIZED:
        case NOT_FOUND:
        case METHOD_NOT_ALLOWED:
            lwM2mresponse = new ClientResponse(fromMqttCode(mqttResponse.getCode()));
            break;
        default:
            handleUnexpectedResponseCode(request.getClient(), mqttRequest, mqttResponse);
        }
    }

    @Override
    public void visit(final ObserveRequest request) {
        switch (fromMqttCode(mqttResponse.getCode())) {
        case CHANGED:
            // ignore changed response (this is probably a NOTIFY)
            lwM2mresponse = null;
            break;
        case CONTENT:
            lwM2mresponse = buildContentResponse(request.getPath(), mqttResponse);
            // observe request succeed so we can add and observation to registry
            final MQTTObservation observation = new MQTTObservation(mqttRequest, request.getClient(),
                        request.getPath());
            messageObserver.addRequest(mqttRequest.getMessageID(), observation);
            observationRegistry.addObservation(observation);
            break;
        case NOT_FOUND:
        case METHOD_NOT_ALLOWED:
            lwM2mresponse = new ValueResponse(fromMqttCode(mqttResponse.getCode()));
            break;
        default:
            handleUnexpectedResponseCode(request.getClient(), mqttRequest, mqttResponse);
        }
    }

    private ValueResponse buildContentResponse(final LwM2mPath path, final Response mqttResponse) {
        final ResponseCode code = ResponseCode.CONTENT;
        LwM2mNode content;
        try {
            content = LwM2mNodeDecoder.decode(mqttResponse.getPayload(),
                    ContentFormat.fromCode(1541), path);
        } catch (final InvalidValueException e) {
            final String msg = String.format("[%s] ([%s])", e.getMessage(), e.getPath().toString());
            throw new ResourceAccessException(code, path.toString(), msg, e);
        }
        return new ValueResponse(code, content);
    }

    @SuppressWarnings("unchecked")
    public T getResponse() {
        return (T) lwM2mresponse;
    }

    /**
     * Throws a generic {@link ResourceAccessException} indicating that the client returned an unexpected response code.
     *
     * @param request
     * @param mqttRequest
     * @param mqttResponse
     */
    private void handleUnexpectedResponseCode(final Client client, final Request mqttRequest, final Response mqttResponse) {
        final String msg = String.format("Client [%s] returned unexpected response code [%s]", client.getEndpoint(),
                mqttResponse.getCode());
        throw new ResourceAccessException(fromMqttCode(mqttResponse.getCode()), mqttRequest.getURIPaths().toString(), msg);
    }

	public void setMessageObserver(MqttV3MessageReceiver messageObserver) {
		this.messageObserver = messageObserver;
		
	}
}
