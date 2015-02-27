package com.ibm.lwm2m.client;

import com.ibm.lwm2m.objects.LwM2MExampleDeviceObject;

/*
 * OMA Lightweight M2M is a protocol for device and service management. 
 * The main purpose of this technology is to address service and management 
 * needs for constrained M2M devices, over a number of transports and bearers. 
 * The current stack for LWM2M relies on CoAP as the protocol.
 * 
 * Our solution involves development of an LWM2M server prototype, as well as, 
 * a client prototype, which make use of MQTT as the underlying M2M protocol. 
 * Thus LWM2M can be used for both CoAP, as well as, MQTT.
 * 
 * This is a simple example client which following Objects and provides
 * list of command line options to perform one or more operations
 *  
 *  >> IPSO temperature Object - <3303/0>
 *  >> OMA LWM2M Server Object - /1/0
 *  >> OMA LWM2M Device Object - /3/0
 *  
 *  List of available commands
 *  
 *  register :: Register this client to server
 *  deregister :: deregister this client from the server
 *  update-register :: updates the registeration
 *  update :: (update <resource-id> <value>) update a local resource value
 *  get :: (get <resource-id>) get a local resource value
 *
 *  >> IPSO temperature Object - <3303/0>
 *    >> 3303/0/5700 - SensorValue
 *    >> 3303/0/5601 - Minimum Measured Value
 *    >> 3303/0/5602 - Maximum Measured Value
 *    >> 3303/0/5603 - Min Range Value
 *    >> 3303/0/5604 - Max Range Value
 */
public class LwM2MExampleClient {

	private LwM2MClient client = null;

	private LwM2MExampleClient() {
		client = LwM2MClient.getClient();
	}

	public static void main(String args[]) {
		LwM2MExampleClient respiClient = new LwM2MExampleClient();
		respiClient.start();
	}


	private void start() {
		client.start();
		// Create the device object
		LwM2MExampleDeviceObject.createObject();
		LwM2MExampleDeviceObject.createObjectInstance();
		client.userAction();
	}
}
