package com.ibm.lwm2m.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.lwm2m.objects.FloatResource;
import com.ibm.lwm2m.objects.RaspiDeviceObject;

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
 * This is a simple Raspberry Pi example which reads the CPU temperature and
 * exposes the same to the LwM2M server
 * 
 * This client exposes following Objects and list of command line options
 * to perform one or more operations
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

public class LwM2MRaspiClient {
	private static final Logger LOG = LoggerFactory
			.getLogger(LwM2MRaspiClient.class);
	
	private static final String CPU_TEMP_FILEPATH = "/sys/class/thermal/thermal_zone0/temp";

	private LwM2MClient client = null;

	// read the CPU temp
	private ScheduledExecutorService scheduler = Executors
			.newSingleThreadScheduledExecutor();

	private LwM2MRaspiClient() {
		client = LwM2MClient.getClient();
	}

	public static void main(String args[]) {
		LwM2MRaspiClient respiClient = new LwM2MRaspiClient();
		respiClient.start();
	}


	private void start() {
		client.start();
		startRaspiReading();
		RaspiDeviceObject.createObject();
		RaspiDeviceObject.createObjectInstance();
		client.userAction();
		
	}

	private void startRaspiReading() {

		// update the min and max range
		((FloatResource) client.getResource("3303/0/5603")).setValue(0);
		((FloatResource) client.getResource("3303/0/5604")).setValue(100);

		// start the cpu temp reader thread
		LOG.info("Staring the scheduler for reading the CPU temperature in Raspberry Pi");
		scheduler.scheduleAtFixedRate(new CpuTemperatureReader(), 0, 1,
				TimeUnit.SECONDS);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				LOG.info("Shutting down the scheduler");
				scheduler.shutdown();
			}
		});
	}

	private class CpuTemperatureReader implements Runnable {

		float minimumMeasuredValue = Float.MAX_VALUE;
		float maximumMeasuredValue = Float.MIN_VALUE;

		@Override
		public void run() {

			Scanner scanner = null;

			float value = 0;

			try {
				scanner = new Scanner(new File(CPU_TEMP_FILEPATH));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			value = scanner.nextFloat() / 1000; // Temp is in millis

			// update the resource values
			// 3303/0/5700 - temperature sensor value
			((FloatResource) client.getResource("3303/0/5700")).setValue(value);

			if (value > maximumMeasuredValue) {
				maximumMeasuredValue = value;
				// 3303/0/5602 - Maximum Measured Value
				((FloatResource) client.getResource("3303/0/5602"))
						.setValue(maximumMeasuredValue);
			}

			if (value < minimumMeasuredValue) {
				minimumMeasuredValue = value;
				// 3303/0/5601 - Minimum Measured Value
				((FloatResource) client.getResource("3303/0/5601"))
						.setValue(minimumMeasuredValue);
			}

			scanner.close();
		}
	}

}
