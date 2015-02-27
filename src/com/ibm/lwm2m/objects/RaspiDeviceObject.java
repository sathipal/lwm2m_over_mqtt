package com.ibm.lwm2m.objects;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.ibm.lwm2m.client.LwM2MClient;
import com.ibm.mqttv3.binding.MQTTExchange;
import com.ibm.mqttv3.binding.MQTTResource;
import com.ibm.mqttv3.binding.Resource;
import com.ibm.mqttv3.binding.ResponseCode;

public class RaspiDeviceObject extends MQTTResource {

	public static final String RESOURCE_NAME = "3";

	// Create an object model
	private StringResource manufacturerResource;
	private StringResource modelResource;
	private StringResource serialNumberResource;
	private StringResource firmwareResource;
	private RebootResource rebootResource;
	private ExecResource factoryResetResource;
	private IntegerMultipleResource powerAvailablePowerResource;
	private IntegerMultipleResource powerSourceVoltageResource;
	private IntegerMultipleResource powerSourceCurrentResource;
	private IntegerResource batteryLevelResource;
	private MemoryFreeResource memoryFreeResource;
	private IntegerMultipleResource errorCodeResource;
	private ExecResource resetErrorCodeResource;
	private TimeResource currentTimeResource;
	private StringResource utcOffsetResource;
	private StringResource timezoneResource;
	private StringResource bindingsResource;

	public RaspiDeviceObject(String name, boolean bInstance) {
		super(name);
		/* Create resources only if its a instance */
		if (bInstance == true) {
			manufacturerResource = new StringResource("0", false,
					"Raspberry Pi");
			modelResource = new StringResource("1", false, "Model B");
			serialNumberResource = new StringResource("2", false,
					getSerialNumber());
			firmwareResource = new StringResource("3", false, "1.0.0");
			rebootResource = new RebootResource("4");
			factoryResetResource = new ExecResource("5");
			powerAvailablePowerResource = new IntegerMultipleResource("6",
					false, new int[] { 0, 4 });
			powerSourceVoltageResource = new IntegerMultipleResource("7",
					false, new int[] { 12000, 5000 });
			powerSourceCurrentResource = new IntegerMultipleResource("8",
					false, new int[] { 150, 75 });
			batteryLevelResource = new IntegerResource("9", false, 100);
			memoryFreeResource = new MemoryFreeResource("10", false);
			errorCodeResource = new IntegerMultipleResource("11", false,
					new int[] { 0 });
			resetErrorCodeResource = new ExecResource("12");
			currentTimeResource = new TimeResource("13", true);
			utcOffsetResource = new StringResource("14", true,
					new SimpleDateFormat("X").format(Calendar.getInstance()
							.getTime()));
			timezoneResource = new StringResource("15", true, TimeZone
					.getDefault().getID());
			bindingsResource = new StringResource("16", false, "MQTT");

		}
	}

	private static String getSerialNumber() {
		StringBuffer output = new StringBuffer();
		
		String[] cmdForSerial = {
				"/bin/sh",
				"-c",
				"cat /proc/cpuinfo | grep Serial | awk ' {print $3}'"
				};
		Process p;
		try {
			p = Runtime.getRuntime().exec(cmdForSerial);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));

			String line = "";
			while ((line = reader.readLine()) != null) {
				output.append(line);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return output.toString();

	}

	private class TimeResource extends LongResource {

		public TimeResource(String name, boolean bWrite) {
			super(name, bWrite, new Date().getTime());
		}

		@Override
		public String getValue() {
			return Long.toString(new Date().getTime());
		}
	}

	private class MemoryFreeResource extends IntegerResource {

		public MemoryFreeResource(String name, boolean bWrite) {
			super(name, bWrite);
		}

		@Override
		public String getValue() {
			return Long.toString(Runtime.getRuntime().freeMemory());
		}
	}

	public static RaspiDeviceObject createObject() {
		RaspiDeviceObject to = new RaspiDeviceObject(RESOURCE_NAME, false);
		LwM2MClient.getRootResource().add(to);
		return to;
	}

	public static RaspiDeviceObject createObjectInstance() {
		RaspiDeviceObject to = new RaspiDeviceObject("0", true);

		Resource resource = LwM2MClient.getRootResource().getChild(
				RESOURCE_NAME);
		resource.add(to);
		to.add(to.manufacturerResource);
		to.add(to.modelResource);
		to.add(to.serialNumberResource);
		to.add(to.firmwareResource);
		to.add(to.rebootResource);
		to.add(to.factoryResetResource);
		to.add(to.powerAvailablePowerResource);
		to.add(to.powerSourceCurrentResource);
		to.add(to.powerSourceVoltageResource);
		to.add(to.batteryLevelResource);
		to.add(to.memoryFreeResource);
		to.add(to.errorCodeResource);
		to.add(to.resetErrorCodeResource);
		to.add(to.currentTimeResource);
		to.add(to.utcOffsetResource);
		to.add(to.timezoneResource);
		to.add(to.bindingsResource);
		return to;
	}

	private class RebootResource extends ExecResource {

		public RebootResource(String name) {
			super(name);
		}

		@Override
		public void handlePOST(MQTTExchange exchange) {
			try {

				Runtime r = Runtime.getRuntime();

				exchange.respond(ResponseCode.CHANGED,
						" Reboot successfully executed");
				// To reboot the Raspi
				r.exec("sudo /sbin/shutdown -r now");

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}