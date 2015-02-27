package leshan.server.mqtt.impl;

import com.ibm.mqttv3.binding.MQTT.GET;
import com.ibm.mqttv3.binding.MQTT.PUT;
import com.ibm.mqttv3.binding.Request;

import leshan.core.node.LwM2mPath;
import leshan.core.node.codec.LwM2mNodeEncoder;
import leshan.server.client.Client;
import leshan.server.request.CreateRequest;
import leshan.server.request.DeleteRequest;
import leshan.server.request.DiscoverRequest;
import leshan.server.request.ExecuteRequest;
import leshan.server.request.LwM2mRequestVisitor;
import leshan.server.request.ObserveRequest;
import leshan.server.request.ReadRequest;
import leshan.server.request.WriteAttributesRequest;
import leshan.server.request.WriteRequest;
import leshan.util.StringUtils;

public class MQTTRequestBuilder implements LwM2mRequestVisitor {

    private Request mqttRequest;
    private final String serverEndpointId;
    private final String serverApplicationId;


    public MQTTRequestBuilder(String serverEndpointId, 
							  String serverApplicationId) {
    	this.serverEndpointId = serverEndpointId;
    	this.serverApplicationId = serverApplicationId;
    }
    
    @Override
    public void visit(ReadRequest request) {
        mqttRequest = Request.newGet();
        setTarget(mqttRequest, request.getClient(), request.getPath());
        mqttRequest.setPayloadContent(GET.READ.toString());
    }

    // 0 – Represents attributes of the object/resource - DiscoverRequest, 
    // 1 – represents Read Request 
    // 2 – represents Observe request
    @Override
    public void visit(DiscoverRequest request) {
        mqttRequest = Request.newGet();
        setTarget(mqttRequest, request.getClient(), request.getPath());
        mqttRequest.setPayloadContent(GET.DISCOVER.toString());
    }

    private void setRequestTopic() {
    	mqttRequest.setRequestorApplicationID(this.serverApplicationId);
    	mqttRequest.setRequestorEndpointID(this.serverEndpointId);
	}

	@Override
    public void visit(WriteRequest request) {
        mqttRequest = request.isReplaceRequest() ? Request.newPut() : Request.newPost();
        //Content write - 1
        mqttRequest.setPayloadContent(PUT.WRITE.toString());
        mqttRequest.addPayloadContent(LwM2mNodeEncoder.encode(request.getNode(), 
        		request.getContentFormat(), request.getPath()));
        setTarget(mqttRequest, request.getClient(), request.getPath());
    }

    @Override
    public void visit(WriteAttributesRequest request) {
        mqttRequest = Request.newPut();
        setTarget(mqttRequest, request.getClient(), request.getPath());

        StringBuilder payload = new StringBuilder();
        // write attributes - 0
        payload.append(PUT.ATTRIBUTES);
        payload.append(" ");
        for (String query : request.getObserveSpec().toQueryParams()) {
        	payload.append(query);
        	payload.append("&");
        }
        payload.deleteCharAt(payload.length() - 1);
        mqttRequest.setPayloadContent(payload.toString());
    }

    @Override
    public void visit(ExecuteRequest request) {
        mqttRequest = Request.newPost();
        setTarget(mqttRequest, request.getClient(), request.getPath());
        mqttRequest.setPayloadContent(request.getParameters());
    }

    @Override
    public void visit(CreateRequest request) {
        mqttRequest = Request.newPost();
        mqttRequest.setPayloadContent(LwM2mNodeEncoder.encode(request.getObjectInstance(), request.getContentFormat(),
                request.getPath()));
        setTarget(mqttRequest, request.getClient(), request.getPath());
    }

    @Override
    public void visit(DeleteRequest request) {
        mqttRequest = Request.newDelete();
        setTarget(mqttRequest, request.getClient(), request.getPath());
    }

    @Override
    public void visit(ObserveRequest request) {
        mqttRequest = Request.newGet();
        mqttRequest.setPayloadContent(GET.OBSERVE.toString());
        //mqttRequest.setObserve();
        setTarget(mqttRequest, request.getClient(), request.getPath());
    }

    private final void setTarget(Request mqttRequest, Client client, LwM2mPath path) {
    	setRequestTopic();
        mqttRequest.setApplicationID(client.getApplicationID());
        mqttRequest.setEndPointId(client.getEndpoint());
        mqttRequest.setOrganizationID(client.getOrganizationID());

        // root path
        if (client.getRootPath() != null) {
            for (String rootPath : client.getRootPath().split("/")) {
                if (!StringUtils.isEmpty(rootPath)) {
                    mqttRequest.addURIPath(rootPath);
                }
            }
        }

        // objectId
        mqttRequest.addURIPath(Integer.toString(path.getObjectId()));

        // objectInstanceId
        if (path.getObjectInstanceId() == null) {
            if (path.getResourceId() != null) {
                mqttRequest.addURIPath("0"); // default instanceId
            }
        } else {
            mqttRequest.addURIPath(Integer.toString(path.getObjectInstanceId()));
        }

        // resourceId
        if (path.getResourceId() != null) {
            mqttRequest.addURIPath(Integer.toString(path.getResourceId()));
        }
    }

    public Request getRequest() {
        return mqttRequest;
    };
}
