package leshan.server.mqtt.impl;

import java.util.List;

import leshan.LinkObject;
import leshan.server.client.BindingMode;
import leshan.server.client.Client;
import leshan.server.client.ClientRegistrationException;
import leshan.server.client.ClientRegistry;
import leshan.server.client.ClientUpdate;
import leshan.util.RandomStringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mqttv3.binding.MQTTExchange;
import com.ibm.mqttv3.binding.MQTTResource;
import com.ibm.mqttv3.binding.Request;
import com.ibm.mqttv3.binding.Resource;
import com.ibm.mqttv3.binding.ResponseCode;

/**
 * A {@link Resource} in charge of handling clients registration requests.
 * <p>
 * This resource is the entry point of the Resource Directory ("/rd"). Each new client is added to the
 * {@link ClientRegistry}.
 * </p>
 */
public class RegisterResource extends MQTTResource {

    private static final String QUERY_PARAM_ENDPOINT = "ep=";

    private static final String QUERY_PARAM_BINDING_MODE = "b=";

    private static final String QUERY_PARAM_LWM2M_VERSION = "lwm2m=";

    private static final String QUERY_PARAM_SMS = "sms=";

    private static final String QUERY_PARAM_LIFETIME = "lt=";

    private static final Logger LOG = LoggerFactory.getLogger(RegisterResource.class);

    public static final String RESOURCE_NAME = "rd";

    private final ClientRegistry clientRegistry;

    public RegisterResource(ClientRegistry clientRegistry) {
        super(RESOURCE_NAME);

        this.clientRegistry = clientRegistry;
    }

    @Override
    public void handlePOST(MQTTExchange exchange) {
        Request request = exchange.getRequest();

        LOG.debug("POST received : {}", request);

        String endpoint = null;
        Long lifetime = null;
        String smsNumber = null;
        String lwVersion = null;
        BindingMode binding = null;
        LinkObject[] objectLinks = null;
        try {

            for (String param : getParameters(request)) {
                if (param.startsWith(QUERY_PARAM_ENDPOINT)) {
                    endpoint = param.substring(3);
                } else if (param.startsWith(QUERY_PARAM_LIFETIME)) {
                    lifetime = Long.valueOf(param.substring(3));
                } else if (param.startsWith(QUERY_PARAM_SMS)) {
                    smsNumber = param.substring(4);
                } else if (param.startsWith(QUERY_PARAM_LWM2M_VERSION)) {
                    lwVersion = param.substring(6);
                } else if (param.startsWith(QUERY_PARAM_BINDING_MODE)) {
                    binding = BindingMode.valueOf(param.substring(2));
                }
            }

            if (endpoint == null || endpoint.isEmpty()) {
                exchange.respond(ResponseCode.BAD_REQUEST, "Client must specify an endpoint identifier");
            } else {
                // register
                String registrationId = RegisterResource.createRegistrationId();
                byte[] objects = getObjects(request);
                if (objects != null) {
                    objectLinks = LinkObject.parse(objects);
                }

                Client client = new Client(registrationId, endpoint, null, 0,
                        lwVersion, lifetime, smsNumber, binding, objectLinks, null);

                client.setApplicationID(request.getRequestorApplicationID());
                client.setOrganizationID(request.getOrganizationID());
                clientRegistry.registerClient(client);
                LOG.debug("New registered client: {}", client);

                exchange.respond(ResponseCode.CREATED, RESOURCE_NAME + "/" + client.getRegistrationId());
            }
        } catch (NumberFormatException e) {
            exchange.respond(ResponseCode.BAD_REQUEST, "Lifetime parameter must be a valid number");
        } catch (ClientRegistrationException e) {
            LOG.debug("Registration failed for client " + endpoint, e);
            exchange.respond(ResponseCode.BAD_REQUEST);
        }
    }

    private byte[] getObjects(Request request) {
    	String content = request.getPayloadText();
    	/*
    	 * Content will have following 2 parameters delimited by space,
    	 * list of registeration parameters delimited by &
    	 * list of objects supported by client 
    	 * 
    	 */
    	String[] parameters = content.split(" ", 2);
    	if(parameters.length >= 2 )
    		return parameters[1].getBytes();
    	else
    		return null;
	}

	private List<String> getParameters(Request request) {
    	String content = request.getPayloadText();
    	/*
    	 * Content will have following 2 parameters delimited by space,
    	 * list of registeration parameters delimited by &
    	 * list of objects supported by client 
    	 * 
    	 */
    	String[] parameters = content.split(" ", 2);
    	return Request.getParameters(parameters[0]);
	}

	/**
     * Updates an existing Client registration.
     *
     * @param exchange the CoAP request containing the updated regsitration properties
     */
    @Override
    public void handlePUT(MQTTExchange exchange) {
        Request request = exchange.getRequest();

        LOG.debug("UPDATE received : {}", request);
        
        List<String> uri = request.getURIPaths();
        if (uri == null || uri.size() != 2 || !RESOURCE_NAME.equals(uri.get(0))) {
            exchange.respond(ResponseCode.NOT_FOUND);
            return;
        }

        String registrationId = uri.get(1);

        Long lifetime = null;
        String smsNumber = null;
        BindingMode binding = null;
        LinkObject[] objectLinks = null;

        for (String param : getParameters(request)) {
            if (param.startsWith(QUERY_PARAM_LIFETIME)) {
                lifetime = Long.valueOf(param.substring(3));
            } else if (param.startsWith(QUERY_PARAM_SMS)) {
                smsNumber = param.substring(4);
            } else if (param.startsWith(QUERY_PARAM_BINDING_MODE)) {
                binding = BindingMode.valueOf(param.substring(2));
            }
        }

        byte[] objects = getObjects(request);
        if (objects != null) {
            objectLinks = LinkObject.parse(objects);
        }
       
        ClientUpdate client = new ClientUpdate(registrationId, null, 0, lifetime,
                smsNumber, binding, objectLinks);

        try {
            Client c = clientRegistry.updateClient(client);
            if (c == null) {
                exchange.respond(ResponseCode.NOT_FOUND);
            } else {
                exchange.respond(ResponseCode.CHANGED);
            }
        } catch (ClientRegistrationException e) {
            LOG.debug("Registration update failed: " + client, e);
            exchange.respond(ResponseCode.BAD_REQUEST);
        }

    }

    @Override
    public void handleDELETE(MQTTExchange exchange) {
        LOG.debug("DELETE received : {}", exchange.getRequest());

        Client unregistered = null;
        List<String> uri = exchange.getRequest().getURIPaths();

        try {
            if (uri != null && uri.size() == 2 && RESOURCE_NAME.equals(uri.get(0))) {
                unregistered = clientRegistry.deregisterClient(uri.get(1));
            }

            if (unregistered != null) {
                exchange.respond(ResponseCode.DELETED);
            } else {
                LOG.debug("Invalid deregistration");
                exchange.respond(ResponseCode.NOT_FOUND);
            }

        } catch (ClientRegistrationException e) {
            LOG.debug("Deregistration failed", e);
            exchange.respond(ResponseCode.BAD_REQUEST);
        }
    }

    /*
     * Override the default behavior so that requests to sub resources (typically /rd/{client-reg-id}) are handled by
     * /rd resource.
     */
    @Override
    public Resource getChild(String name) {
        return this;
    }

    private static String createRegistrationId() {
        return RandomStringUtils.random(10, true, true);
    }

}
